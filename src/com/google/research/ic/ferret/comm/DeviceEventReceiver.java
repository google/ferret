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
package com.google.research.ic.ferret.comm;

import com.google.research.ic.ferret.DemoEventListener;
import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.test.Debug;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.ArrayList;

public class DeviceEventReceiver {

  private Thread receiverThread = null;

  private RemoteConnector server = null;

  private AndroidDebugBridge bridge = null;

  private IDeviceChangeListener deviceChangeListener = null;

  public static final int REFLECTION_LOGGER_PORT = 2416;
  public static final int ACCESSIBILITY_LOGGER_PORT = 3416;

  private int refCount = 0;

  private ArrayList<DemoEventListener> demoEventListeners = new ArrayList<DemoEventListener>();
  
  static DeviceEventReceiver theReceiver = null;
  
  public static void main(String[] args) {
    startServer();
  }
  
  public static void startServer() {
    
    if (theReceiver == null) {
      theReceiver = new DeviceEventReceiver();
      theReceiver.retain();
      DemoDeviceLogEventListener ddleListener = new DemoDeviceLogEventListener(theReceiver);
      theReceiver.server.addEventListener(ddleListener);
    }
  }
  
  public static DeviceEventReceiver getReceiver() {
    if (theReceiver == null) {
      startServer();
    }
    if (theReceiver == null) {
      throw new IllegalStateException("Could not get or start receiver");
    }
    return theReceiver;
  }

  private static final class DemoDeviceLogEventListener implements DeviceLogEventListener {

    private DeviceEventReceiver receiver = null;
    
    public DemoDeviceLogEventListener(DeviceEventReceiver receiver) {
      this.receiver = receiver;    
    }

    @Override
    public void onEvent(String event) {
      Event evt = null;
//      try {
        evt = LogLoader.getLogLoader().getParser().parseEvent(event);
        if (evt != null) {
          receiver.notifyDemoEventListeners(evt);
        }
//      } catch (Exception e) {
//        Debug.log("Problem while trying to parse event " + event.toString() 
//            + ", " + e);
//      }
    }    
  }
  
  private void notifyDemoEventListeners(Event evt) {
    synchronized(demoEventListeners) {
      for(DemoEventListener listener : demoEventListeners) {
        listener.onEventReceived(evt);
      }
    }    
  }

  public void addDemoEventListener(DemoEventListener deListener) {
    synchronized(demoEventListeners) {
      demoEventListeners.add(deListener);
    }
  }
  
  public void removeDemoEventListener(DemoEventListener deListener) {
    synchronized(demoEventListeners) {
      demoEventListeners.remove(deListener);
    }    
  }

  public void removeAllDemoEventListener() {
    synchronized(demoEventListeners) {
      demoEventListeners.clear();
    }    
  }
  
  public static void stop() {
    if (theReceiver != null) {
      theReceiver.release();
      theReceiver = null;
    }
  }

  private DeviceEventReceiver() {
    refCount = 0;
  }
  
  private void run() {
    init();
    loop();
    onRun();
    receiverThread.interrupt();
    AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
  }

  private void onRun() {}

  private void retain() {
    if (refCount == 0) {
      startReceiverThread();
    }
    refCount++;
  }

  private void release() {
    refCount--;
    if (refCount == 0) {
      endReceiverThread();
    }
  }

  public int getPort() {
    String logType = LogLoader.getLogLoader().getLogType();
    if (logType.equals(LogLoader.ACCESSIBILITIY_LOG)) {
      return ACCESSIBILITY_LOGGER_PORT;
    } else if (logType.equals(LogLoader.REFLECTION_LOG)) {
      return REFLECTION_LOGGER_PORT;
    } else {
      throw new IllegalStateException("Can't set port, because log type is " + logType);
    }
  }
  private int getRefCount() {
    return refCount;
  }

  private void startReceiverThread() {
    init();
    loop();
  }

  private void endReceiverThread() {
    receiverThread.interrupt();
    AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
  }

  private void loop() {
    final int port = getPort();
    deviceChangeListener = new IDeviceChangeListener() {
      @Override
      public void deviceDisconnected(IDevice arg0) {
        System.out.println("disconnected");
      }
      @Override
      public void deviceConnected(IDevice arg0) {
        System.out.println("connected");
        tryStartClient();
      }
      @Override
      public void deviceChanged(IDevice device, int arg1) {
        System.out.println("changed");
        try {
          device.createForward(port, port);
        } catch (TimeoutException e) {
          e.printStackTrace();
        } catch (AdbCommandRejectedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
    server = new RemoteConnector("127.0.0.1", port);
    receiverThread = new Thread(server);
    receiverThread.start();
  }

  private void init() {
    if (bridge == null || !bridge.isConnected()) {
      AndroidDebugBridge.init(false);
      bridge = AndroidDebugBridge.createBridge();
    }
  }

  private void tryStartClient() {
    if (bridge != null) {
      IDevice[] devices = bridge.getDevices();
      System.out.println(devices.length + " Android devices detected");
      for (IDevice device : devices) {
        if (!device.isEmulator()) {
          System.out.println(device.toString());
          try {
            String appName = null;
            if (LogLoader.getLogLoader().getLogType().equals(LogLoader.REFLECTION_LOG)) {
              appName = "com.example.com.google.research.ic.qbdila.android/.MainActivity";
            } else {
              appName = "com.google.research.ic.alogger/.ALoggerMainActivity";
            }
            device.executeShellCommand("am start " + "-a android.intent.action.MAIN "
                + "-c android.intent.category.LAUNCHER "
                + "-n " + appName,
                new IShellOutputReceiver() {
                  @Override
                  public boolean isCancelled() {
                    return false;
                  }
                  @Override
                  public void flush() {}
                  @Override
                  public void addOutput(byte[] arg0, int arg1, int arg2) {}
                });
          } catch (TimeoutException e) {
            e.printStackTrace();
          } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
          } catch (ShellCommandUnresponsiveException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        }
      }
    }
  }
}
