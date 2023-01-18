package averroes.gencg;

import java.util.List;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;

/** @author Linghui Luo */
public class SpringEntryPointMethodDetector implements AnnotationEntryPointMethodDetector {

  private Set<String> classSignatures;

  public SpringEntryPointMethodDetector(Set<String> classSignatures) {
    this.classSignatures = classSignatures;
  }

  @Override
  public List<SootMethod> getEntryPointMethods(SootClass cl) {
    return getEntryPointMethods(cl, classSignatures);
  }
}
