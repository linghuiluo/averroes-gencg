package averroes.gencg;

import averroes.soot.Hierarchy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import soot.SootClass;
import soot.SootField;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

public interface AnnotationCreateObjectsDetector extends CreateObjectsDetector {
  default Map<SootClass, Set<SootField>> getCreateObjects(
      Hierarchy classHierarchy, Set<String> classSignatures) {
    Map<SootClass, Set<SootField>> ret = new HashMap<>();
    for (SootClass c : classHierarchy.getApplicationClasses()) {
      for (SootField f : c.getFields()) {
        VisibilityAnnotationTag ftag =
            (VisibilityAnnotationTag) f.getTag("VisibilityAnnotationTag");
        if (ftag != null) {
          for (AnnotationTag t : ftag.getAnnotations()) {
            String type = t.getType().substring(1).replace("/", ".").replace(";", "");
            if (classSignatures.contains(type)) {
              Set<SootField> fields = ret.getOrDefault(c, new HashSet<>());
              fields.add(f);
              ret.put(c, fields);
            }
          }
        }
      }
    }
    return ret;
  }
}
