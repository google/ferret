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
package com.google.research.ic.ferret.uiserver;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.research.ic.ferret.Session;
import com.google.research.ic.ferret.data.DemoManager;
import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.FilterSpec;
import com.google.research.ic.ferret.data.FilteredResultSet;
import com.google.research.ic.ferret.data.FilteredResultSet.FilteredResultSummary;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.attributes.Attribute;
import com.google.research.ic.ferret.data.attributes.CategoricalAttribute;
import com.google.research.ic.ferret.data.attributes.DateTimeAttribute;
import com.google.research.ic.ferret.data.attributes.NumericalAttribute;
import com.google.research.ic.ferret.test.Debug;
import com.google.research.ic.ferret.test.Shell;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/entry-point")
public class RESTHandler {

  Gson gson = null;
  static int numPollAttempts = 0;
  static boolean stopped = false;
  
  private Gson getGson() {
    if (gson == null) {
      gson = LogLoader.getLogLoader().getGson();
    }
    return gson;
  }
  
  @GET
  @Path("pollForEvents")
  @Produces(MediaType.APPLICATION_JSON)
  public String pollForEvents(@QueryParam("reset") boolean shouldReset) {
    Session session = Session.getCurrentSession();
    
    if (shouldReset) {
      session.resetCurrentQuery();
      stopped = false;
      return null;
    }

    if (stopped) {
      return "{ \"status\" : \"stopped\" }";
    } else {
//    List<Event> events = session.dequeueDemoEvents();
      Snippet q = session.getCurrentQuery();      
      String response = getGson().toJson(q, Snippet.class); 
      //String response = getGson().toJson(events, ArrayList.class); // Hmmm, not totally safe b/c events is a List, not ArrayList. But I know what it really is...
      //String response2 = "{ \"numPollAttempts\" : \"" + numPollAttempts++ + "\" }";
      if (response != null && !response.equals("null")) {
        Debug.log("polled, responding " + response + " which has " + response.length());
//        response = null;
      }
      return response;
    }
  }
  
  @POST
  @Path("getDetailedResultsForQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDetailedResultsForQuery(
      @FormParam("q") String querySpec,
      @FormParam("limit") int limit) {
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
    }
    Debug.log("Received query: " + currentQuery);

    Debug.log("limit was " + limit);

    if (limit == 0) {
      limit = 50;
    }
    
