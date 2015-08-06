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

import com.google.gson.Gson;

import java.util.HashMap;

public abstract class Event {

  protected long timeStamp = -1;
  protected String eventTypeName = null;
  protected String componentType = null;
  protected String componentDescription = null;
  protected String componentInfo = null;
  protected String moduleName = null;
  protected String moduleDescription = null;
  protected String deviceId = null;
  protected String userId = null;
  protected String displayTitle = null;
  protected String displayEvent = null;
  protected String displayExtra = null;
  protected String identifier = null;
  protected int identifierId = -1;
  protected int repetitions = 0;
  
  protected HashMap<String, String> keyValuePairs = 
      new HashMap<String, String>();
  
  public abstract void init();
  
  /**
   * @return the timeStamp
   */
  public long getTimeStamp() {
    return timeStamp;
  }
  /**
   * @param timeStamp the timeStamp to set
   */
  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }
  /**
   * @return the typeName
   */
  public String getEventTypeName() {
    return eventTypeName;
  }
  /**
   * @param typeName the typeName to set
   */
  public void setEventTypeName(String typeName) {
    this.eventTypeName = typeName;
  }
  
  /**
   * @return the componentType
   */
  public String getComponentType() {
    return componentType;
  }
  /**
   * @param componentType the componentType to set
   */
  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }
  /**
   * @return a user-friendly description of the component
   */
  public String getComponentDescription() {
    return componentDescription;
  }
  
  /**
   * Set a user-friendly description of the component
   * @param description the description to set
   */
  public void setComponentDescription(String description) {
    this.componentDescription = description;
  }
  
  /**
   * Return additional information about the component that might
   * help user to recognize it (e.g., button text)
   * @return the componentInfo
   */
  public String getComponentInfo() {
    return componentInfo;
  }
  
  /**
   * Set additional information about the component that might
   * help user to recognize it (e.g., button text)
   * @param componentInfo the componentInfo to set
   */
  public void setComponentInfo(String componentInfo) {
    this.componentInfo = componentInfo;
  }
  /**
   * @return the moduleName
   */
  public String getModuleName() {
    return moduleName;
  }
  /**
   * @param moduleName the moduleName to set
   */
  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }
  
  /**
   * @return the human-readable moduleDescription
   */
  public String getModuleDescription() {
    return moduleDescription;
  }
  /**
   * @param moduleDescription the moduleDescription to set
   */
  public void setModuleDescription(String moduleDescription) {
    this.moduleDescription = moduleDescription;
  }
  /**
   * @return the deviceId
   */
  public String getDeviceId() {
    return deviceId;
  }
  /**
   * @param deviceId the deviceId to set
   */
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }
  /**
   * @return the userId
   */
  public String getUserId() {
    return userId;
  }
  /**
   * @param userId the userId to set
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }
  /**
   * @return the keyValuePairs
   */
  public HashMap<String, String> getKeyValuePairs() {
    return keyValuePairs;
  }
  /**
   * @param keyValuePairs the keyValuePairs to set
   */
  public void setKeyValuePairs(HashMap<String, String> keyValuePairs) {
    this.keyValuePairs = keyValuePairs;
  }
  
  /**
   * 
   */
  public void addRepetition() {
    repetitions++;
  }
  
  /**
   * 
   */
  public int getRepetitions() {
    return repetitions;
  }
  
  /**
   * Indicates whether this event is important for analyzing interaction
   * Subclasses should override, as they understand event semantics
   * @return true if important
   */
  public boolean isImportant() {
    return true; 
  }
  
  /**
   * Indicates whether this event could represent the start of an interaction
   * Subclasses should override, as they understand event semantics
   * @return true if the event could be the start
   */  
  public boolean isStartCandidate() {
    return true;
  }

  /**
   * Indicates whether this event could represent the end of an interaction
   * Subclasses should override, as they understand event semantics
   * @return true if the event could be the end
   */
  public boolean isStopCandidate() {
    return true;
  }
  
  public float calculateDistance(Event event) {
    return -1.0f;
  }
  
  public String toString() {
    return new Gson().toJson(this);
  }
  

  public String getIdentifier() {
    if (identifier == null) {
      identifier = getEventTypeName() + "$" + getComponentType() + "$" + getModuleName();
    }
    return identifier;
  }
  
  public void setIdentifierId(int id) {
    this.identifierId = id;
  }
  public int getIdentifierId() {
    return identifierId;
  }
  
  public abstract String getDisplayTitle();  
  public abstract String getDisplayEvent();
  public abstract String getDisplayExtra();  
  
  
}
