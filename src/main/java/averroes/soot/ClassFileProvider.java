package averroes.soot;

import averroes.util.io.Paths;
import averroes.util.io.Resource;
import averroes.util.io.ZipEntryResource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;
import soot.coffi.ClassFile;

/**
 * This class organizes class Files
 *
 * @author Linghui Luo
 */
public class ClassFileProvider {
  private static Logger logger = LoggerFactory.getLogger(ClassFileProvider.class);
  private static Map<String, ClassFile> classFiles = new HashMap<String, ClassFile>();

  public static void prepare() throws IOException {
    logger.info("");
    logger.info("Preparing ...");
    addApplicationArchive();
    addLibraryArchive();
  }

  public static ClassFile getClassFile(SootClass cls) {
    return classFiles.get(cls.getName());
  }

  /**
   * Add the organized application archive to the class provider.
   *
   * @return
   * @throws IOException
   */
  private static List<String> addApplicationArchive() throws IOException {
    return addArchive(Paths.organizedApplicationJarFile(), true);
  }

  /**
   * Add the organized library archive to the class provider.
   *
   * @return
   * @throws IOException
   */
  private static List<String> addLibraryArchive() throws IOException {
    return addArchive(Paths.organizedLibraryJarFile(), false);
  }

  /**
   * Add an archive to the class provider.
   *
   * @param file
   * @param isApplication
   * @return
   * @throws IOException
   */
  public static List<String> addArchive(File file, boolean isApplication) throws IOException {
    logger.info(
        "Adding "
            + (isApplication ? "application" : "library")
            + " archive: "
            + file.getAbsolutePath());
    List<String> result = new ArrayList<String>();
    ZipFile archive = new ZipFile(file);
    Enumeration<? extends ZipEntry> entries = archive.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (entry.getName().endsWith(".class")) {
        String className = addClass(archive, entry, isApplication);
        result.add(className);
      }
    }
    logger.info(
        "Added "
            + (isApplication ? "application" : "library")
            + " archive: #classes "
            + result.size());
    return result;
  }

  /**
   * Add a class file in a zip/jar archive. Returns the class name of the class that was added.
   *
   * @param archive
   * @param entry
   * @param fromApplicationArchive
   * @return
   * @throws IOException
   */
  public static String addClass(ZipFile archive, ZipEntry entry, boolean fromApplicationArchive)
      throws IOException {
    return addClass(entry.getName(), new ZipEntryResource(archive, entry), fromApplicationArchive);
  }

  /**
   * Add a class file from a resource.
   *
   * @param path
   * @param resource
   * @param fromApplicationArchive
   * @return
   * @throws IOException
   */
  public static String addClass(String path, Resource resource, boolean fromApplicationArchive)
      throws IOException {
    ClassFile c = new ClassFile(path);
    InputStream stream = null;
    try {
      stream = resource.open();
      c.loadClassFile(stream);
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
    String className = path.replace('/', '.').replace(".class", "");
    classFiles.put(className, c);
    return className;
  }
}
