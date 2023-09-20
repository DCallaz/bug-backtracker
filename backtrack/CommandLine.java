import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.security.InvalidParameterException;

@SuppressWarnings( "rawtypes" )
public class CommandLine {
  private TypeMap opts;
  public Map<String, TypeKey> keys;

  public CommandLine(Map<String, TypeKey> keys) {
    opts = new TypeMap();
    this.keys = keys;
  }

  /**
   * Given command line arguments, this method will retrive the options and
   * return the rest of the arguments.
   */
  @SuppressWarnings( "unchecked" )
  public String[] getOpts(String[] args) {
    int i = 0;
    List<String> new_args = new ArrayList<String>();
    while (i < args.length) {
      String opt = args[i++];
      if (opt.startsWith("-")) {
        while (opt.startsWith("-")) {
          opt = opt.substring(1);
        }
        TypeKey key = this.keys.get(opt);
        if (key.isArg()) {
          String arg = args[i++];
          Class type = key.type();
          // Check the type and convert accordingly
          if (type.isAssignableFrom(String.class)) {
            opts.put(key, arg);
          } else if (type.isAssignableFrom(Integer.class)) {
            opts.put(key, Integer.parseInt(arg));
          } else if (type.isAssignableFrom(Boolean.class)) {
            opts.put(key, Boolean.parseBoolean(arg));
          } else if (type.isAssignableFrom(Character.class)) {
            if (arg.length() > 1) {
              throw new InvalidParameterException("\""+arg+"\" is not a character");
            }
            opts.put(key, arg.charAt(0));
          }
        } else {
          opts.put(key, true);
        }
      } else {
        new_args.add(opt);
      }
    }
    return new_args.toArray(new String[0]);
  }

  @SuppressWarnings( "unchecked" )
  public <T> T get(String name) {
    TypeKey<T> key = keys.get(name);
    return opts.get(key);
  }

  public boolean contains(String key) {
    return opts.contains(key);
  }
}
