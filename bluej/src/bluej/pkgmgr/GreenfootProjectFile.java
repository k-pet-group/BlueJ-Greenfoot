package bluej.pkgmgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reference to the Greenfoot project file(s). A Greenfoot project file is
 * basically just a BlueJ package with some extra information added.
 * 
 * @author Poul Henriksen
 */
public class GreenfootProjectFile
    implements PackageFile
{
    private static final String pkgfileName = "project.greenfoot";
    private File dir;
    private File pkgFile;

    public GreenfootProjectFile(File dir)
    {
        this.dir = dir;
        this.pkgFile = new File(dir, pkgfileName);
    }

    public String toString()
    {
        return dir.toString() + File.separator + pkgfileName;
    }

    public void load(Properties p)
        throws IOException
    {
        FileInputStream input = null;
        try {
            if (pkgFile.canRead()) {
                input = new FileInputStream(pkgFile);
            }
            else {
                throw new IOException("Can't read from project file: " + pkgFile);
            }
            p.load(input);
        }
        finally {
            if(input != null) {
                input.close();
            }
        }
    }

    /**
     * Save the given properties to the file.
     * 
     * @throws IOException if something goes wrong while trying to write the
     *             file.
     */
    public void save(Properties props)
        throws IOException
    {
        if (!pkgFile.canWrite()) {
            throw new IOException("Greenfoot project file not writable: " + this);
        }

        // This is Greenfoot, this file will contain Greenfoot specific
        // properties as well that we don't want to overwrite, so we load the
        // file first.
        try {
            FileInputStream input = new FileInputStream(pkgFile);
            props.load(input);
        }
        catch (IOException e) {
            // If we can't load it for some reason, we just continue.
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(pkgFile);
            String header = "Greenfoot project file";
            props.store(output, header);
        }
        catch (IOException e) {
            throw new IOException("Error when storing properties to Greenfoot project file: " + this);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    /**
     * Whether a Greenfoot package file exists in this directory.
     */
    public static boolean exists(File dir)
    {
        if (dir == null)
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).
        if (dir.getPath().endsWith(":\\"))
            return false;

        if (!dir.isDirectory())
            return false;

        File packageFile = new File(dir, pkgfileName);
        return packageFile.exists();
    }

}
