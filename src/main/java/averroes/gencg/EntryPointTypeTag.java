package averroes.gencg;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/** @author Linghui Luo */
public class EntryPointTypeTag implements Tag {
  public static String ANNOTATION = "annotation";
  public static String SUBTYPING = "subtyping";

  private String type;

  public EntryPointTypeTag(String type) {
    this.type = type;
  }

  @Override
  public String getName() {
    return "EntryPointTypeTag";
  }

  @Override
  public byte[] getValue() throws AttributeValueException {
    throw new RuntimeException("EntryPointTypeTag has no value for bytecode");
  }

  public boolean hasTypeOfAnnotation() {
    return this.type.equals(ANNOTATION);
  }

  public boolean hasTypeOfSubtyping() {
    return this.type.equals(SUBTYPING);
  }

  @Override
  public String toString() {
    return "EntryPointTypeTag [type=" + type + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EntryPointTypeTag other = (EntryPointTypeTag) obj;
    if (type == null) {
      if (other.type != null) return false;
    } else if (!type.equals(other.type)) return false;
    return true;
  }
}
