package averroes.gencg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

/** @author Linghui Luo */
public interface AnnotationEntryPointMethodDetector extends EntryPointMethodsDetector {

  default List<SootMethod> getEntryPointMethods(SootClass cl, Set<String> classSignatures) {
    List<SootMethod> ret = new ArrayList<>();
    for (SootMethod m : cl.getMethods()) {
      VisibilityAnnotationTag mTag = (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
      if (mTag != null)
        for (AnnotationTag t : mTag.getAnnotations()) {
          String type = t.getType().substring(1).replace("/", ".").replace(";", "");
          if (classSignatures.contains(type)) {
            ret.add(m);
            break;
          }
        }
    }
    return ret;
  }
}
