# Make file - BlueJ/Greenfoot Windows launcher
#
# Must set below, and edit version in bjlaunch.rc and in bjmanifest.xml or gfmanifest.xml

# For BlueJ
#APPNAME=\"BlueJ\"
#GREENFOOTDEF=
#EXECNAME=bjlaunch.exe
#REQUIREDJAVA=\"1.5\"

# For Greenfoot
APPNAME=\"Greenfoot\"
GREENFOOTDEF=-DGREENFOOT
EXECNAME=gflaunch.exe
REQUIREDJAVA=\"1.6\"

JDKDIR=/c/Program Files/Java/jdk1.6.0_05

OBJECTS = resources.o bjlaunch.o javatest.o javaprops.o bjdialog.o


$(EXECNAME): $(OBJECTS)
	g++ -static -mwindows -o $(EXECNAME) $(OBJECTS) -lversion
	strip $(EXECNAME)

resources.o: bjlaunch.rc resources.h
	windres $(GREENFOOTDEF) bjlaunch.rc resources.o

bjlaunch.o: bjlaunch.cc resources.h
	g++ $(GREENFOOTDEF) -DAPPNAME=$(APPNAME) -Os -I"$(JDKDIR)/include" -I"$(JDKDIR)/include/win32" -c bjlaunch.cc -o bjlaunch.o

javaprops.o: javaprops.cc winfilebuf.h
	g++ -DAPPNAME=$(APPNAME) -Os -c javaprops.cc -o javaprops.o

javatest.o: javatest.cc
	g++ -DAPPNAME=$(APPNAME) -Os -c javatest.cc -o javatest.o

bjdialog.o: bjdialog.cc
	g++ -DAPPNAME=$(APPNAME) -Os -c bjdialog.cc -o bjdialog.o


clean:
	rm $(OBJECTS)
