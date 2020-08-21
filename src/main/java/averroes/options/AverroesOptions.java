/**
 * ***************************************************************************** Copyright (c) 2015
 * Karim Ali and Ondřej Lhoták. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Karim Ali - initial API and implementation and/or initial documentation
 *
 * <p>*****************************************************************************
 */
package averroes.options;

import averroes.FrameworkType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import probe.ObjectManager;
import probe.ProbeClass;
import soot.SootClass;

/**
 * A class that holds all the properties required by Averroes to run. For the possible values of
 * each property, you can consult the accompanying averroes.properties.sample file or the online
 * tutorial at {@link http ://karimali.ca/averroes}
 *
 * @author Karim Ali
 * @author Linghui Luo
 */
public final class AverroesOptions {

  private static List<String> dynamicClasses = null;
  private static Set<String> applicationClasses = new HashSet<>();

  private static Option applicationRegex =
      Option.builder("r")
          .longOpt("application-regex")
          .desc(
              "a list of regular expressions for application packages or classes separated by File.pathSeparator")
          .hasArg()
          .argName("regex")
          .required(false)
          .build();

  private static Option mainClass =
      Option.builder("m")
          .longOpt("main-class")
          .desc("the main class that runs the application when the program executes")
          .hasArg()
          .argName("class")
          .required(false)
          .build();

  private static Option frameworkType =
      Option.builder("f")
          .longOpt("framework-type")
          .desc("the type of framework the application uses")
          .hasArg()
          .argName("type")
          .required(false)
          .build();

  private static Option applicationClassPath =
      Option.builder("a")
          .longOpt("application-class-path")
          .desc("a list of the application files separated by File.pathSeparator")
          .hasArg()
          .argName("path")
          .required(true)
          .build();

  private static Option libraryClassPath =
      Option.builder("l")
          .longOpt("library-class-path")
          .desc("a list of the library dependencies separated by File.pathSeparator")
          .hasArg()
          .argName("path")
          .required(false)
          .build();

  private static Option dynamicClassesFile =
      Option.builder("d")
          .longOpt("dynamic-classes-file")
          .desc(
              "a file that contains a list of classes that are loaded dynamically by Averroes (e.g., classes instantiated through reflection)")
          .hasArg()
          .argName("file")
          .required(false)
          .build();

  private static Option tamiflexFactsFile =
      Option.builder("t")
          .longOpt("tamiflex-facts-file")
          .desc(
              "a file that contains reflection facts generated for this application in the TamiFlex format")
          .hasArg()
          .argName("file")
          .required(false)
          .build();

  private static Option outputDirectory =
      Option.builder("o")
          .longOpt("output-directory")
          .desc("the directory to which Averroes will write any output files/folders.")
          .hasArg()
          .argName("directory")
          .required()
          .build();

  private static Option jreDirectory =
      Option.builder("j")
          .longOpt("java-runtime-directory")
          .desc(
              "the directory that contains the Java runtime environment that Averroes should model")
          .hasArg()
          .argName("directory")
          .required()
          .build();

  private static Option help =
      Option.builder("h")
          .longOpt("help")
          .desc("print out this help message")
          .hasArg(false)
          .required(false)
          .build();

  private static Option enableGuards =
      Option.builder("g")
          .longOpt("enable-guards")
          .desc(
              "setting this flag will make Averroes wrap a guard around any statement that is not a cast statement (or a local variable declaration)")
          .hasArg(false)
          .required(false)
          .build();

  private static Option configEntryPoints =
      Option.builder("c")
          .longOpt("config")
          .desc("the directory that containt the entry point configuration files.")
          .hasArg(true)
          .argName("directory")
          .required(false)
          .build();

  private static Options options =
      new Options()
          .addOption(applicationRegex)
          .addOption(mainClass)
          .addOption(applicationClassPath)
          .addOption(libraryClassPath)
          .addOption(frameworkType)
          .addOption(dynamicClassesFile)
          .addOption(tamiflexFactsFile)
          .addOption(outputDirectory)
          .addOption(jreDirectory)
          .addOption(help)
          .addOption(enableGuards);

