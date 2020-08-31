package averroes.gencg.android;

import averroes.options.AverroesOptions;
import averroes.soot.Hierarchy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import soot.SootClass;

/** @author Linghui Luo */
public interface SubTypingEntryPointClassDetector extends EntryPointClassesDetector {

  public EntryPointTypeTag sTag = new EntryPointTypeTag(EntryPointTypeTag.SUBTYPING);

  default List<SootClass> getEntryPointClasses(
      Hierarchy classHierarchy, Set<String> classSignatures) {
    List<SootClass> ret = new ArrayList<>();
    for (String androidClass : classSignatures) {
      SootClass epClass = classHierarchy.getClass(androidClass);
      if (epClass != null) {
        LinkedHashSet<SootClass> entryPointClasses = classHierarchy.getSubclassesOf(epClass);
        entryPointClasses.forEach(
            c -> {
              if (AverroesOptions.isLoadedApplicationClass(c.getName())) {
                c.addTag(sTag);
                ret.add(c);
              }
            });
      }
    }
    return ret;
  }
}
