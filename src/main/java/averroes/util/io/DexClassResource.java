package averroes.util.io;

import java.io.File;
import java.io.InputStream;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;

/** @author Linghui Luo */
public class DexClassResource implements Resource {

  private File apkFile;
  private DexBackedDexFile dexFile;
  private ClassDef classDef;

  public DexClassResource(File apkFile, DexBackedDexFile dexFile, ClassDef classDef) {
    this.dexFile = dexFile;
    this.classDef = classDef;
    this.apkFile = apkFile;
  }

  @Override
  public InputStream open() {
    // TODO Auto-generated method stub
    return null;
  }

  public ClassDef classDef() {
    return classDef;
  }

  public DexBackedDexFile dexFile() {
    return dexFile;
  }

  public File apkFile() {
    return apkFile;
  }
}
