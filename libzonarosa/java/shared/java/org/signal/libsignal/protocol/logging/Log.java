//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import io.zonarosa.libzonarosa.internal.CalledFromNative;

public class Log {

  private Log() {}

  public static void v(String tag, String msg) {
    log(ZonaRosaProtocolLogger.VERBOSE, tag, msg);
  }

  public static void v(String tag, String msg, Throwable tr) {
    log(ZonaRosaProtocolLogger.VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
  }

  public static void d(String tag, String msg) {
    log(ZonaRosaProtocolLogger.DEBUG, tag, msg);
  }

  public static void d(String tag, String msg, Throwable tr) {
    log(ZonaRosaProtocolLogger.DEBUG, tag, msg + '\n' + getStackTraceString(tr));
  }

  public static void i(String tag, String msg) {
    log(ZonaRosaProtocolLogger.INFO, tag, msg);
  }

  public static void i(String tag, String msg, Throwable tr) {
    log(ZonaRosaProtocolLogger.INFO, tag, msg + '\n' + getStackTraceString(tr));
  }

  public static void w(String tag, String msg) {
    log(ZonaRosaProtocolLogger.WARN, tag, msg);
  }

  public static void w(String tag, String msg, Throwable tr) {
    log(ZonaRosaProtocolLogger.WARN, tag, msg + '\n' + getStackTraceString(tr));
  }

  public static void w(String tag, Throwable tr) {
    log(ZonaRosaProtocolLogger.WARN, tag, getStackTraceString(tr));
  }

  public static void e(String tag, String msg) {
    log(ZonaRosaProtocolLogger.ERROR, tag, msg);
  }

  public static void e(String tag, String msg, Throwable tr) {
    log(ZonaRosaProtocolLogger.ERROR, tag, msg + '\n' + getStackTraceString(tr));
  }

  private static String getStackTraceString(Throwable tr) {
    if (tr == null) {
      return "";
    }

    // This is to reduce the amount of log spew that apps do in the non-error
    // condition of the network being unavailable.
    Throwable t = tr;
    while (t != null) {
      if (t instanceof UnknownHostException) {
        return "";
      }
      t = t.getCause();
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    tr.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }

  private static void log(int priority, String tag, String msg) {
    ZonaRosaProtocolLogger logger = ZonaRosaProtocolLoggerProvider.getProvider();

    if (logger != null) {
      logger.log(priority, tag, msg);
    }
  }

  @CalledFromNative
  private static void logFromRust(int priority, String msg) {
    log(priority, "libzonarosa", msg);
  }
}
