package averroes.gencg;

import java.util.Map;
import java.util.Set;
import soot.SootClass;
import soot.SootField;

/** @author Linghui Luo */
public interface CreateObjectsDetector {
  /** @return a map with key to be the class contains the fields that needs to be initialized. */
  public Map<SootClass, Set<SootField>> getCreateObjects();
}
