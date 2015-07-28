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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SearchEngine {

  private static SearchEngine theSearchEngine = null;
  private ArrayList<Snippet> indexedLogs = null;
  private static int MAX_SUBSEQUENCE_LENGTH = 300;
  
  /** NGram string to their index in the vector space */
  private HashMap<String, Integer> identifierIds = new HashMap<String, Integer>();
  private ArrayList<Integer> nGramLengthsInUse = new ArrayList<Integer>();
  
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
  
  public void indexSubSequences(final List<Snippet> logs, boolean reset) {
    if (reset) {
      clearIndex();
    }
    ParallelTaskExecutor.getInstance().compute(logs.size(), new ParallelTask() {
      @Override
      public void init(int taskId) { 
      }

      @Override
      public Boolean compute(int index) {
        indexSubSequences(logs.get(index));
        return true;
      }
    });
    indexedLogs.addAll(logs);
  }
  
  private void indexSubSequences(Snippet snippet) {
    int size = snippet.getEvents().size();
    // for debugging
    final int numSubSequences = size * (size - 1) / 2;
    int numComplete = 0;
    List<SubSequence> subsequences = new ArrayList<SubSequence>(numSubSequences);
    for (int start = 0; start < size; start++) {
      int endIdx = (MAX_SUBSEQUENCE_LENGTH < (size - start)) ? (start + MAX_SUBSEQUENCE_LENGTH) : size;
      for (int end = start + 1; end <= endIdx; end++) {
        SubSequence subsequence = new SubSequence(start, end, snippet);
        subsequence.setNGram(extractNGrams(subsequence));
        subsequences.add(subsequence);
        numComplete++;
      }
    }
    snippet.setIndexedSequences(subsequences);
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

  
  public UberResultSet searchMatches(Snippet query){
    return searchMatches(query, indexedLogs);
  }

  public UberResultSet searchMatches(Snippet query, final List<Snippet> logs) {
    return searchMatchesUsingPFEM(query, logs);
  }
  
  public UberResultSet searchMatchesUsingPFEM(Snippet query, final List<Snippet> logs) {
    List<SubSequence> results = new ArrayList<SubSequence>();
    UberResultSet urs = new UberResultSet();
    
    
    for (Integer i : nGramLengthsInUse) {
      //Debug.log("nGramLengths " + nGramLengthsInUse);
      buildNGramIndex(query, i);
      List<Integer> matchedLocations = new ArrayList<Integer>();
      Map<String, List<Integer>> queryMap = query.getNGramTable(i);
      if (queryMap == null) {
        continue;
      }
      //Debug.log("Got queryMap for nGram length " + i + ": " + queryMap);
      
      for (Snippet log : logs) {
        matchedLocations.clear();
        // go through each ngram in the query and find matching locations in this log
        for(String key : queryMap.keySet()) {
          
          List<Integer> theseMatches = log.getNGramTable(i).get(key);
          if (theseMatches != null) {
            matchedLocations.addAll(theseMatches); 
          }
        }
        // now we have all the locations in the log where any ngram in the query matched
        // so we take a closer look at each location to compile a non-overlapping list of matches
        if (matchedLocations.size() > 0) {
          testMatchedLocations(query, log, matchedLocations, i, urs);        
        }
      }
    }
    return urs;
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
  
  public ResultSet searchMatchesUsingSubsequences(Snippet query) {
    return searchMatchesUsingSubsequences(query, indexedLogs);
  }
  
  public ResultSet searchMatchesUsingSubsequences(Snippet query, final List<Snippet> logs) {
    // parallel searching was causing null entries to be added to the results list
    // So turned off for now
    boolean useParallel = false; 
    
    /** Extract ngrams for the entire query sequence */
    final SubSequence querySequence = new SubSequence(0, query.getEvents().size(), query);
    querySequence.setNGram(extractNGrams(querySequence));
    
    final ArrayList<SubSequence> results = new ArrayList<SubSequence>();
    final Map<String, Attribute> attributes = new HashMap<String, Attribute>();
    /** 
     * Calculate the distance between the query and each indexed subsequence of
     * the log streams based on their ngram representations.
     */
    if (useParallel) {
      ParallelTaskExecutor.getInstance().compute(logs.size(), new ParallelTask() {
        @Override
        public void init(int taskId) { 
        }
        @Override
        public Boolean compute(int index) {
          for (SubSequence subsequence : logs.get(index).getIndexedSubsequences()) {
            double distance = distance(subsequence.getNGram(), querySequence.getNGram());
            subsequence.setDistance(distance);
            results.add(subsequence);
            for (Attribute attr : subsequence.getSnippet().getAttributes()) {
              attributes.put(attr.getKey(), attr);
            }
          }
          return true;
        }
      });

    } else {
      for (Snippet log : logs){
        for (SubSequence subsequence : log.getIndexedSubsequences()) {
          double distance = distance(subsequence.getNGram(), querySequence.getNGram());
          subsequence.setDistance(distance);
          results.add(subsequence);
          for (Attribute attr : subsequence.getSnippet().getAttributes()) {
            attributes.put(attr.getKey(), attr);
          }
        }
      }
    }
    try {
      Collections.sort(results);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ResultSet(results, query);
  }

  private double distance(HashMap<Integer, Integer> ngram1,
      HashMap<Integer, Integer> ngram2) {
    return cosineDistance(ngram1, ngram2);
  }
  
  private double euclideanDistance(HashMap<Integer, Integer> ngram1,
      HashMap<Integer, Integer> ngram2) {
    HashSet<Integer> indices = new HashSet<Integer>();
    indices.addAll(ngram1.keySet());
    indices.addAll(ngram2.keySet());
    float sum = 0;
    for (Integer index : indices) {
      Integer value1 = ngram1.get(index);
      Integer value2 = ngram2.get(index);
      int v1 = 0;
      if (value1 != null) {
        v1 = value1;
      }
      int v2 = 0;
      if (value2 != null) {
        v2 = value2;
      }
      sum += (v1 - v2) * (v1 - v2);
    }
    return sum;
  }
  
  private double cosineDistance(HashMap<Integer, Integer> ngram1,
      HashMap<Integer, Integer> ngram2) {
    double magnitude1 = computeMagnitude(ngram1);
    double magnitude2 = computeMagnitude(ngram2);
    double denominator = magnitude1 * magnitude2;
    HashSet<Integer> indices = new HashSet<Integer>();
    indices.addAll(ngram1.keySet());
    indices.addAll(ngram2.keySet());
    float sum = 0;
    for (Integer index : indices) {
      Integer value1 = ngram1.get(index);
      Integer value2 = ngram2.get(index);
      int v1 = 0;
      if (value1 != null) {
        v1 = value1;
      }
      int v2 = 0;
      if (value2 != null) {
        v2 = value2;
      }
      sum += v1 * v2;
    }
    return Math.acos(sum / denominator) * 2 / Math.PI;
  }

  private double computeMagnitude(HashMap<Integer, Integer> ngram) {
    int sum = 0;
    for (Integer value : ngram.values()) {
      sum += value * value;
    }
    return Math.sqrt(sum);
  }
  
  public ArrayList<Snippet> getLogSnippets() {
    return indexedLogs;
  }
}
