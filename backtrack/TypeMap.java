import java.util.Map;
import java.util.HashMap;

/**
 * A simplified version of java's map that allows for types using type keys.
 */
public class TypeMap {
  private HashMap<String, Object> map;

  public TypeMap() {
    map = new HashMap<String, Object>();
  }

  @SuppressWarnings( "unchecked" )
  public <T> T get( TypeKey<T> key ) {
    return (T) map.get( key.name() );
  }

  public <T> void put( TypeKey<T> key, T value ) {
    map.put( key.name(), value );
  }

  public boolean contains( String key ) {
    return map.containsKey(key);
  }
}
