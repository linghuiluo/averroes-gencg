/**
 * ***************************************************************************** Copyright (c) 2015
 * Karim Ali and Ondřej Lhoták. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Karim Ali - initial API and implementation and/or initial documentation
 * *****************************************************************************
 */
package averroes;

import averroes.gencg.android.AndroidEntryPointClassesDetector;
import averroes.options.AverroesOptions;
import averroes.soot.CodeGenerator;
import averroes.soot.Hierarchy;
import averroes.soot.JarFactoryClassProvider;
import averroes.soot.SootSceneUtil;
import averroes.util.MathUtils;
import averroes.util.TimeUtils;
import averroes.util.io.Paths;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.options.Options;

/**
 * The main Averroes class.
 *
 * @author Karim Ali
 */
public class Main {

  private static Logger logger = LoggerFactory.getLogger(averroes.Main.class);

  /**
   * The main Averroes method.
   *
   * @param args
   */
  public static void main(String[] args) {
    try {
      TimeUtils.splitStart();

      AverroesOptions.processArguments(args);

      organizeInput();

      TimeUtils.reset();

      initializeSootAndLoadClasses();
      double timeUsedBySoot = TimeUtils.elapsedTime();
      logger.info("Soot loaded the input classes in " + timeUsedBySoot + " seconds.");

      if (!AverroesOptions.isAndroidApk()) Scene.v().setMainClassFromOptions();

      // Now let Averroes do its thing
      TimeUtils.reset();

      buildClassHierarchy();

      generateClasses();
      double timeUsedByAverroes = TimeUtils.elapsedTime();
      logger.info(
          "Placeholder library classes created and validated in "
              + timeUsedByAverroes
              + " seconds.");

      TimeUtils.reset();
      addGeneratedClassesToJars();
      double timeUsedByBcel = TimeUtils.elapsedTime();
      logger.info("Placeholder library JAR file verified in " + timeUsedByBcel + " seconds.");
      logger.info(
          "Total time (without verification) is "
              + MathUtils.round(timeUsedBySoot + timeUsedByAverroes)
              + " seconds.");
      logger.info(
          "Total time (with verification) is "
              + MathUtils.round(timeUsedBySoot + timeUsedByAverroes + timeUsedByBcel)
              + " seconds.");

      double total = TimeUtils.elapsedSplitTime();
      logger.info("Elapsed time: " + total + " seconds.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void addGeneratedClassesToJars() throws IOException, URISyntaxException {
    // Create the jar file and add all the generated class files to it.;
    JarFile librJarFile = new JarFile(Paths.placeholderLibraryJarFile());
    librJarFile.addGeneratedLibraryClassFiles();
    JarFile aveJarFile = new JarFile(Paths.averroesLibraryClassJarFile());
    aveJarFile.addAverroesLibraryClassAndDummyMainClassFile();
  }

  private static void generateClasses() throws IOException {
    // Output some code generation statistics
    logger.info("");
    logger.info("Generating extra library classes...");
    logger.info("# generated library classes: " + CodeGenerator.v().getGeneratedClassCount());
    logger.info("# generated library methods: " + CodeGenerator.v().getGeneratedMethodCount());

    // Create the Averroes library class
    logger.info("");
    logger.info("Creating the skeleton for Averroes's main library class...");
    CodeGenerator.v().createAverroesLibraryClass();

    // Create method bodies to the library classes
    logger.info("Generating the method bodies for the placeholder library classes ...");
    CodeGenerator.v().createLibraryMethodBodies();

    // Create empty classes for the basic classes required internally by
    // Soot
    logger.info("Generating empty basic library classes required by Soot...");
    for (SootClass basicClass : Hierarchy.v().getBasicClassesDatabase().getMissingBasicClasses()) {
      CodeGenerator.writeLibraryClassFile(basicClass);
    }

    if (AverroesOptions.isAndroidApk()) {
      AndroidEntryPointClassesDetector detector =
          new AndroidEntryPointClassesDetector(Hierarchy.v());
      List<SootClass> entryPointClasses = detector.getEntryPointClasses();
      CodeGenerator.v().createDummyMainClass(entryPointClasses);
    }
  }

  private static void buildClassHierarchy() {

    logger.info("");
    logger.info("Creating the class hierarchy for the placeholder library...");
    Hierarchy.v();

    // Output some initial statistics
    logger.info("# initial application classes: " + Hierarchy.v().getApplicationClasses().size());
    logger.info("# initial library classes: " + Hierarchy.v().getLibraryClasses().size());
    logger.info("# initial library methods: " + Hierarchy.v().getLibraryMethodCount());
    logger.info("# initial library fields: " + Hierarchy.v().getLibraryFieldCount());
    logger.info("# referenced library methods: " + Hierarchy.v().getReferencedLibraryMethodCount());
    logger.info("# referenced library fields: " + Hierarchy.v().getReferencedLibraryFieldCount());

    // Cleanup the hierarchy
    logger.info("");
    logger.info("Cleaning up the class hierarchy...");
    Hierarchy.v().cleanupLibraryClasses();

    // Output some cleanup statistics
    logger.info("# removed library methods: " + Hierarchy.v().getRemovedLibraryMethodCount());
    logger.info("# removed library fields: " + Hierarchy.v().getRemovedLibraryFieldCount());
    // The +1 is for Finalizer.register that will be added later
    logger.info("# final library methods: " + (Hierarchy.v().getLibraryMethodCount() + 1));
    logger.info("# final library fields: " + Hierarchy.v().getLibraryFieldCount());
  }

  private static void organizeInput() throws IOException, FileNotFoundException, ZipException {
    // Create the output directory and clean up any class files in there
    FileUtils.forceMkdir(Paths.libraryClassesOutputDirectory());
    FileUtils.cleanDirectory(Paths.classesOutputDirectory());

    // Organize the input JAR files
    logger.info("Organizing the JAR files...");
    JarOrganizer jarOrganizer = new JarOrganizer();
    jarOrganizer.organizeInputJarFiles();

    // Print some statistics
    logger.info("# application classes: " + jarOrganizer.applicationClassNames().size());
    logger.info("# library classes: " + jarOrganizer.libraryClassNames().size());
  }

  private static void initializeSootAndLoadClasses() throws IOException {
    // Add the organized archives for the application and its
    // dependencies.
    // Set some soot parameters
    G.reset();
    JarFactoryClassProvider provider = new JarFactoryClassProvider();
    SourceLocator.v().setClassProviders(Collections.singletonList(provider));
    SootSceneUtil.addCommonDynamicClasses(provider);
    Options.v().classes().addAll(provider.getApplicationClassNames());
    if (!AverroesOptions.isAndroidApk()) Options.v().set_main_class(AverroesOptions.getMainClass());
    Options.v().set_validate(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_ignore_resolving_levels(true);
    // Load the necessary classes
    logger.info("");
    logger.info("Soot is Loading classes...");
    Scene.v()
        .loadNecessaryClasses(); // the classes are resolved at signature level, fields, and method
    // signatures are resolved.
  }

  public static void usage() {
    logger.info("");
    logger.info("Usage: java -jar averroes.jar [options]");
    logger.info("  -tfx : enable Tamiflex support");
    logger.info("  -dyn : enable dynamic classes support");
    logger.info("");
    System.exit(1);
  }
}
