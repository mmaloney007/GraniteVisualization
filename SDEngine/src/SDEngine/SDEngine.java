package SDEngine;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glViewport;

import org.lwjgl.util.vector.Vector3f;

import SDEngine.SDGraphics.*;

import static org.lwjgl.opengl.GL11.*;

import static SDEngine.SDUtils.*;
import static SDEngine.SDLog.*;

public class SDEngine extends SDObject {
  protected SDGraphics _graphics;
  protected SDAudio _audio;
  protected SDCamera _camera;

  protected boolean _drawAxes = false, _fpsTracking = true;
  protected long _lastFrame;
  protected int _fps;
  protected long _lastFPS;
  protected long _startFPSTime;

  public SDEngine() throws SDException {
    try {

      if ((new SDLog()) == null)
        throw new SDException();

      SDUtils.loadConfigFile();
      SDGlobal.loadDebugMode();

      if ((_graphics = new SDGraphics()) == null)
        throw new SDException();
      if ((_audio = new SDAudio()) == null)
        throw new SDException();

    } catch (SDException e) {
      throw new SDException();
    }

    out("SDEngine()");
  }

  public boolean init() {
    if (!_graphics.init())
      return false;

    if (!_audio.init())
      return false;

    glViewport(0, 0, _graphics.getWidth(), _graphics.getHeight());

    if ((_camera = new SDCamera(_graphics.getWidth(), _graphics.getHeight(),
        SDGlobal._fov, SDGlobal._nearClip, SDGlobal._farClip)) == null) {
      err("failed to allocate memory");
      return false;
    }

    if (!_camera.init())
      return false;

    _camera.setPitchLock(true, 270f, 90f);

    _lastFrame = getTime();

    return true;
  }

  public void destroy() {
    SDLog.out("SDEngine() shutdown");

    _graphics.destroy();
    _graphics = null;
    _audio.destroy();
    _audio = null;

    SDLog.destroy();
  }

  public void update() {
    if (fpsTracking())
      updateFPS();

    _graphics.update();
    _audio.update();
    _camera.update();
  }

  public void render() {
    _graphics.render();

    if (drawAxes())
      renderAxes();

    if (errorCheck()) {
      destroy();
    }

    glFlush();
  }

  public boolean drawAxes() {
    return _drawAxes;
  }

  public void setAxes(boolean draw) {
    _drawAxes = draw;
  }

  private void renderAxes() {
    glColor3f(1.0f, 0.0f, 0.0f);
    glBegin(GL_LINES);
    {
      glVertex3f(-_camera.farClip() - _camera.pos().x, 0.0f, 0.0f);
      glVertex3f(_camera.farClip() + _camera.pos().x, 0.0f, 0.0f);
    }
    glEnd();
    glColor3f(0.0f, 1.0f, 0.0f);
    glBegin(GL_LINES);
    {
      glVertex3f(0.0f, -_camera.farClip() - _camera.pos().y, 0.0f);
      glVertex3f(0.0f, _camera.farClip() + _camera.pos().y, 0.0f);
    }
    glEnd();
    glColor3f(0.0f, 0.0f, 1.0f);
    glBegin(GL_LINES);
    {
      glVertex3f(0.0f, 0.0f, -_camera.farClip() - _camera.pos().z);
      glVertex3f(0.0f, 0.0f, _camera.farClip() + _camera.pos().z);
    }
    glEnd();
  }

  public void cameraSetup(Vector3f pos, Vector3f lookat, Vector3f up) {
    _camera.setup(pos, lookat, up);
  }

  public void cameraMove(Vector3f pos) {
    _camera.move(pos);
  }

  public void cameraMove(float x, float y, float z) {
    _camera.move(new Vector3f(x, y, z));
  }

  public void cameraLookat(Vector3f pos) {
    _camera.lookat(pos);
  }

  public void cameraLookat(float x, float y, float z) {
    _camera.lookat(new Vector3f(x, y, z));
  }

  public void cameraPitch(float pitch) {
    _camera.pitch(pitch);
  }

  public void cameraYaw(float yaw) {
    _camera.yaw(yaw);
  }

  public void cameraMoveX(float dx) {
    _camera.moveX(dx);
  }

  public void cameraMoveY(float dy) {
    _camera.moveY(dy);
  }

  public void cameraMoveZ(float dz) {
    _camera.moveZ(dz);
  }

  public float cameraX() {
    return _camera.pos().x;
  }

  public float cameraY() {
    return _camera.pos().y;
  }

  public float cameraZ() {
    return _camera.pos().z;
  }

  public float cameraPitch() {
    return _camera.pitch();
  }

  public float cameraYaw() {
    return _camera.yaw();
  }

  public float cameraRoll() {
    return _camera.roll();
  }

  public void cameraReset() {
    _camera.reset();
  }

  public void setOrtho(boolean on) {
    _camera.setOrtho(on);
  }

  public boolean orthoMode() {
    return _camera.orthoMode();
  }

  /**
   * Return milliseconds passed since last frame
   */
  public int getDelta() {
    long time = SDUtils.getTime();
    int delta = (int) (time - _lastFrame);
    _lastFrame = time;

    return delta;
  }

  public boolean fpsTracking() {
    return _fpsTracking;
  }

  public void setFPSTracking(boolean on) {
    _fpsTracking = on;
  }

  /**
   * Calculate FPS
   */
  public void updateFPS() {
    long cur = SDUtils.getTime();
    if (cur - _startFPSTime > 1000) {
      _lastFPS = _fps;
      _fps = 0;
      _startFPSTime = cur;
    }
    _fps++;
  }

  public static void main(String[] argv) {

  }
}
