package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import junit.framework.TestCase;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for the SvnRepository.
 */
public class SvnRepositoryTest extends TestCase {
  private static final String TEST_URL = "http://foobar.com/blah";
  private static final File BASE_MODEL_PATH = new File("/tmp/foo");
  private SVNUpdateClient mockClient;
  private SvnRepository repository;

  @Override
  public void setUp() throws Exception {
    mockClient = Mockito.mock(SVNUpdateClient.class);
    SVNWCClient mockWCClient = Mockito.mock(SVNWCClient.class);
    repository = new SvnRepository(new URI(TEST_URL), mockClient, mockWCClient);
  }

  /**
   * Simulates retrieving the model from sourceforge.
   */
  public void testGetModel() throws SVNException, TzarException, URISyntaxException {
    String revision = "1000";

    String moduleName = "a_project";
    File expectedPath = repository.createModelPath(moduleName, BASE_MODEL_PATH, repository.sourceUri);

    File modelPath = repository.retrieveModel(revision, moduleName, BASE_MODEL_PATH);

    SVNRevision svnRevision = SVNRevision.create(Long.parseLong(revision));
    verify(mockClient).doCheckout(SVNURL.parseURIEncoded(TEST_URL), expectedPath, svnRevision,
        svnRevision, SVNDepth.INFINITY, true);
    assertEquals(expectedPath, modelPath);
  }

  public void testGetModelBadRevision() {
    try {
      repository.retrieveModel("foo", "a_project", BASE_MODEL_PATH);
      fail("Expected TzarException to be thrown.");
    } catch (TzarException e) {
    }
  }
}
