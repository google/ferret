package com.google.research.ic.ferret.test;

public class Tag {
  public long startTime = -1;
  public long endTime = -1;
  public String label = null;
  
  public Tag(long s, long e, String l) {
    startTime = s;
    endTime = e;
    label = l;
  }
  public String toString() {
    return "tag:" + label;
  }
}
