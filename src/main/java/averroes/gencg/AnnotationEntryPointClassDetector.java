package averroes.gencg;

import averroes.soot.Hierarchy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

/** @author Linghui Luo */
public interface AnnotationEntryPointClassDetector extends ClassesDetector {

  public EntryPointTypeTag aTag = new EntryPointTypeTag(EntryPointTypeTag.ANNOTATION);
  public Map<SootClass, Set<SootMethod>> objectProviders = new HashMap<>();
  public Map<Type, Integer> priorities = new HashMap<>();

  default Map<SootClass, SootClass> getEntryPointClasses(
      Hierarchy classHierarchy,
      Set<String> epClassSignatures,
      Set<String> epMethodAnnotationSignatures,
      Set<String> objectProviderAnnotationSignatures) {
    Map<SootClass, SootClass> ret = new HashMap<>();
    for (SootClass c : classHierarchy.getApplicationClasses()) {
      VisibilityAnnotationTag ctag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
      if (ctag != null) {
        for (AnnotationTag t : ctag.getAnnotations()) {
          String type = t.getType().substring(1).replace("/", ".").replace(";", "");
          if (epClassSignatures.contains(type)) {
            c.addTag(aTag);
            ret.put(c, classHierarchy.getClass(type));
            break;
          }
        }
      }
      for (SootMethod m : c.getMethods()) {
        VisibilityAnnotationTag mtag =
            (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
        if (mtag != null) {
          for (AnnotationTag t : mtag.getAnnotations()) {
            String type = t.getType().substring(1).replace("/", ".").replace(";", "");
            if (epMethodAnnotationSignatures.contains(type)) {
              Hierarchy.v().addAnnotatedApplicationMethods(m);
            }
            if (objectProviderAnnotationSignatures.contains(type)) {
              String returnType = m.getReturnType().toString();
              SootClass returnClass = Scene.v().getSootClass(returnType);
              Set<SootMethod> providers =
                  objectProviders.getOrDefault(returnClass, new HashSet<>());
              providers.add(m);
              objectProviders.put(returnClass, providers);
              int priority = priorities.getOrDefault(returnClass.getType(), 0);
              priorities.put(returnClass.getType(), priority);
              List<Type> parameters = m.getParameterTypes();
              // the return type of the provider method is dependent on the parameters of the
              // providers,
              // thus, the paraType has higher priorities.
              for (Type paraType : parameters) {
                int p = priority + 1;
                priorities.put(paraType, p);
              }
            }
          }
        }
      }
    }
    return ret;
  }
}
