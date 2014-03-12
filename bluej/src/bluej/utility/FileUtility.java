/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.awt.Component;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import bluej.Config;
import bluej.prefmgr.PrefMgr;

/**
 * A file utility for various file related actions.
 *
 * @author  Markus Ostman
 * @author  Michael Kolling
 */
public class FileUtility
{
    /** 
     * Enum used to indicate file write capabilities on Windows.
     * @author polle
     * @see FileUtility#getVistaWriteCapabilities(File)
     */
    public enum WriteCapabilities {READ_ONLY, NORMAL_WRITE, VIRTUALIZED_WRITE, UNKNOWN};

    private static final String sourceSuffix = ".java";

    private static JFileChooser pkgChooser = null;
    private static JFileChooser pkgChooserNonBlueJ = null;
    private static JFileChooser fileChooser = null;
    private static PackageChooser directoryChooser = null;
    private static JFileChooser multiFileChooser = null;
    


    public static File getPackageName(Component parent)
    {
        JFileChooser chooser = getPackageChooser();

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        PrefMgr.setProjectDirectory(
                         chooser.getSelectedFile().getParentFile().getPath());
        
        return chooser.getSelectedFile();
    }

    public static File getNonBlueJDirectoryName(Component parent)
    {
        JFileChooser chooser = getNonBlueJPackageChooser();

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    /**
     *  Get file(s) from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     *  @return a File array containing the selected files
     */
    public static File[] getMultipleFiles(Component parent, String title,
            String buttonLabel, FileFilter filter)
    {
        JFileChooser newMultiChooser = getMultipleFileChooser();

        newMultiChooser.setDialogTitle(title);

        if(filter == null)
            filter = newMultiChooser.getAcceptAllFileFilter();
        newMultiChooser.setFileFilter(filter);
        
        int result = newMultiChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION) {
            
            return newMultiChooser.getSelectedFiles();
        }
        else if (result == JFileChooser.CANCEL_OPTION)
            return null;
        else {
            DialogManager.showError(parent, "error-no-name");
            return null;
        }
    }
    
    /**
     *  Get a file name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     */
    public static File getFile(Component parent, String title,
                                   String buttonLabel, FileFilter filter,
                                   boolean rememberDir)
    {
        JFileChooser newChooser = getFileChooser(false, filter);
        
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (rememberDir) {
                PrefMgr.setProjectDirectory(
                      newChooser.getSelectedFile().getParentFile().getPath());
            }
            return newChooser.getSelectedFile();
        }
        else if (result == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        else {
            DialogManager.showError(parent, "error-no-name");
            return null;
        }
    }
    
    public static String getFileName(Component parent, String title,
            String buttonLabel, FileFilter filter,
            boolean rememberDir)
    {
        File file = getFile(parent, title, buttonLabel, filter, rememberDir);
        if (file == null)
            return null;
        else
            return file.getPath();
    }
    
    /**
     *  Get a directory name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     *  
     *  @param parent   The parent component for the dialog display
     *  @param buttonLabel  The label for the select button
     *  @param existingOnly  Whether only existing directories should be
     *                       selectable
     *  @param rememberDir  Whether to remember the parent directory of
     *                      the selected directory across sessions 
     */
    public static File getDirName(Component parent, String title,
            String buttonLabel, boolean existingOnly, boolean rememberDir)
    {
        return getDirName(parent, title, buttonLabel, null, existingOnly, rememberDir);
    }
    
    /**
     *  Get a directory name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     *  
     *  @param parent   The parent component for the dialog display
     *  @param buttonLabel  The label for the select button
     *  @param startDir The directory to show in the file chooser to begin with
     *  @param existingOnly  Whether only existing directories should be
     *                       selectable
     *  @param rememberDir  Whether to remember the parent directory of
     *                      the selected directory across sessions 
     */
    public static File getDirName(Component parent, String title,
            String buttonLabel, File startDir, boolean existingOnly, boolean rememberDir)
    {
        PackageChooser newChooser = getDirectoryChooser();
        if (startDir != null)
        {
            newChooser.setCurrentDirectory(startDir);
        }
        newChooser.setFileFilter(newChooser.getAcceptAllFileFilter());
        newChooser.setAllowNewFiles(! existingOnly);
        
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (rememberDir) {
                if (newChooser.getSelectedFile().getParentFile() != null)
                {
                    PrefMgr.setProjectDirectory(newChooser.getSelectedFile().getParentFile().getPath());
                }
            }
            return newChooser.getSelectedFile();
        }
        else if (result == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        else {
            DialogManager.showError(parent, "error-no-name");
            return null;
        }
    }


