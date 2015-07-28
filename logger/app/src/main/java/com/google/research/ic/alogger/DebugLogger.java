package com.google.research.ic.alogger;

import android.util.Log;

/**
 * Created by marknewman on 3/21/15.
 */
public class DebugLogger {
    public static void log(String message, Exception e) {
        Log.d("alogger_debug", message, e);
    }
    public static void log(String message) {
        Log.d("alogger_debug", message);
    }
}
