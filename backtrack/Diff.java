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

  public Diff(String file, String diff) {
    this.file = file;
    touched = new ArrayList<Chunk>();
    processDiff(diff);
    for (Chunk c : touched) {
      for (int line = c.getNewStart(); line < c.getNewEnd(); line++) {
        lines.add(line);
      }
    }
  }

  public Diff(String file, String rename, String diff) {
    this.file = file;
    this.renamedTo = rename;
    touched = new ArrayList<Chunk>();
    processDiff(diff);
    for (Chunk c : touched) {
      for (int line = c.getNewStart(); line < c.getNewEnd(); line++) {
        lines.add(line);
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

    for (String line : diff.split("\n")) {
      char c = (line.length() > 0) ? line.charAt(0) : '\0';
      switch (c) {
        case '@':
          Pattern p = Pattern.compile("@@ \\-(\\d+),\\d+ \\+(\\d+),\\d+ @@");
          Matcher m = p.matcher(line);
          if (m.find()) {
            orig = Integer.parseInt(m.group(1));
            new_ = Integer.parseInt(m.group(2));
          } else {
            throw new DiffException("Malformed range information: "+line);
          }
          break;
        case '-':
          rems++;
          break;
        case '+':
          adds++;
          break;
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
