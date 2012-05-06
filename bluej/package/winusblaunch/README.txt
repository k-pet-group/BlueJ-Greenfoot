This directory contains the source for a launch for BlueJ / Greenfoot, suitable for inclusion on a USB stick.
The USB launcher will look in the "BlueJ" (or "Greenfoot") subfolder for an installation of the BlueJ/Greenfoot
which must include the standard executable launcher .exe file (bluej.exe or greenfoot.exe):

USB stick
 |
 /--+--BlueJ
    |     |
    |     +-bluej.exe    (standard launcher, installed with BlueJ)
    |     +-lib          (lib folder)
    |     +-(other BlueJ installation files)
    |
    +--JDK   (optional)
    |
    +--userhome   (optional)
    |
    +-bluej.exe  (USB launcher)

It's possible to also install or copy a suitable JDK on to the USB stick. To cause BlueJ/Greenfoot to launch
with the included JDK, put the path to the JDK in the 'bluej.windows.vm' property in the bluej.defs file (in
the 'lib' folder). The path must be relative to the 'bluej.exe' USB launcher (not the standard launcher!)
Example:

    bluej.windows.vm=jdk1.6.0_27

It may also be desirable to set the 'bluej.userHome' setting in the bluej.defs file. *** This time, the path
should be relative to the standard launcher ***. Example:

    bluej.userHome=..\\userhome 

TO BUILD:

See the documentation in the 'winlaunch' folder (sibling to this folder). The build requirements are the same
and build procedure is similar. Edit the Makefile to control whether the build is for BlueJ or Greenfoot.
