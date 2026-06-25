/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016,2019,2026  Michael Kolling and John Rosenberg

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
import bluej.parser.kotlin.KotlinInfoParser;
import bluej.parser.symtab.ClassInfo;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.javafx.FXPlatformSupplier;
import javafx.stage.Window;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
     * <p>If no source files (Java, Stride, or Kotlin) are found, a warning
     * dialog is displayed and the conversion doesn't take place.
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
        // find all sub directories with recognised source files,
        // then find all the source files in those directories
        List<File> interestingDirs = Import.findInterestingDirectories(path);

        // check to make sure the path contains some recognised source files
        if (interestingDirs.size() == 0) {
            DialogManager.showErrorFX(parentWin.get(), "open-non-bluej-no-java");
            return false;
        }

        List<File> sourceFiles = Import.findSourceFiles(interestingDirs);

        // for each source file, check its package directive against the
        // package line we think that it should have. For each mismatch we
        // collect the file, the package line it had, and what we want
        // to convert it to.
        List<File> mismatchFiles = new ArrayList<File>();
        List<String> mismatchPackagesOriginal = new ArrayList<String>();
        List<String> mismatchPackagesChanged = new ArrayList<String>();

        for (File f : sourceFiles) {
            try {
                ClassInfo info = parseSourceFile(f);
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
            catch (IOException ioe) {}
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
     * Parse a source file via the appropriate parser for its extension.
     * Returns null for unrecognised extensions or empty files.
     */
    private static ClassInfo parseSourceFile(File f) throws IOException
    {
        String name = f.getName();
        if (name.endsWith("." + SourceType.Java.getExtension())) {
            return InfoParser.parse(f);
        }
        if (name.endsWith("." + SourceType.Kotlin.getExtension())) {
            try (Reader r = new FileReader(f)) {
                // Pass file name so the parser can derive top-level-functions
                // file names from the stem; package is the only field we need
                // here, so targetPkg validation is skipped (null).
                return KotlinInfoParser.parse(r, null, name);
            }
        }
        // Stride files have generated .java siblings that are picked up
        // by the .java branch above; we don't need to parse .stride here.
        return null;
    }

    /**
     * Find all directories under a certain directory which
     * we deem 'interesting'.
     * An interesting directory is one which either contains
     * a recognised source file (Java, Stride, or Kotlin) or contains
     * a directory which in turn contains such a source file.
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
                if (hasRecognisedSourceExtension(files[i].getName()))
                    imInteresting = true;
            }
        }

        // if we have found anything of interest (either a source
        // file or a subdirectory with source files) then we consider
        // ourselves interesting and add ourselves to the list
        if (imInteresting)
            interesting.add(dir);

        return interesting;
    }

    /**
     * Find all recognised source files (Java, Stride, Kotlin) contained
     * in a list of directory paths. Stride is included for completeness
     * but {@link #convertNonBlueJ} only parses Java and Kotlin for
     * package-mismatch detection.
     */
    public static List<File> findSourceFiles(List<File> dirs)
    {
        List<File> interesting = new LinkedList<File>();

        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                if (f.isFile() && hasRecognisedSourceExtension(f.getName())) {
                    interesting.add(f);
                }
            }
        }

        return interesting;
    }

    /**
     * Whether {@code fileName} ends with one of the source extensions we
     * recognise during non-BlueJ project import: {@code .java},
     * {@code .stride}, or {@code .kt}.
     */
    private static boolean hasRecognisedSourceExtension(String fileName)
    {
        return fileName.endsWith("." + SourceType.Java.getExtension())
            || fileName.endsWith("." + SourceType.Stride.getExtension())
            || fileName.endsWith("." + SourceType.Kotlin.getExtension());
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
