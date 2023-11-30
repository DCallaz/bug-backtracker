import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Iterator;

public class Diff extends BugFile implements Iterable<Chunk> {
  private List<Chunk> touched;
  private String renamedTo;

  public Diff(String file, String rename, String diff) {
    this(file, diff);
    this.renamedTo = rename;
  }

  public Diff(String file, String diff) {
    this.file = file;
    touched = new ArrayList<Chunk>();
    processDiff(diff);
    for (Chunk c : touched) {
      // Add line where things were removed if only removals
      if (c.getNewStart() == c.getNewEnd()) {
        lines.add(c.getNewStart());
      } else {
        for (int line = c.getNewStart(); line < c.getNewEnd(); line++) {
          lines.add(line);
        }
      }
    }
  }

  public Diff(String file) {
    this.file = file;
    touched = null;
    lines = null;
  }

  public void processDiff(String diff) {
    int adds = 0, rems = 0;
    int orig = 0, new_ = 0;

    int comps = 1;
    for (String line : diff.split("\n")) {
      String s = (line.length() > comps) ? line.substring(0, comps) : line+" ";
      Matcher tmp = Pattern.compile("(?:([-+@\\\\])| )+").matcher(s);
      tmp.matches();
      char c = (tmp.group(1) == null) ? ' ' : tmp.group(1).charAt(0);
      switch (c) {
        case '@':
          Pattern p = Pattern.compile("@(@{1,}) (\\-(\\d+)(?:,\\d+)?) (?:\\-(?:\\d+)(?:,\\d+)? )*\\+(\\d+)(?:,\\d+)? @\\1");
          Matcher m = p.matcher(line);
          if (m.find()) {
            orig = Integer.parseInt(m.group(4));
            new_ = Integer.parseInt(m.group(3));
            comps = m.group(1).length();
          } else {
            throw new DiffException("Malformed range information: "+line);
          }
          break;
        case '-':
          if (comps > 1) {
            if (!s.matches("^\\-+$")) {
              continue;
              //throw new DiffException("Combined diffs do not agree: "+line);
            }
          }
          adds++;
          break;
        case '+':
          if (comps > 1) {
            if (!s.matches("^\\++$")) {
              continue;
              //throw new DiffException("Combined diffs do not agree: "+line);
            }
          }
          rems++;
          break;
        case '\\':
          continue;
        default:
          if (adds+rems > 0) {
            touched.add(new Chunk(orig, new_, adds, rems));
            new_ += adds + 1;
            orig += rems + 1;
            adds = 0;
            rems = 0;
          } else {
            orig++;
            new_++;
          }
      }
    }
    if (adds+rems > 0) {
      touched.add(new Chunk(orig, new_, adds, rems));
    }
  }

  public boolean isRenamed() {
    return renamedTo != null;
  }

  public String getRename() {
    return renamedTo;
  }

  public boolean isNull() {
    return touched == null;
  }

  public Iterator<Chunk> iterator() {
    return touched.iterator();
  }

  public String getFile() {
    return this.file;
  }

  public String toChunkString() {
    return file + ": " + touched;
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      try {
        String content = new String(Files.readAllBytes(Paths.get(args[0])),
            StandardCharsets.UTF_8);
        Diff d = new Diff(args[0], content);
        System.out.println(d);
      } catch(IOException e) {}
    }
  }
}