    if (currentQuery != null) {
      long t = System.currentTimeMillis();
      Debug.log("Started searching...");
      ResultSet resultSet = SearchEngine.getSearchEngine().findMatches(currentQuery).getCloseMatches();
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      ResultSet trimmedResultSet = resultSet.filter(new FilterSpec(0.0, 5.0, 20));

      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(trimmedResultSet);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Returning detailed results " + gsonString);
      return gsonString;
    } else {
      return null;
    }
  }

  @POST
  @Path("getSummaryResultsForQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSummaryResultsForQuery(
      @FormParam("q") String querySpec) {
    stopped = true;
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
    }
    Debug.log("Received query: " + currentQuery);
    
    if (currentQuery != null) {
      long t = System.currentTimeMillis();
      Debug.log("Started searching...");
      ResultSet resultSet = SearchEngine.getSearchEngine().findMatches(currentQuery).getCloseMatches();
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("There are " + resultSet.getResults().size() + " subSequences in result set");
      Debug.log("Query is: ");
      Shell.printEvents(currentQuery, 0, currentQuery.size());
      
      Session.getCurrentSession().setCurrentResultSet(resultSet);
      
      FilteredResultSummary[] summaries = new FilteredResultSummary[3];

      int numResults = resultSet.getResults().size();
      double minDist = resultSet.getResults().get(0).getDistance();
      double maxDist = resultSet.getResults().get(numResults / 10).getDistance(); // only look at to 10% of results. Yeah, arbitrary
      double firstQuart = minDist + (maxDist - minDist) / 4;
      double halfWay = minDist + 2 * (maxDist - minDist) / 4;
      
      
      t = System.currentTimeMillis();
      FilteredResultSet bestMatches = resultSet.filter(new FilterSpec(minDist, firstQuart, -1));
      if (bestMatches.getResults() != null) {
        Debug.log("found " + bestMatches.getResults().size() + " best matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
        summaries[0]  = bestMatches.getSummary();
      }
      
      t = System.currentTimeMillis();  
      FilteredResultSet okMatches = resultSet.filter(new FilterSpec(firstQuart + 0.00001, halfWay, -1));
      if (okMatches.getResults() != null) {
        Debug.log("found " + okMatches.getResults().size() + " ok matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
        summaries[1] = okMatches.getSummary();
      }

      t = System.currentTimeMillis();
      FilteredResultSet farMatches = resultSet.filter(new FilterSpec(halfWay + 0.00001, maxDist, -1));
      if (farMatches.getResults() != null) {    
        Debug.log("found " + farMatches.getResults().size() + " far matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
        summaries[2] = farMatches.getSummary();
      }
      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(summaries);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Gonna return " + gsonString);
      return gsonString;
    } else {
      return null;
    }
    
  }
  
  @POST
  @Path("getFilteredResults")
  @Produces(MediaType.APPLICATION_JSON)
  public String getFilteredResults(
      @FormParam("filterParams") String filterParams) {

    Debug.log("filterParams: " + filterParams);

    JsonParser parser = new JsonParser();
    JsonObject rootObj = parser.parse(filterParams).getAsJsonObject();
    String attrName = rootObj.get("attrName").getAsString();
    String values = rootObj.get("values").getAsString();
    
    JsonObject rSummObj = rootObj.getAsJsonObject("rSummary");
    FilteredResultSummary rSummary = getGson().fromJson(rSummObj, FilteredResultSummary.class);
    FilterSpec fSpec = null;
    Attribute attr = rSummary.getAttributes().get(attrName);

    Debug.log("values: " + values);
    Debug.log("attrType: " + attr.getType());
    
    if (attr.getType().equals(CategoricalAttribute.TYPE)) {
      String operator = FilterSpec.EQUALS;
      fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, attrName, operator, values);      
    } else if (attr.getType().equals(NumericalAttribute.TYPE)) {
      if (values.contains("-")) {      
        String min = values.split("-")[0];
        String max = values.split("-")[1];
        String operator = FilterSpec.BETWEEN;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, Double.parseDouble(min), Double.parseDouble(max));
      } else {
        // assume a single value
        String operator = FilterSpec.EQUALS;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, Double.parseDouble(values));        
      }      
    } else if (attr.getType().equals(DateTimeAttribute.TYPE)) {
      if (values.contains("-")) {      
        String min = values.split("-")[0];
        String max = values.split("-")[1];
        String operator = FilterSpec.BETWEEN;
        
        Debug.log("Creating FilterSpec to return " + operator + " " + min + " and " + max);
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, new Date(Long.parseLong(min)), new Date(Long.parseLong(max)));
      } else {
        // assume a single value
        String operator = FilterSpec.EQUALS;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, new Date(Long.parseLong(values)));    
      }
    }

    ResultSet currentResults = Session.getCurrentSession().getCurrentResultSet();
    if (currentResults != null) {
      FilteredResultSet filteredResults = currentResults.filter(fSpec);
      String jsonString = getGson().toJson(filteredResults);   
      //Debug.log("*** Returning Filtered Results: " + jsonString);
      return jsonString;
    } else {
      return null;
    }
  }
  
  /**
   * Used for testing - returns a list of demo snippets to the UI
   * @return
   */
  
  @GET
  @Path("getDemoSnippets")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDemoSnippets() {

    List<Snippet> demoSnippets = DemoManager.getDemoManager().getAllDemoSnippets();
    if (demoSnippets == null || demoSnippets.size() == 0) {
      return null;
    } else {
      return getGson().toJson(demoSnippets);
    }
  }
  
  // OLD AND DEPRECATED BELOW HERE
  
  @GET
  @Path("test")
  @Produces(MediaType.TEXT_PLAIN)
  public String test() {
    return "Test";
  }
  
  @GET
  @Path("foobar")
  @Produces(MediaType.APPLICATION_JSON)
  public String json() {

    return new String("{ \"foo\" : \"bar\" }");
  
  }

  @GET
  @Path("demoSnippet")
  @Produces(MediaType.APPLICATION_JSON)
  public String demoSnippet(@QueryParam("demo") String demo) {
    Debug.log("received query param demo = " + demo);
    List<Snippet> snips = DemoManager.getDemoManager().getAllDemoSnippets();
    Debug.log("Got " + snips + " about to return them as JSON");
    if (snips == null || snips.size() == 0) {
      return "{ \"error\": \"no snips\" }";
    }
    Session.getCurrentSession().setCurrentQuery(snips.get(0));
    String gsonString = getGson().toJson(snips.get(0)); // just do the first one
    return gsonString;
  }
  
  @GET
  @Path("__getDetailedResultsForQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public String submitDemoQuery(
      @QueryParam("q") String querySpec, 
      @QueryParam("limit") int limit) {
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
    }
    Debug.log("Received query: " + currentQuery);

    Debug.log("limit was " + limit);

    if (limit == 0) {
      limit = 50;
    }
    
    if (currentQuery != null) {
      long t = System.currentTimeMillis();
      Debug.log("Started searching...");
      ResultSet resultSet = SearchEngine.getSearchEngine().findMatches(currentQuery).getCloseMatches();
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      ResultSet trimmedResultSet = resultSet.filter(new FilterSpec(0.0, 5.0, 20));

      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(trimmedResultSet);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Gonna return " + gsonString);
      return gsonString;
    } else {
      return null;
    }
  }

}
