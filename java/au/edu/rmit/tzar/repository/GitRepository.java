package au.edu.rmit.tzar.repository;

import java.io.File;
import java.net.URI;

import org.eclipse.jgit.api.Git;

import au.edu.rmit.tzar.api.TzarException;

public class GitRepository extends UrlRepository {

  private Git git;
  
  public GitRepository(URI sourceUri) {
    super(sourceUri);
    // TODO Auto-generated constructor stub
  }

  @Override
  public File retrieveModel(String revision, String name, File modelPath) throws TzarException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision, File destPath)
      throws TzarException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getHeadRevision() throws TzarException {
    // TODO Auto-generated method stub
    return null;
  }

}
