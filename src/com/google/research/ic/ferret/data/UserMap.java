package com.google.research.ic.ferret.data;

import java.util.HashMap;

/**
 * Provides mapping from user ids to readable names
 * For now it just assigns random names from a list to the ids
 */
public class UserMap {

  HashMap<String, String> userIdToNameMap = new HashMap<String, String>();
  
  private String[] fakeUserNames = new String[] {
      "marknewman",
      "liyang",
      "hlv",
      "jameslin",
      "echi"
  };
  
  private int currentNameIdx = 0;

  private static UserMap theMap = null;
  
  private UserMap() {
    
  }
  
  public static UserMap getUserMap() {
    if (theMap == null) {
      theMap = new UserMap();
    }
    return theMap;
  }
  
  public String getUserName(String userId) {
    String n = userIdToNameMap.get(userId);
    if (n == null) {
      n = fakeUserNames[currentNameIdx++];
      userIdToNameMap.put(userId, n);
      currentNameIdx = currentNameIdx % fakeUserNames.length;
    }
    return n;
  }
}
