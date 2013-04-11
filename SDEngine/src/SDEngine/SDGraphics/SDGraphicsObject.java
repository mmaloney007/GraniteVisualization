package SDEngine.SDGraphics;

import org.lwjgl.util.vector.Vector3f;

import SDEngine.*;
import static SDEngine.SDUtils.*;

public class SDGraphicsObject extends SDObject {
  private Vector3f _pos;
  private int _attrib;

  public SDGraphicsObject() {
    _pos = new Vector3f(0, 0, 0);
    _attrib = 0;
  }

  public void render() {

  }

  public Vector3f pos() {
    return _pos;
  }

  public void setPosition(Vector3f pos) {
    setVector(_pos, pos);
  }

  public int getAttributes() {
    return _attrib;
  }

  public void setAttributes(int attrib) {
    _attrib = attrib;
  }
}
