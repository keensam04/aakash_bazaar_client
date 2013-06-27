#!/bin/bash

adb uninstall org.fdroid.fdroid
ant debug
adb install bin/FDroid-debug.apk

adb shell am start -a android.intent.action.MAIN -n org.fdroid.fdroid/.FDroid

