package com.google.research.ic.ferret.data.attributes;

import java.util.Date;

public class DateTimeAttribute implements Attribute {

  public static final String TYPE = "DATETIME";
  
  private String key = null;
  private Date value = null;
  private String type = TYPE;

  public DateTimeAttribute(String key, Date value) {
    this.key = key;
    this.value = value;
  }
  
  @Override
  public String getKey() {
    return key;
  }

  @Override
  public Date getValue() {
    return value;
  }

  @Override
  public String getType() {
    return type;
  }

  
}
