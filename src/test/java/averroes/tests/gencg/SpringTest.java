package averroes.tests.gencg;

import averroes.Main;
import org.junit.Ignore;

public class SpringTest {

  @Ignore
  public void test() {
    String[] args = {
      "-f", "SPRING",
      "-a", "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\onlineshop.jar",
      "-o", "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop",
      "-j", "system"
    };
    Main.main(args);
  }
}
