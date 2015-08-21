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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class Sender implements Runnable {

  public static final String TAG = "Connector";

  private int port;

  private ServerSocket server;

  private Socket client;

  private boolean isRunning;

  private Thread thread;

  private ArrayList<String> samples;

  private static final int DEFAULT_PORT = 2416;

  private static Sender connector;
  
  private static boolean debug = true;

  public static void startSender() {
    startSender(DEFAULT_PORT);
  }
  
  public static void startSender(int port) {
    if (connector == null) {
      connector = new Sender(port);
    }
    connector.start();
  }

  public static void stopSender() {
    if (connector != null) {
      connector.stop();
      connector = null;
    }
  }

  public static void send(String event) {
    if (connector != null) {
      connector.addLogEvent(event);
    } else {
      System.err.println("Connector is still null...");
    }
  }

  private Sender(int port) {
    this.port = port;
    server = null;
    client = null;
    samples = new ArrayList<String>();
  }

  private boolean start() {
    if (thread != null) {
      return false;
    }
    try {
      server = new ServerSocket(port);
    } catch (IOException e) {
      System.err.println("Failed to initiate socket server" + e);
      return false;
    }
    thread = new Thread(this);
    isRunning = true;
    thread.start();
    return isRunning;
  }

  private boolean stop() {
    if (thread != null) {
      isRunning = false;
      thread.interrupt();
      try {
        server.close();
      } catch (IOException e) {
        System.err.println("Failed to close socket server" + e);
      }
      server = null;
      try {
        thread.join();
        thread = null;
        return true;
      } catch (InterruptedException e) {
        System.err.println("Failed to join connector thread" + e);
      }
      return false;
    }
    return false;
  }

  private void addLogEvent(String sample) {
    if (isRunning) {
      synchronized (samples) {
        samples.add(sample);
      }
    }
  }

  @Override
  public void run() {
    if (Thread.currentThread() != thread) {
      return;
    }

    while (isRunning && !Thread.interrupted()) {
      if (client == null) {
        try {
          if (debug) {
            System.err.println("Waiting for connection request");
          }

          try {
            client = server.accept();
          } catch (SocketException e) {
            continue;
          }

          if (debug) {
            System.err.println("Connection request accepted");
          }

          PrintWriter out = new PrintWriter(client.getOutputStream());
          BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
          out.println(String.format("hi from server"));
          out.flush();

          String line = in.readLine(); // server response to handshake
          if (line != null) {

            if (debug) {
              System.err.println("Confirmed");
            }

            synchronized (samples) {
              samples.clear();
            }

            while (isRunning && !Thread.interrupted()) {
              final ArrayList<String> s;

              synchronized (samples) {
                s = new ArrayList<String>(samples);
                samples.clear();
              }

              out.println(s.size());
              for (String sample : s) {
                if (debug) {
                  //System.err.println(sample.toString());
                }
                out.println(sample.toString());
              }
              out.flush();

              if (in.readLine() == null) {
                if (debug) {
                  System.err.println("socket read null");
                }
                break;
              }
            }
          }

        } catch (IOException e) {
          System.err.println("Failed to write/read from the socket" + e);
        }

        if (client != null) {
          try {
            client.close();
          } catch (IOException e) {
            System.err.println("Failed to close socket client" + e);
          }
          client = null;
        }
      }
    }
  }
}
