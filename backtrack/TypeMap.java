import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * A simplified version of java's map that allows for types using type keys.
 */
public class TypeMap {
  private HashMap<String, Object> map;

  public TypeMap() {
    map = new HashMap<String, Object>();
  }

  /**
   * Return the first value given by <code>key</code>, removing it from the map.
   *
   */
  @SuppressWarnings( "unchecked" )
  public <T> T get( TypeKey<T> key ) {
    Object obj = map.get(key.name());
    if (obj instanceof List<?>) {
      List<?> list = (List) obj;
      obj = list.remove(0);
      if (list.size() == 1) {
        map.put(key.name(), (T)list.get(0));
      }
    } else {
      map.remove(key.name());
    }
    return (T) obj;
  }

  @SuppressWarnings( "unchecked" )
  public <T> void put( TypeKey<T> key, T value ) {
    if (map.containsKey(key.name())) {
      Object curr = map.get(key.name());
      if (curr instanceof List<?>) {
        List<T> list = (List<T>) curr;
        list.add(value);
      } else {
        List<T> list = new ArrayList<T>();
        list.add((T)curr);
        list.add(value);
        map.put(key.name(), list);
      }
    } else {
      map.put( key.name(), value );
    }
  }

  public boolean contains( String key ) {
    return map.containsKey(key);
  }
}
