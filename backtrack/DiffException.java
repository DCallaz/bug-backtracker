import java.lang.RuntimeException;

public class DiffException extends RuntimeException {

  private static final long serialVersionUID = 5;

  public DiffException(String message) {
    super(message);
  }
}
