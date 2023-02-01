package averroes.tests.gencg;

import static org.junit.Assert.assertTrue;

import averroes.Main;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class AndroidTest {

  @Test
  public void test() {
    String apk =
        Paths.get("./src/test/resources/ActivityLifecycle1.apk").toAbsolutePath().toString();
    String platforms = Paths.get("./src/test/resources/platforms").toAbsolutePath().toString();
    Path outputDir = Paths.get("./androidOutput");
    if (!outputDir.toFile().exists()) {
      outputDir.toFile().mkdir();
    }
    String[] args = {
      "-f",
      "ANDROID",
      "-j",
      "system",
      "-l",
      platforms,
      "-a",
      apk,
      "-o",
      outputDir.toAbsolutePath().toString(),
    };
    Main.main(args);
    assertTrue(outputDir.resolve("averroes-lib-class.jar").toFile().exists());
    assertTrue(outputDir.resolve("instrumented-app.jar").toFile().exists());
    assertTrue(outputDir.resolve("placeholder-lib.jar").toFile().exists());
  }
}
