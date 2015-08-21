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

import com.google.research.ic.ferret.Config;
import com.google.research.ic.ferret.test.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchEngine {

  private static SearchEngine theSearchEngine = null;
  //private List<Snippet> indexedLogs = null;
  private Map<String, Snippet> indexedLogMap = new HashMap<String, Snippet>();
  
  private HashMap<String, Integer> identifierIds = new HashMap<String, Integer>();
  private ArrayList<Integer> nGramLengthsInUse = new ArrayList<Integer>();
    
  /** inner class definitions */
  private static class LocatedNGram implements Comparable<LocatedNGram> {
    public String nGramId = null;
    public int location = -1;
    public int nGramCount = -1;
    public LocatedNGram(String nGramId, int location, int nGramCount) {
      this.nGramId = nGramId;
      this.location = location;
      this.nGramCount = nGramCount;
    }
    
    @Override
    public int compareTo(LocatedNGram that) {
      return this.location - ((LocatedNGram) that).location;
    }
    
    @Override
    public String toString() {
      return "LocatedNGram[location:" + location + "]"; 
    }
  }
  
  private static class CandidateCollector {
    public List<CandidateLocatedNGram> strongMatchCandidates = new ArrayList<CandidateLocatedNGram>();
    public List<CandidateLocatedNGram> elongationCandidates = new ArrayList<CandidateLocatedNGram>();
    public List<CandidateLocatedNGram> altEndingCandidates = new ArrayList<CandidateLocatedNGram>();

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("CandidateCollector");
      sb.append("\n\tstrong:" + strongMatchCandidates);
      sb.append("\n\telong:" + elongationCandidates);
      sb.append("\n\taltEnd" + altEndingCandidates);
      return sb.toString();
    }
  }
  
  private static class CandidateLocatedNGram extends LocatedNGram {
    
    public double nGramDensity = -1.0d;
    public int sizeToCompare = -1;
    
    public CandidateLocatedNGram(LocatedNGram lng, double nGramDensity, int sizeToCompare) {
      super (lng.nGramId, lng.location, lng.nGramCount);
      this.nGramDensity = nGramDensity;
      this.sizeToCompare= sizeToCompare;
    }
    @Override
    public String toString() {
      return "CandidateLocatedNGram[location:" + location + ",density:" + nGramDensity + "]"; 
    }
  }

  private static class Neighborhood {
    public Snippet snippet = null;
    public int startIndex = -1;
    public int endIndex = -1;
    public List<LocatedNGram> locatedNGrams = new ArrayList<LocatedNGram>();

    @Override
    public String toString() {
      return "NHood[" + startIndex + "-" + endIndex + "],size=" + locatedNGrams.size();
    }
  }
  
  
  
  private static class NeighborhoodCollector {
    public List<Neighborhood> strongMatchNeighborhoods = new ArrayList<Neighborhood>();
    public List<Neighborhood> elongationNeighborhoods = new ArrayList<Neighborhood>();
    public List<Neighborhood> altEndingNeighborhoods = new ArrayList<Neighborhood>();
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("NeighborhoodCollector");
      sb.append("\n\tstrong:" + strongMatchNeighborhoods);
      sb.append("\n\telong:" + elongationNeighborhoods);
      sb.append("\n\taltEnd:" + altEndingNeighborhoods);
      return sb.toString();
    }
  }  
  
  private static class PromotedLocation {
    public int location = -1;
    public double weightedDistance = -1.0d;
    
    public PromotedLocation(int loc, double dist) {
      location = loc;
      weightedDistance = dist;
    }
    
    @Override
    public String toString() {
      return "PLoc[loc:" + location + ",dist:" + weightedDistance + "]";
    }
  }
  
  private static class PromotionCollector {
    public List<PromotedLocation> strongMatchFinalCut = new ArrayList<PromotedLocation>();
    public List<PromotedLocation> elongationFinalCut = new ArrayList<PromotedLocation>();
    public List<PromotedLocation> altEndingFinalCut = new ArrayList<PromotedLocation>();

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("PromotionCollector");
      sb.append("\n\tstrong:" + strongMatchFinalCut);
      sb.append("\n\telong" + elongationFinalCut);
      sb.append("\n\taltEnd" + altEndingFinalCut);
      return sb.toString();
    }
  }

  
  /** end inner class definitions */

  private SearchEngine() {
    indexedLogMap = new HashMap<String, Snippet>();
    //indexedLogs = new ArrayList<Snippet>();
  }
  
  public static SearchEngine getSearchEngine() {
    if (theSearchEngine == null) {
      theSearchEngine = new SearchEngine();
    }
    return theSearchEngine;
  }
  
  public void clearIndex() {
    indexedLogMap.clear();
    nGramLengthsInUse.clear();
  }
  
  public void indexLogs(List<Snippet> logs, int nGramLength) {
    for (Snippet s : logs) {
      buildNGramIndex(s, nGramLength);
      indexedLogMap.put(s.getId(), s);
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
    return findMatches(query, getAllLogs());
  }

  public UberResultSet findMatches(Snippet query, final List<Snippet> logs) {
    return findMatchesUsingPartitioning(query, logs);
  }
  
  public UberResultSet findMatchesUsingPartitioning(Snippet query, final List<Snippet> logs) {
    UberResultSet urs = new UberResultSet(query);
    
    // Loop through all nGramLengths. Typically we only use on nGramLength.
    for (Integer i : nGramLengthsInUse) {
      //Debug.log("nGramLengths " + nGramLengthsInUse);
      buildNGramIndex(query, i);
      List<Integer> matchedLocations = new ArrayList<Integer>();
      List<LocatedNGram> locatedNGrams = new ArrayList<LocatedNGram>();
      Map<String, List<Integer>> queryMap = query.getNGramTable(i);
      List<LocatedNGram> queryNGramLocations = new ArrayList<LocatedNGram>();
      
      if (queryMap == null) {
        continue;
      }
      //Debug.log("Got queryMap for nGram length " + i + ": " + queryMap);
      
      for (Snippet log : logs) {
        locatedNGrams.clear();
        // go through each ngram in the query and find matching locations in this log
        for(String key : queryMap.keySet()) {
          
          List<Integer> nGramLocs = queryMap.get(key);
          for (Integer j : nGramLocs) {
            queryNGramLocations.add(new LocatedNGram(key, j.intValue(), i));
          }
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
        Debug.log("\n\n****Looking at log: " + log.toString() + "****");
        Debug.log("LocatedNGrams: " + locatedNGrams);
        CandidateCollector canColl = extractCandidates(locatedNGrams, query.size());
        Debug.log("Candidates: " + canColl.toString());
        NeighborhoodCollector neighColl = assignNeighborhoods(canColl, query.size());
        Debug.log("Neighborhoods: " + neighColl.toString());
        PromotionCollector promColl = electNeighborhoodRepresentatives(neighColl, query, log, queryNGramLocations);
        Debug.log("Promotions: " + promColl.toString());
        urs.mergeResults(admitResults(promColl, query, log));
        pruneResults(urs);
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

      int elongationLength = querySize * Config.elongationFactor; 
      int altEndLength = (int) Math.ceil(querySize * Config.fractionToMatch + 1);
      
      for (int j = i + 1; j < locatedNGrams.size(); j++) {
        LocatedNGram upstreamLNG = locatedNGrams.get(j);
        if (upstreamLNG.location + upstreamLNG.nGramCount < 
            thisLNG.location + altEndLength) {
          altEndNGramCount++;
        }
        
        if (upstreamLNG.location + upstreamLNG.nGramCount < 
            thisLNG.location + querySize) {
          strongNGramCount++;
        }
        
        if (upstreamLNG.location + upstreamLNG.nGramCount < 
            thisLNG.location + elongationLength) {
          elongNGramCount++;
        } else {
          break; // if we've run off the end of the elongation length, we can stop
        }
      }
      double strongDensity = (double) strongNGramCount / (double) querySize;
      double elongDensity = (double) elongNGramCount / (double) elongationLength;
      double altEndDensity = (double) altEndNGramCount / (double) altEndLength;

      if (strongDensity >= Config.nGramDensity) {
        collector.strongMatchCandidates.add(new CandidateLocatedNGram(thisLNG, strongDensity, querySize));
      }
      if (elongDensity >= Config.nGramDensity) {
        collector.elongationCandidates.add(new CandidateLocatedNGram(thisLNG, elongDensity, elongationLength));
      }
      if (altEndDensity >= Config.nGramDensity) {
        collector.altEndingCandidates.add(new CandidateLocatedNGram(thisLNG, altEndDensity, altEndLength));
      }
    }
    return collector;
  }
  
  public NeighborhoodCollector assignNeighborhoods(CandidateCollector canColl, int querySize) {
    
    NeighborhoodCollector collector = new NeighborhoodCollector();
    
    Neighborhood currentNeighborhood = null;
    
    for (int i = 0; i < canColl.strongMatchCandidates.size(); i++) {
      LocatedNGram thisLNG = canColl.strongMatchCandidates.get(i);
      if (currentNeighborhood != null && 
          ((thisLNG.location > currentNeighborhood.startIndex && 
              thisLNG.location <= currentNeighborhood.endIndex) ||
              (thisLNG.location + querySize >= currentNeighborhood.startIndex && 
              thisLNG.location + querySize < currentNeighborhood.endIndex))) {
        currentNeighborhood.startIndex = Math.min(currentNeighborhood.startIndex, thisLNG.location);
        currentNeighborhood.endIndex = Math.max(currentNeighborhood.endIndex, thisLNG.location + querySize);
        currentNeighborhood.locatedNGrams.add(thisLNG);
      } else {
        currentNeighborhood = new Neighborhood();
        currentNeighborhood.locatedNGrams.add(thisLNG);
        currentNeighborhood.startIndex = thisLNG.location;
        currentNeighborhood.endIndex = thisLNG.location + querySize;
        collector.strongMatchNeighborhoods.add(currentNeighborhood);
      }
    }
    
    currentNeighborhood = null;
    
    int altEndSize = (int) Math.ceil(querySize * Config.fractionToMatch);
    for (int i = 0; i < canColl.altEndingCandidates.size(); i++) {
      LocatedNGram thisLNG = canColl.altEndingCandidates.get(i);
      if (currentNeighborhood != null && 
          ((thisLNG.location > currentNeighborhood.startIndex && 
              thisLNG.location <= currentNeighborhood.endIndex) ||
              (thisLNG.location + altEndSize >= currentNeighborhood.startIndex && 
              thisLNG.location + altEndSize < currentNeighborhood.endIndex))) {
        currentNeighborhood.startIndex = Math.min(currentNeighborhood.startIndex, thisLNG.location);
        currentNeighborhood.endIndex = Math.max(currentNeighborhood.endIndex, thisLNG.location + altEndSize);
        currentNeighborhood.locatedNGrams.add(thisLNG);
      } else {
        currentNeighborhood = new Neighborhood();
        currentNeighborhood.locatedNGrams.add(thisLNG);
        currentNeighborhood.startIndex = thisLNG.location;
        currentNeighborhood.endIndex = thisLNG.location + altEndSize;
        collector.altEndingNeighborhoods.add(currentNeighborhood);
      }
    }
    
    currentNeighborhood = null;
    
    int elongSize = querySize * Config.elongationFactor;
    for (int i = 0; i < canColl.elongationCandidates.size(); i++) {
      LocatedNGram thisLNG = canColl.elongationCandidates.get(i);
      if (currentNeighborhood != null && 
          ((thisLNG.location > currentNeighborhood.startIndex && 
              thisLNG.location <= currentNeighborhood.endIndex) ||
              (thisLNG.location + elongSize >= currentNeighborhood.startIndex && 
              thisLNG.location + elongSize < currentNeighborhood.endIndex))) {
        currentNeighborhood.startIndex = Math.min(currentNeighborhood.startIndex, thisLNG.location);
        currentNeighborhood.endIndex = Math.max(currentNeighborhood.endIndex, thisLNG.location + elongSize);
        currentNeighborhood.locatedNGrams.add(thisLNG);
      } else {
        currentNeighborhood = new Neighborhood();
        currentNeighborhood.locatedNGrams.add(thisLNG);
        currentNeighborhood.startIndex = thisLNG.location;
        currentNeighborhood.endIndex = thisLNG.location + elongSize;
        collector.elongationNeighborhoods.add(currentNeighborhood);
      }
    }
    
    return collector;
  }

  public PromotionCollector electNeighborhoodRepresentatives(NeighborhoodCollector nCollector, 
      Snippet query, Snippet log, List<LocatedNGram> queryNGramLocations) {
    PromotionCollector collector = new PromotionCollector();

    int offset = query.size() / 2;

    for (Neighborhood nHood : nCollector.strongMatchNeighborhoods) {

      int startIndex = nHood.startIndex - offset;
      if (startIndex < 0) {
        startIndex = 0;
      }
      int endIndex = nHood.endIndex + offset;
      if (endIndex > log.getEvents().size()){
        endIndex = log.getEvents().size(); 
      }

      double minDist = Double.MAX_VALUE;
      int bestLoc = -1;

      for (int i = startIndex; i < endIndex; i++) {
        int dist = computeEditDistance(query, log, i, i + query.size());

        double normalizedDist = (double) dist / (double) query.size();
        if (normalizedDist < minDist) {
          minDist = normalizedDist;
          bestLoc = i;
        }
      }
      PromotedLocation pl = new PromotedLocation(bestLoc, minDist);
      collector.strongMatchFinalCut.add(pl);
    }

    int elongSize = query.size() * Config.elongationFactor;
    offset = elongSize / 2;

    for (Neighborhood nHood : nCollector.elongationNeighborhoods) {

      List<LocatedNGram> noiselessVector = null;
      
      List<LocatedNGram> nHoodNGrams = nHood.locatedNGrams;
      Collections.sort(nHoodNGrams);
      
      List<List<LocatedNGram>> nHoodVectors = new ArrayList<List<LocatedNGram>> ();
      
      for (int i = 0; i < nHoodNGrams.size(); i++) {
        LocatedNGram thisLNG = nHoodNGrams.get(i);
        noiselessVector = new ArrayList<LocatedNGram>();
        noiselessVector.add(thisLNG);
        for (int j = i + 1; j < nHoodNGrams.size(); j++) {
          LocatedNGram thatLNG = nHoodNGrams.get(j); 
          if (thatLNG.location < thisLNG.location + elongSize) {
            noiselessVector.add(thatLNG);
          } else {
            break; // list is sorted, so when we find an nGram past elongSize, we're done with this vector
          }
        }
        nHoodVectors.add(noiselessVector);
      }
      
      Collections.sort(queryNGramLocations);
      
      double minDist = Double.MAX_VALUE;
      int bestLoc = -1;

      for (List<LocatedNGram> vector : nHoodVectors) {
        int dist = computeVectorEditDistance(vector, nHoodNGrams, 0, vector.size(), 0, nHoodNGrams.size());
        double normalizedDist = (double) dist / (double) query.size();
        if (normalizedDist < minDist) {
          minDist = normalizedDist;
          bestLoc = vector.get(0).location;
        }
      }
      
      PromotedLocation pl = new PromotedLocation(bestLoc, minDist);
      collector.elongationFinalCut.add(pl);
    }
    
    int altEndSize = (int) Math.ceil(query.size() * Config.fractionToMatch);
    offset = altEndSize / 2;
    for (Neighborhood nHood : nCollector.altEndingNeighborhoods) {

      int startIndex = nHood.startIndex - offset;
      if (startIndex < 0) {
        startIndex = 0;
      }
      int endIndex = nHood.endIndex + offset;
      if (endIndex > log.getEvents().size() - query.size()){
        endIndex = log.getEvents().size() - query.size(); 
      }

      double minDist = Double.MAX_VALUE;
      int bestLoc = -1;

      for (int i = startIndex; i < endIndex; i++) {
        int narrowDist = computeEditDistance(query, log, i, i + altEndSize);
        int wideDist = computeEditDistance(query, log, i, i + query.size() - altEndSize);
        //System.out.println("i = " + i + " query.size() = " + query.size() + " altEndSize = " + altEndSize);
        int endDist = computeEditDistance(query, log, i + query.size() - altEndSize, i + query.size());
        double normalizedNarrowDist = (double) narrowDist / (double) altEndSize;
        double normalizedWideDist = (double) wideDist / (double) (query.size() - altEndSize);
        double normalizedEndDist = (double) endDist / (double) altEndSize;

        if (normalizedEndDist > Config.admittanceThreshold) { // screen out ones where the end matches too well
          double minNWDist = Math.min(normalizedNarrowDist, normalizedWideDist);
          if (minNWDist < minDist) {
            minDist = minNWDist;
            bestLoc = i;
          }
        }
      }
      PromotedLocation pl = new PromotedLocation(bestLoc, minDist);
      collector.altEndingFinalCut.add(pl);
    } 
    return collector;    
  }
  
  public UberResultSet admitResults(PromotionCollector promColl, Snippet query, Snippet log) {

    UberResultSet urs = new UberResultSet(query);

    List<SubSequence> strongMatchResultSequences = new ArrayList<SubSequence>();
    List<SubSequence> elongationResultSequences = new ArrayList<SubSequence>();
    List<SubSequence> altEndingResultSequences = new ArrayList<SubSequence>();
    List<SubSequence> weakMatchResultSequences = new ArrayList<SubSequence>();
    
    List<PromotedLocation> strongMatchPromotions = promColl.strongMatchFinalCut;
    for (PromotedLocation pl : strongMatchPromotions) {
      int endIndex = Math.min(pl.location + query.size(), log.size());
      SubSequence subS = new SubSequence(pl.location, endIndex, log, pl.weightedDistance);
      if (pl.weightedDistance <= Config.admittanceThreshold) {
        strongMatchResultSequences.add(subS);
      } else {
        weakMatchResultSequences.add(subS);
      }
    }
    ResultSet strongMatchResultSet = new ResultSet(strongMatchResultSequences, query);
    strongMatchResultSet.rank();
    urs.setStrongMatches(strongMatchResultSet);
    
    ResultSet weakMatchResultSet = new ResultSet(weakMatchResultSequences, query);
    weakMatchResultSet.rank();
    urs.setWeakMatches(weakMatchResultSet);
    
    List<PromotedLocation> elongationPromotions = promColl.elongationFinalCut;
    int elongSize = query.size() * Config.elongationFactor;
    for (PromotedLocation pl : elongationPromotions) {
      int endIndex = Math.min(pl.location + elongSize, log.size());
      SubSequence subS = new SubSequence(pl.location, endIndex, log, pl.weightedDistance); 
      if (pl.weightedDistance <= Config.admittanceThreshold) {
        elongationResultSequences.add(subS);
      } 
    }
    ResultSet elongResultSet = new ResultSet(elongationResultSequences, query);
    elongResultSet.rank();
    urs.setElongatedMatches(elongResultSet);

    List<PromotedLocation> altEndingPromotions = promColl.altEndingFinalCut;
    for (PromotedLocation pl : altEndingPromotions) {
      int endIndex = Math.min(pl.location + query.size(), log.size());
      SubSequence subS = new SubSequence(pl.location, endIndex, log, pl.weightedDistance); 
      if (pl.weightedDistance <= Config.admittanceThreshold) {
        altEndingResultSequences.add(subS);
      } 
    }
    ResultSet altEndResultSet = new ResultSet(altEndingResultSequences, query);
    altEndResultSet.rank();
    urs.setAltEndingMatches(altEndResultSet);
    
    return urs;
  }
  
  public void pruneResults(UberResultSet urs) {
    //TODO: Prune other results sets. Also, prune across result sets 
    // (e.g., prune elongations that also appear as Strong Results
    ResultSet rs = urs.getStrongMatches();
    List<SubSequence> strongResults = urs.getStrongMatches().getResults();
    List<SubSequence> weakResults = urs.getWeakMatches().getResults();
    
    Collections.sort(strongResults, new Comparator<SubSequence> () {
      @Override
      public int compare(SubSequence a, SubSequence b) {
        return a.getStartIndex() - b.getStartIndex();        
      }
    });

    Map<SubSequence, List<SubSequence>> seqToGroupMap = 
        new HashMap<SubSequence, List<SubSequence>>();
    for(int i = 0; i < strongResults.size(); i++) {
      SubSequence subS = strongResults.get(i);
      for(int j = i + 1; j < strongResults.size(); j++) {
        SubSequence subS2 = strongResults.get(j);
        if (subS.getSnippet().equals(subS2.getSnippet()) && 
            subS2.getStartIndex() >= subS.getStartIndex() &&
            subS2.getStartIndex() < subS.getEndIndex()) {
          Debug.log("Found overlap between " + subS + " and " + subS2 );
          List<SubSequence> overlapGroup = seqToGroupMap.get(subS);
          Debug.log("Found overlapGroup " + overlapGroup);
          if (overlapGroup == null) {
            overlapGroup = new ArrayList<SubSequence>();
            overlapGroup.add(subS);
            seqToGroupMap.put(subS, overlapGroup);
            Debug.log("Created overlapGroup " + overlapGroup);
          }
          if (!overlapGroup.contains(subS2)) {
            overlapGroup.add(subS2);
            seqToGroupMap.put(subS2, overlapGroup);
            Debug.log("Added " + subS2 + " to overlapGroup, which is now " + overlapGroup);
          } else {
            Debug.log("overlapGroup alread contains " + subS2);
          }
        }
      }
    }
    
    for (List<SubSequence> overlapGroup : seqToGroupMap.values()) {
      double minDist = Double.MAX_VALUE;
      SubSequence bestSeq = null;
      for (SubSequence seq : overlapGroup) {
        if (seq.getDistance() < minDist) {
          minDist = seq.getDistance();
          bestSeq = seq;
          Debug.log("Removed " + seq + " from result set. Now looks like: " + urs.getStrongMatches().getResults());
        }
        urs.getStrongMatches().getResults().remove(seq); // remove them all
      }
      urs.getStrongMatches().getResults().add(bestSeq); // add the best one back in
      Debug.log("Added " + bestSeq + " back to result set. Now looks like: " + urs.getStrongMatches().getResults());
    }
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
      SubSequence ss = new SubSequence(j, Math.min(j + query.getEvents().size(), log.getEvents().size()), log, closeLocs.get(j));
      closeOnes.add(ss);
    }
    for (Integer j : weakDistances.keySet()) {
      SubSequence ss = new SubSequence(j, Math.min(j + query.getEvents().size(), log.getEvents().size()), log, weakDistances.get(j));
      weakOnes.add(ss);
    }
    
    ResultSet closeMatches = urs.getStrongMatches();
    if (closeMatches == null) {
      closeMatches = new ResultSet(null, query);
    }
    closeMatches.addResults(closeOnes);
    urs.setStrongMatches(closeMatches);
    
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
   * @return the edit distance
   */
  public int computeEditDistance(Snippet query, Snippet log, int startIndex, int endIndex) {
    
    if (endIndex > log.getEvents().size()) {
      endIndex = log.getEvents().size();
    }
    int len1 = query.getEvents().size();
    int len2 = endIndex - startIndex;
 
//    System.out.println("startIndex: " + startIndex + 
//        ", endIndex: " + endIndex + 
//        ", querySize: " + query.getEvents().size() + 
//        ", logSize: " + log.getEvents().size() + 
//        ", len1: " + len1 + ", len2: " + len2);
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

  
  public <T> int computeVectorEditDistance(List<T> list1, List<T> list2, int startIndex1, int endIndex1,
      int startIndex2, int endIndex2) {
    
    
    endIndex1 = Math.min(endIndex1, list1.size());
    endIndex2 = Math.min(endIndex1, list2.size());
    
    int len1 = endIndex1 - startIndex1;
    int len2 = endIndex2 - startIndex2;
 
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
      Object o1 = list1.get(startIndex1 + i);
      for (int j = 0; j < len2; j++) {
        Object o2 = list2.get(startIndex2 + j);

        //if last two chars equal
        if (o1.equals(o2)) {
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
      }
    }
    return Math.max(dp[len1][len2], Math.abs(len1 - len2));
  }
  
  /**
   * @param nhood  a sorted list of locations where an ngram (any ngram) was found in the log
   * @param qLength the length of the query 
   * @return a map 
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
   
  public Snippet getLogById(String id) {
    return indexedLogMap.get(id);
  }
  
  public List<Snippet> getAllLogs() {
    return new ArrayList(indexedLogMap.values());
  }
}
