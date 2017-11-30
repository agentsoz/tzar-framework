package au.edu.rmit.tzar.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import au.edu.rmit.tzar.api.TzarException;

public class GitRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(GitRepository.class.getName());

  private Git git;
  
  
  public GitRepository(URI sourceUri) {
    super(sourceUri);
    try {
      git = Git.open( new File(sourceUri ) );
    } catch (IOException e) {
    }
  }

  @Override
  public File retrieveModel(String revision, String name, File modelPath) throws TzarException {
    final String branchName = "rev-" + revision;
    if (git == null) {
      throw new TzarException("No git repository at: " + sourceUri);
    }
    File mpath = null;
    if (!modelPath.exists()) {
      try {
        LOG.info("Attempting to clone Git repository to " + modelPath + " now");
        Git.cloneRepository()
        .setURI(sourceUri.toString())
        .setDirectory(modelPath)
        .call();
        mpath = createModelPath(name, modelPath, sourceUri);
      } catch (Exception e) {
        throw new TzarException("Could not clone Git repository: " + e.getMessage());
      }
    }
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
      throw new TzarException("No git repository at: " + sourceUri);
    }
    return new File(new File(sourceUri ), projectParamFilename);
  }

  @Override
  public String getHeadRevision() throws TzarException {
    if (git == null) {
      throw new TzarException("No git repository at: " + sourceUri);
    }
    Repository repository = git.getRepository();
    String rev = null;
    try {
      ObjectId head = repository.resolve("HEAD");
      rev = head.name();
    } catch (Exception e) {
      throw new TzarException("No git repository at: " + sourceUri);
    }
    return rev;
  }

}
