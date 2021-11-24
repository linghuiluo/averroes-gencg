package averroes.gencg;

import averroes.FrameworkType;
import averroes.soot.Hierarchy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

public class SpringEntryPointClassesDetector
    implements AnnotationEntryPointClassDetector,
        AnnotationCreateObjectsDetector,
        AnnotationObjectProvidersDetector {

  private static Logger logger = LoggerFactory.getLogger(SpringEntryPointClassesDetector.class);
  protected Hierarchy classHierarchy;
  protected Set<String> SPRING_ENTRYPOINT_CLASSES;
  protected Set<String> SPRING_ENTRYPOINT_METHODS;
  protected Set<String> SPRING_CREATE_OBJECTS;
  protected Set<String> SPRING_OBJECT_PROVIDERS;

  public SpringEntryPointClassesDetector(Hierarchy hierachy, EntryPointConfigurationReader reader) {
    this.classHierarchy = hierachy;
    this.SPRING_ENTRYPOINT_CLASSES = reader.getEntryPointClasses(FrameworkType.SPRING);
    this.SPRING_ENTRYPOINT_METHODS = reader.getEntryPointMethods(FrameworkType.SPRING);
    this.SPRING_OBJECT_PROVIDERS = reader.getObjectProviders(FrameworkType.SPRING);
    this.SPRING_CREATE_OBJECTS = reader.getCreateObjects(FrameworkType.SPRING);
  }

  @Override
  public Map<SootClass, SootClass> getEntryPointClasses() {
    Map<SootClass, SootClass> epClasses =
        getEntryPointClasses(classHierarchy, SPRING_ENTRYPOINT_CLASSES, SPRING_ENTRYPOINT_METHODS);
    for (SootClass c : epClasses.keySet()) {
      logger.info("Detected entry point class: " + c.getName());
    }
    return epClasses;
  }

  @Override
  public Map<SootClass, Set<SootField>> getCreateObjects() {
    Map<SootClass, Set<SootField>> createObjects =
        getCreateObjects(classHierarchy, SPRING_CREATE_OBJECTS);
    for (Entry<SootClass, Set<SootField>> e : createObjects.entrySet()) {
      for (SootField f : e.getValue())
        logger.info(
            "Detected field "
                + f.getName()
                + " in class "
                + e.getKey().getName()
                + " needs to be initialized.");
    }
    return createObjects;
  }

  @Override
  public Map<SootClass, Set<SootMethod>> getObjectProviders() {
    return null;
  }
}