    public static FileFilter getJavaSourceFilter()
    {
        return new JavaSourceFilter();
    }

    /**
     * Get a file chooser which can be used to select files or directories,
     * and which knows about BlueJ packages.<p>
     * 
     * The caller should use setDialogTitle() to set an appropriate title
     * for the returned chooser.
     * 
     * @param directoriesOnly  True to return a chooser which only allows
     *                         selecting directories
     * @param filter  The file filter to use, may be null to allow 'all files'
     * 
     * @return  A file chooser
     */
    private static JFileChooser getFileChooser(boolean directoriesOnly, FileFilter filter)
    {
        JFileChooser newChooser;
        
        if (directoriesOnly) {
            newChooser = getDirectoryChooser();
        }
        else {
            newChooser = getFileChooser();
            newChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }

        if(filter == null) {
            filter = newChooser.getAcceptAllFileFilter();
        }
        newChooser.setFileFilter(filter);
        
        return newChooser;
    }
    
    
    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    private static JFileChooser getPackageChooser()
    {
        if(pkgChooser == null) {
            pkgChooser = new PackageChooserStrict(new File(PrefMgr.getProjectDirectory()));
        }
        pkgChooser.setDialogTitle(Config.getString("pkgmgr.openPkg.title"));
        pkgChooser.setApproveButtonText(Config.getString("pkgmgr.openPkg.buttonLabel"));

        return pkgChooser;
    }

    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    private static JFileChooser getNonBlueJPackageChooser()
    {
        if(pkgChooserNonBlueJ == null)
            pkgChooserNonBlueJ = new PackageChooser(new File(PrefMgr.getProjectDirectory()), true, true);

        pkgChooserNonBlueJ.setDialogTitle(Config.getString("pkgmgr.openNonBlueJPkg.title"));
        pkgChooserNonBlueJ.setApproveButtonText(Config.getString("pkgmgr.openNonBlueJPkg.buttonLabel"));

        return pkgChooserNonBlueJ;
    }

    /**
     * return a file chooser for choosing any directory
     */
    private static PackageChooser getDirectoryChooser()
    {
        if (directoryChooser == null) {
            directoryChooser = new PackageChooser(new File(PrefMgr.getProjectDirectory()), false, false);
        }
        
        return directoryChooser;
    }

    /**
     * return a file chooser for choosing any file (default behaviour)
     */
    private static JFileChooser getFileChooser()
    {
        if(fileChooser == null) {
            fileChooser = new BlueJFileChooser(PrefMgr.getProjectDirectory());
        }

        return fileChooser;
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     * that is allows selection of multiple files
     */
    private static JFileChooser getMultipleFileChooser()
    {
        if(multiFileChooser == null) {
            multiFileChooser = new BlueJFileChooser(PrefMgr.getProjectDirectory());
            multiFileChooser.setMultiSelectionEnabled(true);
        }

        return multiFileChooser;
    }
    

    private static class JavaSourceFilter extends FileFilter
    {
        /**
         * This method only accepts files that are Java source files.
         * Whether a file is a Java source file is determined by the fact that
         * its filename ends with ".java".
         */
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory() ||
                pathname.getName().endsWith(sourceSuffix))
                   return true;
            else
                return false;
        }

