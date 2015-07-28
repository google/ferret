package com.google.research.ic.alogger;

import com.google.gson.Gson;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;

/**
 * A service to log accessibility events
 */
public class ALoggerService extends AccessibilityService {

    private Gson gson = new Gson();
    private int screenHeight = -1;
    private int screenWidth = -1;

    private String userId = null;
    private String deviceId = null;
    
    private static final int PORT = 3416;

    private ArrayList<EventRecord> eventLog = new ArrayList<>();

    public void onAccessibilityEvent(AccessibilityEvent event) {
        //DebugLogger.log("Event: " + event);
        if (event == null || event.getSource() == null) {
            //DebugLogger.log("Not making an event this time");
            return;
        }
        EventRecord record = new EventRecord(event, userId, deviceId, this);
        // Write to log
        LogWriter.getLogWriter().writeLog(gson.toJson(record));
    }

    @Override
    public void onInterrupt() {
    }

    public Point getScreenSize() {
        return new Point(screenWidth, screenHeight);
    }

    @Override
    public void onServiceConnected() {

        DebugLogger.log("Service connected");

        try {
            LogWriter.getLogWriter();
        } catch (IllegalStateException e) {
            LogWriter.initLogWriter(this);
        }
        Sender.startSender(PORT);

        Point size = new Point();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        AccessibilityServiceInfo serviceInfo = this.getServiceInfo();
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC; // actually NONE
        serviceInfo.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        serviceInfo.notificationTimeout = 100; // TODO: should figure out if this is right

        this.setServiceInfo(serviceInfo);

        deviceId = LogWriter.getLogWriter().getDeviceId();
        userId = LogWriter.getLogWriter().getUserId();

        DebugLogger.log("Service initialized");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Sender.stopSender();
        return true;
    }

}
