#!/bin/sh
# From https://gist.github.com/2blon9/780db8daa059debe65fbf55aa994fb68
# Install `imagemagick` first.
# brew install imagemagick

ICONSETNAME=$1
mkdir $ICONSETNAME.iconset
cp $ICONSETNAME.png $ICONSETNAME.iconset/
cd $ICONSETNAME.iconset
convert -resize 16x16 ../$ICONSETNAME.png icon_16x16.png
convert -resize 32x32 ../$ICONSETNAME.png icon_16x16@2x.png
convert -resize 32x32 ../$ICONSETNAME.png icon_32x32.png
convert -resize 64x64 ../$ICONSETNAME.png icon_32x32@2x.png
convert -resize 128x128 ../$ICONSETNAME.png icon_128x128.png
convert -resize 256x256 ../$ICONSETNAME.png icon_128x128@2x.png
convert -resize 256x256 ../$ICONSETNAME.png icon_256x256.png
convert -resize 512x512 ../$ICONSETNAME.png icon_256x256@2x.png
convert -resize 512x512 ../$ICONSETNAME.png icon_512x512.png
convert -resize 1024x1024 ../$ICONSETNAME.png icon_512x512@2x.png
cd ..
iconutil -c icns $ICONSETNAME.iconset
rm $ICONSETNAME.iconset/*
rmdir $ICONSETNAME.iconset