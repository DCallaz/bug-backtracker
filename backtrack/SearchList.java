import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class SearchList<E extends Comparable<E>> extends ArrayList<E> {
  public SearchList() {
    super();
  }

  @Override
  public boolean add(E obj) {
    if (size() == 0 || obj.compareTo(get(size()-1)) > 0) {
      super.add(obj);
      return true;
    } else {
      throw new IllegalArgumentException(obj+" not greater than "+get(size()-1));
    }
  }

  public boolean addSorted(E obj) {
    if (!super.contains(obj)) {
      if (size() == 0 || obj.compareTo(get(size()-1)) > 0) {
        super.add(obj);
      } else {
        int index = binarySearch(obj, false);
        super.add(index, obj);
      }
    }
    return true;
  }

  public int binarySearch(E obj, boolean before) {
    int start = 0;
    int end = size()-1;
    while (start != end) {
      //System.out.println(start+" "+end+" "+obj+" "+before);
      if (start > end) {
        System.err.println("binary search error");
      }
      int mid;
      if (before) {
        mid = start + (int)Math.ceil((float)(end - start)/2);
      } else {
        mid = start + ((end - start)/2);
      }
      if (get(mid).compareTo(obj) > 0) {
        if (before) {
          end = Math.max(0, mid - 1);
        } else {
          end = mid;
        }
      } else if (get(mid).compareTo(obj) < 0) {
        if (before) {
          start = mid;
        } else {
          start = Math.min(size()-1, mid+1);
        }
      } else {
        return mid;
      }
    }
    return end;
  }

  public void updateRange(int startIndex, UnaryOperator<E> op) {
    for (int i = startIndex; i < size(); i++) {
      set(i, op.apply(get(i)));
    }
  }
}
