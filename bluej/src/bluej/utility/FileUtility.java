package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;


/**
 * A file utility for various file related actions.
 *
 * @version $ $
 * @author Markus Ostman, but most of the methods are just copied across
 * from other parts of bluej.
 */

public class FileUtility
{
    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";

    private static PackageChooser pkgChooser = null;
    private static JFileChooser fileChooser = null;

    //========================= STATIC METHODS ============================

    /**
     *  Get a file name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     */
    public static String getFileName(Component parent, String title,
                                     String buttonLabel)
    {
        JFileChooser newChooser = getFileChooser(false);
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION)
            return newChooser.getSelectedFile().getPath();
        else if (result == JFileChooser.CANCEL_OPTION)
            return null;
        else {
            DialogManager.showError(parent, "error-no-name");
            return null;
        }
    }

    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    public static JFileChooser getPackageChooser()
    {
        if(pkgChooser == null)
            pkgChooser = new PackageChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
        return pkgChooser;
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    public static JFileChooser getFileChooser(boolean directoryOnly)
    {
        if(fileChooser == null) {
            fileChooser = new BlueJFileChooser(
                            Config.getPropString("bluej.defaultProjectPath",
                                                 "."), directoryOnly);
        }

        return fileChooser;
    }

    /**
     * Copy file 'source' to file 'dest'. The source file must exist,
     * the destination file will be created. Returns true if successful.
     */
    public static boolean copyFile(String source, String dest)
    {
        // check whether source and dest are the same
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
        if(srcFile.getAbsolutePath().equals(destFile.getAbsolutePath()))
            return true;  // don't bother - they are the same

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
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
                    File file2 = new File(dest + File.separator + dir[i]);
                    if(!copyFile(file, file2))
                        return COPY_ERROR;
                }
            }
        }
        return NO_ERROR;
    }
    
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
}





