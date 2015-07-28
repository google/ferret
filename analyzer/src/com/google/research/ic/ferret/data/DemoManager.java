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

import java.util.ArrayList;
import java.util.List;

/**
 * A manager for Demo Snippets. Used for testing
 */
public class DemoManager {
  private List <Snippet> demoSnippetList = new ArrayList<Snippet>();

  private static DemoManager theManager = null;
  
  private DemoManager() {}
  
  public static DemoManager getDemoManager() {
    if (theManager == null) {
      theManager = new DemoManager();
    }
    return theManager;
  }
  
  public void addDemoSnippet(Snippet demoSnippet) {
    demoSnippetList.add(demoSnippet);
  }
  
  public List<Snippet> getAllDemoSnippets() {
    return demoSnippetList;
  }
  
  public void setDemoSnippets(List<Snippet> demoSnippets) {
    this.demoSnippetList = demoSnippets;
  }
}
