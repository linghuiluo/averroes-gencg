package averroes.gencg.android;

import averroes.FrameworkType;
import averroes.soot.Hierarchy;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;

public class SpringEntryPointClassesDetector implements AnnotationEntryPointClassDetector {

  private static Logger logger = LoggerFactory.getLogger(SpringEntryPointClassesDetector.class);
  protected Hierarchy classHierarchy;
  protected Set<String> SPRING_ENTRYPOINT_CLASSES;

  public SpringEntryPointClassesDetector(Hierarchy hierachy, EntryPointConfigurationReader reader) {
    this.classHierarchy = hierachy;
    this.SPRING_ENTRYPOINT_CLASSES = reader.getEntryPointClasses(FrameworkType.SPRING);
  }

  @Override
  public List<SootClass> getEntryPointClasses() {
    List<SootClass> epClasses = getEntryPointClasses(classHierarchy, SPRING_ENTRYPOINT_CLASSES);
    for (SootClass c : epClasses) {
      logger.info("Detected entry point class: " + c.getName());
    }
    return epClasses;
  }
}
