package averroes;

import averroes.util.io.FileFilters;
import cgs.CGSerializer;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class CallGraphTest {

  public static void main(String... args) {
    testCG();
  }

  public static void testSoot() {
    G.v().reset();
    List<String> appJar = new ArrayList<String>();
    appJar.add("E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\onlineshop.jar");
    Options.v().set_process_dir(appJar);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_ignore_resolving_levels(true);
    Options.v().set_output_format(Options.output_format_class);
    Options.v().set_output_dir("E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\sootoutput");
    Scene.v().loadNecessaryClasses();
    PackManager.v().runPacks();
    PackManager.v().writeOutput();
  }

  public static void testCG() {
    G.v().reset();
    String jreToModel = System.getProperty("java.home");
    String jrePath =
        FileUtils.listFiles(
                Paths.get(jreToModel).toFile(),
                FileFilters.jreFileFilter,
                FileFilterUtils.trueFileFilter())
            .stream()
            .map(map -> map.getPath())
            .collect(Collectors.joining(File.pathSeparator));

    String appDir = "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\";
    String classPath = jrePath;
    classPath += File.pathSeparator + appDir + "organized-app.jar";
    classPath += File.pathSeparator + appDir + "averroes-lib-class.jar";
    classPath += File.pathSeparator + appDir + "placeholder-lib.jar";
    List<String> appJar = new ArrayList<String>();
    appJar.add(appDir + "organized-app.jar");
    appJar.add(appDir + "averroes-lib-class.jar");
    Options.v().set_process_dir(appJar);
    Options.v().set_soot_classpath(classPath);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_ignore_resolving_levels(true);
    Options.v().set_soot_classpath(classPath);
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().set_whole_program(true);
    Options.v().set_main_class("averroes.DummyMainClass");
    System.out.println(classPath);
    Scene.v().loadNecessaryClasses();
    System.out.println(Scene.v().getClasses().size());
    SootMethod entryDoItAll = Scene.v().getMethod("<averroes.Library: void <clinit>()>");
    List<SootMethod> entryPoints = new ArrayList<>();
    entryPoints.add(entryDoItAll);
    entryPoints.addAll(Scene.v().getEntryPoints());
    Scene.v().setEntryPoints(entryPoints);

    PackManager.v().runPacks();
    CallGraph cg = Scene.v().getCallGraph();
    CGSerializer.serialize(cg, appDir + "onlineshop_cg.json");
  }
}
