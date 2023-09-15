import java.util.ArrayList;
import java.util.List;

public class Chunk {
  private int orig_start, new_start;
  private int adds, rems;

  public Chunk(int orig_start, int new_start, int adds, int rems) {
    this.orig_start = orig_start;
    this.new_start = new_start;
    this.adds = adds;
    this.rems = rems;
  }

  public int getOrigStart() {
    return orig_start;
  }

  public int getNewStart() {
    return new_start;
  }

  public int getAdds() {
    return adds;
  }

  public int getRemoves() {
    return rems;
  }

  public int getNewEnd() {
    return new_start + adds;
  }

  public int getOrigEnd() {
    return orig_start + rems;
  }

  @Override
  public String toString() {
    return "@("+orig_start+","+new_start+"):-"+rems+",+"+adds;
  }
}
