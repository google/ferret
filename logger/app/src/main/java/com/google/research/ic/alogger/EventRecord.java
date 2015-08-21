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
package com.google.research.ic.alogger;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by marknewman on 3/20/15.
 */
public class EventRecord {

  private String userId = null;
  private String deviceId = null;
  private long timeStamp = -1;
  private int eventType = -1;
  private String wText = null;
  private List<CharSequence> wAccessibilityRecordText = null;
  private String wClassName = null;
  private String wPackageName = null;
  private String wResourceName = null;
  private int wHeight = -1;
  private int wWidth = -1;
  private float wRelativeHeight = -1;
  private float wRelativeWidth = -1;
  private int wXPos = -1;
  private int wYPos = -1;
  private float wRelativeXPos = -1;
  private float wRelativeYPos = -1;
  private boolean wIsEnabled = false;
  private boolean wIsChecked = false;
  private boolean wIsFocused = false;
  private boolean wIsFocusable = false;
  private boolean wIsCheckable = false;

  private PathToRoot pathToRoot = null;

  private transient ALoggerService service = null;

  protected static final class PathToRoot {
    private ArrayList<String> ancestorClassNames = new ArrayList<>();
    private ArrayList<Integer> ancestorIndices = new ArrayList<>();
    private int depthFromRoot = -1;

    //for testing
    public PathToRoot(int depth, ArrayList<String> ancestorClassNames, ArrayList<Integer> ancestorIndices) {
      this.depthFromRoot = depth;
      this.ancestorClassNames = ancestorClassNames;
      this.ancestorIndices = ancestorIndices;
    }

    public PathToRoot(AccessibilityNodeInfo info) {
      AccessibilityNodeInfo parent = info; // will immediately be set to info.parent in loop
      AccessibilityNodeInfo child = null; // will be set to parent == info

      depthFromRoot = 0;
      while(parent!=null) {
        child = parent;
        parent = parent.getParent();
        if (parent != null) {
          ancestorClassNames.add(parent.getClassName().toString());
          int i = 0;
          for ( ; i < parent.getChildCount(); i++) {
            if (parent.getChild(i) == null) {
              // it seems that this actually happens sometimes.
              // Unclear how much of a problem it will be.
              break;
            }
            if (child == null) {
              throw new IllegalStateException("child is null, but parent is " + parent.toString());
            }
            if (parent.getChild(i).equals(child)) {
              ancestorIndices.add(new Integer(i));
              break;
            }
          }
          if (i >= parent.getChildCount()) {
            throw new IllegalStateException("Node was not found as a child of its parent");
          }
        }
        depthFromRoot++;
      }
      if (ancestorClassNames.size() != ancestorIndices.size()) {
        throw new IllegalStateException("Ancestor class names and indices arrays " +
            "are of different sizes \n " +
            ancestorClassNames.toString() + " ;;;;;"
            + ancestorIndices.toString());
      }
    }
  }

  /**
   * Empty arg constructor for Json serialization support
   */
  public EventRecord () { }

  public EventRecord(AccessibilityEvent event, String userId, String deviceId, ALoggerService service) {

    if (event == null) {
      throw new IllegalArgumentException("event can't be null");
    }
    AccessibilityNodeInfo info = event.getSource();
    if (info == null) {
      throw new IllegalArgumentException("info can't be null"); // or can it?
    }

    this.userId = userId;
    this.deviceId = deviceId;

    //timeStamp = event.getEventTime(); [[AccesibilityEvent timestamps make no sense]]
    timeStamp = System.currentTimeMillis();

    eventType = event.getEventType();

    
    // DEBUGGING OUTPUT
    if (Config.debug && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
      StringBuilder sb = new StringBuilder();
      sb.append("widget class = " + info.getClassName());
      sb.append("\ninfo.getText() = " + info.getText());
      sb.append("\nevent.getText() = " + event.getText());
      if (event.getRecordCount() == 0) {
        sb.append("\nevent has no records");
      } else {
        for (int i = 0; i < event.getRecordCount(); i++) {
          for (int j = 0; j < event.getRecord(i).getText().size(); j++) {
            sb.append("\nrecord " + i + " text " + j + " = " + event.getRecord(i).getText().get(j).toString());
          }
        }
      }
      if (info.getChildCount() == 0) {
        sb.append("\nevent.info has no children");
      } else {
        for (int i = 0; i < info.getChildCount(); i++) {
          AccessibilityNodeInfo child = info.getChild(i);
          if (child.getText() != null) {
            sb.append("\nevent.info.child " + i + " = " + child.getText().toString());
          }
          if (child.getChildCount() > 0) {
            sb.append("\nchild " + i + " has " + child.getChildCount() + " children");            
          }
        }
      }
      if (info.getParent() != null) {
        sb.append("\nparent text is " + info.getParent().getText());
        sb.append("\nparent has " + info.getParent().getChildCount() + " children");
      }
      DebugLogger.log(sb.toString());
    } // END DEBUGGING OUTPUT
    
    if (info.getClassName() != null) wClassName = info.getClassName().toString();
    if (info.getPackageName() != null) wPackageName = info.getPackageName().toString();
    if (info.getViewIdResourceName() != null) wResourceName = info.getViewIdResourceName();

    Rect outBounds = new Rect();
    info.getBoundsInScreen(outBounds);
    wWidth = outBounds.width();
    wHeight = outBounds.height();
    wRelativeWidth = (float) wWidth / service.getScreenSize().x;
    wRelativeHeight = (float) wHeight / service.getScreenSize().y;

    wXPos = outBounds.left;
    wYPos = outBounds.top;
    wRelativeXPos = (float) wXPos / service.getScreenSize().x;
    wRelativeYPos = (float) wYPos / service.getScreenSize().y;

    wIsEnabled = info.isEnabled();
    wIsChecked = info.isChecked();
    wIsCheckable = info.isCheckable();
    wIsFocused = info.isFocused();
    wIsFocusable = info.isFocusable();

    // try first to get the text from the event
    if (event.getText() != null && event.getText().size() > 0 
        && event.getText().get(0) != null) {
      wText = event.getText().get(0).toString();
    }

    // next from the AccessibilityNodeInfo
    if (wText == null) {
      if (info.getText() != null) {
        wText = info.getText().toString();
      }
    }

    // next, inspect the AccessibilityRecords
    if (wText == null) {
      for (int i = 0; i < event.getRecordCount(); i++) {
        if (event.getRecord(i) != null && 
            event.getRecord(i).getText() != null && 
            event.getRecord(i).getText().size() > 0 && 
            event.getRecord(i).getText().get(0) != null) {
          wText = event.getRecord(i).getText().get(0).toString();
          break;
        }
      }
    }

    // finally, inspect the children of this node to see if any of them contain text
    if (wText == null) {
      for (int i = 0; i < info.getChildCount(); i++) {
        AccessibilityNodeInfo child = info.getChild(i);
        if (child != null && child.getText() != null) {
          wText = child.getText().toString();
          break;
        }
        if (child != null) {
          for (int j = 0; j < child.getChildCount(); j++) {
            AccessibilityNodeInfo gChild = info.getChild(i);
            if (child.getText() != null) {
              wText = gChild.getText().toString();
              break;
            }
          }
        }
        if (wText != null) break;
      }
    }

    try {
      pathToRoot = new PathToRoot(info);
    } catch (IllegalStateException e) {
      DebugLogger.log("Not adding a path to root because: " + e);
      DebugLogger.log("The offending ANI is: " + info);
      pathToRoot = null; // we were unsuccessful in getting the path, so leave it blank
    }
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }

