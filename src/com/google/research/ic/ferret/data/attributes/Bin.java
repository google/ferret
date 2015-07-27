package com.google.research.ic.ferret.data.attributes;


public class Bin {  
  
  protected String binName = null;
  protected Object binMin = null;
  protected Object binMax = null;
  protected String binType = null;
  
  protected int count = 0;

  public Bin(String name, Object min, Object max, String type, int count) {
    this.binName = name;
    this.binMin = min;
    this.binMax = max;
    this.binType = type;
    this.count = count;
    
  }

  public int getCount() {
    return count;
  }
  public String getDisplayName() {
    return binName;
  }
}
