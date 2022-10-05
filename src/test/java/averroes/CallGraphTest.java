package averroes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import soot.Body;
import soot.EntryPoints;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGGraphType;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphConstants;

public class CallGraphTest {

  private DotGraph setDotGraph(String fileName) {
    DotGraph canvas = new DotGraph(fileName);
    canvas.setNodeShape(DotGraphConstants.NODE_SHAPE_BOX);
    canvas.setGraphLabel(fileName);
    return canvas;
  }

  private void plotDotGraph(DotGraph canvas, String filename) {
    canvas.plot("output/" + filename + ".dot");
  }

  public void dumpCFG(String filename, Body body) {
    /**
     * BriefUnitGraph ExceptionalUnitGraph CompleteUnitGraph TrapUnitGraph ClassicCompleteUnitGraph
     * BriefBlockGraph ExceptionalBlockGraph CompleteBlockGraph
     * ClassicCompleteBlockGraph|ArrayRefBlockGraph ZonedBlockGraph AltArrayRefBlockGraph
     * AltBriefUnitGraph AltCompleteUnitGraph AltTrapUnitGraph AltBriefBlockGraph
     * AltCompleteBlockGraph AltZonedBlockGraph
     */
    CFGGraphType graphType = CFGGraphType.getGraphType("BriefBlockGraph");
    DirectedGraph<?> graph = graphType.buildGraph(body);
    CFGToDotGraph drawer = new CFGToDotGraph();
    DotGraph canvas = graphType.drawGraph(drawer, graph, body);
    plotDotGraph(canvas, filename);
  }

  private boolean hasMatch(List<String> pkgs, String classSignature) {
    for (String pkg : pkgs) if (classSignature.contains(pkg)) return true;
    return false;
  }

  public void dumpCallGraph(String filename, Iterable<Edge> callgraph, List<String> pkgs) {
    DotGraph canvas = setDotGraph(filename);
    Set<String> methodSet = new HashSet<>();
    Map<String, String> drawed = new HashMap<>();
    for (Edge edge : callgraph) {
      SootMethod src = edge.getSrc().method();
      SootMethod tgt = edge.getTgt().method();
      String srcName = src.getDeclaringClass().getShortName() + "." + src.getName();
      String dstName = tgt.getDeclaringClass().getShortName() + "." + tgt.getName();
      if (drawed.containsKey(srcName) && drawed.get(srcName).equals(dstName)) continue;
      if (!hasMatch(pkgs, src.method().getDeclaringClass().getName())
          && !hasMatch(pkgs, tgt.method().getDeclaringClass().getName())) continue;
      if (methodSet.add(srcName)) {
        canvas.drawNode(srcName).setLabel(srcName);
      }
      if (methodSet.add(dstName)) {
        canvas.drawNode(dstName).setLabel(dstName);
      }
      System.out.println("CG EDGE: " + srcName + " -> " + dstName);
      canvas.drawEdge(srcName, dstName);
      drawed.put(srcName, dstName);
    }
    plotDotGraph(canvas, filename);
  }