  private static CommandLine cmd;

  /**
   * Process the input arguments of Averroes.
   *
   * @param args
   */
  public static void processArguments(String[] args) {
    try {
      cmd = new DefaultParser().parse(options, args);

      // Do we need to print out help messages?
      if (cmd.hasOption(help.getOpt())) {
        help();
      }
    } catch (ParseException e) {
      e.printStackTrace();
      help();
    }
  }

  /** Print out some help information. */
  private static void help() {
    new HelpFormatter().printHelp("jar -jar averroes.jar", "", options, "", true);
    System.exit(0);
  }

  /**
   * The list of application packages or classes separated by {@link File#pathSeparator}.
   *
   * @return
   */
  public static List<String> getApplicationRegex() {
    if (cmd.hasOption(applicationRegex.getOpt()))
      return Arrays.asList(cmd.getOptionValue(applicationRegex.getOpt()).split(File.pathSeparator));
    else return Collections.emptyList();
  }

  /**
   * The main class that runs the application when the program executes.
   *
   * @return
   */
  public static String getMainClass() {
    return cmd.getOptionValue(mainClass.getOpt());
  }

  /**
   * The list of the application files separated by {@link File#pathSeparator}.
   *
   * @return
   */
  public static List<String> getApplicationClassPath() {
    return Arrays.asList(
        cmd.getOptionValue(applicationClassPath.getOpt()).split(File.pathSeparator));
  }

  /**
   * The list of the library files separated by {@link File#pathSeparator}
   *
   * @return
   */
  public static List<String> getLibraryClassPath() {
    return Arrays.asList(
        cmd.getOptionValue(libraryClassPath.getOpt(), "").split(File.pathSeparator));
  }

  /**
   * Is support for dynamic classes enabled?
   *
   * @return
   */
  public static boolean isDynamicClassesEnabled() {
    return cmd.hasOption(dynamicClassesFile.getOpt());
  }

  /**
   * Get the names of classes that might be dynamically loaded by the input program.
   *
   * @return
   */
  public static List<String> getDynamicClasses() throws IOException {
    if (dynamicClasses == null) {
      dynamicClasses = new ArrayList<String>();

      if (isDynamicClassesEnabled()) {
        BufferedReader in =
            new BufferedReader(new FileReader(cmd.getOptionValue(dynamicClassesFile.getOpt())));
        String line;
        while ((line = in.readLine()) != null) {
          dynamicClasses.add(line);
        }
        in.close();
      }
    }

    return dynamicClasses;
  }

  /**
   * Get the class names of the dynamic library classes.
   *
   * @return
   * @throws IOException
   */
  public static List<String> getDynamicLibraryClasses() throws IOException {
    return getDynamicClasses()
        .stream()
        .filter(AverroesOptions::isLibraryClass)
        .collect(Collectors.toList());
  }

  /**
   * Get the class names of the dynamic application classes.
   *
   * @return
   * @throws IOException
   */
  public static List<String> getDynamicApplicationClasses() throws IOException {
    return getDynamicClasses()
        .stream()
        .filter(AverroesOptions::isApplicationClass)
        .collect(Collectors.toList());
  }

  /**
   * Is support for TamiFlex facts enabled?
   *
   * @return
   */
  public static boolean isTamiflexEnabled() {
    return cmd.hasOption(tamiflexFactsFile.getOpt());
  }

  /**
   * Get the files that contains the reflection facts in the TamiFlex format for this program.
   *
   * @return
   */
  public static String getTamiflexFactsFile() {
    return cmd.getOptionValue(tamiflexFactsFile.getOpt(), "");
  }

  /**
   * The directory to which Averroes will write any output files/folders.
   *
   * @return
   */
  public static String getOutputDirectory() {
    return cmd.getOptionValue(outputDirectory.getOpt());
  }

