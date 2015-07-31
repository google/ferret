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

import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.SubSequence;
import com.google.research.ic.ferret.data.UberResultSet;

public class EvalFramework {

  private static final int NGRAM_SIZE = 3;
  
  private static Map<String, List<LabeledRegion>> tagMap = new HashMap<String, List<LabeledRegion>>();
  private static Map<Snippet, List<LabeledRegion>> snippetTagMap = new HashMap<Snippet, List<LabeledRegion>>();
  
  private static final String DEFAULT_LOG_DIR = "eval-data";
  private static String logDirName = DEFAULT_LOG_DIR;
  
  private static float[] avgPrecisions = new float[41];
  private static float[] avgRecalls = new float[41];

  private static final class TagInstanceResult {
    public String tagInstanceName = null;
    public String tagGroupName = null;
    public float prec = -0.1f;
    public float rec = -0.1f;
    public List<String> closeMatches = null;
    public List<String> weakMatches = null;
    public long searchTime = -1;
    
    public TagInstanceResult() {
      closeMatches = new ArrayList<String>();
      weakMatches = new ArrayList<String>();      
    }
  }
  private static List<TagInstanceResult> tagInstanceResults = new ArrayList<TagInstanceResult>();
  
  public static void main(String[] args) {
    doEval();
  } 
  
  public static void doEval() {

    LogLoader logLoader = LogLoader.getLogLoader();
    List<Snippet> logs = new ArrayList<Snippet>();
    
    File logDir = new File(logDirName);
    for (File file : logDir.listFiles()) {
      if (file.getName().endsWith(".log")) {
        System.out.println("Loading log " + file.getAbsolutePath());
        Snippet s = logLoader.loadLogFile(file.getAbsolutePath()).get(0);
        System.out.println("Finished loading log" + file.getAbsolutePath());
        
        logs.add(s);
        long markerTime = getMarkerTime(s);
        for (File file2 : logDir.listFiles()) {
          if (file2.getName().endsWith(".tag") && 
              file2.getName().split("\\.")[0].equals(file.getName().split("\\.")[0])) {
            List<Tag> tags = processTagList(file2);
            List<LabeledRegion> regions = associateTags(tags, s);
            // Debugging
            
            System.out.println("Processed file: " + file2);
            System.out.println("\tTags found are: " + tags);
            System.out.println("\tLabeledRegions are: " + regions);
            
            snippetTagMap.put(s, regions);
            for (LabeledRegion lr : regions) {
              String lbl = lr.tag.label;
              List<LabeledRegion> rList = tagMap.get(lbl);
              if (rList == null) {
                rList = new ArrayList<LabeledRegion>();
              }
              rList.add(lr);
              tagMap.put(lbl, rList);
              System.out.println("\t\tUpdated mapping for " + lbl + " to " + rList);
            }            
          }
        }
      }
    }

    System.out.println("\\\\\\\\\\\\\\\\\\");
    System.out.println("\\\\\\\\\\\\\\\\\\");
    System.out.println("\\\\\\\\\\\\\\\\\\");
    
    SearchEngine.getSearchEngine().indexLogs(logs, NGRAM_SIZE);
    
    Set<String> tagLabels = tagMap.keySet();
    List<String> tagsToAnalyze = new ArrayList<String>();
    List<String> sortedTagLabels = Arrays.asList(tagLabels.toArray(new String[0]));
    Collections.sort(sortedTagLabels);
    float aggPrecSum = 0.0f;
    float aggRecSum = 0.0f;
    int numTagsProcessed = 0;
    int tagNum = 0;
    
    
    for (String lbl : sortedTagLabels) {
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
      
      numTagsProcessed++;
      System.out.println("\n****** Computing results for tag: " + lbl + " *******\n");

      float precSum = 0.0f;
      float recSum = 0.0f;
      
      // find all instances of a specified tag within the data
      List<LabeledRegion> tagRegions = tagMap.get(lbl);
      System.out.println("Tag instances (" + tagRegions.size() + "): " + tagRegions);

      for (LabeledRegion lr : tagRegions) {
        TagInstanceResult tiResult = new TagInstanceResult();
        tiResult.tagInstanceName = lr.toString();
        tiResult.tagGroupName = lbl;
        float prec = 0.0f;
        float rec = 0.0f;
        System.out.println("\nGetting Results for " + lr);
        //printLabeledRegion(lr);
        Snippet query = lr.getRegionSnippet();
        long t = System.currentTimeMillis();
        UberResultSet urs = SearchEngine.getSearchEngine().findMatches(query);
        tiResult.searchTime = System.currentTimeMillis() - t;
        ResultSet closeMatches = urs.getStrongMatches();
        ResultSet weakMatches = urs.getWeakMatches();
        List<LabeledRegion> resultRegions = null;
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
          if(!foundSelf){
            System.out.println(lr + " did not find itself");
          }
          int fn = tagRegions.size() - tp - 1; // account for self
          prec = ((float)tp/(tp+fp));
          rec = ((float)tp/(tagRegions.size() - 1)); // account for self
          if (Float.isNaN(prec)) prec = 0.0f;
          if (Float.isNaN(rec)) rec = 0.0f;
          tiResult.prec = prec;
          tiResult.rec = rec;

          precSum += prec; // sums used to calculate avg prec & rec for a Tag
          recSum += rec;
          //System.out.println("tp: " + tp + ", fp: " + fp  + ", fn: " + fn + ", prec: " + prec + ", rec: " + rec);
        } else {
          System.out.println("There were no close matches");
          tiResult.prec = Float.NaN;
          tiResult.rec = Float.NaN;
        }
        if (weakMatches != null) {
          resultRegions = getResultRegions(weakMatches);
          for (LabeledRegion rr : resultRegions) {
            tiResult.weakMatches.add(rr.toString());
          }
          //System.out.println("Weak-matching regions are: " + resultRegions);   
        } else {
          System.out.println("There were no weak matches");
        }
        tagInstanceResults.add(tiResult);
      } // end processing tag instance
      
      aggPrecSum += precSum;
      aggRecSum += recSum;

      avgPrecisions[tagNum] = precSum/tagRegions.size();
      avgRecalls[tagNum] = recSum/tagRegions.size();

      //System.out.println("avg prec: " + precSum/tagRegions.size() + ", avg rec: " + recSum/tagRegions.size());
      //System.out.println("\n*************************\n");
    } // end processing tag group
    
