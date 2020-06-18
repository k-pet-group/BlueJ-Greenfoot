#!/bin/bash
#
# Args:
#   $1 - path to zip directory
#        (contains Greenfoot.app/BlueJ.app)
#   $2 - App bundle name ("Greenfoot.app" or "BlueJ.app")
#   $3 - path to JDK to bundle
#   $4 - destination zipfile name

# Copy JDK:
# The -L is very important.  It copies symlinks to regular files, which is important for the signing as Mac signing hates symlinks
cp -R -L "$3"/* "$1/$2/Contents/JDK" || exit 1
# Zip it:
cd "`dirname "$1"`" || exit 6
zip -q -y -r "$4" "`basename "$1"`" || exit 7