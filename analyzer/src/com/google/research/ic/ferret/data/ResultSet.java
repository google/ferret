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
import com.google.research.ic.ferret.data.attributes.AttributeManager;
import com.google.research.ic.ferret.data.attributes.Bin;
import com.google.research.ic.ferret.test.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of results returned by the search engine.
 * The collection can be filtered into a FilteredResultSet by calling filter()
 */
public class ResultSet {

  public static final int MAX_RESULTS_TO_FILTER = 10000;
      
  protected Snippet sourceQuery = null;
  protected List<SubSequence> results = null;

  protected Map<String, Attribute> attributes = new HashMap<String,Attribute>();
  protected Map<String, List<Bin>> attrSummaries = null;
  
  public static final double MAX_DISTANCE = 20.0;
  
  // for subclasses
  protected ResultSet() {
    
  }
  
  public ResultSet(List<SubSequence> subS, Snippet query) {
    results = subS;
    sourceQuery = query;
  }
  
  public List<SubSequence> getResults () {
    return results;
  }
        
  public Snippet getSourceQuery() {
    return sourceQuery;
  }

  public void setSourceQuery(Snippet sourceQuery) {
    this.sourceQuery = sourceQuery;
  }

  public void addResults(List<SubSequence> newResults) {
    if (results == null) {
      results = new ArrayList<SubSequence>();
    }
    results.addAll(newResults);
  }

  /**
   * For all attributes found in this result set
   * Find all values that the attribute can have
   * And count up the number of snippets that have that value
   */
  private void computeSummaries() {    
    attrSummaries = AttributeManager.getManager().computeSummaries(this);
  }
  
  
  /**
   * This could take a while to return if the summaries weren't computed in 
   * advance. Need to keep an eye on this and maybe make async later?
   * @return
   */
  public Map<String, List<Bin>> getAttributeSummaries() {
    if (attrSummaries == null) {
      computeSummaries();
    }
    return attrSummaries;
  }
  
  public void rank() {
    Collections.sort(results, new Comparator<SubSequence>() {
      @Override
      public int compare(SubSequence o1, SubSequence o2) {
        // cosine distances are [0-1], thus most of them would round to 0 when converted to int  
        return (int) (100000 * (o1.getDistance() - o2.getDistance())); 
      }
    });
  }
  
  public FilteredResultSet filter(FilterSpec fSpec) {
    FilteredResultSet frs = new FilteredResultSet(sourceQuery, this, fSpec);
    rank();

    int max = results.size();
    max = MAX_RESULTS_TO_FILTER;
    
    List<SubSequence> someResults = results.subList(0, Math.min(results.size(), max));

    Debug.log("Started filtering results");

    for (SubSequence subS : someResults) {
      frs.insertIfAcceptable(subS);
    }
    if (frs.getResults() != null) {
      Debug.log("Finished filtering results, " + frs.getResults().size() + " were added");
      frs.cleanUp();          
      Debug.log("Finished cleaning up, " + frs.getResults().size() + " remain");
    } else {
      Debug.log("No results left after filtering, filterspec: " + fSpec);
    }
    return frs;    
  }
  
  public int size() {
    return getResults().size();
  }

  public void mergeResults(ResultSet otherRS) {
    if (sourceQuery == null && otherRS.getSourceQuery() != null) {
      sourceQuery = otherRS.getSourceQuery();
    }
    addResults(otherRS.getResults());
  }
  
  
}