    System.out.println("Instance Results: ");
    System.out.println("\ttag-grp \ttag-inst \tprec \trec \tclose-time \tweak-time");
    for (TagInstanceResult tiResult: tagInstanceResults) {
      System.out.printf("\t%s \t%s \t%.2f \t%.2f \t%d \t%s\n", tiResult.tagGroupName,
          tiResult.tagInstanceName, tiResult.prec, tiResult.rec, tiResult.searchTime, tiResult.closeMatches);
    }
    
    System.out.println("tag\t\tprec\t\trec");
    for (int i = 0; i < avgPrecisions.length; i++) {
      System.out.printf("%d \t %.2f \t %.2f\n", i, avgPrecisions[i],  avgRecalls[i]); 
    }
    System.out.println("\nAGGREGATE PRECSION: " + aggPrecSum / numTagsProcessed);
    System.out.println("AGGREGATE RECALL: " + aggRecSum / numTagsProcessed);
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
  
  private static long getMarkerTime(Snippet s) {
    for (Event e : s.getEvents()) {
      if (e.getDisplayExtra().startsWith("Start Session")) {
        return e.getTimeStamp();
      }
    }
    return -1;
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
            System.out.println("Found start event: " + e.getTimeStamp() + ", compared to sync tag: " 
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
      List<LabeledRegion> regionsInSnippet = snippetTagMap.get(sourceSnippet);
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
  
  public static List<LabeledRegion> getResultRegions(ResultSet rs) {
    List<LabeledRegion> regions = new ArrayList<LabeledRegion> ();
    for (SubSequence subS : rs.getResults()) {
      Snippet sourceSnippet = subS.getSnippet();
      List<LabeledRegion> regionsInSnippet = snippetTagMap.get(sourceSnippet);
      Collections.sort(regionsInSnippet);
      for (LabeledRegion reg : regionsInSnippet) {
        if ((subS.getStartIndex() >= reg.startIndex && subS.getStartIndex() < reg.endIndex) ||
            (subS.getEndIndex() > reg.startIndex && subS.getEndIndex() < reg.endIndex)) {
          regions.add(reg); // this would allow more than one tag per result, potentially
        }
      }      
    }
    return regions;
  }
}
