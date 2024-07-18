<img src="bluej/icons/bluej-icon-512-embossed.png" align="left" width="128">
<img src="greenfoot/resources/images/greenfoot-icon-big.jpg" align="right" width="100">

# BlueJ and Greenfoot

BlueJ and Greenfoot are integrated development environments (IDE) aimed at novices.  They support the use of the Java programming language, as well as the frame-based Stride language.

BlueJ and Greenfoot are maintained by Michael Kölling and his research group at King's College London.

Further details and installers
---

More details about BlueJ and Greenfoot, and pre-built installers for each, are available on the main webpages:
 - <a href="https://www.bluej.org/">BlueJ</a>
 - <a href="https://www.greenfoot.org/">Greenfoot</a>

License
---

This repository contains the source code for BlueJ and Greenfoot, licensed under the GPLv2 with classpath exception (see [LICENSE.txt](LICENSE.txt)).

Building and running
---

BlueJ uses Gradle as its automated build tool.  To build you will first need to install a Java (21) JDK.  Check out the repository then execute the following command to run BlueJ:

```
./gradlew runBlueJ
```

Or to run Greenfoot:

```
./gradlew runGreenfoot
```

Development
---

To work on the project, IntelliJ IDEA should import the Gradle project automatically, although you may need to set the JDK (21) and language level (also 21).

Contributing
---

We accept pull requests for translations or bug fixes.  If you plan to add a new feature or change existing behaviour we advise you to get in contact with us first, as we are likely to refuse any pull requests which are not part of our roadmap for BlueJ/Greenfoot.  One of the reasons for BlueJ and Greenfoot's success is their simplicity, which has been achieved by being very conservative in which features we choose to add.  

Building Installers
---

The installers are built automatically on Github.  If you want to build them manually you will need to set any appropriate tool paths in tools.properties, and run the appropriate single command of the following set:

```
./gradlew packageBlueJWindows
./gradlew packageBlueJLinux
./gradlew packageBlueJMacIntel
./gradlew packageBlueJMacAarch
./gradlew packageGreenfootWindows
./gradlew packageGreenfootLinux
./gradlew packageGreenfootMacIntel
./gradlew packageGreenfootMacAarch
```

None of the installers can be cross-built, so you must build Windows on Windows, Mac on Mac and Linux on Debian/Ubuntu.  Windows requires an installation of WiX 3.10 and MinGW64 to build the installer.  On Mac, JAVA_HOME must point to an Intel JDK for the Intel build, and an Aarch/ARM JDK for the Aarch build, so you cannot run them in the same command.

