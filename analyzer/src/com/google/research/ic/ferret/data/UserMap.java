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

import java.util.HashMap;

/**
 * Provides mapping from user ids to readable names
 * For now it just assigns random names from a list to the ids
 */
public class UserMap {

  HashMap<String, String> userIdToNameMap = new HashMap<String, String>();
  
  private String[] fakeUserNames = new String[] {
      "user1",
      "user2",
      "user3",
      "user4",
      "user5",
      "user6",
      "user7",
      "user8",
      "user9",
      "user10",
      "user11",
      "user12",
      "user13",
      "user14",
      "user15",
      "user16",
      "user17",
      "user18",
      "user19",
      "user20"
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
