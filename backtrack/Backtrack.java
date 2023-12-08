import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;

public class Backtrack {

  public static void backtrack_diffs(String shafile, String diff_dir, String bugFix_shafile) {
    try {
      List<String> shas = Files.readAllLines(Paths.get(shafile), StandardCharsets.UTF_8);
      //shas.remove(shas.size()-1);//Last sha doesn't have a diff
      List<String> bugFix_shas = Files.readAllLines(Paths.get(bugFix_shafile),
          StandardCharsets.UTF_8);
      List<DiffSet> all_diffs = new ArrayList<DiffSet>();
      for (String sha : shas) {
        String content = new String(Files.readAllBytes(Paths.get(diff_dir, sha+".diff")),
            StandardCharsets.UTF_8);
        if (bugFix_shas.contains(sha)) {
          all_diffs.add(new DiffSet(content, sha, (bugFix_shas.indexOf(sha)+1)+""));
        } else {
          all_diffs.add(new DiffSet(content, sha));
        }
      }
      backtrack_diff_bugs(shas, bugFix_shas, all_diffs);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void backtrack_diff_bugs(List<String> shas, List<String> bugFix_shas,
      List<DiffSet> all_diffs) {
    int id = 0;
    System.out.println("[");
    for (String bugFix_sha : bugFix_shas) {
      int index;
      for (index = 0; index < all_diffs.size(); index++) {
        if (all_diffs.get(index).getSha().equals(bugFix_sha)) {
          break;
        }
      }
      if (index >= all_diffs.size() ||
          !all_diffs.get(index).getSha().equals(bugFix_sha)) {
        throw new NoSuchElementException("Could not find diff with SHA: "+bugFix_sha);
      }
      backtrack(all_diffs, all_diffs.get(index), bugFix_shas, null);
      if (id < bugFix_shas.size()-1) {
        System.out.println(",");
      }
      id++;
    }
    System.out.println("]");
  }

  public static void backtrack(String shafile, String diff_dir, String bugsFile) {
    try {
      List<String> shas = Files.readAllLines(Paths.get(shafile), StandardCharsets.UTF_8);
      //shas.remove(shas.size()-1);//Last sha doesn't have a diff
      List<BugFix> bugFixes = new ArrayList<BugFix>();
      List<String> bugShas = new ArrayList<String>();
      List<String> bugIds = new ArrayList<String>();

      BufferedReader bugReader = new BufferedReader(new FileReader(bugsFile));
      String line = bugReader.readLine();
      String[] columns = line.split(",");
      String project = columns[0];
      //System.out.println("Project: "+project);
      int bugs = Integer.parseInt(columns[1]);
      line = bugReader.readLine();
      columns = line.split(",");
      for (int i = 0; i < bugs; i++) {
        int bugId = Integer.parseInt(columns[0]);
        assert(bugId == i+1);
        String sha = columns[1];
        List<BugFile> bugFiles = new ArrayList<BugFile>();
        line = bugReader.readLine();
        if (line == null) {
          System.err.println("Invalid bug file");
          return;
        }
        columns = line.split(",");
        while (columns[0].equals("#")) {
          BugFile bf = null;
          if (columns.length > 2) {
            bf = new BugFile(columns[1],
                Arrays.stream(columns[2].split(":")).mapToInt(Integer::parseInt).toArray());
          } else {
            bf = new BugFile(columns[1], new int[0]);
          }
          bugFiles.add(bf);
          line = bugReader.readLine();
          if (line == null) break;
          columns = line.split(",");
        }
        bugFixes.add(new BugFix(sha, bugId+"", bugFiles));
        bugShas.add(sha);
        bugIds.add(bugId+"");
      }

      List<DiffSet> all_diffs = new ArrayList<DiffSet>();
      for (String sha : shas) {
        String content = new String(Files.readAllBytes(Paths.get(diff_dir, sha+".diff")),
            StandardCharsets.UTF_8);
        int index = bugShas.indexOf(sha);
        if (index != -1) {
          all_diffs.add(new DiffSet(content, sha, bugIds.get(index)));
        } else {
          all_diffs.add(new DiffSet(content, sha));
        }
      }

      HashMap<String,DiffSet> start_diffs = new HashMap<String,DiffSet>();
      for (String bugId : bugIds) {
        try {
          String content = new String(Files.readAllBytes(Paths.get(diff_dir,
                  "start-"+bugId+".diff")),
              StandardCharsets.UTF_8);
          start_diffs.put(bugId, new DiffSet(content, bugId));
        } catch (IOException e) {
          start_diffs.put(bugId, new DiffSet("", bugId));
        }
      }
      //System.out.println(shas);
      backtrack_bugs(shas, bugFixes, all_diffs, bugShas, start_diffs);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void backtrack_bugs(List<String> shas, List<BugFix> bugFixes,
      List<DiffSet> all_diffs, List<String> bugShas, HashMap<String,DiffSet> start_diffs) {
    int i  = 0;
    System.out.println("[");
    for (BugFix bugFix : bugFixes) {
      backtrack(all_diffs, bugFix, bugShas, start_diffs.get(bugFix.getId()));
      if (i < bugFixes.size()-1) {
        System.out.println(",");
      }
      i++;
    }
    System.out.println("]");
  }

  //<-------------------- Helper methods ---------------------->

  private static void backtrack(List<DiffSet> diffs, BugFix bugFix, List<String> bugShas,
      DiffSet start_diff) {
    System.out.println("{");
    System.out.println("\"bug\": {"+bugFix+"}");
    if (start_diff != null && !bugFix.applyDiffSet(start_diff, true)) {
      System.out.println(",\""+start_diff.getSha()+"\": \"failed\"");
    } else {
      boolean found = false;
      for (DiffSet diff : diffs) {
        if (!found) {
          if (diff.getSha().equals(bugFix.getSha())) {
            found = true;
          }
          continue;
        } else {
          //System.out.println("Applying diff "+diff.toChunkString());
          boolean passed = bugFix.applyDiffSet(diff, false);
          if (passed) {
            if (bugShas.contains(diff.getSha())) {
              System.out.println(",\""+diff.getId()+"\": {"+bugFix+"}");
              //System.out.println(bugFix);
            }
          } else {
            System.out.println(",\""+diff.getSha()+"\": \"failed\"");
            break;
          }
        }
      }
    }
    System.out.println("}");
  }

  /*private static List<BugFile> processCsv(String content) {
    List<BugFile> bugs = new ArrayList<BugFile>();
    String[] lines = content.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String[] line = lines[i].split(",");
      int bugId = Integer.parseInt(line[0]);
      int
    }
    return bugs;
  }*/

  @SuppressWarnings( "rawtypes" )
  public static void main(String[] args) {
    HashMap<String, TypeKey> keys = new HashMap<String, TypeKey>();
    keys.put("include", new TypeKey<String>("include", String.class, true));
    keys.put("exclude", new TypeKey<String>("exclude", String.class, true));
    keys.put("file-type", new TypeKey<String>("file-type", String.class, true));
    CommandLine cmd = new CommandLine(keys);
    args = cmd.getOpts(args);
    if (args.length > 1) {
      if (cmd.contains("file-type")) {
        String fileType = cmd.get("file-type");
        DiffSet.EXT = (fileType.startsWith(".") ? "" : ".")+fileType;
      }
      while (cmd.contains("include")) {
        if (DiffSet.includes == null) {
          DiffSet.includes = new ArrayList<String>();
        }
        DiffSet.includes.add(cmd.get("include"));
      }
      while (cmd.contains("exclude")) {
        if (DiffSet.excludes == null) {
          DiffSet.excludes = new ArrayList<String>();
        }
        DiffSet.excludes.add(cmd.get("exclude"));
      }
      if (args[0].equals("lines")) {
        Backtrack.backtrack(args[1], args[2], args[3]);
      } else if (args[0].equals("diffs")) {
        Backtrack.backtrack_diffs(args[1], args[2], args[3]);
      } else {
        printUsage();
      }
    } else {
      printUsage();
    }
  }

  public static void printUsage() {
    System.out.println("USAGE: java Backtrack [--file-type f --include i --exclude e] <mode> ...");
    System.out.println("Where <mode> is one of:");
    System.out.println("  1. \"lines\" : java Backtrack lines <shafile> <diff dir> <bugFile>");
    System.out.println("  2. \"diffs\" : java Backtrack diffs <shafile> <diff dir> <bug shafile>");
  }
}
