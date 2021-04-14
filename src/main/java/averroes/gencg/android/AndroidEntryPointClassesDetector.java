package averroes.gencg.android;

import averroes.FrameworkType;
import averroes.options.AverroesOptions;
import averroes.soot.Hierarchy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import soot.SootClass;

/** @author Linghui Luo */
public class AndroidEntryPointClassesDetector implements SubTypingEntryPointClassDetector {
  private static Logger logger = LoggerFactory.getLogger(AndroidEntryPointClassesDetector.class);
  protected Set<String> ANDROID_ENTRYPOINT_CLASSES;
  protected Hierarchy classHierarchy;

  protected String packageName; // package name stored in AndroidManifest.xml

  public AndroidEntryPointClassesDetector(
      Hierarchy hierachy, EntryPointConfigurationReader reader) {
    this.classHierarchy = hierachy;
    this.ANDROID_ENTRYPOINT_CLASSES = reader.getEntryPointClasses(FrameworkType.ANDROID);
    readPackageName();
  }

  public Map<SootClass, SootClass> getEntryPointClasses() {
    Map<SootClass, SootClass> ret =
        getEntryPointClasses(classHierarchy, ANDROID_ENTRYPOINT_CLASSES);

    ret =
        ret.entrySet().stream()
            .filter(x -> !x.getKey().getPackageName().startsWith("android.support."))
            .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

    for (SootClass androidClass : ret.keySet()) {
      logger.info("Detected entry point class: " + androidClass.getName());
    }
    return ret;
  }

  public void readPackageName() {
    String apkPath = AverroesOptions.getAndroidApk();
    InputStream manifestIS = null;
    ZipFile apkFile = null;
    try {
      try {
        apkFile = new ZipFile(apkPath);
        for (Enumeration<? extends ZipEntry> entries = apkFile.entries();
            entries.hasMoreElements(); ) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (entryName.equals("AndroidManifest.xml")) {
            manifestIS = apkFile.getInputStream(entry);
            break;
          }
        }
      } catch (Exception e) {
        logger.error("Exception when looking for manifest in apk: " + apkPath);
      }

      if (manifestIS == null) {
        logger.error("Could not find sdk version in Android manifest!");
      }
      if (manifestIS != null)
        try {
          AxmlReader xmlReader = new AxmlReader(IOUtils.toByteArray(manifestIS));
          xmlReader.accept(
              new AxmlVisitor() {
                private String nodeName = null;

                @Override
                public void attr(String ns, String name, int resourceId, int type, Object obj) {
                  super.attr(ns, name, resourceId, type, obj);
                  if (nodeName != null && name != null) {
                    if (nodeName.equals("manifest")) {
                      if (name.equals("package")) packageName = obj.toString();
                    }
                  }
                }

                @Override
                public NodeVisitor child(String ns, String name) {
                  // update the xml node name
                  nodeName = name;
                  return this;
                }
              });
        } catch (Exception e) {
          e.printStackTrace();
        }
    } finally {
      if (apkFile != null) {
        try {
          apkFile.close();
        } catch (IOException e) {
          throw new RuntimeException("Error when looking for manifest in apk: " + e);
        }
      }
    }
  }
}
