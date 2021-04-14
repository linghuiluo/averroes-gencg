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

import averroes.options.AverroesOptions;
import averroes.util.io.Paths;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer.DexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.dexpler.DexFileProvider;
import soot.dexpler.DexFileProvider.DexContainer;

/**
 * Utility class to organize the input JAR files to Averroes into two JAR files only: one for the
 * application, and another one for the library. This process does not alter the original class
 * files in any way, it's merely copying the class files into these temporary JAR files for
 * convenience.
 *
 * @author karim
 */
public class ArchiveOrganizer {

  private Set<String> classNames;
  private JarFile organizedApplicationJarFile;
  private JarFile organizedLibraryJarFile;
  private JarOutputStream jarFile;
  private ArrayList<String> libraryClassPath;
  private Set<String> applicationClassNames;
  private Set<String> libraryClassNames;

  private Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Construct a new JAR organizer.
   *
   * @throws IOException
   * @throws FileNotFoundException
   */
  public ArchiveOrganizer() throws FileNotFoundException, IOException {
    classNames = new HashSet<String>();
    applicationClassNames = new HashSet<String>();
    libraryClassNames = new HashSet<String>();
    libraryClassPath = new ArrayList<>();
    if (!AverroesOptions.isAndroidApk())
      organizedApplicationJarFile = new JarFile(Paths.organizedApplicationJarFile());
    else jarFile = new JarOutputStream(new FileOutputStream(Paths.organizedApplicationJarFile()));
    organizedLibraryJarFile = new JarFile(Paths.organizedLibraryJarFile());
  }

  /**
   * Get the set of application class names.
   *
   * @return
   */
  public Set<String> applicationClassNames() {
    return applicationClassNames;
  }

  /**
   * Get the set of library class names.
   *
   * @return
   */
  public Set<String> libraryClassNames() {
    return libraryClassNames;
  }

  /**
   * Organize the input JAR files into two JAR files only: one for application classes, the other
   * for library classes.
   *
   * @throws ZipException
   * @throws IOException
   * @throws URISyntaxException
   */
  public void organizeInputJarFiles() throws ZipException, IOException {
    libraryClassPath.addAll(AverroesOptions.getLibraryClassPath());
    processApplicationFiles();
    processDependencies();
    if (organizedApplicationJarFile != null) organizedApplicationJarFile.close();
    organizedLibraryJarFile.close();
  }

  /**
   * Process the application jars.
   *
   * @throws ZipException
   * @throws IOException
   * @throws URISyntaxException
   */
  private void processApplicationFiles() throws ZipException, IOException {
    logger.info("Process application files...");
    switch (AverroesOptions.getFrameworkType()) {
      case FrameworkType.ANDROID:
        processApk(AverroesOptions.getAndroidApk());
        break;
      case FrameworkType.SPRING:
        AverroesOptions.getApplicationClassPath().forEach(jar -> processExecutableJar(jar));
        break;
      default:
        AverroesOptions.getApplicationClassPath().forEach(jar -> processArchive(jar, true));
        return;
    }
  }

  /** Process the dependencies of the input JAR files. */
  private void processDependencies() {
    // Add the application library dependencies
    libraryClassPath.forEach(lib -> processArchive(lib, false));
    if (!AverroesOptions.isAndroidApk()) {
      // Add the JRE libraries
      if ("system".equals(AverroesOptions.getJreDirectory())) {
        processJreArchives(System.getProperty("java.home"));
      } else {
        processJreArchives(AverroesOptions.getJreDirectory());
      }
    }
  }

  /**
   * Process the JRE archives (recognized JAR files are: rt.jar, jsse.jar, jce.jar).
   *
   * @param dir
   */
  private void processJreArchives(String dir) {
    logger.info("Process JRE " + dir);
    File directory = new File(dir);
    org.apache.commons.io.filefilter.IOFileFilter nameFilter =
        FileFilterUtils.or(
            FileFilterUtils.nameFileFilter("rt.jar"),
            FileFilterUtils.nameFileFilter("jsse.jar"),
            FileFilterUtils.nameFileFilter("jce.jar"));

    FileUtils.listFiles(directory, nameFilter, FileFilterUtils.trueFileFilter())
        .forEach(file -> processArchive(file.getPath(), false));
  }

