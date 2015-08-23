
/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * /

/**
 * scripts for qbdila's front end
 */

var demoMap = {};
var currentDemo = {};
var recording = false;

var mdColors = [
    "#e51c23", //red_500
    "#e91e63", //pink_500
    "#9c27b0", //purple_500
    "#673ab7", //deep_purple_500
    "#3f51b5", //indigo_500
    "#5677fc", //blue_500
    "#03a9f4", //light_blue_500
    "#00bcd4", //cyan_500
    "#009688", //teal_500
    "#259b24", //green_500
    "#8bc34a", //light_green_500
    "#cddc39", //lime_500
    "#ffeb3b", //yellow_500
    "#ffc107", //amber_500
    "#ff9800", //orange_500
    "#ff5722", //deep_orange_500
    "#795548", //brown_500
    "#607d8b"  //blue_grey_500
];
var mdGray = "#9e9e9e"; //grey_500
var chartColorIdx = 0;
var eventColorIdx = 0;
var eventIdColorMap = {};



// fetch "canned" demos from the server
function loadDemos() {
  $.ajax( {
    url: "http://127.0.0.1:8080/entry-point/getDemoSnippets",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Retrieved demo ...");
      loadDemoDropDown(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  }); 
}

//load "canned" demos into the dropdown list
function loadDemoDropDown(demoList) {
  console.log(demoList);
  $("#demo-list")
    .find('option')
    .remove();
  if (typeof demoList !== 'undefined') {
    var i;
    for (i = 0; i < demoList.length; i++) {
      console.log("demo " + i + ", " + "length: " + demoList[i].events.length);
      console.log(demoList[i]);
      var demo = demoList[i];
      var value = "demo" + i + "(" + demo.events.length + ")";
      $("#demo-list")
        .append($("<option></option>")
        .attr("value",value)
        .text(value)); 
      demoMap[value] = demo;
    }
  }
}

// called when a "canned" demo is selected from dropdown
function selectDemo() {
  clearDemoPanel();
  clearResultsPanel();
  var localVal = $("#demo-list").val();
  var localDemo = demoMap[localVal];
  currentDemo = localDemo;
  displayDemoSnippet(localDemo);
  console.log("Modifying button");
  resetSearchRecordButton("Search");
}

function resetSearchRecordButton(mode) {
  if (mode === "Record") {
    $("#recordAndSearchButton")    
    .button("option", "label", "Record")
    .button("option", "icons", { primary:"ui-icon-recordicon" })
    .off('click')
    .on('click', startRecording);
    
  } else {
    $("#recordAndSearchButton")
    .button("option", "label", "Search")
    .button("option", "icons", { primary:"ui-icon-searchicon" })
    .off('click')
    .on('click', searchButtonClicked);
  }
}

function clearDemoPanel() {
  var snippet_panel = $("#demo-snippet-panel");
  snippet_panel.empty();
  currentDemo = {};
  eventIdColorMap = {};
  eventColorIdx = 0;
  currentDemo.events = new Array();
}

function clearResultsPanel() {
  var container = $("#results-container-panel");
  container.empty();
}

// this implementation uses simple polling
// we can switch to WebSockets if it's too slow
function startRecording() {
  recording = true;
  resetSearchRecordButton("Search");
  clearDemoPanel();
  clearResultsPanel();
  $.ajax ({
    url: "http://127.0.0.1:8080/entry-point/pollForEvents?reset=true",
    type: "GET",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Polling ...");
      setTimeout(pollForEvents, 500);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed polling attempt...");
      alert("Error: " + xhr);
    } 
  });
}

function pollForEvents() {
  $.ajax ({
    url: "http://127.0.0.1:8080/entry-point/pollForEvents",
    type: "GET",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      if (data != null && data.status === "stopped") {
        // do nothing, just stop polling
      } else {
	    displayDemoSnippet(data);
	    console.log(data);
        //processDemoEvents(data);
        setTimeout(pollForEvents, 2500);
      }
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed polling attempt...");
      alert("Error: " + xhr);
    } 
  });
}

