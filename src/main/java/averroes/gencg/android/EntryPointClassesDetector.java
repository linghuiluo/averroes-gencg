package averroes.gencg.android;

import java.util.List;
import soot.SootClass;

public interface EntryPointClassesDetector {

  public List<SootClass> getEntryPointClasses();
}