  /**
   * The version for the Java runtime environment to be used. This can one of (1.4, 1.6, 1.7, 1.8,
   * system).
   *
   * @return
   */
  public static String getJreDirectory() {
    return cmd.getOptionValue(jreDirectory.getOpt());
  }

  /**
   * Check if a class belongs to the application, based on the {@value #APPLICATION_INCLUDES}
   * property.
   *
   * @param probeClass
   * @return
   */
  public static boolean isApplicationClass(ProbeClass probeClass) {
    for (String entry : getApplicationRegex()) {
      /*
       * 1. If the entry ends with .* then this means it's a package. 2. If the entry
       * ends with .** then it's a super package. 3. If the entry is **, then it's the
       * default package. 4. Otherwise, it's the full class name.
       */
      if (entry.endsWith(".*")) {
        String pkg = entry.replace(".*", "");
        if (probeClass.pkg().equalsIgnoreCase(pkg)) {
          return true;
        }
      } else if (entry.endsWith(".**")) {
        String pkg = entry.replace("**", "");
        if (probeClass.toString().startsWith(pkg)) {
          return true;
        }
      } else if (entry.equalsIgnoreCase("**") && probeClass.pkg().isEmpty()) {
        return true;
      } else if (entry.equalsIgnoreCase(probeClass.toString())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a class belongs to the application, based on the {@link #applicationRegex} option.
   *
   * @param className
   * @return
   */
  public static boolean isApplicationClass(String className) {
    if (AverroesOptions.useApplicationRegex())
      return isApplicationClass(ObjectManager.v().getClass(className));
    else return true;
  }

  public static void loadApplicationClass(String className) {
    applicationClasses.add(className);
  }

  public static boolean isLoadedApplicationClass(String className) {
    return applicationClasses.contains(className);
  }

  /**
   * Check if a class belongs to the application, based on the {@link #applicationRegex} option.
   *
   * @param sootClass
   * @return
   */
  public static boolean isLoadedApplicationClass(SootClass sootClass) {
    return isLoadedApplicationClass(sootClass.getName());
  }

  /**
   * Check if a class belongs to the library (i.e., not an application class).
   *
   * @param className
   * @return
   */
  public static boolean isLibraryClass(String className) {
    return !isApplicationClass(className);
  }

  /**
   * Setting this flag will make Averroes wrap a guard around any statement that is not a cast
   * statement (or a local variable declaration).
   *
   * @return
   */
  public static boolean isEnableGuards() {
    return cmd.hasOption(enableGuards.getOpt());
  }

  public static boolean isAndroidApk() {
    return cmd.getOptionValue(frameworkType.getOpt()).equals(FrameworkType.ANDROID.toString());
  }

  public static boolean isDefaultJavaApplication() {
    return !cmd.hasOption(frameworkType.getOpt());
  }

  public static String getFrameworkType() {
    if (!cmd.hasOption(frameworkType.getOpt())) return FrameworkType.DEFAULT;
    String f = cmd.getOptionValue(frameworkType.getOpt());
    switch (f) {
      case "ANDROID":
        return FrameworkType.ANDROID;
      case "SPRING":
        return FrameworkType.SPRING;
      default:
        return FrameworkType.DEFAULT;
    }
  }

  public static boolean useApplicationRegex() {
    return cmd.hasOption(applicationRegex.getOpt());
  }

  public static String getAndroidApk() {
    assert (!isAndroidApk());
    return cmd.getOptionValue(applicationClassPath.getOpt());
  }

  public static String getEntryPointClasses() {
    String config = "config";
    if (cmd.hasOption(configEntryPoints.getOpt()))
      config = cmd.getOptionValue(configEntryPoints.getOpt());
    return config + File.separator + "EntryPointClasses.txt";
  }

  public static String getEntryPointMethods() {
    String config = "config";
    if (cmd.hasOption(configEntryPoints.getOpt()))
      config = cmd.getOptionValue(configEntryPoints.getOpt());
    return config + File.separator + "EntryPointMethods.txt";
  }
}