function processDemoEvents(events) {

  if (events) {
    console.log(events);
    var snippet_panel = $("#demo-snippet-panel");
    for (var e in events ) {
      if (events[e]) {
        ePanel = createEventPanel(snippet_panel, events[e]);
        currentDemo.events.push(events[e]);
        if (eventIdColorMap[events[e].identifier] == null) {
          eventIdColorMap[events[e].identifier] = mdColors[eventColorIdx];
          eventColorIdx = (eventColorIdx + 1) % mdColors.length;
        }
        ePanel.css('border-color', eventIdColorMap[events[e].identifier]);
        ePanel.attr('title', events[e].identifier);
        //ePanel.addClass(eventIdColorMap[events[e].identifier]);
      }
    }
  }
  if (snippet_panel[0].scrollWidth > snippet_panel.width()) {
    snippet_panel.scrollLeft(snippet_panel[0].scrollWidth - snippet_panel.width());
    console.log("scrolling left, scrollWidth = " + snippet_panel.scrollWidth + ", panel width = " + snippet_panel.width);
  } else {
    console.log("not scrolling left, scrollWidth = " + snippet_panel[0].scrollWidth + ", panel width = " + snippet_panel.width());
  }
}

function searchButtonClicked(eventData) {
  if (recording) {
    recording = false;
  }
  submitQueryExpectSummaries(eventData);    
}

