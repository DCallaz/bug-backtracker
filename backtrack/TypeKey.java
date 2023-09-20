/**
 * A key used in the TypeMap to allow for typed retrieval.
 */
public class TypeKey<T> {
  private String name;
  private boolean arg;
  private Class<T> type;

  public TypeKey(String name, Class<T> type) {
    this.name = name;
    this.arg = false;
    this.type = type;
  }

  public TypeKey(String name, Class<T> type, boolean arg) {
    this.name = name;
    this.arg = arg;
    this.type = type;
  }

  public String name() {
    return name;
  }

  public boolean isArg() {
    return arg;
  }

  public Class<T> type() {
    return type;
  }
}
