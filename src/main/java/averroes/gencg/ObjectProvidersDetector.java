package averroes.gencg;

import java.util.Map;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;

public interface ObjectProvidersDetector {
  /** @return a map with key to be the class contains the methods that return objects. */
  public Map<SootClass, Set<SootMethod>> getObjectProviders();
}
