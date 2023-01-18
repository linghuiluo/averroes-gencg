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

import averroes.exceptions.Assertions;
import averroes.options.AverroesOptions;
import averroes.soot.Names;
import averroes.util.io.Paths;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;
import org.apache.bcel.verifier.VerifierFactory;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import soot.SootMethod;

/**
 * A JAR file is a collection of class files. We use BCEL to verify that the generated JAR files
 * conforms to the JVM standards.
 *
 * @author karim, Linghui Luo
 */
public class JarFile {

  private static Set<JavaClass> bcelClasses = new HashSet<JavaClass>();
  private JarOutputStream jarOutputStream;
  private File fileName;

  /**
   * Construct a new JAR file.
   *
   * @param file
   */
  public JarFile(File file) {
    jarOutputStream = null;
    this.fileName = file;
  }

  /**
   * Get the relative path for an absolute file path.
   *
   * @param base
   * @param absolute
   * @return
   * @throws IOException
   */
  public static String relativize(File base, File absolute) {
    Path pathAbsolute = java.nio.file.Paths.get(absolute.getPath()).normalize();
    Path pathBase = java.nio.file.Paths.get(base.getPath()).normalize();
    return pathBase.relativize(pathAbsolute).toString();
  }

  /**
   * Get the output stream of this JAR archive.
   *
   * @return
   * @throws IOException
   */
  public JarOutputStream getJarOutputStream() throws IOException {
    if (jarOutputStream == null) {
      Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      jarOutputStream = new JarOutputStream(new FileOutputStream(fileName), manifest);
    }
    return jarOutputStream;
  }

