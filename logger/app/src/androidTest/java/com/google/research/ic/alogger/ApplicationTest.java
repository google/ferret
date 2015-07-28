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
 ******************************************************************************/

package com.google.research.ic.alogger;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void setUp() {
        // do nothing
        Log.d("alogger_test", "setting up");
    }
    public void testEventRecord() {

        Log.d("alogger_test", "Is this thing on?");
        EventRecord record = new EventRecord();

        record.setTimeStamp(1000);
        record.setEventType(1);
        record.setwText("foo");

        List<CharSequence> arText = new ArrayList<CharSequence>();
        arText.add("bar");
        arText.add("baz");
        record.setwAccessibilityRecordText(arText);

        record.setwClassName("first class");
        record.setwPackageName("big package");
        record.setwResourceName("renewable resource");
        record.setwHeight(40);
        record.setwWidth(50);
        record.setwRelativeHeight(0.44f);
        record.setwRelativeWidth(0.55f);
        record.setwIsCheckable(true);
        record.setwIsChecked(true);

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> ints = new ArrayList<Integer>();
        names.add("blah");
        names.add("bloo");
        names.add("blech");
        ints.add(new Integer(1));
        ints.add(new Integer(2));
        ints.add(new Integer(3));
        EventRecord.PathToRoot ptr = new EventRecord.PathToRoot(3, names, ints);
        record.setPathToRoot(ptr);

        try {
            long t = System.currentTimeMillis();
            Gson gson = new Gson();
            Log.d("alogger_test", gson.toJson(record));
            Log.d("alogger_test", "It took " + (System.currentTimeMillis() - t) + " millis");
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }

    }
}