        public String getDescription()
        {
            return "Java Source";
        }
    }


    /**
     * Copy file 'source' to file 'dest'. The source file must exist,
     * the destination file will be created. Returns true if successful.
     */
    public static void copyFile(String source, String dest)
        throws IOException
    {
        File srcFile = new File(source);
        File destFile = new File(dest);

        copyFile(srcFile, destFile);
    }


    /**
     * Copy file 'srcFile' to file 'destFile'. The source file must exist,
     * the destination file will be created. Returns true if successful.
     */
    public static void copyFile(File srcFile, File destFile)
        throws IOException
    {
        // check whether source and dest are the same
        if(srcFile.equals(destFile)) {
            return;  // don't bother - they are the same
        }

        InputStream in = null;
        OutputStream out = null;
        
        try {
            in = new BufferedInputStream(new FileInputStream(srcFile));
            out = new BufferedOutputStream(new FileOutputStream(destFile));
            copyStream(in, out);
        } finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }
        }
    }


    /**
     * Copy stream 'in' to stream 'out'.
     */
    public static void copyStream(InputStream in, OutputStream out)
        throws IOException
    {
        for(int c; (c = in.read()) != -1; )
            out.write(c);
    }


    /**
     * Copy (recursively) a whole directory.
     */
    public static final int NO_ERROR = 0;
    public static final int SRC_NOT_DIRECTORY = 2;
    public static final int COPY_ERROR = 3;
    public static final int DEST_EXISTS_NOT_DIR = 4;
    public static final int DEST_EXISTS_NON_EMPTY = 5;

    public static int copyDirectory(File srcFile, File destFile)
    {
        if(!srcFile.isDirectory())
            return SRC_NOT_DIRECTORY;

        if(destFile.exists() && !destFile.isDirectory())
            return DEST_EXISTS_NOT_DIR;
        
        // It's okay if it exists ,provided it is empty:
        if (destFile.exists())
        {
            if (destFile.list().length > 0)
                return DEST_EXISTS_NON_EMPTY;
        }
        else
        {
            if(!destFile.mkdir())
                return COPY_ERROR;
        }

        String[] dir = srcFile.list();
        for(int i=0; i<dir.length; i++) {
            //String srcName = source + File.separator + dir[i];
            File file = new File(srcFile, dir[i]);
            if(file.isDirectory()) {
                if(copyDirectory(file, new File(destFile, dir[i])) != NO_ERROR)
                    return COPY_ERROR;
            }
            else {
                File file2 = new File(destFile, dir[i]);
                try {
                    copyFile(file, file2);
                }
                catch (IOException ioe) {
                    return COPY_ERROR;
                }
            }
        }
        return NO_ERROR;
    }


    /**
     * Recursively copy all files from one directory to another.
     * If destination is a sub directory of source directory then
     * it returns without copying any files.
     *
     * @return An array contained each source file which was
     *         not successfully copied or null if everything went well
     */
    public static File[] recursiveCopyFile(File srcDir, File destDir)
    {
        if (srcDir == null || destDir == null)
            throw new IllegalArgumentException();

        File parentDir = destDir.getParentFile();

        // check to make sure that the destination is not a subdirectory
        // of the source (which would lead to infinite recursion)
        while(parentDir != null) {
            if (parentDir.equals(srcDir))
                return new File[] { srcDir };

            parentDir = parentDir.getParentFile();
        }

        return actualRecursiveCopyFile(srcDir, destDir);
    }

    private static File[] actualRecursiveCopyFile(File srcDir, File destDir)
    {
        // remember every file which we don't successfully copy
        List<File> failed = new ArrayList<File>();

        // check whether source and dest are the same
        if(srcDir.getAbsolutePath().equals(destDir.getAbsolutePath()))
            return null;

        if (!srcDir.isDirectory() || !destDir.isDirectory())
            throw new IllegalArgumentException();

        // get all entities in the source directory
        File[] files = srcDir.listFiles();

        for (int i=0; i < files.length; i++) {
            // handle directories by recursively copying
            if (files[i].isDirectory()) {

                File newDir = new File(destDir, files[i].getName());

                newDir.mkdir();

                if (newDir.isDirectory()) {
                    actualRecursiveCopyFile(files[i], newDir);
                }
                else {
                    failed.add(files[i]);
                }
            }
            else if(files[i].isFile()) {
                // handle all other files
                File newFile = new File(destDir, files[i].getName());

                if (! newFile.exists()) {
                    try {
                        copyFile(files[i], newFile);
                    }
                    catch (IOException ioe) {
                        failed.add(files[i]);
                    }
                }
                else {
                    failed.add(files[i]);
                }
            }
        }

        if (failed.size() > 0)
            return (File [])failed.toArray(new File[0]);
        else
            return null;
    }


    /**
     * Find a file with a given extension in a given directory or any
     * subdirectory. Returns just one randomly selected file with that
     * extension.
     *
     * @return   a file with the given extension in the given directory,
     *           or 'null' if such a file cannot be found.
     */
    public static File findFile(File startDir, String suffix)
    {
        File[] files = startDir.listFiles();

        // look for files here
        for (int i=0; i < files.length; i++) {
            if(files[i].isFile()) {
                if(files[i].getName().endsWith(suffix))
                    return files[i];
            }
        }

        // if we didn't find one, search subdirectories
        for (int i=0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                File found = findFile(files[i], suffix);
                if(found != null)
                    return found;
            }
        }
        return null;
    }


    /**
     * Check whether a given directory contains a file with a given suffix.
     * The search is NOT recursive.
     *
     * @return  true if a file with the given suffix exists in the given
     *          directory.
     */
    public static boolean containsFile(File dir, String suffix)
    {
        if (dir == null)
            throw new IllegalArgumentException();

        File[] files = dir.listFiles();

        if (files == null)
            throw new IllegalArgumentException();

        for (int i=0; i < files.length; i++) {
            if(files[i].isFile() && files[i].getName().endsWith(suffix))
                return true;
        }

        return false;
    }


    /**
     * Delete a directory recursively.
     * This method will delete all files and subdirectories in any
     * directory without asking questions. Use with care.
     *
     * @param directory   The directory that will be deleted.
     *
     */
    public static void deleteDir(File directory)
    {
        File[] fileList = directory.listFiles();

        //If it is a file or an empty directory, delete
        if(fileList == null || Array.getLength(fileList) == 0){
            try{
                directory.delete();
            }catch (SecurityException se){
                Debug.message("Trouble deleting: "+directory+se);
            }
        }
        else{
            //delete all subdirectories
            for(int i=0;i<Array.getLength(fileList);i++){
                deleteDir(fileList[i]);
            }
            //then delete the directory (when it is empty)
            try{
                directory.delete();
            }catch (SecurityException se){
                Debug.message("Trouble deleting: "+directory+se);
            }
        }
    }
    
    /**
     * Find the relative path from some parent directory to a file nested within.
     * For instance, for parent "/a/b" and file "/a/b/c/d/somefile.java" returns
     * "c/d/somefile.java".
     * 
     * @param parent  The containing directory
     * @param file    The file to get the relative path to
     * @return   The relative path between parent and file
     */
    public static String makeRelativePath(File parent, File file)
    {
        String filePath = file.getAbsolutePath();
        String parentPath = parent.getAbsolutePath();
        
        if (filePath.startsWith(parentPath)) {
            // Strip parent path and path separator
            filePath = filePath.substring(parentPath.length() + 1);
        }
        
        return filePath;
    }

    /**
     * Get the file write capabilities of the given directory. <br>
     * To find the capabilities, this method will try creating a
     * temporary file in the directory. <br>
     * See trac tickets 147 and 150 for more details.
     * 
     * @param dir
     *            Directory to check.
     * @return The capabilities of this directory. Will return
     *         {@link WriteCapabilities#UNKNOWN} if the file is not an existing
     *         directory.
     */
    public static WriteCapabilities getVistaWriteCapabilities(File dir) 
    {
        if(!dir.isDirectory()) {
            return WriteCapabilities.UNKNOWN;
        }
        WriteCapabilities capabilities = WriteCapabilities.UNKNOWN;

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("bluej", null, dir);
            tmpFile.deleteOnExit();
            if(isVirtualized(tmpFile)) {
                capabilities = WriteCapabilities.VIRTUALIZED_WRITE;
            } else {
                capabilities = WriteCapabilities.NORMAL_WRITE;
            }
        } catch (IOException e) {
            // We could not write the file
            capabilities = WriteCapabilities.READ_ONLY;
        } finally {
            if(tmpFile != null) {
                tmpFile.delete();
            }
        }
        return capabilities;
    }

    /**
     * Check whether the given file is virtualized by Windows (Vista).
     * 
     */
    private static boolean isVirtualized(File file)
    {
        boolean isVirtualized = false;

        // Virtualization only happens on Windows Vista (or later)
        if (Config.isModernWinOS()) {
            try {
                String canonicalPath = file.getCanonicalPath();
                int colonIndex = canonicalPath.indexOf(":");
                if (colonIndex > 0) {
                    String pathPart = canonicalPath.substring(colonIndex + 1);
                    String virtualStore = System.getenv("localappdata") + File.separator  + "VirtualStore";
                    String virtualTmpFilePath = virtualStore + pathPart;
                    isVirtualized = new File(virtualTmpFilePath).exists();
                }
            } catch (IOException e) {
                Debug.reportError(
                        "Error when testing for Windows virtualisation.", e);
            }
        }
        return isVirtualized;
    }
}
