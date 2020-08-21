package averroes;

import java.util.Collections;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

public class Webapp {

  public static void main(String... args) {
    G.reset();
    String cl = "E:\\onlineshop.jar";
    Options.v().set_process_dir(Collections.singletonList(cl));
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_ignore_resolving_levels(true);
    Scene.v().loadNecessaryClasses();
    for (SootClass c : Scene.v().getApplicationClasses()) {
      VisibilityAnnotationTag ctag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
      if (ctag != null) {
        // System.out.println(c.getName());
        for (AnnotationTag t : ctag.getAnnotations()) {
          // System.out.print(t.getType().replace("/", "."));
        }
        // System.out.println();
      }
      for (SootMethod m : c.getMethods()) {

        VisibilityAnnotationTag tag = (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
        if (tag != null) {
          // System.out.println(m.getSignature());
          for (AnnotationTag t : tag.getAnnotations()) {
            System.out.print(t.getType().replace("/", "."));
          }
          System.out.println();
        }
      }
    }
  }
}
