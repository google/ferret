#!/bin/bash
USERFILE="/data/data/com.google.research.ic.alogger/files/ALoggerDeviceInfo.txt" 
ARG="-rm"

adb root
adb shell rm $USERFILE 

