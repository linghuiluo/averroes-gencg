package averroes.tests.gencg;

import averroes.Main;
import java.io.File;
import org.junit.Ignore;
import org.junit.Test;
import soot.G;

public class AndroidTest {

  @Ignore
  public void test() {
    String[] args = {
      "-f",
      "ANDROID",
      "-a",
      "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1\\chat_hook_case_1.apk",
      "-o",
      "E:\\Git\\Github\\callgraph\\CGBench_Test\\chat_hook_case_1",
      "-j",
      "system",
      "-l",
      "E:\\Git\\androidPlatforms"
    };
    Main.main(args);
  }

  @Test
  public void testAllApks() {
    File taintBench = new File("E:\\Git\\Github\\taintbench\\GitPod-GITHUB\\AllSuite\\allApks");
    String testOutput = "E:\\Git\\Github\\callgraph\\CGBench_Test";
    String platform = "E:\\Git\\androidPlatforms";
    int i = 1;
    for (File f : taintBench.listFiles()) {
      if (i == 3) {
        String apk = f.getName().replace(".apk", "");
        String output = testOutput + File.separator + apk;
        File outFile = new File(output);
        if (!outFile.exists()) outFile.mkdir();
        String apkPath = f.getAbsolutePath();
        System.out.println("Transforming " + apk);
        String[] args = {
          "-f", "ANDROID", "-a", apkPath, "-o", output, "-j", "system", "-l", platform
        };
        Main.main(args);
      }
      i++;
    }
  }

  @Test
  public void testSoot() {
    File taintBench = new File("E:\\Git\\Github\\taintbench\\GitPod-GITHUB\\AllSuite\\allApks");
    String testOutput = "E:\\Git\\Github\\callgraph\\CGBench_Test";
    String platform = "E:\\Git\\androidPlatforms";
    int i = 1;
    for (File f : taintBench.listFiles()) {
      System.out.println(i + " Transforming " + f);
      if (i != 2) {
        String apk = f.getName().replace(".apk", "");
        String output = testOutput + File.separator + apk;
        File outFile = new File(output);
        if (!outFile.exists()) outFile.mkdir();
        String apkPath = f.getAbsolutePath();

        String[] args = {
          "-process-dir",
          apkPath,
          "-android-jars",
          platform,
          "-src-prec",
          "apk",
          "-allow-phantom-refs",
          "-allow-phantom-elms",
          "-output-dir",
          output,
          "-f",
          "class"
        };
        G.v().reset();
        soot.Main.main(args);
      }
      i++;
    }
  }
}
