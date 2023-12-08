import java.util.List;
import java.util.ArrayList;

public class BugFix {
  protected String sha;
  protected String id;
  protected List<BugFile> collection;

  public BugFix(String sha, String id, List<BugFile> collection) {
    this.sha = sha;
    this.id = id;
    this.collection = collection;
  }

  public boolean isEmpty() {
    return collection.isEmpty();
  }

  public boolean applyDiffSet(DiffSet diffSet, boolean noRemove) {
    for (Diff d : diffSet.getDiffSet()) {
      applyDiff(d, noRemove);
      if (isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public void applyDiff(Diff diff, boolean noRemove) {
    List<BugFile> toRemove = new ArrayList<BugFile>();
    for (BugFile bf : collection) {
      if (bf.file_equals(diff)) {
        bf.applyDiff(diff, noRemove);
        if (bf.isEmpty()) {
          toRemove.add(bf);
        }
      }
    }
    collection.removeAll(toRemove);
  }

  public String getSha() {
    return sha;
  }

  public String getId() {
    return id;
  }

  public void sedId(String id) {
    this.id = id;
  }

  private String printCollection() {
    String ret = "";
    for (BugFile bf : collection) {
      ret += bf + ";";
    }
    return ret.substring(0, Math.max(0, ret.length()-1));
  }

  @Override
  public String toString() {
    String collec = collection.toString();
    if (collec.length() >= 2) {
      collec = "{" + collec.substring(1, collec.length()-1) + "}";
    }
    return "\"" + id + "\": " + collec;
  }

  @Override
  public BugFix clone() {
    List<BugFile> new_collection = new ArrayList<BugFile>();
    for (BugFile bf : this.collection) {
      new_collection.add(bf.clone());
    }
    return new BugFix(this.sha, this.id, new_collection);
  }
}
