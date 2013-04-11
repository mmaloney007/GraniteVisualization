package SDEngine.SDGraphics;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import SDEngine.SDGlobal;
import SDEngine.SDUtils;

import static org.lwjgl.util.glu.GLU.gluPerspective;
import static org.lwjgl.opengl.GL11.*;

import static SDEngine.SDLog.*;
import static SDEngine.SDUtils.*;

public class SDCamera extends SDGraphicsObject {
  protected static final short POSITION = 0, LOOKAT = 1, UP = 2, PITCH = 0,
      YAW = 1, ROLL = 2, ANGLE = 0, MIN_ANGLE_LOCK = 1, MAX_ANGLE_LOCK = 2;

  protected Vector3f _initialVector[]; // initial values specified stored for
  // reset
  protected float _angle[][]; // stores rotation and angle restrictions
  protected boolean _lock[]; // which angles are restricted

  protected Vector3f _lookat = null, _up = null;
  protected float _fov = SDGlobal._fov;
  protected float _nearClip = SDGlobal._nearClip, _farClip = SDGlobal._farClip;
  protected int _width = Display.getWidth(), _height = Display.getHeight();
  protected boolean _updateModelView = true, _updateProjection = true,
      _orthoMode = false;

  public SDCamera(Vector3f pos, Vector3f lookat, Vector3f up, int width,
      int height, float fov, float nearClip, float farClip) {
    super();

    _initialVector = new Vector3f[3];
    _angle = new float[3][3];
    _lock = new boolean[3];

    for (int i = 0; i < 3; i++) {
      _initialVector[i] = new Vector3f(0, 0, 0);
      _lock[i] = false;

      for (int j = 0; j < 3; j++)
        _angle[i][j] = 0;
    }

    setVector(_initialVector[POSITION], pos);
    setVector(_initialVector[LOOKAT], lookat);
    setVector(_initialVector[UP], up);

    _fov = fov;
    _nearClip = nearClip;
    _farClip = farClip;
    _width = width;
    _height = height;

    setup(pos, lookat, up);

    out("SDCamera(pos=" + pos.toString() + ", lookat=" + lookat.toString()
        + ", up=" + up.toString() + ", fov=" + _fov + ", nearClip=" + _nearClip
        + ", farClip=" + _farClip + ", width=" + _width + ", height=" + height
        + ", pitch=" + pitch() + ", yaw=" + yaw() + ", roll=" + roll() + ")");
  }

