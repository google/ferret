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
// Copyright 2013 Google Inc. All Rights Reserved.
package com.google.research.reflection.data;

import com.google.gson.Gson;
import com.google.research.ic.ferret.test.Debug;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author liyang@google.com (Yang Li)
 */
public class Observation implements Comparable<Observation> {

  private static final boolean USE_PACKAGE_NAME = true;

  private Calendar calendar;

  private double latitude;

  private double longitude;

  private int ssid;

  private int bssid;

  private int geoid;

  private int towerid;

  private String packageName;

  public String appName;

  private String eventName;

  private String packagedEventName;

  private String participantId;

  private int duration;

  public String genre;

  public int price;

  public boolean isHomeScreen;

  public boolean isSystemEvent;

  public boolean isDebugMessage;

  public Observation(Calendar calendar, String event, double lat, double lon,
  		int towerId, int geoId) {
    this.calendar = calendar;
    packagedEventName = event;
    latitude = lat;
    longitude = lon;
    this.towerid = towerId;
    this.geoid = geoId;
  }

  public Observation() {
  }

  public boolean equals(Observation observation) {
    if (getEventName().equals(observation.getEventName()) &&
        getTime() == observation.getTime()) {
      return true;
    }
    return false;
  }

  public String getDevice() {
    return participantId;
  }

  public int getDuration() {
    return duration;
  }

  public String getEventName() {
    if (USE_PACKAGE_NAME) {
      return packagedEventName;
    } else {
      return eventName;
    }
  }

  public long getTime() {
    if (calendar == null) {
      Debug.log("No calendar object for " + this);
      return -1;
    } else {
      return calendar.getTimeInMillis();
    }
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public int getGeoId() {
    return geoid;
  }

  public int getTowerId() {
    return towerid;
  }

  public int getSSID() {
    return ssid;
  }

  public int getBSSID() {
    return bssid;
  }

  public long getEndTime() {
    return getTime() + duration * 1000;
  }

  @Override
  public int compareTo(Observation observation) {
    if (getTime() > observation.getTime()) {
      return 1;
    } else if (getTime() == observation.getTime()) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public String toString() {
    String dateTimeString = null;
    if (calendar != null) {
      TimeZone tz = calendar.getTimeZone();
      SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
      df.setTimeZone(tz);
      dateTimeString = df.format(calendar.getTime());
    }
    return "participantId=[" + participantId + "] time=[" + dateTimeString +
        "] eventName=[" + getEventName() +  "] duration(sec)=[" + duration +
        "] latitude=[" + latitude + "] longitude=[" + longitude + "]";
  }

  public boolean isSame(Observation entity) {
    return getTime() == entity.getTime() && eventName.equals(entity.eventName);
  }

  public static String getPathContext(String name) {
    int pos = name.indexOf("/", 1);
    if (pos == -1) {
      return "/";
    } else {
      return name.substring(0, pos + 1);
    }
  }

  public static String getInitialSegment(String name) {
    int pos = name.indexOf("/", 1);
    if (pos == -1) {
      return name.substring(1);
    } else {
      return name.substring(1, pos);
    }
  }

  public boolean isHomeScreen() {
    return isHomeScreen;
  }

  public String toSpreadSheet() {
    return participantId + ";" + eventName + ";" + packageName + ";" + appName + ";" + calendar.getTimeInMillis() + ";" +
      duration + ";" + calendar.getTimeZone().getID() + ";" + latitude + ";" + longitude;
  }

  public void setTime(Calendar calendar) {
    this.calendar = calendar;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public void setPackageName(String packageName2) {
    packageName= packageName2;
  }

  public void setAppName(String appName2) {
    appName = appName2;
  }

  public String getAppName() {
    return appName;
  }
  
  public String getPackageName() {
    return packageName;
  }

  public void setPackagedEventName(String packagedEventName) {
    this.packagedEventName = packagedEventName;
  }

  public void setparticipantId(String deviceId) {
    participantId = deviceId;
  }

  public void setLatitude(double latitude2) {
    latitude = latitude2;
  }

  public void setLongitude(double longitude2) {
    longitude = longitude2;
  }

  public void setDuration(int duration2) {
    duration = duration2;
  }

  public void setGenre(String genre2) {
    genre = genre2;
  }

  public void setPrice(int price2) {
    price = price2;
  }

  public void setIsSystemEvent(boolean systemEvent) {
    isSystemEvent = systemEvent;
  }

  public void setIsHomeScreen(boolean homeScreen) {
    isHomeScreen = homeScreen;
  }

  public void setIsDebugMessage(boolean debugEvent) {
    isDebugMessage = debugEvent;
  }

  public void setSsid(int ssid2) {
    ssid = ssid2;
  }

  public void setBssid(int bssid2) {
    bssid = bssid2;
  }

  public void setTowerid(int towerId2) {
    towerid = towerId2;
  }

  public void setGeoid(int geoId2) {
    geoid = geoId2;
  }

  public Calendar getCalendar() {
	return calendar;
  }
}
