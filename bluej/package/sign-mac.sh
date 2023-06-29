#!/bin/bash
#
# Args:
#   $1 - developer name (Part after Developer ID Application in certificate
#   $2 - zipfile name
#   $3 - Apple ID for notarizing
#   $4 - Password for notarizing (Apple-generated password, not the master password)
#   $5 - Apple Team ID (available from developer profile page)

echo '<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd"><plist version="1.0"><dict>    <key>com.apple.security.cs.allow-jit</key>    <true/>    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>    <true/>    <key>com.apple.security.cs.disable-executable-page-protection</key>    <true/>     <key>com.apple.security.cs.disable-library-validation</key><true/><key>com.apple.security.cs.allow-dyld-environment-variables</key>    <true/></dict></plist>' > entitlements.plist

echo "Unzip download..."
TOP_LEVEL=`zipinfo -1 "$2"  | head -n 1 | tr -d '\n/'`
BASE_NAME=`basename $2 ".zip"`

unzip -q "$2"
echo "Unzip download - done"

# JLI causes problems but is not needed:
rm "$TOP_LEVEL"/Contents/PlugIns/x64/Contents/MacOS/libjli.dylib
# Fix permissions on JSA files, which are read-only by default:
chmod u+w $TOP_LEVEL/Contents/PlugIns/x64/Contents/Home/lib/server/*.jsa

# Sign the executable:
echo "Signing BlueJ executable..."
codesign --verbose=4 --timestamp --options=runtime --deep -s "Developer ID Application: $1" --entitlements entitlements.plist "$TOP_LEVEL"/Contents/MacOS/BlueJ
echo "Signing BlueJ executable - done"

# Sign whole thing together:
echo "Signing package..."
codesign --verbose=4 --timestamp --options=runtime --force -s "Developer ID Application: $1" --entitlements entitlements.plist "$TOP_LEVEL"
echo "Verifying bundle"
codesign --verify --deep --verbose=4 --strict "$TOP_LEVEL"/Contents/MacOS/BlueJ
codesign --verify --deep --verbose=4 --strict "$TOP_LEVEL"

spctl -a -t execute --ignore-cache -vv "$TOP_LEVEL" || echo " *** failed to sign or verify Mac bundle ***"
echo "Signing package - done"

# Zip just the app for sending. Must use ditto (not zip) to preserve permissions and meta-data
/usr/bin/ditto -c -k --keepParent "$TOP_LEVEL" notarize.zip

# Notarize the file, waiting for completion (usually around 5 minutes)
echo ""
echo "Notarizing..."
xcrun notarytool submit --apple-id $3 --password $4 --team-id $5 --wait notarize.zip
echo "Notarization complete"

echo ""
echo "Stapling ZIP"
# uncompress the file again, using ditto. Staple.
ditto -x -k notarize.zip .
xcrun stapler staple $TOP_LEVEL

# Now package it with appdmg:
echo ""
echo "Packaging into DMG"
appdmg ../appdmg.json BlueJ-installer.dmg
echo "Notarizing DMG"
xcrun notarytool submit --apple-id $3 --password $4 --team-id $5 --wait BlueJ-installer.dmg
echo "Stapling DMG"
xcrun stapler staple BlueJ-installer.dmg
echo "Finished"
