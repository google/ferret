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
package com.google.research.ic.ferret.test;

public class Tag {
  public long startTime = -1;
  public long endTime = -1;
  public String label = null;
  
  public Tag(long s, long e, String l) {
    startTime = s;
    endTime = e;
    label = l;
  }
  public String toString() {
    return "tag:" + label + ", startTime:" + startTime + ", endTime:" + endTime;
  }
}
