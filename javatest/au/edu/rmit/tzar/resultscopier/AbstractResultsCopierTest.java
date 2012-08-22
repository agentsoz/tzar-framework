package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.runners.NullRunner;
import com.google.common.io.Files;
import junit.framework.TestCase;

import java.io.File;

/**
 * Base class for results copier tests.
 */
public abstract class AbstractResultsCopierTest extends TestCase {
  ResultsCopier copier;
  File baseDestPath;
  File sourcePath;
  File sourcePath2;
  protected File localOutputPath;
  protected Run run;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    File tempSourceDir = Files.createTempDir();
    run = new Run(1234, "a run", "4321", "", Parameters.EMPTY_PARAMETERS, "scheduled", "", "");
    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tempSourceDir, null, new NullRunner());
    localOutputPath = executableRun.getOutputPath();
    localOutputPath.mkdir();
    baseDestPath = Files.createTempDir();
    sourcePath = File.createTempFile("file", null, localOutputPath);
    sourcePath2 = File.createTempFile("file", null, localOutputPath);
  }

}
