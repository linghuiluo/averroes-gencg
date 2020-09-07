package averroes.tests.gencg;

import averroes.Main;
import org.junit.Test;

public class AndroidTest {

  @Test
  public void test() {
    String[] args = {
      "-f", "ANDROID",
      "-a", "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1\\chat_hook_case_1.apk",
      "-o", "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1",
      "-j", "system",
      "-l", "E:\\Git\\androidPlatforms"
    };
    Main.main(args);
  }
}
