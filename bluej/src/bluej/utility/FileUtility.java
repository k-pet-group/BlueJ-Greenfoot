package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.*;

/**
 * A file utility for various file related actions.
 *
 * @author  Markus Ostman
 * @author  Michael Kolling
 * @version $Id: FileUtility.java 557 2000-06-19 02:16:00Z ajp $
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
                                     String buttonLabel, FileFilter filter)
    {
        JFileChooser newChooser = getFileChooser(false);
        newChooser.setDialogTitle(title);

        if(filter == null)
            filter = newChooser.getAcceptAllFileFilter();
        newChooser.setFileFilter(filter);

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

    public static String getFileName(Component parent, String title,
                                     String buttonLabel)
    {
        return getFileName(parent, title, buttonLabel, null);
    }

    public static FileFilter getJavaSourceFilter()
    {
        return new JavaSourceFilter();
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

    private static class JavaSourceFilter extends FileFilter
    {
        /**
         * This method only accepts files that are Java source files.
         * Whether a file is a Java source file is determined by the fact that
         * its filename ends with ".java".
         */
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory() || pathname.getName().endsWith(".java"))
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
     * To serve our purposes we need to redefine some of the methods
     * in JFileChooser.
     *
     * Why is this Class static? Well since it is a nested class and
     * it needs to be instantiated in a class (static) method, this
     * seems to be the only way to do it.
     * Normally an inner class is instantiated by an instance of the
     * outer class but in this case it is the outer class itself that
     * instantiate it.
     */
    private static class BluejFileChooser extends JFileChooser
    {
        /**
         * Create a new BluejFileChooser.
         * @param startDirectory Directory to start the package selection in.
         * @param directoryOnly  Should it display just directories
         **/
        public BluejFileChooser(String startDirectory, boolean directoryOnly)
        {
            super(startDirectory);
            setFileView(new PackageFileView());
            if (directoryOnly)
                setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            else
                setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }

        /**
         * A directory was double-clicked. If it is a BlueJ package maybe
         * we want to treat it differently
         */
        public void setCurrentDirectory(File dir)    // redefined
        {
            //Here we could treat bluej package differently
            //At the moment nothing is done.
            if (Package.isBlueJPackage(dir)) {
                setSelectedFile(new File(""));
                super.setCurrentDirectory(dir);
            }
            else{
                setSelectedFile(new File("")); //clear the textfield
                super.setCurrentDirectory(dir);
            }
        }
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
            in = new BufferedInputStream(new FileInputStream(srcFile));
            out = new BufferedOutputStream(new FileOutputStream(destFile);
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

    /**
     * Recursively copy all files from one directory to another.
     *
     * @return An array contained each source file which was
     *         not successfully copied or null if everything went well
     */
    public static Object[] recursiveCopyFile(File srcDir, File destDir)
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
                    recursiveCopyFile(files[i], newDir);
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
            return failed.toArray();
        else
            return null;
    }
}
