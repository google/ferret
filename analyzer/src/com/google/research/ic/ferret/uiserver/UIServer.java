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

import com.google.research.ic.ferret.test.Debug;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

public class UIServer {

  public static void startServer() throws Exception {

    Thread t = new Thread() {
        @Override
        public void run() { 
          try {
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            
            Server server = new Server(8080);
            server.setHandler(context);
  
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", RESTHandler.class.getCanonicalName());
  
            FilterHolder filter = new FilterHolder();
            filter.setInitParameter("allowedOrigins", "*");
            filter.setInitParameter("allowedMethods", "POST,GET,OPTIONS,PUT,DELETE,HEAD");
            filter.setInitParameter("allowedHeaders", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
            filter.setInitParameter("preflightMaxAge", "728000");
            filter.setInitParameter("allowCredentials", "true");
            CrossOriginFilter corsFilter = new CrossOriginFilter();
            filter.setFilter(corsFilter);
            context.addFilter(filter, "/*", EnumSet.of(DispatcherType.REQUEST));
            
            server.start();
            server.join();
          } catch (Exception e) {
            Debug.log("Error starting uiserver: " + e);
          }
        }
    };
    t.start();


  }

  public static void main(String[] args) throws Exception {
    
    startServer();

  }
  

}
