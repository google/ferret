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

import com.google.research.ic.ferret.data.Snippet;

public class LabeledRegion implements Comparable<LabeledRegion> {
  public Snippet sourceSnippet;
  public Tag tag;
  public int startIndex;
  public int endIndex;

  private Snippet regionSnippet = null;
  
  public LabeledRegion(Snippet s, Tag t, int start, int end) {
    sourceSnippet = s;
    tag = t;
    startIndex = start;
    endIndex = end;
  }
  
  public int compareTo(LabeledRegion that) {
    if (!(that instanceof LabeledRegion)) {
      throw new IllegalArgumentException("Can't compare LabeledRegion to " + that.getClass());
    }
    return this.startIndex - ((LabeledRegion)that).startIndex;
  }
  
  public Snippet getRegionSnippet() {
    if (regionSnippet == null) {
      regionSnippet = new Snippet();
      for(int i = startIndex; i < endIndex; i++) {
        regionSnippet.addEvent(sourceSnippet.getEvents().get(i));
        regionSnippet.setSourceFilename(sourceSnippet.getSourceFilename());
      }
    }
    return regionSnippet;
  }
  
  public String toString() {
    return tag.label + "-" + sourceSnippet.getSourceFilename() + "(" + startIndex + "-" + endIndex + ")";
  }
}

