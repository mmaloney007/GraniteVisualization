package SDEngine.SDGraphics;

import static SDEngine.SDLog.err;
import static SDEngine.SDLog.out;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL;
import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_CURRENT_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import SDEngine.SDException;
import SDEngine.SDUtils;

public class SDSprite extends SDGraphicsObject {

  protected SDTexture _texture;
  protected SDCamera _camera;
  protected Vector3f _vertex[], _rotation;
  protected Vector2f _texCoords[];
  protected Color _col;
  protected float _width, _height;
  protected int _listID, _repeat = GL_REPEAT;
  protected boolean _billboard = false, _compiled = false, _compiling = false;

  protected SDQuaternion _rotationQ;

  public SDSprite(Vector3f pos, SDTexture texture, SDCamera camera,
                  Color colorMod) throws SDException {
    super();

    if (texture == null)
      throw new SDException();

    _texture = texture;
    makeSprite(pos, camera, colorMod, _texture.getImageWidth(),
        _texture.getImageHeight());
  }

  public SDSprite(Vector3f pos, String textureFile, int colorKey[], int filter,
                  SDCamera camera, Color colorMod) throws SDException {
    super();

    try {
      _texture = new SDTexture(textureFile, colorKey, filter);
    } catch (SDException e) {
      err("failed to create a sprite from file: " + textureFile);
      throw new SDException();
    }

    makeSprite(pos, camera, colorMod, _texture.getImageWidth(),
        _texture.getImageHeight());
  }

  public SDSprite(Vector3f pos, SDCamera camera, Color colorMod, float width,
                  float height) throws SDException {
    super();

    if (width < 0 || height < 0)
      throw new SDException("SDSprite width=" + width + ", height=" + height);

    _texture = null;
    makeSprite(pos, camera, colorMod, width, height);
  }

  public SDSprite(Vector3f pos, String textureFile) throws Exception {
    this(pos, textureFile, null, GL_LINEAR, null, null);
  }

  public SDSprite(Vector3f pos, String textureFile, int colorKey[])
      throws Exception {
    this(pos, textureFile, colorKey, GL_LINEAR, null, null);
  }

  public SDSprite(Vector3f pos, String textureFile, int filter)
      throws Exception {
    this(pos, textureFile, null, filter, null, null);
  }

  private void makeSprite(Vector3f pos, SDCamera camera, Color colorMod,
                          float width, float height) {
    setPosition(pos);
    setAttributes(GL_DEPTH_BUFFER_BIT | GL_CURRENT_BIT | GL_COLOR_BUFFER_BIT);

    _camera = camera;
    _col = colorMod;
    _width = width;
    _height = height;
    _vertex = new Vector3f[4];
    _texCoords = new Vector2f[4];
    _rotation = new Vector3f(0, 0, 0);
    _rotationQ = new SDQuaternion();

    for (int i = 0; i < _vertex.length; i++)
      _vertex[i] = new Vector3f();

    _texCoords[0] = new Vector2f(0, 1f);
    _texCoords[1] = new Vector2f(0, 0);
    _texCoords[2] = new Vector2f(1f, 1f);
    _texCoords[3] = new Vector2f(1f, 0);

    _vertex[0].x = -(_width / 2);
    _vertex[0].y = -(_height / 2);
    _vertex[0].z = 0;

    _vertex[1].x = _vertex[0].x;
    _vertex[1].y = +(_height / 2);
    _vertex[1].z = 0;

    _vertex[2].x = +(_width / 2);
    _vertex[2].y = _vertex[0].y;
    _vertex[2].z = 0;

    _vertex[3].x = _vertex[2].x;
    _vertex[3].y = _vertex[1].y;
    _vertex[3].z = 0;

    out(toString());
  }

  @Override
  public boolean init() {
    return true;
  }

  @Override
  public void update() {

    if (billboard()) {
      setYRotation(_camera.yaw());
    }
  }

