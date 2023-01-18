package averroes.gencg;

import java.util.Map;
import soot.SootClass;

/** @author Linghui Luo */
public interface EntryPointClassesDetector {

  /**
   * @return a map with key to be the application entry point class and value to be the library
   *     entry point class
   */
  public Map<SootClass, SootClass> getEntryPointClasses();
}
