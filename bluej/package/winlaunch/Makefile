# Make file - BlueJ/Greenfoot Windows launcher
#
# Must set below, and edit version in bjlaunch.rc and in bjmanifest.xml or gfmanifest.xml

# For BlueJ
ifeq ($(BUILD),bluej)
APPNAME=\"BlueJ\"
GREENFOOTDEF=
EXECNAME=bjlaunch.exe
REQUIREDJAVA=\"11\"
else
# For Greenfoot
APPNAME=\"Greenfoot\"
GREENFOOTDEF=-DGREENFOOT
EXECNAME=gflaunch.exe
REQUIREDJAVA=\"11\"
endif

# JDKDIR is set externally to something along the lines of:
# /c/Program Files/Java/jdk1.6.0_45

OBJECTS = resources.o bjlaunch.o javatest.o javaprops.o
CXXFLAGS = -Os -Wall

$(EXECNAME): $(OBJECTS)
	g++ -static -mwindows -o $(EXECNAME) $(OBJECTS) -lversion -lshlwapi
	strip $(EXECNAME)

resources.o: bjlaunch.rc resources.h
	windres $(GREENFOOTDEF) bjlaunch.rc resources.o

bjlaunch.o: bjlaunch.cc resources.h
	g++ $(GREENFOOTDEF) -DAPPNAME=$(APPNAME) -DREQUIREDJAVA=$(REQUIREDJAVA) $(CXXFLAGS) -I"$(JDKDIR)/include" -I"$(JDKDIR)/include/win32" -c bjlaunch.cc -o bjlaunch.o

javaprops.o: javaprops.cc winfilebuf.h
	g++ $(GREENFOOTDEF) -DAPPNAME=$(APPNAME) $(CXXFLAGS) -c javaprops.cc -o javaprops.o

javatest.o: javatest.cc
	g++ -DAPPNAME=$(APPNAME) $(CXXFLAGS) -Os -c javatest.cc -o javatest.o

clean:
	del $(OBJECTS)
