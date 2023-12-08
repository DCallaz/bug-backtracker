import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;

public class Backtrack {

  public static void backtrack_diffs(String shafile, String diff_dir, String bugFix_shafile) {
    try {
      List<String> bugFix_shas = Files.readAllLines(Paths.get(bugFix_shafile),
          StandardCharsets.UTF_8);
      HashMap<String, Node> all_nodes = getNodes(shafile, diff_dir, bugFix_shas);
      backtrack_diff_bugs(bugFix_shas, all_nodes);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void backtrack_diff_bugs(List<String> bugFix_shas, HashMap<String,
      Node> all_nodes) {
    int id = 0;
    System.out.println("[");
    for (String bugFix_sha : bugFix_shas) {
      Node bugFix = all_nodes.get(bugFix_sha);
      if (bugFix == null) {
        throw new NoSuchElementException("Could not find diff with SHA: "+bugFix_sha);
      }
      backtrack(all_nodes, bugFix.diff(), bugFix_shas, null);
      if (id < bugFix_shas.size()-1) {
        System.out.println(",");
      }
      id++;
    }
    System.out.println("]");
  }

  public static void backtrack(String shafile, String diff_dir, String bugsFile) {
    try {
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
      HashMap<String, Node> all_nodes = getNodes(shafile, diff_dir, bugShas);

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
      backtrack_bugs(bugFixes, all_nodes, bugShas, start_diffs);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void backtrack_bugs(List<BugFix> bugFixes, HashMap<String, Node> all_nodes,
      List<String> bugShas, HashMap<String,DiffSet> start_diffs) {
    int i  = 0;
    System.out.println("[");
    for (BugFix bugFix : bugFixes) {
      backtrack(all_nodes, bugFix, bugShas, start_diffs.get(bugFix.getId()));
      if (i < bugFixes.size()-1) {
        System.out.println(",");
      }
      i++;
    }
    System.out.println("]");
  }

  //<-------------------- Helper methods ---------------------->

  private static void backtrack(HashMap<String, Node> nodes, BugFix bugFix, List<String> bugShas,
      DiffSet start_diff) {
    System.err.println();
    System.out.println("{");
    System.out.println("\"bug\": {"+bugFix+"}");
    if (start_diff != null && !bugFix.applyDiffSet(start_diff, true)) {
      System.out.println(",\""+start_diff.getSha()+"\": \"failed\"");
    } else {
      Node curr = nodes.get(bugFix.getSha());
      printed = new HashMap<String, String>();
      backtrack(curr, bugFix, bugShas, false, new ArrayList<String>());
    }
    System.out.println("}");
  }

  static HashMap<String, String> printed;
  private static BugFix backtrack(Node curr, BugFix bugFix, List<String> bugShas,
      boolean psuedo, List<String> sha_stops) {
    while (curr.numParents() > 0 && !sha_stops.contains(curr.sha())) {
      if (!psuedo) {
        System.err.println(curr.sha());
      }
      int numParents = curr.numParents();
      if (numParents > 1) {
        String ancestor = (curr.ancestor() == null) ? null : curr.ancestor().sha();
        sha_stops.add(ancestor);
        if (psuedo) {
          for (int b = 0; b < numParents; b++) {
            Node branch = curr.parent(b);
            if (branch == null) {
              continue;
            }
            boolean passed = bugFix.applyDiffSet(branch.diff(), false);
            bugFix = backtrack(branch, bugFix, bugShas, true, sha_stops);
          }
        } else {
          BugFix branchBugFix = null;
          for (int b = 0; b < numParents; b++) {
            branchBugFix = bugFix.clone();
            Node branch = curr.parent(b);
            if (branch == null) {
              continue;
            }
            for (int o = 0; o < numParents; o++) {
              Node other = curr.parent(o);
              if (o != b && other != null) {
                branchBugFix.applyDiffSet(other.diff(), false);
                branchBugFix = backtrack(other, branchBugFix, bugShas, true, sha_stops);
              }
            }
            if (branch.sha() != ancestor) {
              boolean passed = branchBugFix.applyDiffSet(branch.diff(), false);
              if (!passed) {
                System.err.println(",\""+branch.sha()+"\": \"failed\"");
                continue;
              } else if (bugShas.contains(branch.sha()) &&
                  !printed.containsKey(branch.diff().getId())) {
                System.out.println(",\""+branch.diff().getId()+"\": {"+bugFix+"}");
                printed.put(branch.diff().getId(), bugFix+"");
                //System.out.println(bugFix);
              }
              branchBugFix = backtrack(branch, branchBugFix, bugShas, false, sha_stops);
            }
          }
          bugFix = branchBugFix;
        }
        sha_stops.remove(ancestor);
        if (curr.ancestor() == null) {
          break;
        }
        curr = curr.ancestor();
      } else {
        Node parent = curr.parent(0);
        boolean passed = bugFix.applyDiffSet(parent.diff(), false);
        if (passed) {
          if (!psuedo && bugShas.contains(parent.sha()) &&
              !printed.containsKey(parent.diff().getId())) {
            System.out.println(",\""+parent.diff().getId()+"\": {"+bugFix+"}");
            printed.put(parent.diff().getId(), bugFix+"");
            //System.out.println(bugFix);
          }
        } else {
          if (!psuedo) {
            System.err.println(",\""+parent.sha()+"\": \"failed\"");
          }
          break;
        }
        curr = parent;
      }
    }
    /*
    boolean found = false;
    for (DiffSet diff : diffs) {
      if (!found) {
        if (diff.getSha().equals(bugFix.getSha())) {
          found = true;
        }
        continue;
      } else {
        //System.out.println("Applying diff "+diff.toChunkString());
        //System.out.println(diff.sha);
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
    */
    return bugFix;
  }

  private static HashMap<String, Node> getNodes(String shafile, String diff_dir,
      List<String> bugFix_shas) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(shafile), StandardCharsets.UTF_8);
    //lines.remove(lines.size()-1);//Last sha doesn't have a diff
    Collections.reverse(lines);
    HashMap<String, Node> all_nodes = new HashMap<String, Node>();
    for (String line : lines) {
      String[] line_split = line.split(" ");
      String sha = line_split[0];
      String content = new String(Files.readAllBytes(Paths.get(diff_dir, sha+".diff")),
          StandardCharsets.UTF_8);
      DiffSet diff;
      if (bugFix_shas.contains(sha)) {
        diff = new DiffSet(content, sha, (bugFix_shas.indexOf(sha)+1)+"");
      } else {
        diff = new DiffSet(content, sha);
      }
      Node n;
      if (line_split.length > 2) {
        Node[] parents = new Node[line_split.length-2];
        for (int i = 0; i < parents.length; i++) {
          parents[i] = all_nodes.get(line_split[1+i]);
        }
        n = new Node(sha, diff, parents, all_nodes.get(line_split[line_split.length-1]));
        all_nodes.put(sha, n);
      } else if (line_split.length > 1) {
        n = new Node(sha, diff, all_nodes.get(line_split[1]));
        all_nodes.put(sha, n);
      } else {
        n = new Node(sha, diff, null);
        all_nodes.put(sha, n);
      }
    }
    return all_nodes;
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
