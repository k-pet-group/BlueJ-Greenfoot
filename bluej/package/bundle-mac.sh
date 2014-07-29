#/bin/sh
#
# Args:
#   $1 - path to zip directory
#        (contains Greenfoot.app/BlueJ.app)
#   $2 - App bundle name ("Greenfoot.app" or "BlueJ.app")
#   $3 - path to JDK to bundle
#   $4 - destination zipfile name

# Copy JDK:

cp -r "$3"/* "$1/$2/Contents/Frameworks/jdk.framework/Versions/A" || exit 1

# Allow signing the JDK as a framework:
cd "$1/$2/Contents/Frameworks/jdk.framework" || exit 1
mkdir -p Resources || exit 1
cd Resources || exit 1
ln -s ../Versions/A/Contents/Info.plist Info.plist || exit 1

# Zip it:

cd "`dirname "$1"`" || exit 1
zip -y -r "$4" "`basename "$1"`" || exit
