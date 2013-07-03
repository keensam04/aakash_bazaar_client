#!/bin/bash
# startup script for aakash bazaar

# this will update a repo for `ant` to build an apk
# android update project --path . --target android-15 --subprojects

# uncomment below 2 line to remove bin/ and gen/
# rm -rvf bin/
# rm -rvf gen/

# uninstall apk
adb uninstall org.fdroid.fdroid

# build apk in debug mode
ant debug

# install apk
adb install bin/FDroid-debug.apk

# start Main activity
adb shell am start -a android.intent.action.MAIN -n org.fdroid.fdroid/.FDroid

