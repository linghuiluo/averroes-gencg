package averroes.gencg;

import java.util.List;
import soot.SootClass;
import soot.SootMethod;

public interface EntryPointMethodsDetector {

  public List<SootMethod> getEntryPointMethods(SootClass cl);
}