  @Override
  public void render() {
    /*
     * if (compiled()) { glCallList(_listID); return; }
     */

    glEnable(GL_COLOR_MATERIAL);

    if (_col != null)
      glColor4d(_col.r, _col.g, _col.b, _col.a);
    else
      glColor4d(1, 1, 1, 1);

    if (_texture != null) {

      glEnable(GL_TEXTURE_2D);
      _texture.bind();
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, _repeat);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, _repeat);

    } else
      glDisable(GL_TEXTURE_2D);

    glTranslatef(pos().x, pos().y, pos().z);

    glMultMatrix(SDQuaternion.matrixFromQuaternion(_rotationQ));

    glBegin(GL_TRIANGLE_STRIP);
    {
      for (int i = 0; i < _vertex.length; i++) {
        if (_texture != null) {
          glTexCoord2f(_texCoords[i].x, _texCoords[i].y);
        }

        glVertex3f(_vertex[i].x, _vertex[i].y, _vertex[i].z);
      }
    }
    glEnd();
  }

  @Override
  public void destroy() {
    _col = null;
    _rotation = null;
    if (_texture != null) {
      _texture.destroy();
      _texture = null;
    }
  }

  public boolean compiled() {
    return _compiled;
  }

  public void compile() {
    _compiling = true;

    _listID = glGenLists(1);
    glNewList(_listID, GL_COMPILE);
    render();
    glEndList();

    _compiling = false;
    _compiled = true;
  }

  public void setTexCoords(Vector2f coords[]) {
    if (coords.length > _texCoords.length) {
      err("too many texture coordinates: " + coords.length
          + ", should have been <= " + _texCoords.length);
      return;
    }

    for (int i = 0; i < _texCoords.length; i++) {
      _texCoords[i].x = coords[i].x;
      _texCoords[i].y = coords[i].y;
    }
  }

  public void setSize(float width, float height) {
    _width = width;
    _height = height;
  }

  public void setWidth(float width) {
    setSize(width, height());
  }

  public void setHeight(float height) {
    setSize(width(), height);
  }

  public float width() {
    return _width;
  }

  public float height() {
    return _height;
  }

  public void setBillboard(boolean billboard) {
    if (billboard && _camera == null) {
      err("cannot billboard without a camera");
      return;
    }
    _billboard = billboard;
  }

  public boolean billboard() {
    return _billboard;
  }

  public void setRepeat(boolean on) {
    _repeat = on ? GL_REPEAT : GL_CLAMP;
  }

  public boolean repeat() {
    return (_repeat == GL_REPEAT) ? true : false;
  }

  @Override
  public void setPosition(Vector3f pos) {
    super.setPosition(pos);
  }

  public void translate(Vector3f move) {
    setPosition(pos().translate(move.x, move.y, move.z));
  }

  public float getXRotation() {
    return _rotation.x;
  }

  public float getYRotation() {
    return _rotation.y;
  }

  public float getZRotation() {
    return _rotation.z;
  }

  public void setXRotation(float angle) {
    _rotation.x = angle;
    _rotationQ.setXRotation(angle);
  }

  public void setYRotation(float angle) {
    _rotation.y = angle;
    _rotationQ.setYRotation(angle);
  }

  public void setZRotation(float angle) {
    _rotation.z = angle;
    _rotationQ.setZRotation(angle);
  }

  public void rotateX(float angle) {
    _rotation.x = SDUtils.fixAngle(_rotation.x + angle);
    _rotationQ.rotateX(angle);
  }

  public void rotateY(float angle) {
    _rotation.y = SDUtils.fixAngle(_rotation.y + angle);
    _rotationQ.rotateY(angle);
  }

  public void rotateZ(float angle) {
    _rotation.z = SDUtils.fixAngle(_rotation.z + angle);
    _rotationQ.rotateZ(angle);
  }

  @Override
  public String toString() {
    if (_texture != null) {
      return "SDSprite(file=" + _texture._filePath + ", x=" + pos().x + ", y="
          + pos().y + ", z=" + pos().z + ", width=" + _width + ", height="
          + _height + ")";
    } else {
      return "SDSprite(x=" + pos().x + ", y=" + pos().y + ", z=" + pos().z
          + ", width=" + _width + ", height=" + _height + ")";
    }
  }
}
