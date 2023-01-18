package averroes.soot;

/** @author Linghui Luo */
public class NonFilterClasses {
  private static String[] classes = {"java.lang.Math", "java.lang.Runnable", "java.lang.Thread"};

  public static boolean isNonFilterClass(String classSig) {
    for (String c : classes) {
      if (c.equals(classSig)) return true;
    }
    return false;
  }
}
