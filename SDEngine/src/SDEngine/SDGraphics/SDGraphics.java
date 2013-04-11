package SDEngine.SDGraphics;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import org.newdawn.slick.Color;

import static org.lwjgl.opengl.GL11.*;

import static SDEngine.SDLog.*;
import static SDEngine.SDUtils.*;
import SDEngine.*;

public class SDGraphics extends SDObject {
  // Standard Properties
  protected int _curWidth = 800, _curHeight = 600;
  protected boolean _fullscreen = false;
  protected boolean _vsync = false;
  protected boolean _sync = false;
  protected int _syncRate = 60;

  protected ArrayList<SDGraphicsObject> _objects;
  protected Color _clearColor;

  public SDGraphics() throws SDException {
    try {
      _objects = new ArrayList<SDGraphicsObject>();
      _clearColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    } catch (Exception e) {
      throw new SDException();
    }

    out("SDGraphics()");
  }

  public boolean init() {
    try {
      Mouse.setGrabbed(true);
      setDisplayMode(_curWidth, _curHeight, _fullscreen);
      Display.create();
    } catch (LWJGLException e) {
      e.printStackTrace();
      System.exit(0);
      return false;
    }

    initGL();

    return true;
  }

  public void destroy() {
    for (SDGraphicsObject temp : _objects)
      temp.destroy();
    _objects.clear();
    _objects = null;

    Display.destroy();
  }

  public void update() {
    if (!Display.isCreated())
      return;

    if (_sync)
      Display.sync(_syncRate); // cap fps

    for (SDGraphicsObject temp : _objects)
      temp.update();

    Display.update();
  }

  public void render() {
    if (!Display.isCreated())
      return;

    glClearColor(_clearColor.r, _clearColor.g, _clearColor.b, _clearColor.a);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    for (SDGraphicsObject temp : _objects) {

      glPushAttrib(temp.getAttributes());
      glPushMatrix();

      temp.render();

      glPopMatrix();
      glPopAttrib();

    }
  }

  public static void initGL() {
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glEnable(GL_TEXTURE_2D);
    glCullFace(GL_BACK);
    glShadeModel(GL_SMOOTH);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

    lighting(true);
    setAmbient(0.2f, 0.2f, 0.2f, 1.0f);
  }

  public static void lighting(boolean on) {
    if (on) {
      glEnable(GL_LIGHTING);
      glEnable(GL_COLOR_MATERIAL);
      glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
    } else {
      glDisable(GL_LIGHTING);
    }
  }

  public static void setAmbient(float r, float g, float b, float a) {
    glLightModel(
        GL_LIGHT_MODEL_AMBIENT,
        (FloatBuffer) (BufferUtils.createFloatBuffer(4).put(new float[] { r, g,
            b, a })).flip());
  }

  public void add(SDGraphicsObject obj) {
    if (_objects != null)
      _objects.add(obj);
  }

  public int getWidth() {
    return _curWidth;
  }

  public int getHeight() {
    return _curHeight;
  }

  public float getAspectRatio() {
    return (float) getWidth() / (float) getHeight();
  }

  public Color clearColor() {
    return new Color(_clearColor);
  }

  public void setClearColor(Color c) {
    _clearColor = c;
  }

  public boolean vsync() {
    return _vsync;
  }

  public void setVSync(boolean vsync) {
    _vsync = vsync;
    Display.setVSyncEnabled(_vsync);
  }

  /**
   * Change Display Mode
   * 
   * @param width
   * @param height
   * @param fullscreen
   */
  public void setDisplayMode(int width, int height, boolean fullscreen) {

    if ((Display.getDisplayMode().getWidth() == width)
        && (Display.getDisplayMode().getHeight() == height)
        && (Display.isFullscreen() == fullscreen)) {
      return;
    }

    try {
      DisplayMode targetDisplayMode = null;

      if (fullscreen) {
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        int freq = 0;

        for (int i = 0; i < modes.length; i++) {
          DisplayMode current = modes[i];

          if ((current.getWidth() == width) && (current.getHeight() == height)) {
            if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
              if ((targetDisplayMode == null)
                  || (current.getBitsPerPixel() > targetDisplayMode
                      .getBitsPerPixel())) {
                targetDisplayMode = current;
                freq = targetDisplayMode.getFrequency();
              }
            }

            if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode()
                .getBitsPerPixel())
                && (current.getFrequency() == Display.getDesktopDisplayMode()
                    .getFrequency())) {
              targetDisplayMode = current;
              break;
            }
          }
        }
      } else {
        targetDisplayMode = new DisplayMode(width, height);
      }

      if (targetDisplayMode == null) {
        err("failed to find value mode: width=" + width + ", height=" + height
            + ", fullscreen=" + fullscreen);
        return;
      }

      Display.setVSyncEnabled(_vsync);
      Display.setDisplayMode(targetDisplayMode);
      Display.setFullscreen(fullscreen);

    } catch (LWJGLException e) {
      err("unable to setup mode: width=" + width + ", height=" + height
          + ", fullscreen=" + fullscreen + ", exception=" + e);
    }
  }
}