/*package SDEngine.SDGraphics;

import static SDEngine.SDLog.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_AMBIENT;
import static org.lwjgl.opengl.GL11.GL_DIFFUSE;
import static org.lwjgl.opengl.GL11.GL_POSITION;
import static org.lwjgl.opengl.GL11.GL_SPECULAR;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

public class SDLight extends SDGraphicsObject {
  boolean _enabled = true;
  boolean _specularEnabled = true;
  int _num = GL_LIGHT0;
  FloatBuffer _position, _ambient, _diffuse, _specular, _blank;

  public SDLight(int num, Vector3f pos, FloatBuffer ambient,
      FloatBuffer diffuse, FloatBuffer specular) {

    if (num < 0 || num > 7) {
      err("SDLight: Invalid light number specified: " + num);
      return;
    }

    _num = num + GL_LIGHT0;
    _pos = new Vector3f(pos.x, pos.y, pos.z);
    _position = (FloatBuffer) BufferUtils.createFloatBuffer(4)
        .put(new float[] { pos.x, pos.y, pos.z, 0.0f }).flip();
    _pos = new Vector3f(pos.x, pos.y, pos.z);
    _ambient = ambient;
    _diffuse = diffuse;
    _specular = specular;
    _blank = (FloatBuffer) BufferUtils.createFloatBuffer(4)
        .put(new float[] { 0.0f, 0.0f, 0.0f, 0.0f }).flip();

    out(toString());
  }

  public SDLight(int num, Vector3f pos) {
    this(num, // light number
        new Vector3f(1.0f, 1.0f, 1.0f), // pos
        (FloatBuffer) BufferUtils.createFloatBuffer(4)
            .put(new float[] { 0.2f, 0.2f, 0.2f, 1.0f }).flip(), // ambient
        (FloatBuffer) BufferUtils.createFloatBuffer(4)
            .put(new float[] { 0.70f, 0.75f, 1.45f, 1.0f }).flip(), // diffuse
        (FloatBuffer) BufferUtils.createFloatBuffer(4)
            .put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }).flip()); // specular
  }

  public SDLight(int num) {
    this(num, new Vector3f(1.0f, 1.0f, 1.0f));
  }

  public SDLight() {
    this(0);
  }

  public void update() {
    if (!_enabled) {
      glDisable(_num);
      return;
    }

    if (!_specularEnabled) {
      glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, _blank);
      glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 0);
    } else {
      glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, _specular);
      glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 10.0f);
    }

    glLight(_num, GL_POSITION, _position);
    glLight(_num, GL_AMBIENT, _ambient);
    glLight(_num, GL_DIFFUSE, _diffuse);
    glLight(_num, GL_SPECULAR, _specular);

    glEnable(_num);
  }

  public void on() {
    _enabled = true;
  }

  public void off() {
    _enabled = false;
  }

  public void specularOn() {
    _specularEnabled = true;
  }

  public void specularOff() {
    _specularEnabled = false;
  }

  public int getNum() {
    return _num;
  }

  public String toString() {
    return "SDLight(num=" + (_num - GL_LIGHT0) + ", pos=" + _pos.toString()
        + ")";
  }
}
*/