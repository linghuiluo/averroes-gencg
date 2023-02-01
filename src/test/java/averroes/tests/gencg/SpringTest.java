package averroes.tests.gencg;

import static org.junit.Assert.assertTrue;

import averroes.Main;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class SpringTest {

  @Test
  public void test() {
    String app = Paths.get("./src/test/resources/getmapping.jar").toAbsolutePath().toString();
    Path outputDir = Paths.get("./springOutput");
    if (!outputDir.toFile().exists()) {
      outputDir.toFile().mkdir();
    }
    String[] args = {
      "-f", "SPRING", "-j", "system", "-a", app, "-o", outputDir.toAbsolutePath().toString(),
    };
    Main.main(args);
    assertTrue(outputDir.resolve("averroes-lib-class.jar").toFile().exists());
    assertTrue(outputDir.resolve("instrumented-app.jar").toFile().exists());
    assertTrue(outputDir.resolve("placeholder-lib.jar").toFile().exists());
  }
}
