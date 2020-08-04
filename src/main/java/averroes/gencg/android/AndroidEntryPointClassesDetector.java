package averroes.gencg.android;

import averroes.options.AverroesOptions;
import averroes.soot.Hierarchy;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import soot.SootClass;

public class AndroidEntryPointClassesDetector {
  private static Logger logger = LoggerFactory.getLogger(AndroidEntryPointClassesDetector.class);
  public static final String ACTIVITY_CLASS = "android.app.Activity";
  public static final String SERVICE_CLASS = "android.app.Service";
  public static final String GCMBASE_INTENT_SERVICE_CLASS =
      "com.google.android.gcm.GCMBaseIntentService";
  public static final String GCMLISTENER_SERVICE_CLASS =
      "com.google.android.gms.gcm.GcmListenerService";
  public static final String BROADCASTRECEIVER_CLASS = "android.content.BroadcastReceiver";
  public static final String CONTENTPROVIDER_CLASS = "android.content.ContentProvider";
  public static final String APPLICATION_CLASS = "android.app.Application";
  public static final String FRAGMENT_CLASS = "android.app.Fragment";
  public static final String SUPPORTFRAGMENT_CLASS = "android.support.v4.app.Fragment";
  public static final String SERVICECONNECTIONINTERFACE = "android.content.ServiceConnection";
  public static final String APPCOMPATACTIVITYCLASS_V4 = "android.support.v4.app.AppCompatActivity";
  public static final String APPCOMPATACTIVITYCLASS_V7 = "android.support.v7.app.AppCompatActivity";

  public static final String[] ANDROID_ENTRYPOINT_CLASSES = {
    ACTIVITY_CLASS,
    SERVICE_CLASS,
    BROADCASTRECEIVER_CLASS,
    CONTENTPROVIDER_CLASS,
    GCMBASE_INTENT_SERVICE_CLASS,
    GCMLISTENER_SERVICE_CLASS,
    APPLICATION_CLASS,
    FRAGMENT_CLASS,
    SUPPORTFRAGMENT_CLASS,
    SERVICECONNECTIONINTERFACE,
    APPCOMPATACTIVITYCLASS_V4,
    APPCOMPATACTIVITYCLASS_V7
  };

  protected Hierarchy classHierarchy;

  protected String packageName; // package name stored in AndroidManifest.xml

  public AndroidEntryPointClassesDetector(Hierarchy hierachy) {
    this.classHierarchy = hierachy;
    readPackageName();
  }

  public List<SootClass> getEntryPointClasses() {
    List<SootClass> ret = new ArrayList<>();
    for (String androidClass : ANDROID_ENTRYPOINT_CLASSES) {
      SootClass epClass = classHierarchy.getClass(androidClass);
      if (epClass != null) {
        LinkedHashSet<SootClass> entryPointClasses = classHierarchy.getSubclassesOf(epClass);
        for (SootClass c : entryPointClasses)
          if (c.getPackageName().startsWith(packageName)) {
            ret.add(c);
            logger.info(
                "Detected Android entry point for class " + androidClass + ": " + c.getName());
          }
      }
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
