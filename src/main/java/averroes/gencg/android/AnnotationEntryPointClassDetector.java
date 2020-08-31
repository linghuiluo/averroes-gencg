package averroes.gencg.android;

import averroes.soot.Hierarchy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import soot.SootClass;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

/** @author Linghui Luo */
public interface AnnotationEntryPointClassDetector extends EntryPointClassesDetector {

  public EntryPointTypeTag aTag = new EntryPointTypeTag(EntryPointTypeTag.ANNOTATION);

  default List<SootClass> getEntryPointClasses(
      Hierarchy classHierarchy, Set<String> classSignatures) {
    List<SootClass> ret = new ArrayList<>();
    for (SootClass c : classHierarchy.getApplicationClasses()) {
      VisibilityAnnotationTag ctag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
      if (ctag != null) {
        for (AnnotationTag t : ctag.getAnnotations()) {
          String type = t.getType().substring(1).replace("/", ".").replace(";", "");
          if (classSignatures.contains(type)) {
            c.addTag(aTag);
            ret.add(c);
            break;
          }
        }
      }
    }
    return ret;
  }
}