  /**
   * Add all the generated class files in directory to the Jar file.
   *
   * @throws IOException
   */
  public void addGeneratedClassFilesToJar(File dir, File jarFile) throws IOException {
    Set<String> classFiles = new HashSet<>();
    // Add the class files to the crafted JAR file.
    FileUtils.listFiles(dir, new String[] {"class"}, true).stream()
        .forEach(
            file -> {
              try {
                String className = relativize(dir, file);
                if (!className.equals(Names.AVERROES_LIBRARY_CLASS_BC_SIG + ".class")
                    && !className.equals(Names.DUMMYMAIN_CLASS_BC_SIG + ".class")) {
                  if (AverroesOptions.isAndroidApk()
                      && className.startsWith("java\\")
                      && className.startsWith("javax\\")) return;
                  add(dir, file);
                  classFiles.add(className.replace("\\", "/"));
                }
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    close();

    // Now add all those class files in the crafted JAR file to the BCEL
    // repository.
    for (String classFile : classFiles) {
      ClassParser parser = new ClassParser(jarFile.getPath(), classFile);
      JavaClass cls = parser.parse();
      bcelClasses.add(cls);
    }
  }

  /**
   * Add all the generated framework class files to the Jar file.
   *
   * @throws IOException
   */
  public void addGeneratedFrameworkClassFiles() throws IOException {
    Set<String> classFiles = new HashSet<>();
    File dir = Paths.frameworksLibraryClassesOutputDirectory();
    File placeholderJar = Paths.placeholderFrameworkJarFile();

    // Add the class files to the crafted JAR file.
    FileUtils.listFiles(dir, new String[] {"class"}, true).stream()
        .forEach(
            file -> {
              try {
                String className = relativize(dir, file);
                add(dir, file);
                classFiles.add(className);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
    close();

    // Set BCEL's repository class path.
    SyntheticRepository rep =
        SyntheticRepository.getInstance(new ClassPath(placeholderJar.toString()));
    Repository.setRepository(rep);

    // Now add all those class files in the crafted JAR file to the BCEL
    // repository.
    for (String classFile : classFiles) {
      classFile = classFile.replace('\\', '/');
      ClassParser parser = new ClassParser(placeholderJar.getPath(), classFile);
      JavaClass cls = parser.parse();
      bcelClasses.add(cls);
    }

    // Now we need to add all the BCEL classes
    bcelClasses.forEach(c -> Repository.getRepository().storeClass(c));
  }

  /**
   * Add the generated LibraryClass and DummyMainClass file to the averroes-lib-class.jar file
   *
   * @throws IOException
   * @throws URISyntaxException
   */
  public void addAverroesLibraryClassAndDummyMainClassFile()
      throws IOException, URISyntaxException {
    File dir = Paths.libraryClassesOutputDirectory();
    File placeholderJar = Paths.placeholderLibraryJarFile();
    File averroesLibraryClassJar = Paths.averroesLibraryClassJarFile();

    Set<String> classFiles = new HashSet<>();
    FileUtils.listFiles(dir, new String[] {"class"}, true).stream()
        .forEach(
            f -> {
              try {
                String s = relativize(dir, f);
                if (s.equals(Names.AVERROES_LIBRARY_CLASS_BC_SIG + ".class")
                    || s.equals(Names.DUMMYMAIN_CLASS_BC_SIG + ".class")) {
                  if (f.isFile()) {
                    add(dir, f);
                    classFiles.add(s);
                  }
                }
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    close();

    // Set BCEL's repository class path.
    SyntheticRepository rep =
        SyntheticRepository.getInstance(
            new ClassPath(
                averroesLibraryClassJar
                    + File.pathSeparator
                    + placeholderJar
                    + File.pathSeparator
                    + Paths.organizedApplicationJarFile()));
    Repository.setRepository(rep);

    // Now add the class files (including ones from placeholder JAR) to the
    // BCEL repository.
    // Now add all those class files in the crafted JAR file to the BCEL
    // repository.
    for (String classFile : classFiles) {
      ClassParser parser =
          new ClassParser(averroesLibraryClassJar.getPath(), classFile.replace("\\", "/"));
      JavaClass cls = parser.parse();
      bcelClasses.add(cls);
    }

    // Now we need to add all the BCEL classes (including ones from previous
    // placeholder JAR to force BCEL to load
    // those crafted files when it looks them up
    bcelClasses.forEach(c -> Repository.getRepository().storeClass(c));
  }

  /**
   * Add a class file from source to the Jar file.
   *
   * @param dir
   * @param source
   * @throws IOException
   */
  public void add(File dir, File source) throws IOException {
    BufferedInputStream in = null;
    try {
      if (source.isDirectory()) {
        String name = relativize(dir, source).replace("\\", "/");
        if (!name.isEmpty()) {
          if (!name.endsWith("/")) name += "/";
          JarEntry entry = new JarEntry(name);
          entry.setTime(source.lastModified());
          getJarOutputStream().putNextEntry(entry);
          getJarOutputStream().closeEntry();
        }
        for (File nestedFile : source.listFiles()) add(dir, nestedFile);
        return;
      }

      JarEntry entry = new JarEntry(relativize(dir, source).replace("\\", "/"));
      entry.setTime(source.lastModified());
      getJarOutputStream().putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(source));

      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1) break;
        getJarOutputStream().write(buffer, 0, count);
      }
      getJarOutputStream().closeEntry();
    } finally {
      if (in != null) in.close();
    }
  }

  /**
   * Add the given file with the given entry name to this JAR file.
   *
   * @param source
   * @param entryName
   * @throws IOException
   */
  public void add(File source, String entryName) throws IOException {
    JarEntry entry = new JarEntry(entryName);
    entry.setTime(source.lastModified());
    getJarOutputStream().putNextEntry(entry);
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));

    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
      getJarOutputStream().write(buffer, 0, len);
    }
    getJarOutputStream().closeEntry();
    in.close();
  }

  /**
   * Add the file read from the source input stream with the given entry name to this JAR file.
   *
   * @param source
   * @param entryName
   * @throws IOException
   */
  public void add(InputStream source, String entryName) throws IOException {
    JarEntry entry = new JarEntry(entryName);
    entry.setTime(System.currentTimeMillis());
    getJarOutputStream().putNextEntry(entry);

    byte[] buffer = new byte[1024];
    int len;
    while ((len = source.read(buffer)) != -1) {
      getJarOutputStream().write(buffer, 0, len);
    }
    getJarOutputStream().closeEntry();
    source.close();
  }

  /**
   * Close the JAR output stream.
   *
   * @throws IOException
   */
  public void close() throws IOException {
    getJarOutputStream().close();
  }

  /**
   * Verify the integrity of the JAR file.
   *
   * @throws IOException
   * @throws ClassFormatException
   */
  public void verify() throws ClassFormatException, IOException {
    for (JavaClass cls : bcelClasses) {
      Verifier verifier = VerifierFactory.getVerifier(cls.getClassName());
      Method[] methods = cls.getMethods();
      for (int i = 0; i < methods.length; i++) {
        VerificationResult vr;
        // Do a pass 3a for the constructor of java.lang.Object because
        // we are using an uninitialized "this".
        if (cls.getClassName().equals(Names.JAVA_LANG_OBJECT)
            && methods[i].getName().equals(SootMethod.constructorName)) {
          vr = verifier.doPass3a(i);
        } else {
          vr = verifier.doPass3b(i);
        }

        Assertions.verificationResultOKAssertion(vr, cls.getClassName(), methods[i].getName());
      }
    }
  }

  /**
   * Verify the integrity of the given JAR file using ASM.
   *
   * @param file
   */
  public static void verifyJarFile(String file) {
    try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new File(file))) {
      jarFile.stream()
          .forEach(
              entry -> {
                if (entry.getName().endsWith(".class")) {
                  ClassReader classReader;
                  try {
                    classReader = new ClassReader(jarFile.getInputStream(entry));
                    ClassWriter classWriter =
                        new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor classVisitor = new CheckClassAdapter(classWriter, true);
                    classReader.accept(classVisitor, 0);
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    CheckClassAdapter.verify(
                        new ClassReader(classWriter.toByteArray()), false, printWriter);
                    Assertions.asmVerificationOk(stringWriter);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
