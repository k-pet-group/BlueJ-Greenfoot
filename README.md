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


Releases and Release Candidates
---

Both release candidates and releases are built by the **Build Installers** workflow (`.github/workflows/build-installers.yml`).  More detailed step-by-step checklists (including the website and bug-tracker steps) are in `bluej/doc/HOWTO.make-release` and `greenfoot/doc/HOWTO.release`.

### Version numbers

The version (major/minor/release) of each product is set in `version.properties`: `bluej_*` and `greenfoot_*` each have major/minor/release/suffix settings.  If you change the version to a new x.y.z you must also add a new GUID for it further down in the same file (see the comment at the top), even if you are not building for Windows.

The release candidate number is **not** taken from `version.properties` when building a candidate: it comes from the tag (see below).  The `*_rcnumber` settings are only used for builds not triggered by an RC tag (e.g. a manual run or a push to `version.properties`), where they are added to the installer filenames as `-rcN`.

### Making a release candidate

1. Make sure the version in `version.properties` is the one you are releasing, and commit and push it.
2. Tag the commit `BLUEJ-RC-x.y.z-rcN` or `GREENFOOT-RC-x.y.z-rcN` and push the tag.  The x.y.z must match the version in `version.properties` (otherwise the job fails, to catch typos), and N is the candidate number, starting at 1:

   ```
   git tag BLUEJ-RC-6.0.1-rc1
   git push origin BLUEJ-RC-6.0.1-rc1
   ```
3. Pushing the tag runs **Build Installers**, which builds BlueJ and Greenfoot installers for Windows (msi and zip), Linux x64 and arm64 (deb, plus the generic jar), and Mac Intel and Apple Silicon (signed and notarised dmg, using secrets configured in the repository).  The installers for the product named in the tag get `-rcN` from the tag in their filenames.
4. When the builds finish, a **Publish-Prerelease** job creates a public GitHub **pre-release** for the tag with that product's installers attached.  Pre-releases never count as the "Latest" release, and can be downloaded without a GitHub account, so send testers the link to the pre-release page.
5. If testing finds problems, fix them and push a new tag with the next number (`-rc2`, ...).  There is no need to edit `version.properties` for a new candidate.

You can also start **Build Installers** manually from the **Actions** tab (**Build Installers** -> **Run workflow**), or by pushing a change to `version.properties`, but this only produces workflow artifacts (downloadable from the run's summary page by logged-in GitHub users, and expiring after a few months), not a pre-release.

### Making a release

1. Make a final `version.properties` change that drops the RC suffix, and commit and push it.  (Old pre-releases for candidates can be deleted once the release is out.)
2. Tag the release commit `BLUEJ-RELEASE-x.y.z` or `GREENFOOT-RELEASE-x.y.z` and push the tag:

   ```
   git tag BLUEJ-RELEASE-6.0.1
   git push origin BLUEJ-RELEASE-6.0.1
   ```
3. Pushing a tag of that form runs **Build Installers** again, and its final **Publish-Release** job collects the installers for the matching product (BlueJ for a `BLUEJ-RELEASE-*` tag, Greenfoot for `GREENFOOT-RELEASE-*`) and creates a **draft** GitHub Release for the tag with them attached.  The installer filenames already include the version and architecture, so nothing needs renaming.
4. On the repository's **Releases** page, open the draft, write the release notes, check the attached files, and click **Publish release**.  GitHub Releases is the download location; there is no separate source upload as the source is available at the tag.
5. Complete the remaining announcement and website steps in the HOWTO file for the product.
