CONFIGURATION AND CHANGE OF BEHAVIOUR OF PI4J WITH BLUEJ.
---------------------------------------------------------

F. Heday, 2014

What is Pi4J?
------------_
The Pi4J library is a JAVA layer for wiringPi. It allows java to use GPIO's in the raspberryPi. We included a modified
version of Pi4J with Bluej; the source for it can be found in the pi4j folder. The original project is found at:

	http://pi4j.com/


How to Build
------------
To Build Pi4J, we need to open the maven project at bluej/p4j, then build it. Part of this project needs to be compiled
on a raspberry pi. 

In order to compile, we need to configure the file pi4j/pom.xml, at the lines 92 to 96, we need to input the raspberry
pi machine where the compilation will take place:

		<!-- DEFAULT RASPBERRY PI PROPERTIES -->
		<pi.host>xxxx</pi.host>
		<pi.port>22</pi.port>
		<pi.user>pi</pi.user>
		<pi.password>raspberry</pi.password>

the machine name should also be present at the line 102:
		<pi.host.hard-float>xxxx</pi.host.hard-float>

The produced JARs are in the folder pi4j/pi4j-distribution/target/distro-contents/lib/, and are the following:
pi4j-device.jar
pi4j-gpio-extension.jar
pi4j-service.jar
pi4j-core.jar

Three ANT tasks were also created in the BlueJ build script:
pi4j-compile: calls maven to compile pi4j.
clean-pi4j: calls maven to clean pi4j.
pi4j-move-to-lib: move the pi4j jars to the lib/userlib folder in bluej.


What has been changed
---------------------
Compared to the regular distribution of Pi4J, the version included here has one main change.

The behaviour of mainstream Pi4J whenever requesting a GpioPin from the GpioController (via provisionPin method),
is to check if the pin is already allocated/provised. If the pin is already allocated/provised, then an exception
is raised and the method return no pins.

This behaviour puts the pin management duty on the user, making the code much more complex.

A common problem with this approach on BlueJ is when a user allocate a pin, then changes its code and run the 
new code on the same pin without unprovise/unallocate the pin: now we are in a situation where we can't 
unprovise/unallocate the pin since the reference to it was lost when we ran the new code. The only option left is to
restart the JVM.

The new behaviour implemented in the Pi4J present in the BlueJ repository still checks if the pin is already 
allocated/provised, however if the pin is already allocated/provised, then the pin is unallocated/unprovised 
(adittionally any listeners and or triggers registered to that pin are also unregistered), then the pin is 
provised/allocated normally, as if it wasn't allocated/provised in the first place.

This behaviour makes the user code much more simpler and more suitable for re-running  the same application
on BlueJ without issues with provised/allocated pins not being freed by the user. No need to restart the JVM 
anymore.
