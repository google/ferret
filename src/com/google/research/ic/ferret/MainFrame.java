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
package com.google.research.ic.ferret;

import com.google.research.ic.ferret.data.DemoLogParser;
import com.google.research.ic.ferret.data.DemoManager;
import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.SubSequence;
import com.google.research.ic.ferret.test.Debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class MainFrame extends JFrame {

  private JPanel exampleSection = null;
  private JPanel resultsSection= null;
  private JPanel exampleSnippetContainerPanel = null; // this is where we will put new events
  private JPanel demoTriggerPanel = null;
  private JPanel detailedResultsPanel = null;
  private JPanel summaryResultsPanel = null;
  
  public static final Color ACCENT_COLOR = new Color(0x6095c9);

  private boolean demoMode = true;
  
  private static Preferences prefs = null;
  private static MainFrame theFrame = null;
  
  private static int MAX_DETAILED_RESULTS = 20;
  
  private SnippetPanel exampleSnippetPanel = null; 
  
  protected static void initAndShowUI() {
    ResourceManager.getResourceManager().loadResources();
//    DeviceEventReceiver.startServer();
//    Session.getCurrentSession().init();
//    AttributeManager.getManager().addHandler(new UserNameAttributeHandler());
//
//    long t = System.currentTimeMillis();
//    List<Snippet> snippets = LogLoader.getLogLoader().loadLogs();   
//    Debug.log("Loaded logs in " + (System.currentTimeMillis() - t) + " ms");
//
//    t = System.currentTimeMillis();    
//    SearchEngine.getSearchEngine().index(snippets);
//    Debug.log("Indexed logs in " + (System.currentTimeMillis() - t) + " ms");
    //loadDemos();

    try {
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      // If Nimbus is not available, we'll stick with the default LAF
    }

    theFrame = new MainFrame();
    theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    theFrame.setPreferredSize(new Dimension(1000, 1000));
    theFrame.setSize(new Dimension(1000, 1000));
    theFrame.getContentPane().setBackground(Color.WHITE);
    theFrame.initLayout();
    theFrame.setVisible(true);
  }

  public static MainFrame getMainFrame() {
    return theFrame;
  }
  
  public void initLayout() {

    // Set the Frame's layout manager to Border
    setLayout(new BorderLayout());

    setupExampleSection();
    setupResultsSection();
    setupDemoSection();

    getContentPane().add(demoTriggerPanel, BorderLayout.SOUTH);
    getContentPane().add(exampleSection, BorderLayout.NORTH);
    getContentPane().add(resultsSection, BorderLayout.CENTER);

  }

  private void setupExampleSection() {
    // Create the example panel at the top
    exampleSection = new JPanel();
    exampleSection.setLayout(new BorderLayout());
    exampleSection.setBorder(BorderFactory.createEmptyBorder());
    exampleSection.setBackground(Color.white);
    exampleSection.setOpaque(true);
//    exampleSection.setPreferredSize(new Dimension(1000, 250));

    // Create the title that says "Example" and add it to the examplePanel
    JLabel exampleTitle = new JLabel("<html><div style="
        + "\"font-size:large;color:white;padding:15px 5px\">"
        + "EXAMPLE</div></html>");
    exampleTitle.setBackground(ACCENT_COLOR);
    exampleTitle.setOpaque(true);
    exampleTitle.setPreferredSize(new Dimension(1000, 35));
    exampleSection.add(exampleTitle, BorderLayout.NORTH);

    // Create the Container that holds the control panel and the snippet panel
    JPanel exampleContent = new JPanel();
    exampleContent.setLayout(new BoxLayout(exampleContent, BoxLayout.X_AXIS));
    exampleContent.setBackground(Color.white);
    exampleContent.setOpaque(true);

    //Create the control panel that holds the "Record/Search" button
    JPanel exampleControlPanel = new JPanel();      
    exampleControlPanel.setLayout(new BoxLayout(exampleControlPanel, BoxLayout.X_AXIS));
    JButton recordAndSearchButton = new JButton("Record", 
        ResourceManager.getResourceManager().getRedDotImageIcon());
    recordAndSearchButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JButton theButton = (JButton) e.getSource();
        if (theButton.getText().equalsIgnoreCase("record")) {
          setDemoMode(true);
          theButton.setText("Search");
          resetExampleSnippet();
        } else if (theButton.getText().equalsIgnoreCase("search")) {
          setDemoMode(false);
          theButton.setText("Record");
          
          ResultSet results =
            SearchEngine.getSearchEngine().searchMatches(
                exampleSnippetPanel.getSnippet()).getCloseMatches();
          showSummaryResults(results);     
        }
        theButton.revalidate();
      }
      
    });
    recordAndSearchButton.setHorizontalTextPosition(JButton.CENTER);
    recordAndSearchButton.setVerticalTextPosition(JButton.BOTTOM);
    recordAndSearchButton.setPreferredSize(new Dimension(80,80));
    recordAndSearchButton.setMaximumSize(new Dimension(80,80));
    recordAndSearchButton.setBackground(Color.white);
    //recordAndSearchButton.setFocusPainted(false);
    exampleControlPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    exampleControlPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
    exampleControlPanel.add(Box.createRigidArea(new Dimension(25, 25)));
    exampleControlPanel.add(recordAndSearchButton);
    exampleControlPanel.add(Box.createRigidArea(new Dimension(25, 25)));
    exampleControlPanel.setPreferredSize(new Dimension(150, 120));
    exampleControlPanel.setBackground(Color.white);
    exampleControlPanel.setOpaque(true);
    
    // create the panel that will eventually hold the example snippet
    // (or rather, the scrolling panel that will hold the snippet events)
    exampleSnippetContainerPanel = new JPanel();
    //exampleSnippetContainerPanel.setPreferredSize(new Dimension(850, 200));
    exampleSnippetContainerPanel.setBackground(Color.white);
    exampleSnippetContainerPanel.setOpaque(true);

    exampleContent.add(exampleControlPanel);
    exampleContent.add(exampleSnippetContainerPanel);

    exampleSection.add(exampleContent, BorderLayout.SOUTH);
  }
  
  private void setupResultsSection() {
    resultsSection = new JPanel();
    resultsSection.setLayout(new BorderLayout());
    resultsSection.setBorder(BorderFactory.createEmptyBorder());
    resultsSection.setBackground(Color.white);
    resultsSection.setOpaque(true);
//    resultsSection.setPreferredSize(new Dimension(1000, 250));

    // Create the title that says "results" and add it to the resultsPanel
    JLabel resultsTitle = new JLabel("<html><div style="
        + "\"font-size:large;color:white;padding:15px 5px\">"
        + "RESULTS</div></html>");
    resultsTitle.setBackground(ACCENT_COLOR);
    resultsTitle.setOpaque(true);
    resultsTitle.setPreferredSize(new Dimension(1000, 35));
    resultsSection.add(resultsTitle, BorderLayout.NORTH);

  }
  
  public void showDetailedResults(ResultSet results) {

    List<SubSequence> subSeqs = results.getResults();

    subSeqs = subSeqs.subList(0, MAX_DETAILED_RESULTS);
    
    //Debug.log(subSeqs.toString());
    if (detailedResultsPanel == null) {
      detailedResultsPanel = new JPanel(new BorderLayout());
      resultsSection.add(detailedResultsPanel);
    }
    detailedResultsPanel.removeAll(); 

    // now we have a clean slate, fill it up with the results
    JPanel detailedResultsContentPanel = new JPanel();
    detailedResultsContentPanel.setLayout(
        new BoxLayout(detailedResultsContentPanel, BoxLayout.Y_AXIS));
    
    JScrollPane scrollingResultsPanel = new JScrollPane(
        detailedResultsContentPanel);
    scrollingResultsPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollingResultsPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    for (SubSequence s : subSeqs) {
      Debug.log("Adding subSeq with distance = " + s.getDistance());
      JPanel mdPanel = new SnippetMetadataPanel(s);
      JPanel sPanel = new SnippetPanel(s.getSubSnippet());
      JPanel rowPanel = new JPanel();
      rowPanel.setAlignmentY(CENTER_ALIGNMENT);
      rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
      rowPanel.add(mdPanel);
      rowPanel.add(Box.createHorizontalGlue());
      rowPanel.add(sPanel);
      rowPanel.setPreferredSize(new Dimension(950, 150));
      rowPanel.setBackground(Color.white);
      rowPanel.setOpaque(true);
      detailedResultsContentPanel.add(rowPanel);
    }
    detailedResultsPanel.add(scrollingResultsPanel, BorderLayout.CENTER);
    detailedResultsPanel.invalidate();
    resultsSection.invalidate();
  }
  
  public void showSummaryResults(ResultSet results) {
    Debug.log("We should see " + results.getAttributeSummaries().keySet().size() + "summaries");
    try {
      resultsSection.remove(detailedResultsPanel);
    } catch (Exception e) {
      // no worries, it probably wansn't there.
    }
    if (summaryResultsPanel == null) {
      summaryResultsPanel = new JPanel();
      summaryResultsPanel.setLayout(new BoxLayout(summaryResultsPanel, BoxLayout.Y_AXIS));
      resultsSection.add(summaryResultsPanel, BorderLayout.CENTER);
    }
    summaryResultsPanel.add(new ResultSetSummaryPanel(results));
    summaryResultsPanel.invalidate();
    resultsSection.invalidate();
    invalidate();
  }
  
  private void setupDemoSection() {
    //loadDemos();

    List<Snippet> demoSnippets = 
        DemoManager.getDemoManager().getAllDemoSnippets();
    Debug.log("Loaded " + demoSnippets.size() + " demos");

    demoTriggerPanel = new JPanel(new FlowLayout());
    JButton demoButton = null;
    int i = 1;

    for (Iterator<Snippet> iterator = demoSnippets.iterator(); iterator.hasNext();) {
      Snippet snippet = iterator.next();
      demoButton = new JButton("Demo " + i++);
      demoButton.addActionListener(new DemoButtonListener(snippet));
      demoTriggerPanel.add(demoButton);
    }
  }
  
  final class DemoButtonListener implements ActionListener {

    private Snippet demoSnippet = null;

    public DemoButtonListener(Snippet demoSnippet) {
      this.demoSnippet = demoSnippet;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Debug.log("Loading demo snippet that starts at " + 
          demoSnippet.getEvents().get(0).getTimeStamp());
      loadDemoSnippet(demoSnippet); 
      
      ArrayList<SubSequence> subSeqs = new ArrayList<SubSequence>(10);
      for(int i = 0; i < 10; i++) {
        SubSequence s =  new SubSequence(0, 30, demoSnippet);
        subSeqs.add(s);
      }
      showDetailedResults(new ResultSet(subSeqs, demoSnippet));
    }     
  }
  
  public static void loadDemos() {
    // read demos in
    // create a demo trigger for each one
    DemoLogParser.getParser().readDemoLogDirectory();
  }

  public void loadDemoSnippet(Snippet demoSnippet) {
    loadSnippetIntoExamplePanel(demoSnippet, exampleSnippetContainerPanel);
  }

  public void loadSnippetIntoExamplePanel(Snippet snippet, JPanel containerPanel) {
    Debug.log("Loading snippet into panel " + containerPanel.getHeight() 
        + "," + containerPanel.getWidth());
    exampleSnippetPanel = new SnippetPanel(snippet);
    containerPanel.removeAll(); // clean it out
    containerPanel.add(exampleSnippetPanel);
    containerPanel.revalidate();
    containerPanel.repaint();
  }

  public void resetExampleSnippet() {
    Snippet s = new Snippet(); // TODO: Need to generalize
    loadSnippetIntoExamplePanel(s, exampleSnippetContainerPanel);
  }
  
  public void addExampleEvent(Event evt) {
    Debug.log("Adding event to example panel");
    if (exampleSnippetPanel == null) {
      Snippet newSnippet = new Snippet();
      exampleSnippetPanel = new SnippetPanel(newSnippet);
    }
    exampleSnippetPanel.addEvent(evt);
    exampleSnippetPanel.revalidate();
    exampleSnippetContainerPanel.revalidate();
  }
  
  public void setDemoMode(boolean b) {
    demoMode = b;
  }
  
  public boolean isDemoMode() {
    return demoMode;
  }
  
  public static Preferences getPrefs() {
    if (prefs == null) {
      prefs = Preferences.userNodeForPackage(MainFrame.class);
    }
    return prefs;
  }
  
//  public static void main (String[] args) {
//
//    SwingUtilities.invokeLater(new Runnable() {
//      public void run() {
//        initAndShowUI();
//      }
//    });
//  }

}


