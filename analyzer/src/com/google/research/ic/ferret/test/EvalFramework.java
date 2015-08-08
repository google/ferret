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
import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.SubSequence;
import com.google.research.ic.ferret.data.UberResultSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class EvalFramework {
  
  private static List<Snippet> logs = new ArrayList<Snippet>();
  private static List<Snippet> queries = new ArrayList<Snippet>();
  private static boolean crossVal = true;

  private static Map<String, List<LabeledRegion>> logTagToRegionMap = new HashMap<String, List<LabeledRegion>>();
  private static Map<String, List<LabeledRegion>> queryTagToRegionMap = new HashMap<String, List<LabeledRegion>>();
  private static Map<Snippet, List<LabeledRegion>> logSnippetToTagMap = new HashMap<Snippet, List<LabeledRegion>>();
  private static Map<Snippet, List<LabeledRegion>> querySnippetToTagMap = new HashMap<Snippet, List<LabeledRegion>>();

  private static float[] avgPrecisions = new float[41];
  private static float[] avgRecalls = new float[41];

  private static final class TagInstanceResult {
    public String tagInstanceName = null;
    public String tagGroupName = null;
    public float prec = -0.1f;
    public float rec = -0.1f;
    public int fn = -1;
    public int tp = -1;
    public int fp = -1;
    public List<String> closeMatches = null;
    public List<String> weakMatches = null;
    public List<String> elongMatches = null;
    public List<String> altEndMatches = null;
    
    public long searchTime = -1;
    
    public TagInstanceResult() {
      closeMatches = new ArrayList<String>();
      weakMatches = new ArrayList<String>();
      elongMatches = new ArrayList<String>();
      altEndMatches = new ArrayList<String>();
    }
  }
  private static List<TagInstanceResult> tagInstanceResults = new ArrayList<TagInstanceResult>();
  
  public static void main(String[] args) {

    Config.parseArgs(args);
    
    String [] tagIDs = Config.tagList.split(",");
    doEval(tagIDs, Config.logDir, Config.queryDir);
  } 
  
  public static void doEval(String[] tagIDs, String logDirName, String expertQueryDirName) {

    crossVal = (expertQueryDirName == null);

    loadLabeledLogs(logDirName, logs, logSnippetToTagMap, logTagToRegionMap);

    if (expertQueryDirName == null) {
      expertQueryDirName = logDirName;
    }
    loadLabeledLogs(expertQueryDirName, queries, querySnippetToTagMap, queryTagToRegionMap);
    
    SearchEngine.getSearchEngine().indexLogs(logs, Config.nGramSize);

    Set<String> tagLabels = logTagToRegionMap.keySet();
    List<String> sortedTagLabels = Arrays.asList(tagLabels.toArray(new String[0]));
    Collections.sort(sortedTagLabels);
    int tagNum = 0;

    for (String lbl : sortedTagLabels) {
      if (tagIDs == null || tagIDs[0].equals("all") || Arrays.asList(tagIDs).contains(lbl)) {
        try {
          tagNum = Integer.parseInt(lbl);
        } catch (Exception e) { 
          if (lbl.startsWith("x")) {
            continue;
          }
          if (lbl.startsWith("a")) {
            tagNum = 35;
          } else if (lbl.startsWith("b")) {
            tagNum = 36;
          } else if (lbl.startsWith("c")) {
            tagNum = 37;
          } else if (lbl.startsWith("d")) {
            tagNum = 38;
          } else if (lbl.startsWith("e")) {
            tagNum = 39;
          } else if (lbl.startsWith("f")) {
            tagNum = 40;
          }
        }

        Debug.log("\n****** Computing results for tag: " + lbl + " *******\n");

        float precSum = 0.0f;
        float recSum = 0.0f;

        // find all instances of a specified tag within the query source
        List<LabeledRegion> queryTagRegions = queryTagToRegionMap.get(lbl);

        for (LabeledRegion lr : queryTagRegions) {
          TagInstanceResult tiResult = new TagInstanceResult();
          tiResult.tagInstanceName = lr.toString();
          tiResult.tagGroupName = lbl;
          float prec = 0.0f;
          float rec = 0.0f;
          Snippet query = lr.getRegionSnippet();
          long t = System.currentTimeMillis();
          UberResultSet urs = SearchEngine.getSearchEngine().findMatches(query);
          tiResult.searchTime = System.currentTimeMillis() - t;
          ResultSet closeMatches = urs.getStrongMatches();
          ResultSet weakMatches = urs.getWeakMatches();
          ResultSet elongMatches = urs.getElongatedMatches();
          ResultSet altEndingMatches = urs.getAltEndingMatches();

          List<ResultRegion> resultRegions = null;
          if (closeMatches != null) {
            resultRegions = getResultRegions(closeMatches);
            for (LabeledRegion rr : resultRegions) {
              tiResult.closeMatches.add(rr.toString());
            }
            //System.out.println("Close-matching regions are: " + resultRegions);
            int tp = 0;
            int fp = 0;
            boolean foundSelf = false;
            for (LabeledRegion r : resultRegions) {      
              if (r.sourceSnippet.getSourceFilename().equals(lr.sourceSnippet.getSourceFilename())) {
                foundSelf = true;
              } else if (r.tag.label.equals(lbl)) {
                tp++;
              } else {
                fp++;
              }
            }
            if(!foundSelf && crossVal){
              System.out.println("WARNING: " + lr + " did not find itself");
            }
            int expectedFinds = logTagToRegionMap.get(lbl).size();
            int fn = expectedFinds - tp; 
            prec = ((float)tp/(tp+fp));
            rec = ((float)tp/expectedFinds); 
            if (Float.isNaN(prec)) prec = 0.0f;
            if (Float.isNaN(rec)) rec = 0.0f;
            tiResult.prec = prec;
            tiResult.rec = rec;
            tiResult.fn = fn;
            tiResult.fp = fp;
            tiResult.tp = tp;
            precSum += prec; // sums used to calculate avg prec & rec for a Tag
            recSum += rec;
          } else {
            //System.out.println("There were no close matches");
            tiResult.prec = Float.NaN;
            tiResult.rec = Float.NaN;
          }
          if (weakMatches != null) {
            resultRegions = getResultRegions(weakMatches);
            for (LabeledRegion rr : resultRegions) {
              tiResult.weakMatches.add(rr.toString());
            }
          } else {
            //System.out.println("There were no weak matches");
          }
          if (elongMatches != null) {
            resultRegions = getResultRegions(elongMatches);
            for (LabeledRegion rr : resultRegions) {
              tiResult.elongMatches.add(rr.toString());
            }
          } else {
            //System.out.println("There were no elongations");
          }
          if (altEndingMatches != null) {
            resultRegions = getResultRegions(altEndingMatches);
            for (LabeledRegion rr : resultRegions) {
              tiResult.altEndMatches.add(rr.toString());
            }
          } else {
            //System.out.println("There were no alternate endings");
          }

          tagInstanceResults.add(tiResult);
        } // end processing tag instance

        avgPrecisions[tagNum] = precSum/queryTagRegions.size();
        avgRecalls[tagNum] = recSum/queryTagRegions.size();

      } // end processing tag group
    }
    
    // PRINT RESULTS

    System.out.println("Results for nGramSize=" + Config.nGramSize + 
        ", nGramDensity=" + Config.nGramDensity +
        ", admittanceThreshhold=" + Config.admittanceThreshold +
        ", elongationFactor=" + Config.elongationFactor +
        ", fractionToMatch=" + Config.fractionToMatch + 
        ", useAggressiveFiltering=" + Config.useAggressiveFiltering);
    System.out.println("Instance Results: ");
    System.out.println("tag \tstag-inst                    " +
        "\tprec \trec \ttp \tfp \tfn \ttime(ms) \tclose-matches \tweak-matches \telongations \talt-endings");
    for (TagInstanceResult tiResult: tagInstanceResults) {
      System.out.printf("%s \t%-30s \t%.2f \t%.2f \t%d \t%d \t%d \t%d \t%s \t%s \t%s \t%s\n", 
          tiResult.tagGroupName,tiResult.tagInstanceName, tiResult.prec, tiResult.rec, 
          tiResult.tp, tiResult.fp, tiResult.fn,
          tiResult.searchTime, tiResult.closeMatches, tiResult.weakMatches,
          tiResult.elongMatches, tiResult.altEndMatches);
    }
    if (crossVal) {
      System.out.println("Tag Group Results:  ");
      System.out.println("tag\t\tprec\t\trec");
      for (int i = 0; i < tagIDs.length + 1; i++) {
        System.out.printf("%d \t %.2f \t %.2f\n", i, avgPrecisions[i],  avgRecalls[i]); 
      }
    }
  }

  public static void loadLabeledLogs(String logDirName, 
      List<Snippet> logList, Map<Snippet, 
      List<LabeledRegion>> snippetToRegionMap,
      Map<String, List<LabeledRegion>> tagToRegionMap) {
    LogLoader logLoader = LogLoader.getLogLoader();
    File logDir = new File(logDirName);
    for (File file : logDir.listFiles()) {
      if (file.getName().endsWith(".log")) {
        Debug.log("Loading log " + file.getAbsolutePath());
        Snippet s = logLoader.loadLogFile(file.getAbsolutePath()).get(0);
        Debug.log("Finished loading log" + file.getAbsolutePath());

        logList.add(s);
        for (File file2 : logDir.listFiles()) {
          if (file2.getName().endsWith(".tag") && 
              file2.getName().split("\\.")[0].equals(file.getName().split("\\.")[0])) {
            List<Tag> tags = processTagList(file2);
            List<LabeledRegion> regions = associateTags(tags, s);

            Debug.log("Processed file: " + file2);
            Debug.log("\tTags found are: " + tags);
            Debug.log("\tLabeledRegions are: " + regions);

            snippetToRegionMap.put(s, regions);
            
            for (LabeledRegion lr : regions) {
              String lbl = lr.tag.label;
              List<LabeledRegion> rList = tagToRegionMap.get(lbl);
              if (rList == null) {
                rList = new ArrayList<LabeledRegion>();
              }
              rList.add(lr);
              tagToRegionMap.put(lbl, rList);
              Debug.log("\t\tUpdated mapping for " + lbl + " to " + rList);
            }            
          }
        }
      }
    } // end processing of tags for logs
  }

  public static List<Tag> processTagList(File tagFile) {
    List<Tag> tags = new ArrayList<Tag>();
    
    long offset = 0;
    long syncStamp = 0;
    long syncEventStamp = 0;
    
    try {
      Scanner tagScanner = new Scanner(tagFile);
      Tag tag = null;
      String tagLabel = null;
      long startStamp = -1;
      long endStamp = -1;
                
      while (tagScanner.hasNext()) {
        String line = tagScanner.nextLine();
        if (line.startsWith("start")) {
          String [] startTagStamp = line.split(" ")[1].split(":");
          tagLabel = startTagStamp[0];

          startStamp = Long.parseLong(startTagStamp[1]) - offset;

          if (tag != null) {
            if (tag.endTime == -1) {
              tag.endTime = startStamp - 1; // handles two start stamps in a row
            }
          }
          tag = new Tag(startStamp, -1, tagLabel);
          if (tagLabel.contains("sync")) {
            tags.add(tag);
            tag = null; // don't look for an end tag
          }
        } else if (line.startsWith("end")) {          
          String [] endTagStamp = line.split(":");
          endStamp = Long.parseLong(endTagStamp[1]) - offset;
          if (tag != null) {
            tag.endTime = endStamp;
            tags.add(tag);
            tag = null;
          } 
        } else if (line.startsWith("offset")) {
          String [] offStr = line.split(":");
          offset = Long.parseLong(offStr[1]);    
          if (tags.size() > 0) {
            System.out.println("An offset tag appeared after some tags had been processed! Fix file " + tagFile);
          }
        } 
      }
      tagScanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return tags;
  }
  

  public static List<LabeledRegion> associateTags(List<Tag> tags, Snippet logSnippet) {
    
    ArrayList<LabeledRegion>regions = new ArrayList<LabeledRegion>();
    
    int logIndex = 0;
    int startIndex = -1;
    int endIndex = -1;
    Event startEvent = null;
    Event endEvent = null;
    
    long offset = 0;
    
    for (Tag tag : tags) {
      for ( ; logIndex < logSnippet.size(); logIndex++) {
        Event e = logSnippet.getEvents().get(logIndex);
        if (tag.label.contains("sync")) {
          String wText = e.getKeyValuePairs().get("wText");
          if (wText != null && wText.startsWith("Start Session")) {
            offset = e.getTimeStamp() - tag.startTime;
            Debug.log("Found start event: " + e.getTimeStamp() + ", compared to sync tag: " 
                + tag.startTime + ", offset = " + offset);
            break;
          } else {
            continue;
          }
        }
        long offsetTimeStamp = e.getTimeStamp() - offset;
        if (startEvent == null && offsetTimeStamp > tag.startTime) {
          startEvent = e;
          startIndex = logIndex;
        }
        if (offsetTimeStamp > tag.endTime) {
          endEvent = e;
          endIndex = logIndex;

          regions.add(new LabeledRegion(logSnippet, tag, startIndex, endIndex));

          startEvent = null;
          endEvent = null;
          startIndex = -1;
          endIndex = -1;
          break; // go to next tag
        }
      }
      // if we've run off the end of the log but still have an open tag, close it.
      if (startEvent != null) {
        endIndex = logSnippet.size() - 1;
        endEvent = logSnippet.getEvents().get(endIndex);        
        regions.add(new LabeledRegion(logSnippet, tag, startIndex, endIndex));
        break; // if there are any tags left, they can't have any associated events
      }
    }
    return regions;
  }

  public static void printLabeledSnippet(List<LabeledRegion> regions, Snippet s) {
    LabeledRegion currentRegion = null;
    String currentLabel = " ";
    int nextRegionIndex = 0;
    Collections.sort(regions);

    for (int i = 0; i < s.size(); i++) {

      if (nextRegionIndex >= regions.size()) {
        break;
      }
      if (currentRegion == null) {
        LabeledRegion r = regions.get(nextRegionIndex);
        if (i >= r.startIndex) {
          currentRegion = r;
          currentLabel = r.tag.label;
        }
      } else if (i >= currentRegion.endIndex) {
        currentRegion = null;
        currentLabel = " ";
        nextRegionIndex++;
      }
      System.out.println(currentLabel + "\t" + i + "\t" + s.getEvents().get(i).getTimeStamp() + "\t" + s.getEvents().get(i));
    }
  }

  public static void printLabeledRegion(LabeledRegion r) {
    Snippet s = r.getRegionSnippet();
    System.out.println("in file " + s.getSourceFilename());
    Shell.printEvents(s, 0, s.size());
  }
  
  public static List<Tag> getResultTags(ResultSet rs) {
    List<Tag> tags = new ArrayList<Tag> ();
    for (SubSequence subS : rs.getResults()) {
      Snippet sourceSnippet = subS.getSnippet();
      List<LabeledRegion> regionsInSnippet = logSnippetToTagMap.get(sourceSnippet);
      Collections.sort(regionsInSnippet);
      for (LabeledRegion reg : regionsInSnippet) {
        if ((subS.getStartIndex() >= reg.startIndex && subS.getStartIndex() < reg.endIndex) ||
            (subS.getEndIndex() > reg.startIndex && subS.getEndIndex() < reg.endIndex)) {
          tags.add(reg.tag); // this would allow more than one tag per result, potentially
        }
      }      
    }
    return tags;
  }
  
  public static List<ResultRegion> getResultRegions(ResultSet rs) {

    Map<LabeledRegion, SubSequence> foundResultMap = 
        new HashMap<LabeledRegion, SubSequence>();
    List<ResultRegion> regions = new ArrayList<ResultRegion> ();
    
    for (SubSequence subS : rs.getResults()) {
      Debug.log("Examining result " + subS);
      Snippet sourceSnippet = subS.getSnippet();
      List<LabeledRegion> regionsInSnippet = logSnippetToTagMap.get(sourceSnippet);
      Collections.sort(regionsInSnippet);
      boolean matchFound = false;
      for (LabeledRegion reg : regionsInSnippet) {
        if ((subS.getStartIndex() >= reg.startIndex && subS.getStartIndex() < reg.endIndex) ||
            (subS.getEndIndex() > reg.startIndex && subS.getEndIndex() <= reg.endIndex) ||
            (subS.getStartIndex() + subS.getLength() / 2 >= reg.startIndex &&
            subS.getStartIndex() + subS.getLength() / 2 <= reg.endIndex)) {
          SubSequence prevFoundMatch = foundResultMap.get(reg);
          if (prevFoundMatch != null) {
            if ((prevFoundMatch.getStartIndex() >= subS.getStartIndex() &&
                prevFoundMatch.getStartIndex() < subS.getEndIndex()) ||
                (prevFoundMatch.getEndIndex() > subS.getStartIndex() &&
                prevFoundMatch.getEndIndex() < subS.getEndIndex())) {
              System.err.println("Duplicate results found for " + reg + ": " + subS + " and " + prevFoundMatch);
            }
            //ignore the new result if we've found an overlap previously
          } else {
            ResultRegion rReg = new ResultRegion(reg, subS);
            regions.add(rReg); 
            foundResultMap.put(reg, subS);
            Debug.log("Added ResultRegion: " + rReg);
            matchFound = true;
          }
        }
      }  
      if (!matchFound) {
        Debug.log("No matching LabeledRegion found for " + subS);
      }
    }
    return regions;
  }
}