  public SDCamera(int width, int height, float fov, float nearClip,
      float farClip) {
    this(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0),
        width, height, fov, nearClip, farClip);
  }

  public SDCamera() {
    this(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0),
        Display.getWidth(), Display.getHeight(), SDGlobal._fov,
        SDGlobal._nearClip, SDGlobal._farClip);
  }

  public boolean init() {
    _updateModelView = _updateProjection = true;
    update();

    return true;
  }

  public void update() {
    if (_updateProjection)
      loadProjection();
    if (_updateModelView)
      loadModelview();

    _updateModelView = false;
    _updateProjection = false;
  }

  private void loadProjection() {
    glMatrixMode(GL11.GL_PROJECTION);
    glLoadIdentity();

    if (_orthoMode) {
      glOrtho(0, _width, _height, 0, _nearClip, _farClip);
    } else {
      gluPerspective(_fov, getAspectRatio(), _nearClip, _farClip);
    }
  }

  public void loadModelview() {
    glMatrixMode(GL11.GL_MODELVIEW);
    glLoadIdentity();

    if (!_orthoMode) {
      glRotatef(-pitch(), 1.0f, 0, 0);
      glRotatef(-yaw(), 0.0f, 1.0f, 0);
      glRotatef(-roll(), 0.0f, 0.0f, 1.0f);
      glTranslatef(-pos().x, -pos().y, -pos().z);
    }
  }

  public void moveX(float dx) {
    pos().x += dx;
    _lookat.x += dx;
    _updateModelView = true;
  }

  public void moveY(float dy) {
    pos().y += dy;
    _lookat.y += dy;
    _updateModelView = true;
  }

  public void moveZ(float dz) {
    pos().z += dz;
    _lookat.z += dz;
    _updateModelView = true;
  }

  public void reset() {
    for (int i = 0; i < 3; i++) {
      _angle[i][ANGLE] = 0;
      _lock[i] = false;
    }
    setup(_initialVector[POSITION], _initialVector[LOOKAT], _initialVector[UP]);
    init();
  }

  public void setOrtho(boolean on) {
    if (on)
      enterOrtho();
    else
      leaveOrtho();
  }

  public boolean orthoMode() {
    return _orthoMode;
  }

  public void enterOrtho() {
    _orthoMode = true;

    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_ENABLE_BIT);
    glPushMatrix();
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_LIGHTING);

    _updateProjection = true;
  }

  public void leaveOrtho() {
    _orthoMode = false;

    loadProjection();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
    glPopAttrib();

    _updateProjection = true;
  }

  public String toString() {
    return "SDCamera(x=" + pos().x + ", y=" + pos().y + ", z=" + pos().z
        + ", lookat.x=" + lookat().x + ", lookat.y=" + lookat().y
        + ", lookat.z=" + lookat().z + ", up.x=" + up().x + ", up.y=" + up().y
        + ", up.z=" + up().z + ", pitch=" + pitch() + ", yaw=" + yaw()
        + ", roll=" + roll() + ", width=" + width() + ", height=" + height()
        + ")";
  }

  public void setup(Vector3f pos, Vector3f lookat, Vector3f up) {
    if (!pos.equals(pos()) || !lookat.equals(lookat()) || !up.equals(up())) {
      setPosition(pos);

      if (_lookat != null)
        setVector(_lookat, lookat);
      else
        _lookat = copyVector(lookat);

      if (_up != null)
        setVector(_up, up);
      else
        _up = copyVector(up);

      _updateProjection = _updateModelView = true;
    }
  }

  public void move(Vector3f pos) {
    setup(pos, _lookat, _up);
  }

  public void lookat(Vector3f lookat) {
    setup(pos(), lookat, _up);
  }

  public Vector3f lookat() {
    return copyVector(_lookat);
  }

  public void up(Vector3f up) {
    setup(pos(), _lookat, up);
  }

  public Vector3f up() {
    return copyVector(_up);
  }

  public float yaw() {
    return _angle[YAW][ANGLE];
  }

  public float pitch() {
    return _angle[PITCH][ANGLE];
  }

  public float roll() {
    return _angle[ROLL][ANGLE];
  }

  public float width() {
    return _width;
  }

  public float height() {
    return _height;
  }

  public float farClip() {
    return _farClip;
  }

  public float nearClip() {
    return _nearClip;
  }

  private void setAngleLock(short angle, boolean on, float min, float max) {
    _lock[angle] = on;
    _angle[angle][MIN_ANGLE_LOCK] = min;
    _angle[angle][MAX_ANGLE_LOCK] = max;
  }

  public void setPitchLock(boolean on, float min, float max) {
    setAngleLock(PITCH, on, min, max);
  }

  public boolean pitchLock() {
    return _lock[PITCH];
  }

  public void setYawLock(boolean on, float min, float max) {
    setAngleLock(YAW, on, min, max);
  }

  public boolean yawLock() {
    return _lock[YAW];
  }

  public void setRollLock(boolean on, float min, float max) {
    setAngleLock(ROLL, on, min, max);
  }

  public boolean rollLock() {
    return _lock[ROLL];
  }

  private float constrainAngle(short angle, float angleToCorrect) {
    if (angleToCorrect < _angle[angle][MIN_ANGLE_LOCK]
        && angleToCorrect > _angle[angle][MAX_ANGLE_LOCK]
        && angleToCorrect > _angle[angle][ANGLE])

      angleToCorrect = _angle[angle][MAX_ANGLE_LOCK];

    else if (angleToCorrect < _angle[angle][MIN_ANGLE_LOCK]
        && angleToCorrect > _angle[angle][MAX_ANGLE_LOCK]
        && angleToCorrect < _angle[angle][ANGLE])

      angleToCorrect = _angle[angle][MIN_ANGLE_LOCK];

    return angleToCorrect;
  }

  private void setAngle(short angle, float value) {
    value += _angle[angle][ANGLE];

    if (_lock[angle])
      value = constrainAngle(angle, value);

    _angle[angle][ANGLE] = fixAngle(value);
    _updateModelView = true;
  }

  public void pitch(float pitch) {
    setAngle(PITCH, pitch);
  }

  public void yaw(float yaw) {
    setAngle(YAW, yaw);
  }

  public void roll(float roll) {
    setAngle(ROLL, roll);
  }

  public float getAspectRatio() {
    return (float) _width / (float) _height;
  }

}
