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

import com.google.research.ic.ferret.test.Debug;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

public class SubSequence implements Comparable<SubSequence> {
  private transient HashMap<Integer, Integer> ngram;
  private final int startIndex; /** Inclusive */
  private final int endIndex; /** Exclusive */
  private final Snippet snippet;
  private double distance;
  
  public SubSequence(int start, int end, Snippet snippet, double distance)  {
    startIndex = start;
    endIndex = end;
    this.snippet = snippet;
    this.distance = distance;
  }

  public List<Event> getEvents() {
    return snippet.getEvents().subList(startIndex, endIndex);
  }

  public void setNGram(HashMap<Integer, Integer> map) {
    ngram = map;
  }

  public HashMap<Integer, Integer> getNGram() {
    return ngram;
  }
  
  public void setDistance(double dist) {
    distance = dist;
  }
  
  public double getDistance() {
    return distance;
  }

  /**
   * @return the startIndex
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * @return the endIndex
   */
  public int getEndIndex() {
    return endIndex;
  }

  /**
   * @return the snippet
   */
  public Snippet getSnippet() {
    return snippet;
  }

  public int getLength() {
    return endIndex - startIndex -1;
  }
  
  /**
   * Return the snippet that represents just this subSequence
   * @return
   */
  public Snippet getSubSnippet() {
    Snippet subSnippet = new Snippet();
    List<Event> snipEvents = snippet.getEvents(); 
//    Debug.log("Getting subSnippet from snippet with " 
//        + snippet.getEvents().size() + " events, " +
//        "and getting events from " + startIndex +
//        " to " + (endIndex - 1));
    for (int i = startIndex; i < endIndex - 1; i++) {
      subSnippet.addEvent(snipEvents.get(i));
    }
    return subSnippet;
  }
  
  /**
   * Get the snippet representing just the events in this subsequence
   * @return
   */
  
  @Override
  public int compareTo(SubSequence o) {
    if (o == null) {
      Debug.log("Found a null one");
      return -1;
    }
      
    return Double.compare(distance, o.distance);
  }
  
  public String toString() {
    StringWriter sw = new StringWriter();
    sw.append("subS:" + startIndex + "-" + endIndex);
    sw.append(String.format(",dist:%.2f",distance)); 
    sw.append(",source:" + snippet.getSourceFilename());
    return sw.toString();
  }
}
