package averroes.tests.gencg;

import averroes.Main;
import org.junit.Ignore;

public class AndroidTest {

  @Ignore
  public void test() {
    String[] args = {
      "-f", "ANDROID",
      "-a", "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1\\chat_hook_case_1.apk",
      "-o", "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1",
      "-j", "system",
      "-l", "E:\\Git\\androidPlatforms\\android-30\\android.jar"
    };
    Main.main(args);
  }
}
