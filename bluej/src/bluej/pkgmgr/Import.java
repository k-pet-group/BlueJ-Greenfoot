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

import javax.swing.JFrame;
import java.io.*;
import java.util.*;
import java.text.DateFormat;

/**
 *
 * @version $Id: Import.java 559 2000-06-19 02:24:16Z ajp $
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

    public static boolean importDir(File dir, Package rootPackage)
    {
/*                // handle Java source by doing an import
                if (files[i].getName().endsWith(".java")) {
                    result = rootPackage.importFile(files[i]);

                    switch(result) {
                     case Package.FILE_NOT_FOUND:
                     case Package.ILLEGAL_FORMAT:
                        System.out.println("notfound/illegal");
                        break;
                     case Package.COPY_ERROR:
                     case Package.CLASS_EXISTS:
                        System.out.println("gone");
                        break;
                     case Package.NO_ERROR:
                        break;
                    }
                } */
        return true;
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

    /**
     * Find all directories under a certain directory which
     * we deem 'interesting'.
     * An interesting directory is one which either contains
     * a java source file or contains a directory which in
     * turn contains a java source file.
     */
    public static List findInterestingDirectories(File dir)
    {
        List interesting = new LinkedList();

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

                    List subInteresting = findInterestingDirectories(files[i]);

                    if (subInteresting.size() > 0) {
                        interesting.addAll(subInteresting);
                        imInteresting = true;
                    }
                }
            }
            else {
                if (files[i].getName().endsWith(".java"))
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
     * Convert an existing directory structure to one
     * that BlueJ can open as a project.
     */
    public static void convertDirectory(File dir)
    {
        List l = findInterestingDirectories(dir);

        // create a bluej.pkg file in every directory that
        // we have determined to be interesting

        Iterator i = l.iterator();

        while(i.hasNext()) {
            File f = (File) i.next();

            File bluejFile = new File(f, Package.pkgfileName);

            try {
                bluejFile.createNewFile();
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
