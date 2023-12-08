import java.util.List;

public class Node {
  private String sha;
  private DiffSet diff;
  private Node[] parents;
  private Node ancestor;

  public Node(String sha, DiffSet diff, Node parent) {
    this.sha = sha;
    this.diff = diff;
    if (parent == null) {
      parents = new Node[0];
    } else {
      this.parents = new Node[] {parent};
    }
  }

  public Node(String sha, DiffSet diff, Node[] parents, Node ancestor) {
    this.sha = sha;
    this.diff = diff;
    boolean not_empty = false;
    for (Node p : parents) {
      if (p != null) {
        not_empty = true;
      }
    }
    if (not_empty) {
      this.parents = parents;
    } else {
      this.parents = new Node[0];
    }
    this.ancestor = ancestor;
  }

  public String sha() {
    return sha;
  }

  public Node ancestor() {
    return ancestor;
  }

  public boolean isMerge() {
    return numParents() > 1;
  }

  public int numParents() {
    return parents.length;
  }

  public Node parent(int i) {
    if (i > numParents()-1) {
      throw new ArrayIndexOutOfBoundsException("No parent number "+i+" for node "+sha);
    }
    return parents[i];
  }

  public DiffSet diff() {
    return diff;
  }
}
