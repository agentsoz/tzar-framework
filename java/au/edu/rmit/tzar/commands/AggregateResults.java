package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.resultscopier.SshClientFactoryKeyAuth;
import au.edu.rmit.tzar.resultscopier.SshClientFactoryPasswordAuth;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.LOAD_RUNS_FLAGS;

/**
 * Copy a set of results to a single location for analysis, renaming and flattening
 * the files for ease of analysis and to avoid clashes.
 */
class AggregateResults implements Command {
  public static final Object[] FLAGS = new Object[]{CommandFlags.AGGREGATE_RESULTS_FLAGS, DB_FLAGS, LOAD_RUNS_FLAGS};
  private static final Logger LOG = Logger.getLogger(AggregateResults.class.getName());

  private final List<Integer> runIds;
  private final RunDao runDao;
  private final Optional<String> runset;
  private final File destPath;
  private final List<String> states;
  /**
   * Host name (of machine on which run was executed) to filter results by
   */
  private final Optional<String> filterHostname;
  /**
   * Hostname of this machine
   */
  private final String hostname;
  /**
   * Collection of SSH connections, keyed by server name to avoid duplicating connections
   * or having to reconnect each time.
   */
  private final LoadingCache<String, SSHClient> connections;
  private final Utils.RegexFilter regexFilter;
  private final boolean skipExistingRuns;
  private final int startRunId;

  /**
   * Constructor.
   *
   * @param runIds         list of ids of runs to query. Empty to not filter by id.
   * @param states         list of states of runs to query. Empty to default to 'copied'.
   * @param filterHostname host name (of machine on which run was executed) to filter results by
   * @param runset         name of runset to filter by, or not set to not filter by runset
   * @param runDao         for accessing the database of runs
   * @param hostname       host name of this machine (used to determine if ssh is required to copy files)
   * @param flags          command line parameters for this command
   */
  public AggregateResults(List<Integer> runIds, List<String> states, Optional<String> filterHostname,
      Optional<String> runset, RunDao runDao, String hostname, final CommandFlags.AggregateResultsFlags flags) {
    this.runIds = runIds;

    if (states.isEmpty()) {
      this.states = Lists.newArrayList("copied");
    } else {
      this.states = states;
    }
    this.filterHostname = filterHostname;
    this.runset = runset;
    this.destPath = flags.getOutputPath();
    this.skipExistingRuns = flags.isSkipExistingRuns();
    this.startRunId = flags.getStartRunId();
    this.runDao = runDao;
    this.hostname = hostname;

    // We use a cache builder so that we can memoize and reuse ssh connections, as they're expensive to create.
    connections = CacheBuilder.newBuilder().build(new CacheLoader<String, SSHClient>() {
      @Override
      public SSHClient load(String sourceHost) {
        try {
          if (flags.isPasswordPrompt()) {
            String password = new String(System.console().readPassword("Enter the ssh password for the machine %s: ",
                sourceHost));
            return new SshClientFactoryPasswordAuth(sourceHost, flags.getScpUserName(), password).createSSHClient();
          } else {
            return new SshClientFactoryKeyAuth(sourceHost, flags.getScpUserName(),
                flags.getPemFile()).createSSHClient();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
    regexFilter = Utils.RegexFilter.of(flags.getFilenameFilter());
  }

  @Override
  public boolean execute() throws TzarException {
    List<Run> runs = runDao.getRuns(states, filterHostname, runset, runIds);
    Set<Integer> previouslyCopiedRuns = getPreviouslyCopiedRuns();

    try {
      for (Run run : runs) {
        // check if we already have files with that run_id, and if so, skip this run.
        if (skipExistingRuns && previouslyCopiedRuns.contains(run.getRunId())) {
          LOG.info(String.format("Results found locally for run: %d. Skipping copying this run.", run.getRunId()));
          continue;
        }

        if (run.getRunId() < startRunId) {
          LOG.info(String.format("Run %d less that start run id. Skipping copying this run.", run.getRunId()));
          continue;
        }

        LOG.info("Copying results for run: " + run);
        String sourceHost = run.getOutputHost();
        File remoteOutputPath = run.getRemoteOutputPath();
        if (hostname.equals(sourceHost)) {
          LOG.fine("Results are on localhost. Using file copy to copy results.");
          Utils.copyDirectory(remoteOutputPath, destPath, new RunIdRenamer(run.getRunId()), regexFilter);
        } else {
          LOG.fine("Results are on machine: " + sourceHost + ". Using ssh to copy results.");

          SCPFileTransfer scpFileTransfer;
          try {
            scpFileTransfer = connections.get(sourceHost).newSCPFileTransfer();
          } catch (ExecutionException e) {
            throw new TzarException(e.getCause());
          }
          File tempPath = Files.createTempDir();
          scpFileTransfer.download(remoteOutputPath.getPath(), tempPath.getPath());
          File localPath = new File(tempPath, remoteOutputPath.getName());
          Utils.copyDirectory(localPath, destPath, new RunIdRenamer(run.getRunId()), regexFilter);
        }
      }
    } catch (IOException e) {
      throw new TzarException(e);
    } finally {
      for (SSHClient connection : connections.asMap().values()) {
        try {
          connection.disconnect();
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Error closing SSH connection.", e);
        }
      }
    }
    return true;
  }

  /**
   * Returns Set containing runs that have already been copied (by the heuristic that there exist files in
   * the output directory starting with "<runid>_".
   * @return
   */
  private Set<Integer> getPreviouslyCopiedRuns() {
    File[] files = destPath.listFiles();
    if (files == null) { return Sets.newHashSet(); }

    Set<Integer> copiedRuns = new HashSet<Integer>(files.length);
    Pattern pattern = Pattern.compile("(\\d+)_.*");
    for (File file : files) {
      Matcher matcher = pattern.matcher(file.getName());
      if (matcher.matches()) {
        copiedRuns.add(Integer.parseInt(matcher.group(1)));
      }
    }
    return copiedRuns;
  }

  private class RunIdRenamer implements Utils.RenamingStrategy {
    private final int runId;

    public RunIdRenamer(int runId) {
      this.runId = runId;
    }

    public File rename(File file) {
      Joiner j = Joiner.on('_');
      return new File(String.format("%d_%s", runId, j.join(file.getPath().split(File.separator))));
    }
  }
}
