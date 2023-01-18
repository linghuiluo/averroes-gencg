package averroes.gencg;

import java.util.List;
import soot.SootClass;
import soot.SootMethod;

/** @author Linghui Luo */
public interface EntryPointMethodsDetector {

  public List<SootMethod> getEntryPointMethods(SootClass cl);
}