  /**
   * Process a give APK file.
   *
   * @param apk
   */
  private void processApk(String apk) {
    try {
      File apkFile = new File(apk);

      logger.info("Processing input apk: " + apkFile.getAbsolutePath());
      List<DexContainer<? extends DexFile>> dexFiles =
          DexFileProvider.v().getDexFromSource(apkFile);
      for (DexContainer<? extends DexFile> dex : dexFiles) {
        DexEntry<? extends DexFile> base = dex.getBase();
        for (ClassDef c : base.getDexFile().getClasses()) {
          String typeName = c.getType();
          typeName = typeName.replace('/', '.');
          String className = typeName.substring(1, typeName.length() - 1);
          addDexClass(className, true);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void processExecutableJar(String fileName) {
    // Exit if the fileName is empty
    if (fileName.trim().length() <= 0) {
      return;
    }

    File file = new File(fileName);
    if (file.getName().endsWith(".jar")) {
      logger.info("Processing executable jar: " + file.getAbsolutePath());

      try {
        ZipFile archive = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = archive.entries();

        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().endsWith(".class")) {
            if (entry.getName().startsWith("BOOT-INF/classes/")) {

              addClass(archive, entry, true);
            } else {

              // TODO. check if it is spring class or not
              addClass(archive, entry, false);
            }
          } else if (entry.getName().endsWith(".jar")) {
            if (FrameworkType.SPRING.equals(AverroesOptions.getFrameworkType())) {
              // logger.info("Extracting dependency " + entry.getName());
              File dir = new File(AverroesOptions.getOutputDirectory() + File.separator + "lib");
              if (!dir.exists()) dir.mkdirs();
              File f =
                  new File(dir + File.separator + entry.getName().replace("BOOT-INF/lib/", ""));
              if (!f.exists()) {
                FileOutputStream fos = new FileOutputStream(f);
                InputStream is = archive.getInputStream(entry);
                while (is.available() > 0) {
                  fos.write(is.read());
                }
                fos.close();
                is.close();
              }
              this.libraryClassPath.add(f.toString());
            }
          }
        }
        archive.close();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   * Process a given JAR file.
   *
   * @param fileName
   * @param fromApplicationArchive
   */
  private void processArchive(String fileName, boolean fromApplicationArchive) {

    // Exit if the fileName is empty
    if (fileName.trim().length() <= 0) {
      return;
    }

    File file = new File(fileName);
    logger.debug(
        "Processing "
            + (fromApplicationArchive ? "application" : "library")
            + " archive: "
            + file.getAbsolutePath());
    if (file.getName().endsWith(".jar")) {

      try {
        ZipFile archive = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = archive.entries();

        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().endsWith(".class")) {
            addClass(archive, entry, fromApplicationArchive);
          }
        }
        archive.close();
      } catch (IOException e) {
        e.printStackTrace();
        logger.info("Couldn't process " + file.getAbsolutePath());
      }
    }
  }

  /**
   * Determine whether the given class file will be added to the list of application or library
   * class files depending on the AverroesProperties file.
   *
   * @param archive
   * @param entry
   * @param fromApplicationArchive
   * @throws IOException
   */
  private void addClass(ZipFile archive, ZipEntry entry, boolean fromApplicationArchive)
      throws IOException {
    String className = entry.getName().replace('/', '.').replace(".class", "");

    if (classNames.contains(className)) {
      /*
       * Ignore, this means we encountered another copy of the class later on the
       * path, it should be ignored. This is the case in some benchmarks where the jar
       * file contains both application and library classes, while some of those
       * library classes are also in the rt.jar or deps.jar. In such a case, we want
       * to add the classes from the jar file first and ignore the repetition.
       */
      logger.debug("class " + className + " has already been added to this class provider.");
    } else {
      /*
       * The class has to be from an application archive & an application class. Some
       * classes in xalan are application classes based on their package name only,
       * while they're in fact not part of the application and they come from rt.jar
       * (e.g., org.apache.xalan.templates.OutputProperties$1).
       */
      if (AverroesOptions.isApplicationClass(className) && fromApplicationArchive) {
        extractApplicationClassFile(archive, entry);
      } else {
        extractLibraryClassFile(archive, entry);
      }

      classNames.add(className);
    }
  }

  private void addDexClass(String className, boolean fromApplicationArchive) {
    if (classNames.contains(className)) {
      /*
       * Ignore, this means we encountered another copy of the class later on the
       * path, it should be ignored. This is the case in some benchmarks where the jar
       * file contains both application and library classes, while some of those
       * library classes are also in the rt.jar or deps.jar. In such a case, we want
       * to add the classes from the jar file first and ignore the repetition.
       */
      logger.info("class " + className + " has already been added to this class provider.");
    } else {
      /*
       * The class has to be from an application archive & an application class. Some
       * classes in xalan are application classes based on their package name only,
       * while they're in fact not part of the application and they come from rt.jar
       * (e.g., org.apache.xalan.templates.OutputProperties$1).
       */
      if (AverroesOptions.isApplicationClass(className) && fromApplicationArchive) {
        applicationClassNames.add(className);
        AverroesOptions.loadApplicationClass(className);
      } else {
        libraryClassNames.add(className);
      }
      classNames.add(className);
    }
  }

  /**
   * Extract an application class file.
   *
   * @param sourceArchive
   * @param entry
   * @throws IOException
   */
  private void extractApplicationClassFile(ZipFile sourceArchive, ZipEntry entry)
      throws IOException {
    String entryName = entry.getName();
    if (FrameworkType.SPRING.equals(AverroesOptions.getFrameworkType())) {
      if (entry.getName().startsWith("BOOT-INF/classes/")) {
        entryName = entryName.replace("BOOT-INF/classes/", "");
        extractClassFile(sourceArchive, entry, entryName, organizedApplicationJarFile);
        String className = entryName.replace("/", ".").replace(".class", "");
        applicationClassNames.add(className);
        AverroesOptions.loadApplicationClass(className);
      }
    } else {
      extractClassFile(sourceArchive, entry, entryName, organizedApplicationJarFile);
      String className = entryName.replace("/", ".").replace(".class", "");
      applicationClassNames.add(className);
      AverroesOptions.loadApplicationClass(className);
    }
    logger.info("Extracting application class " + entryName);
  }

  /**
   * Extract a library class file.
   *
   * @param sourceArchive
   * @param entry
   * @throws IOException
   */
  private void extractLibraryClassFile(ZipFile sourceArchive, ZipEntry entry) throws IOException {
    extractClassFile(sourceArchive, entry, entry.getName(), organizedLibraryJarFile);
    libraryClassNames.add(entry.getName().replace("/", "."));
  }

  /**
   * Extract a class file to specified file.
   *
   * @param sourceArchive
   * @param entry
   * @param destArchive
   * @throws IOException
   */
  private void extractClassFile(
      ZipFile sourceArchive, ZipEntry entry, String entryName, JarFile destArchive)
      throws IOException {
    // Write out the class file to the destination archive directly. No
    // temporary file used.
    destArchive.add(sourceArchive.getInputStream(entry), entryName);
  }
}
