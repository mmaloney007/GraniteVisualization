package SDEngine;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import static SDEngine.SDLog.*;

public class SDUtils {
  private static final String _timeFormat = "HH:mm:ss";
  private static final String _timeAndDate = "MM/dd/yyyy HH:mm:ss";
  private static Properties _configFileProperties = null;
  private static String _configFile = "config.properties";

  // Get system time in milliseconds
  public static long getTime() {
    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
  }

  public static double toRadians(float angle) {
    return SDGlobal.DEG_TO_RAD * angle;
  }

  public static String getStringFromCalendar(String format) {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    return sdf.format(cal.getTime());
  }

  public static String getTimeString() {
    return getStringFromCalendar(_timeFormat);
  }

  public static String getDateAndTimeString() {
    return getStringFromCalendar(_timeAndDate);
  }

  public static String stackTrace() {
    String errMsg = "";

    try {
      StackTraceElement[] s = Thread.currentThread().getStackTrace();

      for (int i = 0; i < s.length; i++) {
        if (i != 0 && i != 1) {
          errMsg += s[i].getClassName() + "::" + s[i].getMethodName()
              + ": line " + s[i].getLineNumber();
          if (i + 1 != s.length)
            errMsg += "\n";
        }
      }
    } catch (Exception e) {
      err("stack trace exception");
    }

    return errMsg;
  }

  public static boolean errorCheck() {
    int errorValue = GL11.glGetError();
    if (errorValue != GL11.GL_NO_ERROR) {
      err("OpenGL failure: " + GLU.gluErrorString(errorValue) + "\n"
          + stackTrace());
      return true;
    }
    return false;
  }

  public static boolean loadConfigFile() {
    return loadConfigFile(_configFile);
  }

  public static boolean loadConfigFile(String name) {
    try {
      _configFileProperties = new Properties();
      _configFileProperties.load(new FileInputStream(_configFile));
    } catch (Exception e) {
      err("failed to load the configuration file \"" + _configFile + "\"");
      return false;
    }
    return true;
  }

  public static boolean configLoaded() {
    return (_configFileProperties != null) ? true : false;
  }

  public static String getConfigValue(String key) {
    if (_configFileProperties == null) {
      err("configuration file: " + _configFile
          + " was not loaded prior to function call");
      return null;
    }
    try {
      return _configFileProperties.getProperty(key);
    } catch (Exception e) {
      err("failed to retrieve specified key: " + key);
      return null;
    }
  }

  public static Vector3f setVector(Vector3f dest, Vector3f source) {
    if (dest == null) {
      err("destination vector was null");
      return null;
    }
    if (source == null) {
      err("source vetcor was null");
      return null;
    }
    dest.x = source.x;
    dest.y = source.y;
    dest.z = source.z;

    return dest;
  }

  public static Vector3f copyVector(Vector3f source) {
    return new Vector3f(source.x, source.y, source.z);
  }

  public static float fixAngle(float angle) {
    if (angle > 360)
      angle -= 360f;
    else if (angle < 0)
      angle = 360f + angle;

    return angle;
  }

  public static double roundd(double num, int n) {
    if (num == 0) {
      return 0;
    }

    final double d = Math.ceil(Math.log10(num < 0 ? -num : num));
    final int power = n - (int) d;

    final double magnitude = Math.pow(10, power);
    final long shifted = Math.round(num * magnitude);
    return shifted / magnitude;
  }

  public static double roundd(double num) {
    return roundd(num, SDGlobal._decimalPrecision);
  }

  public static float roundf(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }

  public static float roundf(float d) {
    return roundf(d, SDGlobal._decimalPrecision);
  }

}
