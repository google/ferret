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
package com.google.research.ic.ferret.test;

import com.google.research.ic.ferret.Config;
import com.google.research.ic.ferret.Session;
import com.google.research.ic.ferret.SessionListener;
import com.google.research.ic.ferret.comm.DeviceEventReceiver;
import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.SubSequence;
import com.google.research.ic.ferret.data.UberResultSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Shell {

  
  private static int NGRAM_LENGTH = 3;
  private static List<Snippet> queries = null;
  private static List<String> queryNames = null;
  private static List<Snippet> logs = null;
  private static ShellSessionListener theListener = null;
  private static Session session = null;
  private static boolean done = false;
  private static Mode mode = Mode.NONE;
  private static PrintWriter stampFileWriter = null;

  public enum Mode {
    RECORD, SPLIT, NONE
  }
  
  public static void main(String [] args) {

    Config.DEBUG = false;
    for (String s : args) {
      if (s.equals(Config.DEBUGARG)) {
        Config.DEBUG = true;
      }
    }
    
    String in = null;
    String response = null;
    LogLoader.getLogLoader().setLogType(LogLoader.ACCESSIBILITIY_LOG);
    theListener = new ShellSessionListener();

    Scanner scanIn = new Scanner(System.in);
    
    while (!done) {      
      System.out.print(">>> ");
      in = scanIn.nextLine();
      response = processCommandLine(in);
      System.out.println(response);
    }
    scanIn.close();
    System.exit(0);
  }
  
  private static String processCommandLine(String in) {
    String command = null;
    String arg = null;
    String[] params = null;
    String[] commandParts = in.split("\\s");
    String response = null;
    
    if (commandParts.length >= 1) {
      command = commandParts[0];
      if (commandParts.length >= 2) {
        arg = commandParts[1];
      } 
      if (commandParts.length > 2) {
        params = new String[commandParts.length - 2];
        for (int i = 2; i < commandParts.length; i++) {
          params[i-2] = commandParts[i];
        }
      }
    }
    
    if (in == null || in.equals("") || in.startsWith("help")) {
      response = "USAGE: \n"
          + "\t exit (duh) \n"
          + "\t connect (connect to device via adb) \n"
          + "\t echo (print events to the screen) \n"
          + "\t rec <file> (start recording events to <file>) \n"
          + "\t split <file> (must be recording, start recording to a second <file>) \n"
          + "\t stop (stop the current recording or split) \n"
          + "\t print <file> (print contents of <file>) \n"
          + "\t index <file> (index contents of a file or directory) \n"
          + "\t logs (list all previously indexed logs) \n"
          + "\t query <file> | <index> (submit query contained in <file> or in stored query <index>) \n"
          + "\t loadq <file> (load stored query from file or directory) \n"
          + "\t listq (list stored queries) \n"
          + "\t result <index> (print result <index>)"
          ;
    } else if (command.equals("exit")) {
      done = true;
      response = "We're done!";
    } else if (command.equals("exec")) {
      if (arg != null) {
        try {
          FileInputStream cmdFIS = new FileInputStream(arg);
          Scanner cmdIn = new Scanner(cmdFIS);
          while (cmdIn.hasNextLine()) {
            String line = cmdIn.nextLine();
            System.out.println(processCommandLine(line));
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    } else if (command.equals("echo")) {
    
      if (arg != null) {
        boolean b = Boolean.parseBoolean(arg);
        theListener.setPrintMode(b);
        response = b ? "Now printing to screen" : "Stopped printing to screen";
      } else {
        theListener.setPrintMode(true);
        response = "Now printing to screen";
      }
    } else if (command.equals("rec")) {
      if (arg != null) {
        mode = Mode.RECORD;
        theListener.startRecording(arg);
        response = "recording to " + arg;
      } else {          
        List<Snippet> snippets = null;

        response = "please specify filename for recording";
      }
    } else if (command.equals("split")) {
      if (arg != null) {
        mode = Mode.SPLIT;
        theListener.startSplit(arg);
        response = "splitting to " + arg;
      } else {
        response = "please specify filename for split";
      }
    } else if (command.equals("logs")) {
      logs = SearchEngine.getSearchEngine().getLogSnippets();
      if (logs != null && logs.size() > 0) {
        int i = 0;
        for (Snippet log : logs) {
          System.out.println(i++ + " " + log.getSourceFilename() + " " + log.getUserName() + "@" + log.getStartDate().getTime());
        }
        response = "";
      }
      
    } else if (command.equals("print")) {      

      Snippet snippet = getSnippetFromLog(arg);
      int startIdx = 0;
      int endIdx = 0;
      
      if (params != null && params.length == 2) {
        startIdx = Integer.parseInt(params[0]);
        endIdx = Integer.parseInt(params[1]);
      } else {
        startIdx = 0;
        endIdx = snippet.getEvents().size();
      }
      printEvents(snippet, startIdx, endIdx);
      response = "finished dump";         
      
    } else if (command.equals("extract")) {      
      String extractFileName = null;
      Snippet snippet = getSnippetFromLog(arg);
      int startIdx = 0;
      int endIdx = 0;
      
      if (params != null && params.length > 2) {
        startIdx = Integer.parseInt(params[0]);
        endIdx = Integer.parseInt(params[1]);
        extractFileName = params[2];
      } else {
        startIdx = 0;
        endIdx = snippet.getEvents().size();
        extractFileName = params[0];
      }
      
      for (int i = startIdx; i < endIdx; i++) {
        LogLoader.getLogLoader().getParser().writeEvent(snippet.getEvents().get(i), new File(extractFileName));
      }
      response = "wrote " + (endIdx - startIdx) + " events to " + extractFileName;  
      
    } else if (command.equals("tagfile")) {
      File stampFile = new File(arg);
      try {
        stampFileWriter = new PrintWriter(stampFile);
      } catch (IOException e) {
        
        System.out.println("Could not open file " + arg);
      }
    } else if (command.equals("tag")) {
      if (stampFileWriter != null) {
        Date now = new Date();
        String stamp = "start " + arg + ":" + now.getTime();        
        stampFileWriter.println(stamp);
        stampFileWriter.flush();
        response = "wrote " + stamp;
      } else {
        response = "Need to identify tagfile before tagging";
      }
    } else if (command.equals("end")) {
      if (stampFileWriter != null) {
        Date now = new Date();
        String stamp = "end:" + now.getTime();
        stampFileWriter.println(stamp);
        stampFileWriter.flush();
        response = "wrote " + stamp;
      } else {
        System.out.println("Need to identify tagfile before ending tag");
      }
    } else if (command.equals("assoc")) {
      File tagFile = new File(arg + ".tag");
      List<Tag> tags = EvalFramework.processTagList(tagFile);
      Snippet logSnippet = LogLoader.getLogLoader().loadLogFile(arg + ".log").get(0);
      List<LabeledRegion> labeledRegions = EvalFramework.associateTags(tags, logSnippet);
      EvalFramework.printLabeledSnippet(labeledRegions, logSnippet);
    } else if (command.equals("index")) {
      if (arg != null) {
        File logLoc = new File(arg);
        List<Snippet> snippets = null;
        if (logLoc.exists() && logLoc.isDirectory()) {
          snippets = LogLoader.getLogLoader().loadLogs(arg);            
        } else if (logLoc.exists() && logLoc.isFile()) {
          snippets = LogLoader.getLogLoader().loadLogFile(arg);
        } else {
          response = "please specify index file or dir that actually exists";
        }
        if (snippets != null) {
          System.out.println("Starting to index " + arg);
          long t = System.currentTimeMillis();
          SearchEngine.getSearchEngine().indexLogs(snippets, NGRAM_LENGTH);
          //printNGramTables(snippets);
          logs = SearchEngine.getSearchEngine().getLogSnippets();
          response = "Finished indexing " + arg + " after " + (System.currentTimeMillis() - t) + "ms";
        } else {
          response = "Nothing to index in " + arg;
        }
      } else {
        response = "please specify filename for index";
      }   
    } else if (command.equals("loadq")) {
      if (arg != null) {
        File logLoc = new File(arg);
        queries = new ArrayList<Snippet>();
        queryNames = new ArrayList<String>();
        if (logLoc.exists() && logLoc.isDirectory()) {
          File[] files = logLoc.listFiles();
          for (File  f : files) {
            queries.add(LogLoader.getLogLoader().loadLogFile(f.getAbsolutePath()).get(0));
            queryNames.add(f.getName());
          }
        } else if (logLoc.exists() && logLoc.isFile()) {
          queries = LogLoader.getLogLoader().loadLogFile(arg);
        } else {
          response = "please specify index file or dir that actually exists";
        }
      }
      listQueries();        
    } else if (command.equals("listq")) {
      listQueries();
    } else if (command.equals("printq")) {
      if (Character.isDigit(arg.charAt(0))) {
        int idx = Integer.parseInt(arg);
        if (queries != null && idx < queries.size()) {
          Snippet query = queries.get(idx); 
          printEvents(query, 0, query.getEvents().size());
        }
      } else {
        response = "no query at index " + arg;
      }
    } else if (command.equals("query")) {
    
      if (arg != null) {
        Snippet query = null;
        
        if (Character.isDigit(arg.charAt(0))) {
          int idx = Integer.parseInt(arg);
          if (queries != null && idx < queries.size())
          query = queries.get(idx);
        } else {
          File logLoc = new File(arg);
          List<Snippet> snippets = null;
          if (logLoc.exists() && logLoc.isFile()) {
            snippets = LogLoader.getLogLoader().loadLogFile(arg);
            if (snippets.size() == 1) {
              query = snippets.get(0);
            } else {
              response = "invalid query, there were " + snippets.size() + " snippets in file " + arg;
            }
          } else {
            response = "please specify index file or dir that actually exists";
          }
        }
        if (query != null) {
          UberResultSet urs = SearchEngine.getSearchEngine().findMatches(query);

          ResultSet closeMatches = urs.getStrongMatches();
          closeMatches.rank();
          System.out.println("\nTop " + closeMatches.getResults().size() + " close matches: ");
          for (int i = 0; i < 50; i++) {
            if (closeMatches.getResults().size() > i) {
              SubSequence match = closeMatches.getResults().get(i);
              System.out.println("\t" + match + " (from " + getSnippetLabel(match.getSnippet()) + ")");
            } else {
              continue;
            }
          }

          ResultSet weakMatches = urs.getWeakMatches();
          weakMatches.rank();
          System.out.println("\nTop " + weakMatches.getResults().size() + " weak matches: ");
          for (int i = 0; i < 50; i++) {
            if (weakMatches.getResults().size() > i) {
              SubSequence match = weakMatches.getResults().get(i);
              System.out.println("\t" + match + " (from " + getSnippetLabel(match.getSnippet()) + ")");
            } else {
              continue;
            }
          }
          
          ResultSet elongationMatches = urs.getElongatedMatches();
          elongationMatches.rank();
          System.out.println("\nTop " + elongationMatches.getResults().size() + " elongated matches: ");
          for (int i = 0; i < 50; i++) {
            if (elongationMatches.getResults().size() > i) {
              SubSequence match = elongationMatches.getResults().get(i);
              System.out.println("\t" + match + " (from " + getSnippetLabel(match.getSnippet()) + ")");
            } else {
              continue;
            }
          }

          ResultSet altEndingMatches = urs.getAltEndingMatches();
          altEndingMatches.rank();
          System.out.println("\nTop " + altEndingMatches.getResults().size() + " alternat ending matches: ");
          for (int i = 0; i < 50; i++) {
            if (altEndingMatches.getResults().size() > i) {
              SubSequence match = altEndingMatches.getResults().get(i);
              System.out.println("\t" + match + " (from " + getSnippetLabel(match.getSnippet()) + ")");
            } else {
              continue;
            }
          }

          response = "";
        }
      } else {
        response = "please specify filename for query";
      }  
    } else if (command.equals("connect")) {
      DeviceEventReceiver.startServer();
      session = Session.getCurrentSession();
      session.init();

      session.addListener(theListener);
      session.setRecordingMode(true);
      response = "initiated connection to device";
    } else if (command.equals("stop")) {
      if (mode == Mode.SPLIT) {
        mode = Mode.RECORD;
        theListener.stopSplit();
        response = "stopped split, still recording";
      } else if (mode == Mode.RECORD) {
        mode = Mode.NONE;
        theListener.stopRecording();
        response = "stopped recording";
      } else {
        response = "already stopped";
      }        
    } else {
      response = "Invalid command " + in;
    }
   return response;
  }

  
  private static final class ShellSessionListener implements SessionListener {

    boolean printToConsole = false;
    String recordFileName = null;
    String splitFileName = null;

    @Override
    public void sessionUpdated(Session s) {
      List<Event> events = s.dequeueDemoEvents();
      for (Event e : events) {
        //System.out.println(e);
        if (printToConsole) {
          System.out.println("In Shell, dequeued event: " + e);
        }
        if (recordFileName != null) {
          LogLoader.getLogLoader().getParser().writeEvent(e, new File(recordFileName));
        }
        if (splitFileName != null) {
          LogLoader.getLogLoader().getParser().writeEvent(e, new File(splitFileName));          
        }
      }
    }
    
    public void startRecording(String fileName) {
      recordFileName = fileName;
    }
    
    public void stopRecording() {
      recordFileName = null;
    }
    
    public void startSplit(String fileName) {
      splitFileName = fileName;
    }
    
    public void stopSplit() {
      splitFileName = null;
    }
    
    public boolean getPrintMode() {
      return printToConsole;
    }
    
    public void setPrintMode(boolean b) {
      printToConsole = b;
    }
  }
  
  private static void printNGramTables(List<Snippet> snippets) {
    for(Snippet s : snippets) {
      Map <String, List<Integer>> map = s.getNGramTable(NGRAM_LENGTH);
      for (String k : map.keySet()) {
        System.out.println(k + ":" + map.get(k));
      }
    }
  }

  private static void listQueries() {
    if (queries == null || queries.size() < 1) {
      System.out.println("No queries");
    } else {
      for(int i = 0; i < queries.size(); i++) {
        System.out.println(i + " " + queryNames.get(i));
      }
    }
  }
  
  public static void printEvents(Snippet snip, int startIdx, int endIdx) {
    for (int i = startIdx; i < endIdx; i++) {
      Event e = snip.getEvents().get(i);
      System.out.println(i + " " + e);
    }
  }
  
  private static String getSnippetLabel(Snippet snip) {
    return snip.getUserName() + "@" + snip.getStartDate().getTime();
  }
  
  private static Snippet getSnippetFromLog(String logName) {
    List<Snippet> snippets = null;
    Snippet snippet = null; 
    if (logName != null) {   
      if (Character.isDigit(logName.charAt(0))) {
        int logIdx = Integer.parseInt(logName);
        snippets = SearchEngine.getSearchEngine().getLogSnippets();
        if (logIdx < snippets.size()) {
          snippet = snippets.get(logIdx);
        } else {
          throw new IllegalArgumentException("Log at " + logName + " doesn't exist");
        }
      } else {
        snippets = LogLoader.getLogLoader().loadLogFile(logName);
        if (snippets.size() > 1) {
          throw new IllegalStateException("log file " + logName + " contains " + snippets.size() + " snippets");
        }
        snippet = snippets.get(0);
      }
    }
    return snippet;
  }
}
