/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2018,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A file utility for various file related actions.
 *
 * @author  Markus Ostman
 * @author  Michael Kolling
 */
@OnThread(Tag.Swing)
public class FileUtility
{
    /** 
     * Enum used to indicate file write capabilities on Windows.
     * @author polle
     * @see FileUtility#getVistaWriteCapabilities(File)
     */
    public enum WriteCapabilities {READ_ONLY, NORMAL_WRITE, VIRTUALIZED_WRITE, UNKNOWN}

    /**
     * Gets a directory containing a project to open.
     *
     * First, the user is shown a directory chooser.  If this is a BlueJ/Greenfoot *package*,
     * we navigate up the hierarchy to find the uppermost (parent-most?) directory with
     * a package, i.e. the surrounding project.  (This effectively was done using the Swing chooser, because it was
     * impossible to navigate into a project to choose a sub-package, and attempting
     * to paste the path of a sub-package would navigate upwards to find the project.)  We
     * don't want to let the user select sub-packages, anyway.
     *
     * If the user selects a directory which is not a BlueJ/Greenfoot package, we present
     * a dialog telling them so, and invite them to choose again or cancel.  As an additional
     * help, if any subdirectories of the chosen item are packages, we offer a handful of them
     * as options in this dialog.
     *
     * @param parent The parent window for the file chooser and dialog
     * @return A chosen File with a BlueJ/Greenfoot project, or null if the user cancelled.
     */
    @OnThread(Tag.FXPlatform)
    public static File getOpenProjectFX(Window parent)
    {
        File originalDir = getOpenDirFX(parent, Config.getString("pkgmgr.openPkg.title"), true);
        // They cancelled; nothing more to do:
        if (originalDir == null)
            return null;

        File dir = originalDir;
        // Navigate up the parents if they are projects too
        //   We don't need to check if we are currently a package.
        //   If we aren't, it's good we go to the parent if it is.
        //   If we are a package, we want to favour the parent.
        while (dir != null && dir.getParentFile() != null && Package.isPackage(dir.getParentFile()))
        {
            dir = dir.getParentFile();
        }

        if (!Package.isPackage(dir))
        {
            // We are not a package.  See if any child directories are:
            List<File> subDirs = null;
            if (dir != null)
            {
                subDirs = Arrays.asList(dir.listFiles(f -> f.isDirectory() && Package.isPackage(f)));
            }

            NotAProjectDialog dlg = new NotAProjectDialog(parent, originalDir, subDirs);
            dlg.showAndWait();
            if (dlg.isCancel())
                return null;
            else if (dlg.isChooseAgain())
                return getOpenProjectFX(parent);
            else
                return dlg.getSelectedDir();
        }
        else
        {
            // We are a package; all is well:
            return dir;
        }
    }

    @OnThread(Tag.FXPlatform)
    public static File getSaveProjectFX(Project project, Window parent, String title)
    {
        // JavaFX only has a directory-open dialog, so we use that:
        File chosen = new ProjectLocationDialog(project, parent, title).showAndWait();

        // If they cancelled, just stop:
        if (chosen == null)
            return null;

        if (chosen != null && chosen.getParentFile() != null)
        {
            PrefMgr.setProjectDirectory(chosen.getParentFile().getPath());
        }
        return chosen;
    }

    @OnThread(Tag.FXPlatform)
    public static List<File> getMultipleFilesFX(Window parent, String title, ExtensionFilter filter)
    {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(PrefMgr.getProjectDirectory());
        if (filter != null)
            chooser.getExtensionFilters().setAll(filter);
        chooser.setTitle(title);
        return chooser.showOpenMultipleDialog(parent);
    }

    @OnThread(Tag.FXPlatform)
    public static List<File> getOpenFilesFX(Window parent, String title,
                                            List<FileChooser.ExtensionFilter> filters,
                                            boolean rememberDir)
    {
        FileChooser newChooser = new FileChooser();
        newChooser.getExtensionFilters().setAll(filters);
        newChooser.setTitle(title);
        newChooser.setInitialDirectory(PrefMgr.getProjectDirectory());
        
        List<File> chosen = newChooser.showOpenMultipleDialog(parent);

        if (chosen != null && chosen.size() > 0 && chosen.get(0).getParentFile() != null && rememberDir)
        {
            PrefMgr.setProjectDirectory(chosen.get(0).getParentFile().getPath());
        }
        return chosen;
    }

    @OnThread(Tag.FXPlatform)
    public static File getSaveFileFX(Window parent, String title,
                                            List<FileChooser.ExtensionFilter> filters,
                                            boolean rememberDir)
    {
        FileChooser newChooser = new FileChooser();
        if (filters != null)
            newChooser.getExtensionFilters().setAll(filters);
        newChooser.setTitle(title);
        newChooser.setInitialDirectory(PrefMgr.getProjectDirectory());

        File chosen = newChooser.showSaveDialog(parent);

        if (chosen != null && chosen.getParentFile() != null &&  rememberDir)
        {
            PrefMgr.setProjectDirectory(chosen.getParentFile().getPath());
        }
        return chosen;
    }

    @OnThread(Tag.FXPlatform)
    public static File getOpenArchiveFX(Window parent, String title, boolean rememberDir)
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new ExtensionFilter("ZIP/JAR file", "*.zip", "*.jar"));
        if (title != null)
            chooser.setTitle(title);
        chooser.setInitialDirectory(PrefMgr.getProjectDirectory());

        File chosen = chooser.showOpenDialog(parent);

        if (chosen != null && chosen.getParentFile() != null && rememberDir)
        {
            PrefMgr.setProjectDirectory(chosen.getParentFile().getPath());
        }
        return chosen;
    }

    @OnThread(Tag.FXPlatform)
    public static File getOpenDirFX(Window parent, String title, boolean rememberDir)
    {
        DirectoryChooser newChooser = new DirectoryChooser();
        newChooser.setTitle(title);
        if (PrefMgr.getProjectDirectory() != null)
            newChooser.setInitialDirectory(PrefMgr.getProjectDirectory());

        File chosen = newChooser.showDialog(parent);

        if (chosen != null && chosen.getParentFile() != null &&  rememberDir)
        {
            PrefMgr.setProjectDirectory(chosen.getParentFile().getPath());
        }
        return chosen;
    }

    @OnThread(Tag.FX)
    public static ExtensionFilter getJavaStrideSourceFilterFX()
    {
        return new ExtensionFilter("Java/Stride source", "*." + SourceType.Java.getExtension(), "*." + SourceType.Stride.getExtension());
    }

    /**
     * Copy file 'source' to file 'dest'. The source file must exist,
     * the destination file will be created. Returns true if successful.
     */
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
    public static void copyStream(InputStream in, OutputStream out)
        throws IOException
    {
        byte[] buffer = new byte[4096];
        for(int c; (c = in.read(buffer)) != -1; )
            out.write(buffer, 0, c);
    }


    /**
     * Copy (recursively) a whole directory.
     */
    public static final int NO_ERROR = 0;
    public static final int SRC_NOT_DIRECTORY = 2;
    public static final int COPY_ERROR = 3;
    public static final int DEST_EXISTS_NOT_DIR = 4;
    public static final int DEST_EXISTS_NON_EMPTY = 5;

    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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

    @OnThread(Tag.Any)
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

        if (failed.size() > 0) {
            return failed.toArray(new File[0]);
        }
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
