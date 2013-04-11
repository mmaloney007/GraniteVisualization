package SDEngine;

import java.io.BufferedWriter;
import java.io.FileWriter;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static SDEngine.SDGlobal.*;
import static SDEngine.SDUtils.*;

/**
 * 
 * @author Stephen Dunn
 * 
 */

public class SDLog {
  private static String _logFile = "debug.log";
  private static BufferedWriter _writer = null;

  public SDLog(String logFile) throws SDException {
    try {
      _writer = new BufferedWriter(new FileWriter(logFile, true));
      out("***********************************************************************************");
      out(SDUtils.getDateAndTimeString() + ": SDEngine log file \"" + logFile
          + "\" initialized");
    } catch (Exception e) {
      dbgErr("could not open or create the log file: " + logFile);
      _writer = null;
      throw new SDException();
    }
  }

  public SDLog() throws SDException {
    this(_logFile);
  }

  public static void destroy() {
    try {
      if (_writer != null)
        _writer.close(); // Close will flush first
      _writer = null;
    } catch (Exception e) {
    }
  }

  public static void dbg(String msg) {
    System.out.println(msg);
  }

  public static void dbgErr(String msg) {
    System.err.println(msg + "\n" + stackTrace());
  }

  public static void out(String msg, boolean error) {
    String t = SDUtils.getDateAndTimeString();
    if (_debugMode) {
      if (error) {
        System.err.println(t + ": " + msg);
      } else {
        System.out.println(t + ": " + msg);
      }
    }
    write(t + ": " + msg);
  }

  public static void out(String msg) {
    out(msg, false);
  }

  public static void err(String msg) {
    out(msg + "\n" + stackTrace(), true);
  }

  public static void msg(String message) {
    msg(null, message);
  }

  public static void msg(JFrame panel, String message) {
    JOptionPane.showMessageDialog(panel, message);
  }

  public static synchronized void write(String text) {
    try {
      if (_writer != null) {
        _writer.write(text + System.getProperty("line.separator"));
        _writer.flush();
      } else {
        dbgErr("failed to log an error because the writer is null");
      }
    } catch (Exception e) {
      dbgErr("failed to write to the log file: " + _logFile);
    }

  }

  public static boolean isLoaded() {
    return (_writer != null) ? true : false;
  }
}