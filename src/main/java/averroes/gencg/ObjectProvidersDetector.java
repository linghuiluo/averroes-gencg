package averroes.gencg;

import java.util.Map;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;

/** @author Linghui Luo */
public interface ObjectProvidersDetector {
  /**
   * @return a map with key to be the return type of provider methods, value to be the set of
   *     provider methods
   */
  public Map<SootClass, Set<SootMethod>> getObjectProviders();
}