function submitQueryExpectSummaries(eventData) {
  
  $.ajax ({
    url: "http://127.0.0.1:8080/entry-point/getSummaryResultsForQuery",
    type: "POST",
    dataType: "json",
    data: { q : JSON.stringify(currentDemo),
          limit : '20' },
    mimeType: "application/json", 
    success: function (data) {
//      console.log(data);
      $("#status").html("Retrieved result set ...");
      displaySummaryResults(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  });
}

function getFilteredResults(filterParams) {
  console.log(filterParams);
  $.ajax ({
    url: "http://127.0.0.1:8080/entry-point/getFilteredResults",
    type: "POST",
    dataType: "json",
    data: { filterParams : JSON.stringify(filterParams),
          limit : '20' },
    mimeType: "application/json", 
    success: function (data) {
//      console.log(data);
      $("#status").html("Retrieved result set ...");
      displayDetailedResults(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  });
}

//display summary results from a query that's been submitted
function displaySummaryResults(resultSummaries) {
  resetSearchRecordButton("Record");
  console.log(resultSummaries);

  var container = $("#results-container-panel");
  container.empty();

  // we get back four result set summaries by default
  for (rsIdx in resultSummaries) {    

    colorIdx = 0;
    var rSummary = resultSummaries[rsIdx];
    
    if (rSummary != null && rSummary.size > 0) {
      var cluster_label=jQuery('<div/>', {
        id: "result-summary-label-" + rsIdx,
        class: "result-summary-label"
      });
  
      cluster_label.append(rSummary.displayName + " (" + rSummary.size + ")");
      container.append(cluster_label);
    
      var chart_container = jQuery('<div/>', {
        id: "result-summary-chart-container-" + rsIdx,
        class: "result-summary-chart-container"
      });
      container.append(chart_container);
      // each result set summary contains a set of attribute summaries
      // we want to create a chart for each individual attribute
      var attributeSummaries = resultSummaries[rsIdx].attrSummaries;
      for (var attrName in attributeSummaries) {
        constructChartForAttribute(rsIdx, rSummary, attrName, attributeSummaries, chart_container);
        
      }
    } else {
   		var msg = "";
   		if (rsIdx == 0) {
   		  msg = "No strong matches";
   		} else if (rsIdx == 1) {
   		  msg = "No elongations";
   		} else if (rsIdx == 2) {
   		  msg = "No alternate endings";
   		} else if (rsIdx == 3) {
   		  msg = "No weak matches";
   		}
   		
   		
   		var cluster_label=jQuery('<div/>', {
        id: "result-summary-label-" + rsIdx,
        class: "result-summary-label"
      });
  
      cluster_label.append(msg);
      container.append(cluster_label);

    }
  }
}

function displayError(errmsg) {
	alert("Error: " + errmsg);
}

function constructChartForAttribute(rsIdx, rSummary, attrName, attributeSummaries, chart_container) {
  //Set up the chart and data table
  var chart_id="result-summary-" + rsIdx + "-chart-" + attrName;
  var chart_div=jQuery('<div/>', {
    id: chart_id ,
    class: "attr-summary-chart"
  });
  chart_container.append(chart_div);
  var options = {'title': attrName,
          'width': 300,
          'height': 100,
          'legend' : { position: 'none' },
          'titleTextStyle' : { 
            bold : 'false',
            fontName: 'Verdana',
            fontSize: 10
          },
          'colors' : [mdColors[chartColorIdx]],
          'animation' : { startup: 'true' },
          'chartArea' : { left : 100 }
  };
  chartColorIdx = (chartColorIdx + 1) % mdColors.length;

  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Attribute');
  data.addColumn('number', 'Count');

  // each attribute summary maps an attribute name to a set of attribute values
  // we want to add each value summary as a row in the data table
  var oneAttrSummary = attributeSummaries[attrName];      
  for (valueName in oneAttrSummary) {
    var valueSummary = oneAttrSummary[valueName];
    data.addRow([valueSummary.binName, valueSummary.count]);
  }
  var chart = new google.visualization.BarChart($("#" + chart_id).get(0));
  google.visualization.events.addListener(chart, 'select', function() {
    var selection = chart.getSelection();
    if (typeof selection !== 'undefined' && typeof selection[0] !== 'undefined') {
      console.log(selection);
      var row = selection[0].row;
      var rowLabel = data.getValue(row, 0);

      var attrType = rSummary.attributes[attrName].type;

      var theBin;
      var binMin;
      var binMax;
      for (bin in oneAttrSummary) {
        if (oneAttrSummary[bin].binName === rowLabel) {
          theBin = oneAttrSummary[bin];
          binMin = theBin.binMin;
          binMax = theBin.binMax;
        }
      }
      
      var values;
      if (attrType === 'DATETIME' || attrType === 'NUMERICAL') {
        values = binMin + "-" + binMax;
      } else {
        values = rowLabel;
      } 
      
      //for numerical and categorical just use the rowLabel
      var filterParams = { 'rSummary' : rSummary,
              'attrName' : attrName, 
              'values' : values };
      getFilteredResults(filterParams);
    } else {
      console.log("selection was undefined");
    }
  });
  google.visualization.events.addListener(chart, 'click', function(eventData) {
    console.log(eventData);
  });
  chart.draw(data, options);
}


// display detailed results from a query that's been submitted
function displayDetailedResults(resultSet) {
  console.log("Displaying detailed results");
  console.log(resultSet);
  
  var container = $("#results-container-panel");
  
  container.empty();
  
  var numResults = resultSet.results.length;
  
  //iterate through all of the result snippets
  for (var i = 0; i < numResults; i++) {    
    var subSequence = resultSet.results[i ];
    var snippet = subSequence.snippet;
    var start = subSequence.startIndex;
    var end = subSequence.endIndex;
    var startTime = moment(snippet.events[start].timeStamp);
    var endTime = moment(snippet.events[end - 1].timeStamp);
    
    // create the summary panel that describes this result
    var dr_summary_panel = jQuery('<div/>', {
      id: "dr-summary-panel-" + i,
      class: "dr-summary-panel"
    });
    
    var attrString = "";
    jQuery.each(snippet.attributes, function(index, attr) {
      var val = attr.value;
      var valDisp = attr.value;
      console.log("typeof val: " + (typeof val) + ", val is " + val);
      if (attr.key === "xduration") { // disabled for now
        var d = new Date(val);
        var h = val.getUTCHours();
        var m = val.getUTCMinutes();
        var s = val.getUTCSeconds();
        if (h != 0) {
          valDisp = h + "h" + m + "m";
        } else if (m != 0) {
          valDisp = m + "m" + h + "s";
        } else {
          valDisp = s + "s" + Math.round(val.getUTCMilliseconds()/100) + "ms";
        }
      }
      attrString += attr.key + ": " + valDisp + "<br/>";
    });
    attrString += "distance: " + subSequence.distance + "<br/>";
    attrString += "start time: " + startTime.format('M/D/YY H:mm:ss');

    dr_summary_panel.append(attrString);
        
    // create the snippet panel that shows the matching portion of the result snippet    
    var dr_matched_snip_panel=jQuery('<div/>', {
      id: "dr-matched-snip-panel-" + i,
      class: "dr-snippet-panel"
    });
    
    var j = start;
    for ( ; j < end; j++ ) {
      var evt = subSequence.snippet.events[j];
      var event_panel = createEventPanel(dr_matched_snip_panel, evt);
      var color = eventIdColorMap[evt.identifier];
      if (color != null) {
        event_panel.css('border-color', color);
      } else {
        event_panel.css('border-color', mdGray);        
      }
    }
    
    var dr_matched_snip_label=jQuery('<div/>', {
      id: "dr-matched-result-label-" + i,
      class: "dr-snippet-label dr-matching-label"
    });

    dr_matched_snip_label.append("Matching");
    
    var dr_matched_result_container=jQuery('<div/>', {
      id: "dr-matched-result-container-" + i,
      class: "dr-snippet-container"
    });    

    dr_matched_result_container.append($(dr_matched_snip_label));
    dr_matched_result_container.append($(dr_matched_snip_panel));
        
    // now display the events just before the matching region
    var dr_before_snip_panel=jQuery('<div/>', {
      id: "dr-before-snip-panel-" + i,
      class: "dr-snippet-panel"
    });
    
    var j = start - 50;
    if (j < 0) j = 0;
    for ( ; j < start; j++ ) {
      var evt = subSequence.snippet.events[j];
      var event_panel = createEventPanel(dr_before_snip_panel, evt);
      var color = eventIdColorMap[evt.identifier];
      if (color != null) {
        event_panel.css('border-color', color);
      } else {
        event_panel.css('border-color', mdGray);        
      }
    }
    
    var dr_before_snip_label=jQuery('<div/>', {
      id: "dr-before-result-label-" + i,
      class: "dr-snippet-label dr-before-label"
    });
    dr_before_snip_label.append("Before");   

    var dr_before_result_container=jQuery('<div/>', {
      id: "dr-before-result-container-" + i,
      class: "dr-snippet-container"
    });    
    dr_before_result_container.append($(dr_before_snip_label));
    dr_before_result_container.append($(dr_before_snip_panel));
        
    // now create the layout for this detailed result row
    var dr_result_container=jQuery('<div/>', {
      id: "dr-result-container" + i,
      class: "dr-result-container"
    });
    
    dr_result_container.append($(dr_before_result_container));
    dr_result_container.append($(dr_matched_result_container));
    
    var dr_row=jQuery('<div/>', {
      id: "dr-row-" + i,
      class: "dr-row"
    });
    
    dr_row.append($(dr_summary_panel));
    dr_row.append($(dr_result_container));
    container.append($(dr_row));  
  }
}

// Get detailed results for a "live" demo
function getDetailedResultsForDemo(){
  console.log("Calling getDetailedResultsForDemo but shouldn't be");
  $.ajax( {
    url: "http://127.0.0.1:8080/entry-point/getDetailedResults?q=current",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Retrieved detailed results...");
//      displayDetailedReults(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed detailed results query...");
      alert("Error: " + xhr.blah);
    } 
  });
}

//test function, deprecated
function fetchDemo() {  
  $("#status").html("Fetching demo from localhost...");
  $.ajax( {
    url: "http://127.0.0.1:8080/entry-point/demoSnippet?demo=true",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Retrieved demo ...");
      displayDemoSnippet(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  });  
}

function displayDemoSnippet(snippet) {
  clearDemoPanel();
  clearResultsPanel();
  processDemoEvents(snippet.events);
}

function displayResultSnippet(snippet, snippet_panel){

  snippet_panel.empty();
  
  jQuery.each(snippet.events, function(index, event) {
    createEventPanel(snippet_panel, event);
  });
}

function createEventPanel(container, event) {
  var event_id = event.userId + "-" + event.timeStamp + 
    Math.floor((Math.random() * 1000000000) + 1);;
  var eventName = event.displayEvent;
  var eventText = event.displayExtra;  
  var eventModule = event.displayTitle;
  
  var screenWidth = event.wWidth / event.wRelativeWidth;
  var screenHeight = event.wHeight / event.wRelativeHeight;
  var aspectRatio = screenWidth / screenHeight;
  
  var canvasHeight = 60;
  var canvasWidth = canvasHeight * aspectRatio;
  
  var widgetWidth = canvasWidth * event.wRelativeWidth;
  var widgetHeight = canvasHeight * event.wRelativeHeight;
  var widgetXPos = canvasWidth * event.wRelativeXPos;
  var widgetYPos = canvasHeight * event.wRelativeYPos;
  
  var thumbCanvas=jQuery('<canvas/>', {
  	id: "thumb-canvas-" + event_id,
  	width: canvasWidth,
  	height: canvasHeight, 
  	class: "thumb-canvas"
  });
  thumbCanvas.prop({width: canvasWidth, height: canvasHeight});
  
  var ctx = thumbCanvas.get(0).getContext("2d");
  ctx.fillStyle = "#AA4488";
  ctx.fillRect(widgetXPos, widgetYPos, widgetWidth, widgetHeight);
      
  if (eventName == "POPUP") {
    eventName = "NEW SCREEN";
  } else if (eventName == "TEXT CHANGE") {
    eventName = "TEXT CHANGED";
  }
      
  var mainLabel=jQuery('<div/>', {
    id: "event-mainlabel-" + event_id,
    html: eventName,
    class: "event-mainlabel"
  });
  var subLabel=jQuery('<div/>', {
    id: "event-sublabel-" + event_id,
    html: eventText,
    class: "event-sublabel"
  });
  var extraLabel=jQuery('<div/>', {
    id: "event-extra-" + event_id,
    html: eventModule,
    class: "event-extra"
  });

  var event_panel=jQuery('<div/>', {
    id: "event-panel-" + event_id,
    class: "event-panel"
  })
  .append($(thumbCanvas))
  .append($(mainLabel))
  .append($(subLabel))
  .attr('title', event.identifier);
  
  var arrow_panel=jQuery('<span/>', {
    id: "arrow-panel-" + event_id,
    "class": "fa fa-arrow-circle-right fa-1x arrow-icon-panel",
  });
  
  container.append($(event_panel));
  container.append($(arrow_panel));
  return event_panel;
}

function createMiniEventPanel(container, event) {
  var event_id = event.userId + "-" + event.timeStamp + 
  Math.floor((Math.random() * 1000000000) + 1);
  
  var event_title = event.displayTitle;
  var event_description = "::" + event.displayEvent;
  var event_extra = "::" + event.displayExtra;  
  
  var tooltip = event_title + event_description + event_extra + "(" + event.identifier + ")";
  var event_panel=jQuery('<div/>', {
    id: "event-panel-" + event_id,
    "class": "mini-event-panel",
  });
  event_panel.attr("title", tooltip);
  
  container.append($(event_panel));
  return event_panel;
}


///////////////////////////////
// deprecated
function __fetchFoobar () {
  $("#status").html("Sending Ajax query to localhost...");
  $.ajax( {
    url: "http://127.0.0.1:8080/entry-point/foobar",
    dataType: "json",
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Succeded Ajax query...");
      alert("Success: " + data.foo);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  });  
}
//deprecated
function generateDemo() {
  $("#demo-snippet-panel").empty();
  var i = Math.floor((Math.random() * 100) + 1);
  var til = i+20;
  console.log("counting from " + i + " to " + til);
  for(; i < til; i++) {
    var container=jQuery('<div/>', {
      id: "event-panel-" + i,
      class: "event-panel"
    });
    $("#demo-snippet-panel").append(container);
    var title=jQuery('<div/>', {
      id: "event-title-" + i,
      html: "Event " + i,
      class: "event-title"
    });
    $("#event-panel-" + i).append($(title));
    var contents=jQuery('<div/>', {
      id: "event-contents-" + i,
      html: "<br/>Contents " + i,
      class: "event-content"
    });
    $("#event-panel-" + i).append($(contents));  
  }
}

//submit a query based on the demo currently loaded in the example pane
function submitQuery(eventData) {
  $.ajax ({
    url: "http://127.0.0.1:8080/entry-point/getDetailedResultsForQuery",
    type: "POST",
    dataType: "json",
    data: { q : JSON.stringify(currentDemo) },
    mimeType: "application/json", 
    success: function (data) {
      console.log(data);
      $("#status").html("Retrieved summary results ...");
//      displayDetailedResults(data);
    },
    error: function(xhr, status, error) {
      console.log(xhr);
      $("#status").html("Failed Ajax query...");
      alert("Error: " + xhr.blah);
    } 
  });
}
