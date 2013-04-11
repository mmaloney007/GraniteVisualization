package SDEngine;

public class SDGlobal {
  public static final double DEG_TO_RAD = 0.01745329251;
  public static final double RAD_TO_DEG = 57.2957795131;

  public static boolean _debugMode = true;
  public static int _decimalPrecision = 10;
  public static float _mouseSensitivity = 0.1f;
  public static float _moveSpeed = 2.0f;
  public static float _fov = 45.0f;
  public static float _nearClip = 0.1f, _farClip = 50000.0f;

  public static boolean loadDebugMode() {
    if (SDUtils.configLoaded())
      _debugMode = SDUtils.getConfigValue("DEBUG").toLowerCase().trim()
          .equals("true") ? true : false;
    return _debugMode;
  }
}
