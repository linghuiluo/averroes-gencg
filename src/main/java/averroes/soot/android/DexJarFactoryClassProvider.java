package averroes.soot.android;

import averroes.util.io.DexClassResource;
import averroes.util.io.Paths;
import averroes.util.io.Resource;
import averroes.util.io.ZipEntryResource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.slf4j.LoggerFactory;
import soot.ClassProvider;
import soot.ClassSource;
import soot.CoffiClassSource;
import soot.DexClassSource;
import soot.FoundFile;
import soot.coffi.ClassFile;
import soot.dexpler.DexFileProvider;
import soot.dexpler.DexFileProvider.DexContainer;

/**
 * This class provider adds the Dex classes from the Android apk to the list of application classes,
 * and Java classes from the library JAR to the list of library classes.
 *
 * @author Linghui Luo
 */
public class DexJarFactoryClassProvider implements ClassProvider {

  private Set<String> applicationClassNames;
  private Set<String> libraryClassNames;
  private Map<String, Resource> classes;

  public DexJarFactoryClassProvider() throws IOException {
    this.applicationClassNames = new HashSet<String>();
    this.libraryClassNames = new HashSet<String>();
    classes = new HashMap<String, Resource>();
    prepareDexJarFactoryClassPath();
  }

  private void prepareDexJarFactoryClassPath() throws IOException {
    LoggerFactory.getLogger(getClass()).info("");
    LoggerFactory.getLogger(getClass()).info("Preparing Averroes for Android ...");
    addAndroidApk();
    addLibraryArchive();
  }

  /**
   * Add the organized library archive to the class provider.
   *
   * @return
   * @throws IOException
   */
  private List<String> addLibraryArchive() throws IOException {
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
  public List<String> addArchive(File file, boolean isApplication) throws IOException {
    LoggerFactory.getLogger(getClass())
        .info(
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
  public String addClass(ZipFile archive, ZipEntry entry, boolean fromApplicationArchive)
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
  public String addClass(String path, Resource resource, boolean fromApplicationArchive)
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

    if (classes.containsKey(className)) {
      // This means we encountered another copy of the class later on the
      // path, this should never happen!
      throw new RuntimeException(
          "class " + className + " has already been added to this class provider.");
    } else {
      if (fromApplicationArchive) {
        applicationClassNames.add(className);
      } else {

        libraryClassNames.add(className);
      }
      classes.put(className, resource);
    }

    return className;
  }

  private void addAndroidApk() throws IOException {
    List<DexContainer> dexFiles = DexFileProvider.v().getDexFromSource(Paths.androidApkFile());
    for (DexContainer dex : dexFiles) {
      DexBackedDexFile base = dex.getBase();
      for (ClassDef c : base.getClasses()) {
        String typeName = c.getType();
        typeName = typeName.replace('/', '.');
        applicationClassNames.add(typeName.substring(1, typeName.length() - 1));
        classes.put(c.getType(), new DexClassResource(Paths.androidApkFile(), base, c));
      }
    }
  }

  @Override
  public ClassSource find(String className) {
    if (classes.containsKey(className)) {
      Resource res = classes.get(className);
      boolean print = false;
      if (applicationClassNames.contains(className)) {
        if (res instanceof DexClassResource) {
          DexClassResource dex = (DexClassResource) classes.get(className);
          return new DexClassSource(className, dex.apkFile());
        } else {
          print = true;
        }
      }
      if (libraryClassNames.contains(className)) {
        if (res instanceof ZipEntryResource) {

          ZipEntryResource zer = (ZipEntryResource) classes.get(className);
          String entry = zer.entry().getName();
          String archive = zer.archive().getName();
          FoundFile foundFile = new FoundFile(archive, entry);
          if (print)
            LoggerFactory.getLogger(getClass())
                .info("Application class resolved in file " + archive + ": " + className);
          return new CoffiClassSource(className, foundFile);
        }
      }
    }
    return null;
  }

  public Collection<? extends String> getApplicationClassNames() {
    return this.applicationClassNames;
  }
}
