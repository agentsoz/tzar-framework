package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.hash.Hashing;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class GitRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(GitRepository.class.getName());

  private Git git;

  private Path tmpdir;
  
  GitRepository(URI sourceUri) {
    super(sourceUri);
    try {
        git = clone(sourceUri.toString());
    } catch(Exception e) {
      LOG.severe("Could not clone repository " + sourceUri.toString() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public File retrieveModel(String revision, String name, File modelPath) throws TzarException {
    final String branchName = "rev-" + revision;
    if (git == null) {
      throw new TzarException("Could not clone repository " + sourceUri.toString());
    }
    File mpath;
    // 1. Delete the locally checked out model if it already exists
    if (modelPath.exists()) {
      deleteDir(modelPath);
    }
    // 2. Locally clone the the git repository into the model directory
    try {
      LOG.info("Attempting to locally clone Git repository to " + modelPath + " now from "+ tmpdir);
      Git.cloneRepository()
              .setURI(tmpdir.toString())
              .setDirectory(modelPath)
              .call();
    } catch (Exception e) {
      throw new TzarException("Could not clone Git repository: " + e.getMessage());
    }
    // 3. Checkout the master/HEAD
    try {
      LOG.info("Moving the repository to master/HEAD");
      Git.open(modelPath)
              .checkout()
              .setName("master")
              .setStartPoint("HEAD") // checkout the revision in the branch
              .call();
    } catch (Exception e) {
      throw new TzarException("Could not checkout git repository master/HEAD: " + e.getMessage());
    }
    // 4. Force delete the branch of same name as the revision
    try {
      LOG.info("Force delete branch " + branchName);
      Git.open(modelPath)
              .branchDelete()
              .setBranchNames(branchName)
              .setForce(true)
              .call();

    } catch (Exception e) {
      throw new TzarException("Could not force delete git branch "+ branchName + ": " + e.getMessage());
    }
    // 5. Now checkout a new branch of same name as the revision
    try {
      LOG.info("Checking out repository version " + revision + " on new branch " + branchName);
      Git.open(modelPath)
              .checkout()
              .setCreateBranch(true).setName(branchName) // create a branch with the same name as revision
              .setStartPoint(revision) // checkout the revision in the branch
              .call();
    } catch (Exception e) {
      throw new TzarException("Could not checkout git version " + revision + " on branch " + branchName + ": " + e.getMessage());
    }
    // 6. Clean all files
    try {
      LOG.info("Cleaning repository version " + revision + " on new branch " + branchName);
      Git.open(modelPath)
              .clean()
              .setCleanDirectories(true) // also clean directories, not just files
              .setIgnore(false) // clean also ignored files
              .call();
    } catch (Exception e) {
      throw new TzarException("Could not clean git version " + revision + " on branch " + branchName + ": " + e.getMessage());
    }

    LOG.info("Done. Model at " + modelPath + " is a clean working copy now");
    mpath = modelPath; // = createModelPath(name, modelPath, sourceUri);
    return mpath;
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision, File destPath)
      throws TzarException {
    if (git == null) {
      throw new TzarException("Could not clone repository " + sourceUri.toString());
    }
    return new File(tmpdir.toString(), projectParamFilename);
  }

  @Override
  public String getHeadRevision() throws TzarException {
    if (git == null) {
      throw new TzarException("Could not clone repository " + sourceUri.toString());
    }
    Repository repository = git.getRepository();
    String rev;
    try {
      ObjectId head = repository.resolve("HEAD");
      rev = head.name();
    } catch (Exception e) {
      throw new TzarException("No git repository at: " + sourceUri);
    }
    return rev;
  }

  /**
   * Clones the given Git repository.
   * The repository is cloned into a tmp direcotry using the URI hash as directory name. Cloning is
   * bypassed if the directory already exists, in which case we just open a handle to the repository.
   * The benefit is that remote repositories are therefore downloaded and cloned only once.
   *
   * @param uri the URI for the repository
   * @return
   * @throws TzarException
   */
  private Git clone(String uri) throws TzarException {
    Git repo = null;
    if (tmpdir == null) {
        tmpdir = Paths.get(System.getProperty("java.io.tmpdir"), Hashing.crc32().hashString(sourceUri.toString(), StandardCharsets.UTF_8).toString());
        try {
          if (!tmpdir.toFile().exists()) {
            LOG.info("Attempting to clone Git repository " + uri + " to " + tmpdir + " now");
            repo = Git.cloneRepository()
                    .setURI(uri)
                    .setDirectory(tmpdir.toFile())
                    .call();
          } else {
            LOG.info("Git repository clone already exists in " + tmpdir + " so will pull changes to it now");
            Git.open(tmpdir.toFile())
                    .pull()
                    .call();
            repo = Git.open(tmpdir.toFile());
          }
        } catch (Exception e) {
          throw new TzarException("Could not clone Git repository: " + e.getMessage());
        }
    }
    return repo;
  }

  /**
   * Recursively deletes the contents of the given directory
   * @param file
   */
  private void deleteDir(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
      for (File f : contents) {
        deleteDir(f);
      }
    }
    file.delete();
  }
}
