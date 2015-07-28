/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
