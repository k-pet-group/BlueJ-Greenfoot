package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.compiler.*;
import bluej.debugger.*;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.editor.moe.MoeEditorManager;
import bluej.graph.Graph;
import bluej.graph.Vertex;
import bluej.utility.*;
import bluej.utility.filefilter.*;
import bluej.views.Comment;
import bluej.views.CommentList;
import bluej.classmgr.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.JFrame;
import java.io.*;
import java.util.*;
import java.text.DateFormat;

/**
 *
 * @version $Id: Import.java 505 2000-05-24 05:44:24Z ajp $
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 */
public class Import
{
    /**
     * Enable to open a plain directory as a package by adding BlueJ specific
     * information to that directory. If the directory contains subdirs
     * prompt the user if they should be searched recursively. If Java source
     * files are found in a directory, information about them (names,
     * generated layout) is written into a newly created package file in that
     * directory. So, after this method is run the directory can be opened as
     * a BlueJ package.
     *
     * NOT YET WORKING! Named and nested packages do not work currently.
     * That's why in this method six lines are commented out. TEMPORARILY!!!
     *
     * @param dir the directory denoting the potential Java package.
     * @param frame the frame where questions may be displayed.
     * @return true if any Java source was found, false otherwise.
     */
    public static boolean importPackage(File dir, JFrame frame)
    {
/*
        Package newPkg = null; // = new Package();
 //       newPkg.dirname = dir.getPath();
//          newPkg.packageName = dir.getName();

        // try to find sources in the directory itself
        boolean found = importJavaSources(dir, newPkg);

        // try to find subpackages and create targets for them
        File[] subdirs = dir.listFiles(dirsOnly);
        if (subdirs.length > 0) {

            DialogManager.showMessage(frame,"cannot-search-subdirs");

//              int answer = DialogManager.askQuestion(frame,
//                                                     "also-search-subdirs");

//              if (answer == 0) { // yes, search subdirs
//                  found = (importSubPackages(subdirs, newPkg) || found);
//              }
        }

        if (found) {
            newPkg.doDefaultLayout();
//            newPkg.save(newPkg.dirname);

            return true;
        }
        else */
            return false;
    }


    /**
     * Try to import the directories in "dirlist" as subpackages into
     * "parent".
     *
     * @param dirlist a list of directories.
     * @param parent the parent package for the subpackages found.
     * @return true if at least one of the directories could be imported
     * as a subpackage (i.e. contains Java sources or subpackages itself),
     * false otherwise.
     */
    private static boolean importSubPackages(File[] dirlist, Package parent)
    {
        boolean found = false;
/*        for (int i = 0; i < dirlist.length; i++) {
            File subdir = dirlist[i];
            Package subpackage = importSubPackage(subdir, parent.packageName);
            if(subpackage != null) {
                Target t = new PackageTarget(parent, subdir.getName(),
                                             subpackage.packageName);
                parent.addTarget(t);
                found = true;
            }
        } */
        return found;
    }


    /**
     * Find Java source files in a directory that is nested in a package.
     * If source files are found in this or any subdirectory then the
     * appropriate package files are written.
     * @param dir the directory denoting the potential subpackage.
     * @param parentName the name of the parent package.
     * @return a package for dir if sources found, null otherwise.
     */

    private static Package importSubPackage(File dir, String parentName)
    {
        /*Package newPkg = new Package();
        newPkg.dirname = dir.getPath();
        newPkg.packageName = parentName + "." + dir.getName();

        // if it is already a BlueJ package return just the name information
        if (isBlueJPackage(dir))
            return newPkg;

        // try to find sources in the directory itself
        boolean found = importJavaSources(dir, newPkg);

        // try to find subpackages
        found = (importSubPackages(dir.listFiles(dirsOnly), newPkg) || found);

        if (found) {
            newPkg.doDefaultLayout();
            newPkg.save(newPkg.dirname);
            return newPkg;
        }
        else*/
            return null;
    }


    /**
     * Import the Java source files from a given directory into a package.
     *
     * @param dir the directory to be searched.
     * @param pkg the package the source shall be imported into.
     * @return true if any Java source file was found in dir, false otherwise.
     */
    private static boolean importJavaSources(File dir, Package pkg)
    {
/*
        // create targets for all Java source files and add them to pkg
        File[] files = dir.listFiles(javaOnly);
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            // remove ".java"
            String classname = filename.substring(0, filename.length() - 5);
            ClassTarget t = new ClassTarget(pkg, classname);
            pkg.addTarget(t);
        }
        return (files.length > 0); */
		return true;
    } // importJavaSources
}
