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