  public int getEventType() {
    return eventType;
  }

  public void setEventType(int eventType) {
    this.eventType = eventType;
  }

  public String getwText() {
    return wText;
  }

  public void setwText(String wText) {
    this.wText = wText;
  }

  public List<CharSequence> getwAccessibilityRecordText() {
    return wAccessibilityRecordText;
  }

  public void setwAccessibilityRecordText(List<CharSequence> wAccessibilityRecordText) {
    this.wAccessibilityRecordText = wAccessibilityRecordText;
  }

  public CharSequence getwClassName() {
    return wClassName;
  }

  public void setwClassName(String wClassName) {
    this.wClassName = wClassName;
  }

  public CharSequence getwPackageName() {
    return wPackageName;
  }

  public void setwPackageName(String wPackageName) {
    this.wPackageName = wPackageName;
  }

  public CharSequence getwResourceName() {
    return wResourceName;
  }

  public void setwResourceName(String wResourceName) {
    this.wResourceName = wResourceName;
  }

  public int getwHeight() {
    return wHeight;
  }

  public void setwHeight(int wHeight) {
    this.wHeight = wHeight;
  }

  public int getwWidth() {
    return wWidth;
  }

  public void setwWidth(int wWidth) {
    this.wWidth = wWidth;
  }

  public float getwRelativeHeight() {
    return wRelativeHeight;
  }

  public void setwRelativeHeight(float wRelativeHeight) {
    this.wRelativeHeight = wRelativeHeight;
  }

  public float getwRelativeWidth() {
    return wRelativeWidth;
  }

  public void setwRelativeWidth(float wRelativeWidth) {
    this.wRelativeWidth = wRelativeWidth;
  }

  public int getwXPos() {
    return wXPos;
  }

  public void setwXPos(int wXPos) {
    this.wXPos = wXPos;
  }

  public int getwYPos() {
    return wYPos;
  }

  public void setwYPos(int wYPos) {
    this.wYPos = wYPos;
  }

  public float getwRelativeXPos() {
    return wRelativeXPos;
  }

  public void setwRelativeXPos(float wRelativeXPos) {
    this.wRelativeXPos = wRelativeXPos;
  }

  public float getwRelativeYPos() {
    return wRelativeYPos;
  }

  public void setwRelativeYPos(float wRelativeYPos) {
    this.wRelativeYPos = wRelativeYPos;
  }

  public boolean iswIsEnabled() {
    return wIsEnabled;
  }

  public void setwIsEnabled(boolean wIsEnabled) {
    this.wIsEnabled = wIsEnabled;
  }

  public boolean iswIsChecked() {
    return wIsChecked;
  }

  public void setwIsChecked(boolean wIsChecked) {
    this.wIsChecked = wIsChecked;
  }

  public boolean iswIsFocused() {
    return wIsFocused;
  }

  public void setwIsFocused(boolean wIsFocused) {
    this.wIsFocused = wIsFocused;
  }

  public boolean iswIsFocusable() {
    return wIsFocusable;
  }

  public void setwIsFocusable(boolean wIsFocusable) {
    this.wIsFocusable = wIsFocusable;
  }

  public boolean iswIsCheckable() {
    return wIsCheckable;
  }

  public void setwIsCheckable(boolean wIsCheckable) {
    this.wIsCheckable = wIsCheckable;
  }

  public PathToRoot getPathToRoot() {
    return pathToRoot;
  }

  public void setPathToRoot(PathToRoot pathToRoot) {
    this.pathToRoot = pathToRoot;
  }
  
}
