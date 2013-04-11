package SDEngine;

public interface SDGame {
  public boolean init();

  public void destroy();

  public void update();

  public void render();

  public int run();
}
