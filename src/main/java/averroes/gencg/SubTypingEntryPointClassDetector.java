package averroes.gencg;

import averroes.options.AverroesOptions;
import averroes.soot.Hierarchy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import soot.SootClass;

/** @author Linghui Luo */
public interface SubTypingEntryPointClassDetector extends ClassesDetector {

  public EntryPointTypeTag sTag = new EntryPointTypeTag(EntryPointTypeTag.SUBTYPING);

  default Map<SootClass, SootClass> getEntryPointClasses(
      Hierarchy classHierarchy, Set<String> classSignatures) {
    Map<SootClass, SootClass> ret = new HashMap<>();
    for (String androidClass : classSignatures) {
      SootClass epClass = classHierarchy.getClass(androidClass);
      if (epClass != null) {
        LinkedHashSet<SootClass> entryPointClasses = classHierarchy.getSubclassesOf(epClass);
        entryPointClasses.forEach(
            c -> {
              if (AverroesOptions.isLoadedApplicationClass(c.getName())) {
                c.addTag(sTag);
                ret.put(c, epClass);
              }
            });
      }
    }
    return ret;
  }
}
