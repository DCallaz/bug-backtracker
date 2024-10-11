import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class BugFile {
  protected SearchList<Integer> lines;
  protected String file;

  public BugFile() {
    lines = new SearchList<Integer>();
  }

  public BugFile(String file, int[] lines) {
    this.lines = new SearchList<Integer>();
    for (int i = 0; i < lines.length; i++) {
      this.lines.add(lines[i]);
    }
    this.file = file;
  }

  public boolean isEmpty() {
    return lines == null || lines.isEmpty();
  }

  public boolean file_equals(String file) {
    return file.equals(file);
  }

  public boolean file_equals(Diff d) {
    return file.equals(d.getFile());
  }

  public void applyDiff(Diff diff, boolean noRemove) {
    if (isEmpty() || diff.isNull()) {
      if (diff.isNull()) {
        //The above means this commit created this file.
        if (lines != null) {
          lines.clear();
        }
      }
      return;
    }
    //System.out.println("File "+file);
    if (diff.isRenamed()) {
      this.file = diff.getRename();
    }
    List<IntPair> updates = new ArrayList<IntPair>();
    List<Integer> toRemove = new ArrayList<Integer>();
    List<Integer> toAdd = new ArrayList<Integer>();
    for (Chunk c : diff) {
      //System.out.println("Chunk "+c);
      int first = lines.binarySearch(c.getOrigStart(), false);
      int last = lines.binarySearch(c.getOrigEnd(), true);
      //System.out.println("first: "+first+", last: "+last);
      if (lines.get(first) >= c.getOrigStart()) {
        if (c.getOrigEnd() >= lines.get(last) && last >= first) {
          for (int i = c.getOrigStart(); i < c.getOrigEnd(); i++) {
            if (lines.contains(i)) {
              toRemove.add(lines.indexOf(i));
              if (noRemove) {
                String line = c.getOrigLine(i);
                int max_lcs = 0;
                int line_max = -1;
                for (int j = c.getNewStart(); j < c.getNewEnd(); j++) {
                  String newLine = c.getNewLine(j);
                  int cur_lcs = LCS.lcs(line, newLine);
                  if (cur_lcs > max_lcs) {
                    max_lcs = cur_lcs;
                    line_max = j;
                  }
                }
                if (line_max != -1 && max_lcs >= 70) {
                  // System.out.println("Swapping ("+max_lcs+"):");
                  // System.out.println(line);
                  // System.out.println(c.getNewLine(line_max));
                  toAdd.add(line_max);
                }
              }
            }
          }
          //Update from first after the overlap section
          first = lines.binarySearch(c.getOrigEnd(), false);
          if (lines.get(first) >= c.getOrigStart()) {
            updates.add(new IntPair(first, c.getAdds()-c.getRemoves()));
          }
        } else {
          updates.add(new IntPair(first, c.getAdds()-c.getRemoves()));
        }
      }
    }
    for (IntPair update : updates) {
      lines.updateRange(update.start, (e) -> e + update.amount);
    }
    Collections.sort(toRemove);
    Collections.reverse(toRemove);
    for (int index : toRemove) {
      lines.remove(index);
    }
    for (int lineNum: toAdd) {
      lines.addSorted(lineNum);
    }
  }

  private static class IntPair {
    public int start;
    public int amount;
    public IntPair(int s, int a) {
      start = s;
      amount = a;
    }
  }

  private String printLines() {
    String ret = "";
    for (Integer i : lines) {
      ret += i + ":";
    }
    return ret.substring(0, Math.max(0, ret.length()-1));
  }

  @Override
  public String toString() {
    return "\""+file + "\": " + lines;
  }
}
