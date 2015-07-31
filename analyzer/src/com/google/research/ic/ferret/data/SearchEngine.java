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

import com.google.research.ic.ferret.data.attributes.Attribute;
import com.google.research.ic.ferret.test.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SearchEngine {

  private static SearchEngine theSearchEngine = null;
  private ArrayList<Snippet> indexedLogs = null;
  
  private HashMap<String, Integer> identifierIds = new HashMap<String, Integer>();
  private ArrayList<Integer> nGramLengthsInUse = new ArrayList<Integer>();
  
  
  // Algorithm parameters
  private double nGramDensity = 0.33; // how many ngrams must be found for a region to be considered a candidate?
  private double admittanceThreshold = 0.5; // for all algorithms
  private int elongationFactor = 6; // multiples of query length, for finding elongations
  private double fractionToMatch = 0.25; // fraction of the query to match, for finding alternate endings
  
  /** inner class definitions */
  private static class LocatedNGram implements Comparable {
    public String nGramId = null;
    public int location = -1;
    public int nGramLength = -1;
    
    public LocatedNGram(String nGramId, int location, int nGramLength) {
      this.nGramId = nGramId;
      this.location = location;
      this.nGramLength = nGramLength;
    }
    
    public int compareTo(Object that) {
      return this.location - ((LocatedNGram) that).location;
    }
  }
  
  private static class Neighborhood {
    public Snippet snippet = null;
    public int startIndex = -1;
    public int endIndex = -1;
    public List<LocatedNGram> locatedNGrams = new ArrayList<LocatedNGram>();
  }
  
  private static class VectorNeighborhood extends Neighborhood {
    List<List<LocatedNGram>> vectors = new ArrayList<List<LocatedNGram>>();
  }
  
  private static class NeighborhoodCollector {
    public List<Neighborhood> strongMatchNeighborhoods = new ArrayList<Neighborhood>();
    public List<Neighborhood> elongationNeighborhoods = new ArrayList<Neighborhood>();
    public List<Neighborhood> alternateEndingNeighborhoods = new ArrayList<Neighborhood>();
    public List<Neighborhood> weakMatchNeighborhoods = new ArrayList<Neighborhood>();
  }  
  
  private static class CandidateCollector {
    public List<LocatedNGram> strongMatchCandidates = new ArrayList<LocatedNGram>();
    public List<LocatedNGram> elongationCandidates = new ArrayList<LocatedNGram>();
    public List<LocatedNGram> altEndingCandidates = new ArrayList<LocatedNGram>();
  }
  /** end inner class definitions */

  private SearchEngine() {
    indexedLogs = new ArrayList<Snippet>();
  }
  
  public static SearchEngine getSearchEngine() {
    if (theSearchEngine == null) {
      theSearchEngine = new SearchEngine();
    }
    return theSearchEngine;
  }
  
  public void clearIndex() {
    indexedLogs.clear();
  }
  
  public void indexLogs(List<Snippet> logs, int nGramLength) {
    for (Snippet s : logs) {
      buildNGramIndex(s, nGramLength);
      indexedLogs.add(s);
    }
    //indexSubSequences(logs, true);
  }
  
  private void buildNGramIndex(Snippet snippet, int nGramLength) {
    if (!nGramLengthsInUse.contains(Integer.valueOf(nGramLength))) {
      nGramLengthsInUse.add(Integer.valueOf(nGramLength));
    }
    int snipSize = snippet.getEvents().size();
    String nGramId = null;
    for (int i = 0; i < snipSize; i++) {
      nGramId = "|";
      if (i <= snipSize - nGramLength) {
        for (int j = 0; j < nGramLength; j++) {
          // construct an index for the ngram going forward from here
          nGramId += snippet.getEvents().get(j + i).getIdentifier() + "|";
        }
        snippet.addNGram(nGramLength, nGramId, i);
      }
    }
  }

  
  private HashMap<Integer, Integer> extractNGrams(SubSequence subsequence) {
    Event lastEvent = null;
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    int numEvents = identifierIds.size();
    for (Event evt : subsequence.getEvents()) {
      if (lastEvent != null) {
        int index = 
            numEvents * lastEvent.getIdentifierId() + evt.getIdentifierId();
        Integer value = map.get(index);
        if (value == null) {
          value = 0;
        }        
        map.put(index, value + 1);
      }
      lastEvent = evt;
    }
    return map;
  }

  public int retrieveOrRegisterIdentifier(String key) {
    Integer index = identifierIds.get(key);
    if (index == null) {
      index = identifierIds.size();
      identifierIds.put(key, index);
    }
    return index;
  }

  
  public UberResultSet findMatches(Snippet query){
    return findMatches(query, indexedLogs);
  }

  public UberResultSet findMatches(Snippet query, final List<Snippet> logs) {
    return findMatchesUsingPFEM(query, logs);
  }
  
  public UberResultSet findMatchesUsingPFEM(Snippet query, final List<Snippet> logs) {
    UberResultSet urs = new UberResultSet();
    
    // Loop through all nGramLengths. Typically we only use on nGramLength.
    for (Integer i : nGramLengthsInUse) {
      //Debug.log("nGramLengths " + nGramLengthsInUse);
      buildNGramIndex(query, i);
      List<Integer> matchedLocations = new ArrayList<Integer>();
      List<LocatedNGram> locatedNGrams = new ArrayList<LocatedNGram>();
      Map<String, List<Integer>> queryMap = query.getNGramTable(i);

      if (queryMap == null) {
        continue;
      }
      //Debug.log("Got queryMap for nGram length " + i + ": " + queryMap);
      
      for (Snippet log : logs) {
        locatedNGrams.clear();
        // go through each ngram in the query and find matching locations in this log
        for(String key : queryMap.keySet()) {
          
          List<Integer> theseMatches = log.getNGramTable(i).get(key);

          if (theseMatches != null) {
            for (Integer j: theseMatches) {
              LocatedNGram lng = new LocatedNGram(key, j.intValue(), i);   
              locatedNGrams.add(lng);
            }
          }
        }
        // now we have all the locations in the log where any ngram in the query matched
        // so we take a closer look at each location to compile a non-overlapping list of matches

        
        Collections.sort(locatedNGrams);
        
        //Some debugging output
        System.out.println("\n\n Located NGrams for log " + log.toString());
        for (LocatedNGram lng : locatedNGrams) {
          System.out.println(lng.location + "\t" + lng.nGramId);
        }

        CandidateCollector canColl = extractCandidates(locatedNGrams, query.size());
        NeighborhoodCollector neighColl = assignNeighborhoods(canColl, query.size());
        
//        if (matchedLocations.size() > 0) {
//          testMatchedLocations(query, log, matchedLocations, i, urs);        
//        }
      }
    }
    return urs;
  }
  
  private CandidateCollector extractCandidates(List<LocatedNGram> locatedNGrams, int querySize) {
    
    CandidateCollector collector = new CandidateCollector();

    Collections.sort(locatedNGrams);
    
    for (int i = 0; i < locatedNGrams.size(); i++) {
      LocatedNGram thisLNG = locatedNGrams.get(i);
      int strongNGramCount = 0;
      int elongNGramCount = 0;
      int altEndNGramCount = 0;

      int elongationLength = querySize * elongationFactor; 
      int altEndLength = (int) Math.ceil(querySize * fractionToMatch + 1);
      
      for (int j = i + 1; j < locatedNGrams.size(); j++) {
        LocatedNGram upstreamLNG = locatedNGrams.get(j);
        if (upstreamLNG.location + upstreamLNG.nGramLength < 
            thisLNG.location + altEndLength) {
          altEndNGramCount++;
        }
        
        if (upstreamLNG.location + upstreamLNG.nGramLength < 
            thisLNG.location + querySize) {
          strongNGramCount++;
        }
        
        if (upstreamLNG.location + upstreamLNG.nGramLength < 
            thisLNG.location + elongationLength) {
          elongNGramCount++;
        } else {
          break; // if we've run off the end of the elongation length, we can stop
        }
      }
      double strongDensity = (double) strongNGramCount / (double) querySize;
      double elongDensity = (double) elongNGramCount / (double) elongationLength;
      double altEndDensity = (double) altEndNGramCount / (double) altEndLength;

      if (strongDensity >= nGramDensity) {
        collector.strongMatchCandidates.add(thisLNG);
      }
      if (elongDensity >= nGramDensity) {
        collector.elongationCandidates.add(thisLNG);
      }
      if (altEndDensity >= nGramDensity) {
        collector.altEndingCandidates.add(thisLNG);
      }
    }
    return collector;
  }
  
  public NeighborhoodCollector assignNeighborhoods(CandidateCollector canColl, int querySize) {
    
    NeighborhoodCollector collector = new NeighborhoodCollector();
    
    Neighborhood currentNeighborhood = null;
    
    for (int i = 0; i < canColl.strongMatchCandidates.size(); i++) {
      LocatedNGram thisLNG = canColl.strongMatchCandidates.get(i);
      if (currentNeighborhood == null) {
        currentNeighborhood = new Neighborhood();
        currentNeighborhood.locatedNGrams.add(thisLNG);
        currentNeighborhood.startIndex = thisLNG.location;
        currentNeighborhood.endIndex = thisLNG.location + querySize;
        
      }
      
    }     
    
    return collector;
  }
  
  public void testMatchedLocations(Snippet query, Snippet log, List<Integer> matchedLocations, int nGramLength, UberResultSet urs) {

    int minDist = Integer.MAX_VALUE;
    int bestLoc = -1;
    int threshhold = query.getEvents().size() / 2;
    int length = query.getEvents().size();
    int offset = length / 2;
    
    Map<Integer, Integer> closeLocs = new HashMap<Integer, Integer>();
    List<List<Integer>> neighborhoods = new ArrayList<List<Integer>>();
    List<Integer> currentNeighborhood = new ArrayList<Integer>();
    
    Map<Integer, Integer> weakDistances = new HashMap<Integer, Integer>();
    
    Collections.sort(matchedLocations);

    // calculate neighborhoods
    Integer last = -1;
    for (Integer i : matchedLocations) {
      if (last != -1 && i > last + offset) {
        neighborhoods.add(currentNeighborhood);
        currentNeighborhood = new ArrayList<Integer>();
      }
      currentNeighborhood.add(i);
      last = i;
    }
    neighborhoods.add(currentNeighborhood);
    
//    Debug.log("Examining snippet " + log.getUserName() + "-" 
//        + log.getStartDate().getTime().toString() 
//        + " (size=" + log.getEvents().size() + ")"
//        + ", found neighborhoods: " + neighborhoods);
    
    for (List<Integer> nhood : neighborhoods) {
      
      if (nhood == null || nhood.size() < 1) {
        continue;   
      }
      
      int startIndex = nhood.get(0) - offset;
      if (startIndex < 0) {
        startIndex = 0;
      }
      int endIndex = nhood.get(nhood.size() - 1) + offset;
      if (endIndex > log.getEvents().size()){
        endIndex = log.getEvents().size(); 
      }
//      Debug.log("Testing locations around " + nhood + " using startIndex: " 
//          + startIndex + " and endIndex: "+ endIndex);
      
      for (int j = startIndex; j < endIndex; j++) {
        int dist = computeEditDistance(query, log, j, j + length);
//        Debug.log("Computed distance between query (size=" 
//            + query.getEvents().size() + ") and log from location " 
//            + j + " to " + (j + length) + " which was " + dist
//            + "(minDist is " + minDist + " and threshold is " + threshhold);
        if (dist < minDist) {
          minDist = dist;
          bestLoc = j;
        }
      }
      if (minDist <= threshhold) {
        closeLocs.put(Integer.valueOf(bestLoc), minDist);// add bestLoc to matches
      } else {
        Map <Integer, Integer> bestWeakDist = computeWeakDistance(nhood, nGramLength, query.getEvents().size());
        weakDistances.putAll(bestWeakDist);
      }
      minDist = Integer.MAX_VALUE; 
    }
    
    List<SubSequence> closeOnes = new ArrayList<SubSequence>();
    List<SubSequence> weakOnes = new ArrayList<SubSequence> ();
    
    for(Integer j : closeLocs.keySet()) {
      SubSequence ss = new SubSequence(j, Math.min(j + query.getEvents().size(), log.getEvents().size()), log);
      ss.setDistance(closeLocs.get(j));
      closeOnes.add(ss);
    }
    for (Integer j : weakDistances.keySet()) {
      SubSequence ss = new SubSequence(j, Math.min(j + query.getEvents().size(), log.getEvents().size()), log);
      ss.setDistance(weakDistances.get(j));
      weakOnes.add(ss);
    }
    
    ResultSet closeMatches = urs.getCloseMatches();
    if (closeMatches == null) {
      closeMatches = new ResultSet(null, query);
    }
    closeMatches.addResults(closeOnes);
    urs.setCloseMatches(closeMatches);
    
    ResultSet weakMatches = urs.getWeakMatches();
    if (weakMatches == null) {
      weakMatches = new ResultSet(null, query);
    }
    weakMatches.addResults(weakOnes);
    urs.setWeakMatches(weakMatches);
  }
  
  /**
   * 
   * @param query
   * @param log
   * @param startIndex is inclusive
   * @param endIndex is exclusive
   * @return
   */
  public int computeEditDistance(Snippet query, Snippet log, int startIndex, int endIndex) {
    
    if (endIndex > log.getEvents().size()) {
      endIndex = log.getEvents().size();
    }
    int len1 = query.getEvents().size();
    int len2 = endIndex - startIndex;
 
    // len1+1, len2+1, because finally return dp[len1][len2]
    
    int[][] dp = new int[len1 + 1][len2 + 1];
 
    for (int i = 0; i <= len1; i++) {
        dp[i][0] = i;
    }
 
    for (int j = 0; j <= len2; j++) {
        dp[0][j] = j;
    }
 
    //iterate though, and check last char
    for (int i = 0; i < len1; i++) {
        Event e1 = query.getEvents().get(i);
        for (int j = 0; j < len2; j++) {
//            System.out.println("Comparing event " + (j + startIndex) + " in log of size " + 
//                log.getEvents().size() + " to event " + i +
//                " in query of size " + query.getEvents().size() +
//                " startIndex is " + startIndex + 
//                " and endIndex is " + endIndex);
            
             
            Event e2 = log.getEvents().get(startIndex + j);
 
            //if last two chars equal
            if (e1.getIdentifier().equals(e2.getIdentifier())) {
                //update dp value for +1 length
                dp[i + 1][j + 1] = dp[i][j];
            } else {
                int replace = dp[i][j] + 1;
                int insert = dp[i][j + 1] + 1;
                int delete = dp[i + 1][j] + 1;
 
                int min = replace > insert ? insert : replace;
                min = delete > min ? min : delete;
                dp[i + 1][j + 1] = min;
            }
//            printMatrix(dp);
        }
    }
 
    return Math.max(dp[len1][len2], Math.abs(len1 - len2));
  }

  /**
   * 
   * @param nhood: a sorted list of locations where an ngram (any ngram) was found in the log
   * @param qLength: the length of the query 
   * @return
   */
  public Map<Integer, Integer> computeWeakDistance(List<Integer> nhood, int nGramLength, int qLength) {
    float minDist = Float.MAX_VALUE;
    int bestLoc = -1;
    Integer[] nh = nhood.toArray(new Integer[0]);
    for (int i = 0; i < nh.length; i++) {
      int loc = nh[i];
      int count = 1;
      for (int j = i; j < nh.length; j++) {
        if (nh[j] < loc + qLength) {
          count++;
        } else {
          break;
        }         
      }
      float dist = (qLength / nGramLength) / count;
      if (dist < minDist) {
        minDist = dist;
        bestLoc = loc;
      }
    }
    //Debug.log("Examining " + nhood + ", best dist is " + minDist + " and best loc is " + bestLoc);
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(Integer.valueOf(bestLoc), Math.round(minDist * 100));
    return map;
  }
  
  public void printMatrix(int[][] m) {
    for (int i = 0; i < m.length; i++) {
      for (int j = 0; j < m[i].length; j++) {
        System.out.print(m[i][j] + " ");
      }
      System.out.print("\n");
    }
  }
    
  public ArrayList<Snippet> getLogSnippets() {
    return indexedLogs;
  }
}
