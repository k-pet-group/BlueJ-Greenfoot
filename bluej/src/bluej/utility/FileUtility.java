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
 * @version $Id: FileUtility.java 2873 2004-08-16 05:50:32Z davmac $
 */
public class FileUtility
{
    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";

    private static JFileChooser pkgChooser = null;
    private static JFileChooser pkgChooserNonBlueJ = null;
    private static JFileChooser fileChooser = null;
    private static JFileChooser multiFileChooser = null;
    

    //========================= STATIC METHODS ============================

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
    public static String getFileName(Component parent, String title,
                                     String buttonLabel, boolean directoryOnly,
                                     FileFilter filter, boolean rememberDir)
    {
        JFileChooser newChooser = getFileChooser();

        newChooser.setDialogTitle(title);

        if (directoryOnly)
            newChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else
            newChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        if(filter == null)
            filter = newChooser.getAcceptAllFileFilter();
        newChooser.setFileFilter(filter);

        int result = newChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION) {
            if(rememberDir) {
                PrefMgr.setProjectDirectory(
                      newChooser.getSelectedFile().getParentFile().getPath());
            }
            return newChooser.getSelectedFile().getPath();
        }
        else if (result == JFileChooser.CANCEL_OPTION)
            return null;
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
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    private static JFileChooser getPackageChooser()
    {
        // find current dir name
        String currentDir = (new File("x")).getAbsolutePath();

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
        // find current dir name
        String currentDir = (new File("x")).getAbsolutePath();

        if(pkgChooserNonBlueJ == null)
            pkgChooserNonBlueJ = new PackageChooser(
                                          new File(PrefMgr.getProjectDirectory()));

        pkgChooserNonBlueJ.setDialogTitle(Config.getString("pkgmgr.openNonBlueJPkg.title"));
        pkgChooserNonBlueJ.setApproveButtonText(Config.getString("pkgmgr.openNonBlueJPkg.buttonLabel"));

        return pkgChooserNonBlueJ;
    }


    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    private static JFileChooser getFileChooser()
    {
        String currentDir = (new File("x")).getAbsolutePath();

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
        String currentDir = (new File("x")).getAbsolutePath();

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
    public static boolean copyFile(String source, String dest)
    {
        File srcFile = new File(source);
        File destFile = new File(dest);

        return copyFile(srcFile, destFile);
    }


    /**
     * Copy file 'srcFile' to file 'destFile'. The source file must exist,
     * the destination file will be created. Returns true if successful.
     */
    public static boolean copyFile(File srcFile, File destFile)
    {
        // check whether source and dest are the same
        if(srcFile.equals(destFile)) {
            return true;  // don't bother - they are the same
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(srcFile));
            out = new BufferedOutputStream(new FileOutputStream(destFile));
            copyStream(in, out);
            return true;
        } catch(IOException e) {
            return false;
        } finally {
            try {
                if(in != null)
                    in.close();
                if(out != null)
                    out.close();
            } catch (IOException e) {}
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
    public static final int DEST_EXISTS = 1;
    public static final int SRC_NOT_DIRECTORY = 2;
    public static final int COPY_ERROR = 3;

    public static int copyDirectory(String source, String dest,
                                    boolean excludeBlueJ,
                                    boolean excludeSource)
    {
        File srcFile = new File(source);
        File destFile = new File(dest);

        if(!srcFile.isDirectory())
            return SRC_NOT_DIRECTORY;

        if(destFile.exists())
            return DEST_EXISTS;

        if(!destFile.mkdir())
            return COPY_ERROR;

        String[] dir = srcFile.list();
        for(int i=0; i<dir.length; i++) {
            String srcName = source + File.separator + dir[i];
            File file = new File(srcName);
            if(file.isDirectory()) {
                if(copyDirectory(srcName, dest + File.separator + dir[i],
                                 excludeBlueJ, excludeSource) != NO_ERROR)
                    return COPY_ERROR;
            }
            else {
                if(!skipFile(dir[i], excludeBlueJ, excludeSource)) {
                    File file2 = new File(dest, dir[i]);
                    if(!copyFile(file, file2))
                        return COPY_ERROR;
                }
            }
        }
        return NO_ERROR;
    }


    /**
     * Checks whether a file should be skipped during a copy operation.
     * You can specify to skip BlueJ specific files and/or Java source
     * files.
     */
    public static boolean skipFile(String fileName,
                            boolean skipBlueJ, boolean skipSource)
    {
        if(skipBlueJ)
            if(fileName.startsWith(packageFilePrefix) ||
               fileName.endsWith(contextSuffix))
                return true;

        if(skipSource)
            if(fileName.endsWith(sourceSuffix))
                return true;

        return false;
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
        List failed = new ArrayList();

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

                if(newFile.exists() || !copyFile(files[i], newFile))
                    failed.add(files[i]);
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
}
