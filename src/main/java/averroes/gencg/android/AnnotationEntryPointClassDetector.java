package averroes.gencg.android;

import averroes.soot.Hierarchy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import soot.SootClass;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

/** @author Linghui Luo */
public interface AnnotationEntryPointClassDetector extends EntryPointClassesDetector {

  public EntryPointTypeTag aTag = new EntryPointTypeTag(EntryPointTypeTag.ANNOTATION);

  default Map<SootClass, SootClass> getEntryPointClasses(
      Hierarchy classHierarchy, Set<String> classSignatures) {
    Map<SootClass, SootClass> ret = new HashMap<>();
    for (SootClass c : classHierarchy.getApplicationClasses()) {
      VisibilityAnnotationTag ctag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
      if (ctag != null) {
        for (AnnotationTag t : ctag.getAnnotations()) {
          String type = t.getType().substring(1).replace("/", ".").replace(";", "");
          if (classSignatures.contains(type)) {
            c.addTag(aTag);
            ret.put(c, classHierarchy.getClass(type));
            break;
          }
        }
      }
    }
    return ret;
  }
}
