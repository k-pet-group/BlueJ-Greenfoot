# Make file - BlueJ/Greenfoot Windows USB launcher
#
# Must set below

# For BlueJ
ifeq ($(BUILD),bluej)
APPNAME=\"BlueJ\"
EXECNAME=bluej.exe
GREENFOOTDEF=
else
# For Greenfoot
APPNAME=\"Greenfoot\"
EXECNAME=greenfoot.exe
GREENFOOTDEF=-DGREENFOOT
endif

EXENAME=\"$(EXECNAME)\"

# JDKDIR is set externally to something along the lines of:
# /c/Program Files/Java/jdk1.6.0_45

OBJECTS = resources.o usblaunch.o


$(EXECNAME): $(OBJECTS)
	g++ -static -mwindows -o $(EXECNAME) $(OBJECTS) -lversion
	strip $(EXECNAME)

resources.o: usblaunch.rc
	windres $(GREENFOOTDEF) usblaunch.rc resources.o

usblaunch.o: usblaunch.cc
	g++ -DAPPNAME=$(APPNAME) -DEXENAME=$(EXENAME) -Os -I"$(JDKDIR)/include" -I"$(JDKDIR)/include/win32" -c usblaunch.cc -o usblaunch.o

clean:
	del $(OBJECTS)
