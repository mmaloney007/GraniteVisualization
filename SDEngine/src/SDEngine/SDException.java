package SDEngine;

public class SDException extends Exception {

  public SDException() {

  }

  public SDException(String msg) {
    super(msg);
  }

  public SDException(Throwable msg) {
    super(msg);
  }

  public SDException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
