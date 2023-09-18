import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DiffSet extends BugFix {
  public static String EXT = ".java";

  public DiffSet(String full_diff, String sha, String src) {
    super(sha, sha, processDiff(sha, full_diff, src));
    //System.out.println(this);
  }

  public DiffSet(String full_diff, String sha, String id, String src) {
    super(sha, id, processDiff(sha, full_diff, src));
  }

  private static List<BugFile> processDiff(String sha, String full_diff, String src) {
    //System.out.println("sha: "+sha);
    List<BugFile> collection = new ArrayList<BugFile>();
    String[] full = full_diff.split("\n");
    if (full.length == 0 || full[0].equals("")) {
      return collection;
    }
    AtomicInteger i = new AtomicInteger(0);
outer:
    while (i.get() < full.length) {
      String line = full[i.getAndIncrement()];
      //System.out.println(line);
      assert(line.startsWith("diff"));
      line = full[i.getAndIncrement()];
      if (line.startsWith("deleted file mode")) {
        line = full[i.getAndIncrement()];
        line = full[i.getAndIncrement()];
        if (line.startsWith("Binary")) {
          skip(i, full);
        } else {
          if (!line.startsWith("---")) {
            skip(i, full);
            continue;
          }
          assert(line.startsWith("---"));
          String file = line.substring(6);
          if (file.endsWith(EXT)) {
            collection.add(new Diff(file));
          }
          skip(i, full);
        }
      } else if (line.startsWith("new file mode")) {
        skip(i, full);
      } else {
        boolean rename = false;
        while (!line.startsWith("index") && !line.startsWith("---")
            && !line.startsWith("diff")) {
          if (line.startsWith("rename")) {
            rename = true;
          }
          //System.out.println("Unknown git diff mode: \""+line+"\" skipping...");
          if (i.get() >= full.length) {
            break outer;
          }
          line = full[i.getAndIncrement()];
        }
        if (!line.startsWith("diff")) {
          if (line.startsWith("index")) {
            line = full[i.getAndIncrement()];
          }
          if (!line.startsWith("---")) {
            skip(i, full);
            continue outer;
          }
          String file = line.substring(4);
          Matcher match = Pattern.compile("(?:\\w|\\p{Punct})*"+src+"/?(\\S+)(\\s+.*)?").matcher(file);
          if (!match.matches()) { //check if in the source directory
            skip(i, full);
          } else {
            file = match.group(1);
            line = full[i.getAndIncrement()];
            assert(line.startsWith("+++"));
            String newFile = null;
            line = line.substring(4);
            match = Pattern.compile("(?:\\w|\\p{Punct})*"+src+"/?(\\S+)(\\s+.*)?").matcher(line);
            if (!match.matches()) {
              if (file.endsWith(EXT)) {
                collection.add(new Diff(file));
              }
              skip(i, full);
            } else {
              if (rename) {
                newFile = match.group(1);
              } else {
                assert(file.equals(match.group(1)));
              }

              if (!file.endsWith(EXT)) {
                //System.out.println("Skipping file: "+file);
                skip(i, full);
              } else {
                String subset = skip(i, full);
                Diff d;
                if (rename) {
                  d = new Diff(file, newFile, subset);
                } else {
                  d = new Diff(file, subset);
                }
                collection.add(d);
              }
            }
          }
        } else {
          i.getAndDecrement();
        }
      }
    }
    return collection;
  }

  /**
   * Returns a list of Diffs from this DiffSet. NOTE: this list of diffs is NOT
   * the underlying DiffSet, and therefore modifications will not reflect.
   * Please use the <code>remove(...)</code> method to remove elements from
   * this DiffSet.
   */
  public List<Diff> getDiffSet() {
    List<Diff> diffs = new ArrayList<Diff>();
    for (BugFile bf : collection) {
      diffs.add((Diff)bf);
    }
    return diffs;
  }

  public void remove(List<Diff> toRemove) {
    this.collection.removeAll(toRemove);
  }

  private static String skip(AtomicInteger i, String[] full) {
    int start = i.get();
    String subset = "";
    if (i.get() >= full.length) {
      return subset;
    }
    String line = full[i.get()];
    while (!line.startsWith("diff")) {
      subset += line+"\n";
      if (i.get()+1 < full.length) {
        line = full[i.incrementAndGet()];
      } else {
        i.incrementAndGet();
        break;
      }
    }
    return subset.substring(0, Math.max(subset.length()-1, 0));
  }

  @Override
  public String toString() {
    String collec = getDiffSet().toString();
    if (collec.length() >= 2) {
      collec = "{" + collec.substring(1, collec.length()-1) + "}";
    }
    return "\"" + id + "\": "+ collec;
  }

  public String superString() {
    return super.toString();
  }

  public String toChunkString() {
    String ret = "[";
    for (BugFile bf : collection) {
      Diff d = (Diff)bf;
      ret += d.toChunkString()+", ";
    }
    if (ret.length() > 1) {
      return ret.substring(0, ret.length()-2)+"]";
    } else {
      return "[]";
    }
  }

  public static void main(String[] args) {
    if (args.length > 1) {
      try {
        String content = new String(Files.readAllBytes(Paths.get(args[0])),
            StandardCharsets.UTF_8);
        DiffSet ds = new DiffSet(content, (args.length > 2) ? args[2] : null, args[1]);
        System.out.println(ds.superString());
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
}
