/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import bluej.extensions2.SourceType;
import bluej.parser.InfoParser;
import bluej.parser.symtab.ClassInfo;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.javafx.FXPlatformSupplier;
import javafx.stage.Window;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions to help in the process of importing directory
 * structures into BlueJ.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 */
public class Import
{
    /**
     * Attempt to convert a non-bluej Path to a Bluej project.
     * 
     * <p>If no java source files are found, a warning dialog is displayed and
     * the conversion doesn't take place.
     * 
     * <p>If source files are found whose package line mismatches the apparent
     * package, a warning dialog is displayed and the user is prompted to
     * either allow the package line to be corrected, or to cancel the
     * conversion.
     *
     * @param parentWin  The parent window (used for centering dialogs)
     * @param path       The path of the directory containing the project-to-be
     * @return  true if the conversion was successfully completed
     */
    public static boolean convertNonBlueJ(FXPlatformSupplier<Window> parentWin, File path)
    {
        // find all sub directories with Java files in them
        // then find all the Java files in those directories
        List<File> interestingDirs = Import.findInterestingDirectories(path);

        // check to make sure the path contains some java source files
        if (interestingDirs.size() == 0) {
            DialogManager.showErrorFX(parentWin.get(), "open-non-bluej-no-java");
            return false;
        }

        List<File> javaFiles = Import.findJavaFiles(interestingDirs);

        // for each Java file, lets check its package line against the
        // package line we think that it should have
        // for each mismatch we collect the file, the package line it had,
        // and what we want to convert it to
        List<File> mismatchFiles = new ArrayList<File>();
        List<String> mismatchPackagesOriginal = new ArrayList<String>();
        List<String> mismatchPackagesChanged = new ArrayList<String>();

        Iterator<File> it = javaFiles.iterator();

        while (it.hasNext()) {
            File f = it.next();

            try {
                ClassInfo info = InfoParser.parse(f);
                if (info != null && ! info.hadParseError()) {

                    String qf = JavaNames.convertFileToQualifiedName(path, f);

                    if (!JavaNames.getPrefix(qf).equals(info.getPackage())) {
                        mismatchFiles.add(f);
                        mismatchPackagesOriginal.add(info.getPackage());
                        mismatchPackagesChanged.add(qf);
                    }
                }
            }
            catch (FileNotFoundException fnfe) {}
        }

        // now ask if they want to continue if we have detected mismatches
        if (mismatchFiles.size() > 0) {
            boolean shouldContinue;
            ImportMismatchDialog imd = new ImportMismatchDialog(parentWin.get(), mismatchFiles);
            shouldContinue = imd.showAndWait().orElse(false);

            if (!shouldContinue)
                return false;
        }

        // now add bluej.pkg files through the directory structure
        Import.convertDirectory(interestingDirs);
        return true;
    }
    
    /**
     * Find all directories under a certain directory which
     * we deem 'interesting'.
     * An interesting directory is one which either contains
     * a java source file or contains a directory which in
     * turn contains a java source file.
     *
     * @param   dir     the directory to look in
     * @returns         a list of File's representing the
     *                  interesting directories
     */
    public static List<File> findInterestingDirectories(File dir)
    {
        List<File> interesting = new LinkedList<File>();

        File[] files = dir.listFiles();

        if (files == null)
            return interesting;

        boolean imInteresting = false;

        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                // if any of our sub directories are interesting
                // then we are interesting
                // we ensure that the subdirectory would have
                // a valid java package name before considering
                // anything in it
                if(JavaNames.isIdentifier(files[i].getName())) {
                    List<File> subInteresting = findInterestingDirectories(files[i]);

                    if (subInteresting.size() > 0) {
                        interesting.addAll(subInteresting);
                        imInteresting = true;
                    }
                }
            }
            else {
                if (files[i].getName().endsWith("." + SourceType.Java.toString().toLowerCase()))
                    imInteresting = true;
            }
        }

        // if we have found anything of interest (either a java
        // file or a subdirectory with java files) then we consider
        // ourselves interesting and add ourselves to the list
        if (imInteresting)
            interesting.add(dir);

        return interesting;
    }

    /**
     * Find all Java files contained in a list of
     * directory paths.
     */
    public static List<File> findJavaFiles(List<File> dirs)
    {
        List<File> interesting = new LinkedList<File>();

        Iterator<File> it = dirs.iterator();

        while(it.hasNext()) {
            File dir = it.next();

            File[] files = dir.listFiles();

            if (files == null) {
                continue;
            }

            for (int i=0; i<files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith("." + SourceType.Java.toString().toLowerCase())) {
                    interesting.add(files[i]);
                }
            }
        }

        return interesting;
    }

    /**
     * Convert an existing directory structure to one
     * that BlueJ can open as a project.
     */
    public static void convertDirectory(List<File> dirs)
    {
        // create a BlueJ package file in every directory that
        // we have determined to be interesting

        Iterator<File> i = dirs.iterator();

        while(i.hasNext()) {
            File f = (File) i.next();
            try {
                PackageFileFactory.getPackageFile(f).create();
            }
            catch (IOException e) {
                Debug.reportError("Could not create package files in dir: " + f, e);
            }
          
        }
    }
}
