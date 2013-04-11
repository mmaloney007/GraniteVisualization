package SDEngine.SDGraphics;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

import SDEngine.SDException;
import SDEngine.SDObject;
import static SDEngine.SDLog.*;

public class SDTexture extends SDObject {

  public Texture _texture;
  public int _key[];
  public String _filePath;

  public SDTexture(String file, int colorKey[], int filter) throws SDException {
    _key = colorKey;
    _filePath = file;

    try {

      if (colorKey == null) {
        _texture = InternalTextureLoader.get().getTexture(file, false, filter);
      } else {
        _texture = InternalTextureLoader.get().getTexture(file, false, filter,
            _key);
      }
    } catch (Exception e) {
      err("SDTexture::SDTexture: Failed to load texture: " + file);
      throw new SDException();
    }
    out(toString());
  }

  public SDTexture(String file, int colorKey[]) throws SDException {
    this(file, colorKey, GL_LINEAR);
  }

  public SDTexture(String file, int filter) throws SDException {
    this(file, null, filter);
  }

  public SDTexture(String file) throws SDException {
    this(file, null, GL_LINEAR);
  }

  public void destroy() {
    _texture.release();
    _texture = null;
    _key = null;
    _filePath = null;
  }

  public void bind() {
    _texture.bind();
  }

  public int getTextureWidth() {
    return _texture.getTextureWidth();
  }

  public int getTextureHeight() {
    return _texture.getTextureHeight();
  }

  public int getImageWidth() {
    return _texture.getImageWidth();
  }

  public int getImageHeight() {
    return _texture.getImageHeight();
  }

  public String getFileFormat() {
    String parts[] = _filePath.split("\\.");
    return parts[parts.length - 1].toUpperCase();
  }

  public String toString() {
    return "SDTexture(file=" + _filePath + ")";
  }

  public void test() {
    int len = _texture.getTextureData().length;
    ByteBuffer data = BufferUtils.createByteBuffer(len);
    ByteBuffer modded = BufferUtils.createByteBuffer(data.capacity());

    bind();
    glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA8, GL_UNSIGNED_BYTE, data);

    data.rewind();
    byte cur = 0;
    for (int i = 0; i < data.capacity(); i++) {
      if (i != 0 && i % 4 == 0) {
        modded.put((byte) 0xFF);
        cur++;
      } else
        modded.put(cur);
    }
    modded.flip();

    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, _texture.getImageWidth(),
        _texture.getImageHeight(), GL_RGBA, GL_UNSIGNED_BYTE, modded);
  }
}
