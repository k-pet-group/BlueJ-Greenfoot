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

BlueJ currently uses ant as its automated build tool, although this may change in future.  To build you will first need to install Ant (any recent version), Java (currently 17) JDK and JavaFX (also 17).  With ant on your path, check out the repository then execute (or equivalent commands on Linux):

```
cd bluej
cp build.properties.template build.properties
nano build.properties
```

In the build.properties file you will need to set the paths for `build_java_home` to your JDK and `openjfx_path` to your JavaFX installation.  Then you can run:

```
ant ready-to-run
```

To run BlueJ, you can then execute: `ant run`.

To build Greenfoot you must first perform the other steps above.  Then, assuming you are still in the BlueJ directory run:

```
cd ../greenfoot
ant ready-to-run
```

There is no properties file that needs editing for Greenfoot; it uses the values from BlueJ that you already set.

To run Greenfoot, you can then execute: `ant run-greenfoot`.

Development
---

Relatively recent instructions on setting up the project in IntelliJ IDEA are in the file <a href="bluej/doc/HOWTO.setup.bluej-greenfoot-in-intellij">bluej/doc/HOWTO.setup.bluej-greenfoot-in-intellij</a>.

Contributing
---

We accept pull requests for translations or bug fixes.  If you plan to add a new feature or change existing behaviour we advise you to get in contact with us first, as we are likely to refuse any pull requests which are not part of our roadmap for BlueJ/Greenfoot.  One of the reasons for BlueJ and Greenfoot's success is their simplicity, which has been achieved by being very conservative in which features we choose to add.  


