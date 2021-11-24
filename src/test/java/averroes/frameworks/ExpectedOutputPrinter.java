package averroes.frameworks;

import averroes.frameworks.options.FrameworksOptions;
import averroes.soot.SootSceneUtil;
import averroes.util.io.Printers;
import averroes.util.io.Printers.PrinterType;
import java.util.List;
import soot.G;
import soot.Scene;
import soot.SootMethod;
import soot.options.Options;

/**
 * A simple printer that outputs both Jimple representation of the code models we write by hand, and
 * the JSON files that describe method invocations, casts, field reads and writes. This information
 * is used later for to validate the code generated by averroes.
 *
 * @author Karim Ali
 */
public class ExpectedOutputPrinter {

  public static void main(String[] args) {
    try {
      // Process the arguments
      FrameworksOptions.processArguments(args);

      // Reset Soot
      G.reset();

      // Cleanup output directory
      averroes.util.io.Paths.deleteJimpleExpectedDirectory();
      averroes.util.io.Paths.deleteJsonExpectedDirectory();

      // Set some soot parameters
      Options.v().classes().addAll(FrameworksOptions.getClasses());
      Options.v().set_soot_classpath(FrameworksOptions.getSootClassPath());
      // Options.v().setPhaseOption("jb.dae", "enabled:false");
      // Options.v().set_verbose(true);

      // Load the necessary classes
      Scene.v().loadNecessaryClasses();

      // Print out Jimple files
      SootSceneUtil.getClasses().stream()
          .map(c -> c.getMethods())
          .flatMap(List::stream)
          .filter(SootMethod::isConcrete)
          .forEach(
              m -> {
                Printers.printJimple(PrinterType.EXPECTED, m);
              });

      // Print out JSON files
      SootSceneUtil.getClasses()
          .forEach(
              c -> {
                Printers.printJson(PrinterType.EXPECTED, c);
              });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
