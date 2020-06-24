#!/bin/bash
#
# Args:
#   $1 - zipfile name
#   $2 - developer name (Part after Developer ID Application in certificate

echo '<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd"><plist version="1.0"><dict>    <key>com.apple.security.cs.allow-jit</key>    <true/>    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>    <true/>    <key>com.apple.security.cs.disable-executable-page-protection</key>    <true/>     <key>com.apple.security.cs.disable-library-validation</key><true/><key>com.apple.security.cs.allow-dyld-environment-variables</key>    <true/></dict></plist>' > entitlements.plist

TOP_LEVEL=`zipinfo -1 "$1"  | head -n 1 | tr -d '\n'`
unzip "$1"
# Get rid of jmods which cause issue for signing and are not used by BlueJ as far as I know:
rm -rf "$TOP_LEVEL"/*.app/Contents/JDK/Home/jmods
# JLI causes problems but is not needed:
rm "$TOP_LEVEL"/*.app/Contents/JDK/MacOS/libjli.dylib
# Sign all executables and dylibs:
find "$TOP_LEVEL"/*.app/Contents/JDK "$TOP_LEVEL"/*.app/Contents/Resources/Java/javafx -type f -and \( -perm +111 -or -iname "*.dylib" \) -exec codesign --timestamp --verbose --options=runtime --force -s "Developer ID Application: $2" --deep --entitlements entitlements.plist {} +
# Sign the launcher:
codesign --verbose=4 --timestamp --options=runtime --deep --force -s "Developer ID Application: $2" --entitlements entitlements.plist "$TOP_LEVEL"/*.app/Contents/MacOS/JavaAppLauncher
# Sign whole thing together:
codesign --verbose=4 --timestamp --options=runtime --deep --force -s "Developer ID Application: $2" --entitlements entitlements.plist "$TOP_LEVEL"/*.app/Contents/JDK
codesign --verbose=4 --timestamp --options=runtime --deep --force -s "Developer ID Application: $2" --entitlements entitlements.plist "$TOP_LEVEL"/*.app
echo Verifying bundle
codesign --verify --deep --verbose=4 "$TOP_LEVEL"/*.app/Contents/MacOS/JavaAppLauncher
codesign --verify --deep --verbose=4 "$TOP_LEVEL"/*.app
spctl -a -t exec -vv "$TOP_LEVEL"/*.app || echo " *** failed to sign or verify Mac bundle ***"

# Zip just the app for sending
cd "$TOP_LEVEL"/ && zip -r -q -T -m ../notarize.zip * && cd ../

echo "Next, do the following to notarize the app (replace username/keychain if necessary)"
echo "Run (will take a few minutes to upload):"
echo "  xcrun altool --primary-bundle-id org.bluej --username michael.kolling@kcl.ac.uk --password @keychain:'BlueJ Altool' --notarize-app --file notarize.zip"
echo "That will give you a UUID.  Use this command to check on status (takes about 5 minutes after upload complete):"
echo "  xcrun altool --primary-bundle-id org.bluej --username michael.kolling@kcl.ac.uk --password @keychain:'BlueJ Altool' --notarization-info REPLACE-WITH-UUID"
echo "If that is successful, run this command line to unzip, staple and re-zip:"
echo "  unzip -r notarize.zip -d \"$TOP_LEVEL\" && xcrun stapler staple \"$TOP_LEVEL\"/*.app && rm \"$1\" && zip -r -q -T -m \"$1\" \"$TOP_LEVEL\""