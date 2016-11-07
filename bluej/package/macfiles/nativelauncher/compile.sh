#!/bin/bash

# You need to download the "Command line tools for XCode" from developer.apple.com,
# as well as XCode itself.
#
# JAVA_HOME should be set to something like:
#     /Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home

gcc -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/darwin" -o JavaAppLauncher \
	-framework Cocoa -arch x86_64 \
	-isysroot /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk \
	-mmacosx-version-min=10.7 \
	-Wall \
	main.m
