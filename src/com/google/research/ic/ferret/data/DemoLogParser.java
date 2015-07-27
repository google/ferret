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
package com.google.research.ic.ferret.data;

import com.google.gson.Gson;
import com.google.research.ic.ferret.data.ext.alogger.AccessibilityLogEvent;
import com.google.research.ic.ferret.test.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads demo snippets off the disk
 */
public class DemoLogParser {
  Gson gson = new Gson();
  private static DemoLogParser theParser = null;

  private DemoLogParser() {}
  
  public static DemoLogParser getParser() {
    if (theParser == null) {
      theParser = new DemoLogParser();
    }
    return theParser;
  }
  
  public static final String DEMO_LOG_FILE_DIR = "logs/demos"; //assume cwd/logs
  
  public void readDemoLogDirectory() {
    File logDir = new File(System.getProperty("user.dir"), DEMO_LOG_FILE_DIR);
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept(File file) {
        return (file.getName().endsWith(".txt") || 
            file.getName().endsWith(".log") ||
            file.getName().endsWith(".json"));
      }
    };
    File[] demoLogFiles = logDir.listFiles(filter);
    for (int i = 0; i < demoLogFiles.length; i++) {
      File logFile = demoLogFiles[i];
      readDemoLogFile(logFile);
    }
  }
  
  public void readDemoLogFile(File logFile) {
    Debug.log("Reading demo log file: " + logFile.getName());
    
    try {
      FileReader fr = new FileReader(logFile);
      BufferedReader br = new BufferedReader(fr);
      String line = null;
      Snippet demoSnippet = null;
      while ((line = br.readLine()) != null) {
        Debug.log("Reading line: " + line);
        AccessibilityLogEvent alE = gson.fromJson(line, AccessibilityLogEvent.class);
        alE.init();
        if (demoSnippet == null) {
          demoSnippet = new Snippet(alE);
        }
        else demoSnippet.addEvent(alE);
      }
      DemoManager.getDemoManager().addDemoSnippet(demoSnippet);
      Debug.log("Read demo in from: " + logFile);
      br.close();      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
