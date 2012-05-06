# Make file - BlueJ/Greenfoot Windows USB launcher
#
# Must set below

# For BlueJ
APPNAME=\"BlueJ\"
EXECNAME=bluej.exe
GREENFOOTDEF=

# For Greenfoot
#APPNAME=\"Greenfoot\"
#EXECNAME=greenfoot.exe
#GREENFOOTDEF=-DGREENFOOT

EXENAME=\"$(EXECNAME)\"

JDKDIR=/c/Program Files/Java/jdk1.7.0_03

OBJECTS = resources.o usblaunch.o


$(EXECNAME): $(OBJECTS)
	g++ -static -mwindows -o $(EXECNAME) $(OBJECTS) -lversion
	strip $(EXECNAME)

resources.o: usblaunch.rc
	windres $(GREENFOOTDEF) usblaunch.rc resources.o

usblaunch.o: usblaunch.cc
	g++ -DAPPNAME=$(APPNAME) -DEXENAME=$(EXENAME) -Os -I"$(JDKDIR)/include" -I"$(JDKDIR)/include/win32" -c usblaunch.cc -o usblaunch.o

clean:
	rm $(OBJECTS)
