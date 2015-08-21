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

import android.app.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by marknewman on 3/21/15.
 */
public class LogWriter {

    private static String LOG_FILE_NAME = "ALoggerLog.txt";
    private static String DEVICE_INFO_FILE_NAME = "ALoggerDeviceInfo.txt";

    private String userId = null;
    private String deviceId = null;
    private BufferedWriter bufferedLogWriter = null;

    private static LogWriter theWriter = null;

    private Service loggingService = null;

    private boolean demoMode = true;

    private LogWriter(Service service) {
        this.loggingService = service;
    }

    public static void initLogWriter(Service service) {
        if (theWriter == null) {
            theWriter = new LogWriter(service);
        }
    }

    public static LogWriter getLogWriter() {
        if (theWriter == null) {
            throw new IllegalStateException("log writer must be initialized before you can get it");
        }
        return theWriter;
    }

    public void writeLog(String eventString) {

        if (isDemoMode()) {
            //DebugLogger.log("Sending: " + eventString);
            Sender.send(eventString);
        } else {
            try {
                openLogForWriting();
                if (bufferedLogWriter != null) {
                    bufferedLogWriter.append(eventString);
                    bufferedLogWriter.append("\n");
                    bufferedLogWriter.flush();
                    bufferedLogWriter.close();
                }
                DebugLogger.log("Writing: " + eventString);

            } catch (IOException e) {
                DebugLogger.log("Couldn't write to " + bufferedLogWriter);
                e.printStackTrace();
            }
        }
    }

    private void openLogForWriting() {
        File appDir = loggingService.getFilesDir();
        File logFile = new File(appDir, LOG_FILE_NAME);
        //DebugLogger.log("writing to " + logFile.getAbsolutePath().toString());
        try {
            FileWriter fw = new FileWriter(logFile, true);
            bufferedLogWriter = new BufferedWriter(fw);
        } catch (IOException e) {
            DebugLogger.log("Couldn't open " + appDir + "/" + LOG_FILE_NAME + " for writing");
            e.printStackTrace();
        }
    }

    private void readOrCreateDeviceInfoFile() {
        File appDir = loggingService.getFilesDir();
        File deviceInfoFile = new File(appDir, DEVICE_INFO_FILE_NAME);
        Gson gson = new Gson();
        if (deviceInfoFile.exists()) {
            try {
                FileReader fr = new FileReader(deviceInfoFile);
                BufferedReader br = new BufferedReader(fr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    DeviceAndUserInfo duInfo = gson.fromJson(line, DeviceAndUserInfo.class);
                    this.deviceId = duInfo.deviceID;
                    this.userId = duInfo.userID;
                }
                br.close();
            } catch (IOException e) {
                DebugLogger.log("Couldn't open " + appDir + "/" + DEVICE_INFO_FILE_NAME + " for writing");
                e.printStackTrace();
            }
        } else { // need to generate new IDs and write the file
            UUID deviceUUID = UUID.randomUUID();
            UUID userUUID = UUID.randomUUID();
            this.deviceId = deviceUUID.toString(); // replace with MAC address?
            this.userId = userUUID.toString(); // replace with something more human-readable?

            try {
                FileWriter fw = new FileWriter(deviceInfoFile, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fw);
                DeviceAndUserInfo duInfo = new DeviceAndUserInfo(deviceId,userId);
                String duInfoAsJson = gson.toJson(duInfo);
                bufferedWriter.append(duInfoAsJson);
                bufferedWriter.append("\n");
                bufferedWriter.close();
            } catch (IOException e) {
                DebugLogger.log("Couldn't open " + appDir + "/" + LOG_FILE_NAME + " for writing");
                e.printStackTrace();
            }
        }
    }

    public String getDeviceId() {
        if (deviceId == null) {
            readOrCreateDeviceInfoFile();
        }
        return deviceId;
    }

    public String getUserId() {
        if (userId == null) {
            readOrCreateDeviceInfoFile();
        }
        return userId;

    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public static class DeviceAndUserInfo {
        String deviceID;
        String userID;

        public DeviceAndUserInfo() {}

        public DeviceAndUserInfo(String dId, String uId) {
            deviceID = dId;
            userID = uId;
        }
    }
}

