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
package com.google.research.ic.ferret;

import com.google.research.ic.ferret.comm.DeviceEventReceiver;
import com.google.research.ic.ferret.data.DemoManager;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.attributes.AttributeManager;
import com.google.research.ic.ferret.data.attributes.DurationAttributeHandler;
import com.google.research.ic.ferret.data.attributes.UserNameAttributeHandler;
import com.google.research.ic.ferret.test.Debug;
import com.google.research.ic.ferret.uiserver.UIServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AppMain {

  public static final String ARG_LOGDIR = "--logdir=";
  public static final String ARG_LOGTYPE = "--logtype=";
  public static final String ARG_LOGTYPE_ACCESSIBILITY = "accessibility";

  public static final String ARG_LOADDEMOS = "--loaddemos";
  public static final String ARG_DONTLOAD = "--dontload";
  public static final String ARG_DONTINDEX = "--dontindex";
  public static final String ARG_NODEVICESERVER = "--nodeviceserver";
  public static final String ARG_NOUISERVER = "--nouiserver";
  
    /**
   * For starting the application in "headless" mode
   * @param args
   */
  public static void main(String[] args) {
    
    List<String> argList = Arrays.asList(args);

    Config.DEBUG = false;

    for (String a : argList) {
      if (a.startsWith(ARG_LOGTYPE)) {
        String logType = a.split("=")[1];
        Debug.log("Setting log type to " + logType);
        if (logType.equalsIgnoreCase(ARG_LOGTYPE_ACCESSIBILITY)) {
          Debug.log("logType is accessibility, yep");
          LogLoader.getLogLoader().setLogType(LogLoader.ACCESSIBILITIY_LOG);
        } else {
          throw new IllegalArgumentException("Log type " + logType + " is not supported.");
        }
      }
    }

    if (argList.contains(Config.DEBUGARG)) {
      Config.DEBUG = true;
    }
    
    if (!argList.contains(ARG_NODEVICESERVER)) {
      Debug.log("Starting device server...");
      DeviceEventReceiver.startServer();
    } else {
      Session.getCurrentSession().setDeviceMode(false);
    }

    Session.getCurrentSession().init();
    
    if (!argList.contains(ARG_NOUISERVER)) {
      Debug.log("Starting UI server...");
      try {
        UIServer.startServer();
      } catch (Exception e) {
        Debug.log("Couldn't start uiserver: " + e);
      }
    }
    
    if(!argList.contains(ARG_DONTLOAD)) {
      Debug.log("Loading logs...");
      AttributeManager.getManager().addHandler(new UserNameAttributeHandler());
      AttributeManager.getManager().addHandler(new DurationAttributeHandler());

      String logDir = null;

      for (String a : argList) {
        if (a.startsWith(ARG_LOGDIR)) {
          logDir = a.split("=")[1];
        }
      }

      long t = System.currentTimeMillis();
      List<Snippet> snippets = LogLoader.getLogLoader().loadLogs(logDir); // null arg: use default dir
      Debug.log("Loaded logs in " + (System.currentTimeMillis() - t) + " ms");

      if (!argList.contains(ARG_DONTINDEX)) {
        Debug.log("Indexing logs...");
        t = System.currentTimeMillis();    
        SearchEngine.getSearchEngine().indexLogs(snippets, 4);
        Debug.log("Indexed logs in " + (System.currentTimeMillis() - t) + " ms.");
      }
    }
    
    
    if(argList.contains(ARG_LOADDEMOS)) {
      Debug.log("Loading demos...");
      List<Snippet> demoSnippets = LogLoader.getLogLoader().getParser().readDemoDirectory(null);
      DemoManager.getDemoManager().setDemoSnippets(demoSnippets);
      //DemoLogParser.getParser().readDemoLogDirectory();
      
    }

    Debug.log("Server initialized. Ready for action.");
    
    String os = System.getProperty("os.name");
    try {
      if (os.startsWith("Linux")) {
        Runtime.getRuntime().exec("xdg-open webui/index.html");
      } else if (os.startsWith("MacOS")) {      
        Runtime.getRuntime().exec("open webui/index.html");
      }
    } catch (IOException e) {
      Debug.log("Couldn't launch front end. " + e);
    }
  }
}
