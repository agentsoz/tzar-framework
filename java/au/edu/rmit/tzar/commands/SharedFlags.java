package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.db.Utils;
import au.edu.rmit.tzar.repository.CodeSource;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line commandFlags which are common for all commands.
 */
@com.beust.jcommander.Parameters(separators = "= ")
class SharedFlags {
  private SharedFlags() {
  }

  public static final CreateRunsFlags CREATE_RUNS_FLAGS = new CreateRunsFlags();
  public static final DbFlags DB_FLAGS = new DbFlags();
  public static final LoadRunsFlags LOAD_RUNS_FLAGS = new LoadRunsFlags();
  public static final RunnerFlags RUNNER_FLAGS = new RunnerFlags();
  public static final PrintTableFlags PRINT_TABLE_FLAGS = new PrintTableFlags();
  public static final CommonFlags COMMON_FLAGS = new CommonFlags();

  @Parameters(separators = "= ")
  public static class RunnerFlags {
    private RunnerFlags() {
    }

    @Parameter(names = "--tzarbasedir", description = "Base directory to store tzar files, including temporary " +
        "output files, and downloaded source code.",
        converter = FileConverter.class)
    private File tzarBaseDirectory = Constants.DEFAULT_TZAR_BASE_DIR;

    public File getBaseModelPath() {
      return new File(tzarBaseDirectory, Constants.DEFAULT_MODEL_CODE_DIR);
    }

    public File getTzarBaseDirectory() {
      return tzarBaseDirectory;
    }
  }

  @Parameters(separators = "= ")
  public static class DbFlags {
    private DbFlags() {
    }

    @com.beust.jcommander.Parameter(names = "--dburl", description = "The jdbc access URL for the database.")
    private String dbUrl = System.getenv(Constants.DB_ENVIRONMENT_VARIABLE_NAME);

    public String getDbUrl() {
      return dbUrl;
    }
  }

  @Parameters(separators = "= ")
  public static class CreateRunsFlags {
    private CreateRunsFlags() {
    }

    @Parameter(names = {"-n", "--numruns"}, description = "Number of runs to schedule")
    private int numRuns = 1;

    @Parameter(names = "--projectpath", description = "URI path to the repository where the project spec and model " +
        "code can be found. The structure of this value will depend on the type set in --repotype. If --repotype is " +
        "\"local_file\", then this should be a local file path to the folder containing projectparams.yaml and the " +
        "model code. If it's \"svn\" for example, then it should be a URL pointing to a directory in a subversion " +
        "repository that contains the projectparams.yaml and the model code.", required=true)
    private String projectPath = null;

    @Parameter(names = "--revision", description = "The source control revision of the model code to schedule for " +
        "execution. Must be either an appropriate value for the repository, or 'head' to use the latest version (at " +
        "time of running this command.")
    private String revision = "head";


    @Parameter(names = "--repotype", description = "The type of repository that the project spec and model code " +
        "should be retrieved from. Currently accepted values are: 'LOCAL_FILE', 'SVN'. If not provided, Tzar will " +
        "attempt to guess based on the projectpath flag.")
    private CodeSource.RepositoryType repositoryType;

    @Parameter(names = "--runset", description = "Name of runset to schedule.")
    private String runset = Constants.DEFAULT_RUNSET;

    public int getNumRuns() {
      return numRuns;
    }

    public String getRunset() {
      return runset;
    }

    public String getRevision() {
      return revision;
    }

    public URI getProjectUri() {
      try {
        URI uri = new URI(projectPath);
        if (uri.getScheme() == null) { // no scheme (eg http, ftp). assuming it's a file path
          String absolutePath = new File(projectPath).getAbsolutePath();
          return new URI("file", uri.getHost(), absolutePath, uri.getFragment());
        } else {
          return uri;
        }
      } catch (URISyntaxException e) {
        throw new ParseException("Couldn't parse project path: " + projectPath + ". Must be a valid URI, or a file " +
            "path. Error: " + e.getMessage());
      }
    }

    public CodeSource.RepositoryType getRepositoryType() {
      if (repositoryType == null) {
        String scheme = getProjectUri().getScheme();
        if ("http".equals(scheme)) {
          return CodeSource.RepositoryType.SVN;
        } else if ("file".equals(scheme) || scheme == null) {
          return CodeSource.RepositoryType.LOCAL_FILE;
        } else {
          throw new ParseException("No repository type given, and couldn't guess type based on " +
              "provided projectpath. Try specifying the repotype explicitly.");
        }
      }
      return repositoryType;
    }
  }

  /**
   * Flags for command which load a set of runs matching some criteria.
   */
  @Parameters(separators = "= ")
  public static class LoadRunsFlags {
    @Parameter(names = "--states", description = "Run states to filter by.")
    private List<String> states = new ArrayList<String>();

    @Parameter(names = "--hostname", description = "Host name to filter by.")
    private String hostName = null;

    @Parameter(names = "--runset", description = "Runset name to filter by.")
    private String runset = null;

    @Parameter(names = "--runids", description = "List of run ids.")
    private List<Integer> runIds = null;

    public List<String> getStates() {
      return states;
    }

    public String getHostName() {
      return hostName;
    }

    public String getRunset() {
      return runset;
    }

    public List<Integer> getRunIds() {
      return runIds;
    }
  }

  /**
   * Flags for commands which print tables.
   */
  @Parameters(separators = "= ")
  public static class PrintTableFlags {
    private PrintTableFlags() {
    }

    @Parameter(names = "--csv", description = "Set if output should be CSV format.")
    private boolean outputType;

    @Parameter(names = "--notruncate", description = "Set if output fields should be arbitrarily long.")
    private boolean noTruncateOutput = false;

    public boolean isTruncateOutput() {
      return !noTruncateOutput;
    }

    public Utils.OutputType getOutputType() {
      return outputType ? Utils.OutputType.CSV : Utils.OutputType.PRETTY;
    }
  }

  public static class CommonFlags {
    private CommonFlags() {
    }

    @Parameter(names = {"-v", "--verbose"}, description = "Verbose logging to console.")
    private boolean verbose = false;

    @Parameter(names = {"-q", "--quiet"}, description = "Quiet logging to console.")
    private boolean quiet = false;

    @Parameter(names = {"-h", "--help"}, description = "Show help info.")
    private boolean help = false;

    @Parameter(names = {"--version"}, description = "Show version info.")
    private boolean version = false;

    public LogLevel getLogLevel() throws ParseException {
      if (verbose && quiet) {
        throw new ParseException("Can not specify both --verbose and --quiet.");
      } else if (verbose) {
        return LogLevel.VERBOSE;
      } else if (quiet) {
        return LogLevel.QUIET;
      } else {
        return LogLevel.NORMAL;
      }
    }

    public boolean isHelp() {
      return help;
    }

    public boolean isVersion() {
      return version;
    }

    public enum LogLevel {
      QUIET,
      NORMAL,
      VERBOSE
    }
  }
}
