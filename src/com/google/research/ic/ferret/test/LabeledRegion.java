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

