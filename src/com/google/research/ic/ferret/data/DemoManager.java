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
