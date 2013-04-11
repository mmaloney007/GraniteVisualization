package physics;

import static SDEngine.SDGlobal._mouseSensitivity;
import static SDEngine.SDGlobal._moveSpeed;
import static SDEngine.SDLog.err;
import static SDEngine.SDLog.out;
import static SDEngine.SDUtils.fixAngle;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import SDEngine.SDEngine;
import SDEngine.SDException;
import SDEngine.SDGame;
import SDEngine.SDGlobal;
import SDEngine.SDGraphics.SDGraphics;
import SDEngine.SDGraphics.SDSprite;

public class Visualizer extends SDEngine implements SDGame {
  public static final String _version = "v0.1";
  protected boolean _exit = false;
  SDSprite test;

  public Visualizer() throws SDException {
    super();
  }

  @Override
  public boolean init() {
    if (!super.init())
      return false;

    SDGraphics.lighting(false);
    setAxes(true);

    if (!GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
      err("OpenGL version < 1.5");
      return false;
    }

    try {

      SDSprite spriteTest = new SDSprite(new Vector3f(0, 0, -30),
          "Test_1.png", new int[] { 255, 0, 0 }, GL11.GL_LINEAR, _camera,
          null);

      // spriteTest.setBillboard(true);
      // spriteTest.compile();
      // spriteTest.setSize(200, 200);
      _graphics.add(spriteTest);
      // spriteTest._texture.test();
      test = new SDSprite(new Vector3f(0, 0, -500), _camera,
          new Color(0.5f, 0.2f, 0.5f), 512.0f, 512.0f);
      //test.setBillboard(true);
      //test.rotateY(45);

      //test.rotateZ( test.getZRotation()+0.5f );
      //test.rotateZ(10);
      //test.setYRotation(90);
      _graphics.add(test);

    } catch (Exception e) {
      err("??");
    }

    return true;
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public void update() {
    //  test.setZRotation( test.getZRotation()+0.001f );
    super.update();

    Display.setTitle("FPS: " + _lastFPS);

    if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
      exit();
      return;
    }

    while (Keyboard.next()) {
      if (Keyboard.getEventKey() == Keyboard.KEY_M) {
        if (!Keyboard.getEventKeyState())
          Mouse.setGrabbed(!Mouse.isGrabbed());
      }
      if (Keyboard.getEventKey() == Keyboard.KEY_C) {
        if (!Keyboard.getEventKeyState())
          out(_camera.toString());
      }
      if (Keyboard.getEventKey() == Keyboard.KEY_R) {
        if (!Keyboard.getEventKeyState())
          cameraReset();
      }
      if (Keyboard.getEventKey() == Keyboard.KEY_F1) {
        if (!Keyboard.getEventKeyState()) {
          setAxes(!drawAxes());
        }
      }
      if (Keyboard.getEventKey() == Keyboard.KEY_F) {
        if (!Keyboard.getEventKeyState()) {

        }
      }
      if (Keyboard.getEventKey() == Keyboard.KEY_TAB) {
        if (!Keyboard.getEventKeyState()) {
          _camera.setOrtho(!orthoMode());
        }
      }
    }

    // Camera updating ****************************************
    double forward = SDGlobal.DEG_TO_RAD * _camera.yaw();
    double left = SDGlobal.DEG_TO_RAD * fixAngle(_camera.yaw() + 90.0f);

    if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
      float fSin = (float) Math.sin(forward);
      float fCos = (float) Math.cos(forward);
      cameraMoveX(-fSin * _moveSpeed);
      cameraMoveZ(-fCos * _moveSpeed);
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
      float fSin = (float) Math.sin(forward);
      float fCos = (float) Math.cos(forward);
      cameraMoveX(fSin * _moveSpeed);
      cameraMoveZ(fCos * _moveSpeed);
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
      float lSin = (float) Math.sin(left);
      float lCos = (float) Math.cos(left);
      cameraMoveX(-lSin * _moveSpeed);
      cameraMoveZ(-lCos * _moveSpeed);
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
      float lSin = (float) Math.sin(left);
      float lCos = (float) Math.cos(left);
      cameraMoveX(lSin * _moveSpeed);
      cameraMoveZ(lCos * _moveSpeed);
    }

    float dx = Mouse.getDX() * _mouseSensitivity;
    float dy = Mouse.getDY() * _mouseSensitivity;

    if (dx != 0 || dy != 0) {
      cameraYaw(-dx);
      cameraPitch(dy); // non-inverted mouse
    }

    /*
     * while (Mouse.next()) { if (Mouse.getEventButton() == 0) { } }
     */
  }

  @Override
  public void render() {
    super.render();
  }

  @Override
  public int run() {

    while (!Display.isCloseRequested() && !_exit) {
      update();
      if (!_exit)
        render();
    }

    return 0;
  }

  public void exit() {
    _exit = true;
  }

  public static void main(String[] args) {
    Visualizer game;

    try {
      game = new Visualizer();
    } catch (SDException e) {
      err("failed to allocate memory");
      return;
    }

    if (!game.init()) {
      return;
    }

    out("game terminated with exit code: " + game.run());

    game.destroy();
  }

}