  public void constructCallGraph(String appPath, String libPath, String cgAlgo) {
    G.v().reset();
    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_keep_line_number(true);
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_ignore_resolving_levels(true);
    Options.v().set_whole_program(true);

    // append default JDK path
    Options.v().set_prepend_classpath(true);
    if (appPath != null) {
      List<String> processDirs = new ArrayList<>();
      for (String ap : appPath.split(File.pathSeparator)) processDirs.add(ap);
      // set up application code path
      Options.v().set_process_dir(processDirs);
    }
    // set up library code path
    Options.v().set_soot_classpath(libPath);

    // set up call graph algorithm
    switch (cgAlgo) {
      case "cha":
        Options.v().setPhaseOption("cg.cha", "on");
        break;
      case "rta":
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "rta:true");
        Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        break;
      case "vta":
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "vta:true");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        break;
      case "spark":
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        break;
    }
    Scene.v().loadNecessaryClasses();
    List<SootMethod> entrypoints = Scene.v().getEntryPoints();
    for (SootMethod m : EntryPoints.v().mainsOfApplicationClasses())
      if (!entrypoints.contains(m)) entrypoints.add(m);
    for (SootMethod m : EntryPoints.v().clinits()) if (!entrypoints.contains(m)) entrypoints.add(m);
    Scene.v().setEntryPoints(entrypoints);
    PackManager.v().getPack("cg").apply();
  }

  String GUICE_APP_PATH = "/Users/llinghui/Projects/GitHub/SpringDemo/guice-credentials/";
  String GUICE_LIB_PATH =
      "/Users/llinghui/.m2/repository/com/google/inject/guice/4.1.0/guice-4.1.0.jar";
  String SPRING_APP_PATH = "/Users/llinghui/Projects/GitHub/SpringDemo/multipleRequests/";

  @Test
  public void testGuiceExampleWithoutModel() {
    String appPath = GUICE_APP_PATH + "target/guice-1.0-SNAPSHOT.jar";
    constructCallGraph(appPath, GUICE_LIB_PATH, "vta");
    CallGraph cg = Scene.v().getCallGraph();
    List<String> pkgs = new ArrayList<>();
    pkgs.add("guice.example");
    dumpCallGraph("guice-without-model", cg, pkgs);
  }

  @Test
  public void testGuiceExampleWithModel() {
    String appPath = GUICE_APP_PATH + "gencg-output/instrumented-app.jar";
    String modelPath = GUICE_APP_PATH + "gencg-output/averroes-lib-class.jar";
    String placeholderPath = GUICE_APP_PATH + "gencg-output/placeholder-lib.jar";
    List<String> extraEntryPoints = new ArrayList<>();
    extraEntryPoints.add("averroes.Library");
    constructCallGraph(appPath + ":" + modelPath, placeholderPath, "vta");
    CallGraph cg = Scene.v().getCallGraph();
    List<String> pkgs = new ArrayList<>();
    pkgs.add("guice.example");
    pkgs.add("averroes");
    dumpCallGraph("guice-with-model", cg, pkgs);
    Body body = Scene.v().getSootClass("averroes.Library").getMethodByName("main").getActiveBody();
    dumpCFG("guice-with-model-cfg", body);
  }

  @Test
  public void testSpringExampleWithoutModel() {
    String appPath = SPRING_APP_PATH + "target/multiplerequsts-0.0.1-SNAPSHOT.jar";
    constructCallGraph(appPath, appPath, "vta");
    CallGraph cg = Scene.v().getCallGraph();
    List<String> pkgs = new ArrayList<>();
    pkgs.add("springbench");
    dumpCallGraph("spring-without-model", cg, pkgs);
  }

  @Test
  public void testSpringExampleWithModel() {
    String appPath = SPRING_APP_PATH + "gencg-output/instrumented-app.jar";
    String modelPath = SPRING_APP_PATH + "gencg-output/averroes-lib-class.jar";
    String placeholderPath = SPRING_APP_PATH + "gencg-output/placeholder-lib.jar";
    List<String> extraEntryPoints = new ArrayList<>();
    extraEntryPoints.add("averroes.Library");
    constructCallGraph(appPath + ":" + modelPath, placeholderPath, "vta");
    CallGraph cg = Scene.v().getCallGraph();
    List<String> pkgs = new ArrayList<>();
    pkgs.add("springbench");
    pkgs.add("averroes");
    dumpCallGraph("spring-with-model", cg, pkgs);
    Body main = Scene.v().getSootClass("averroes.Library").getMethodByName("main").getActiveBody();
    dumpCFG("spring-with-model-cfg-main", main);
    Body doItAll =
        Scene.v().getSootClass("averroes.Library").getMethodByName("doItAll").getActiveBody();
    dumpCFG("spring-with-model-cfg-doItAll", doItAll);
  }
}
