package bluej.pkgmgr;

import java.io.IOException;
import java.util.Properties;

/**
 * Interface to a package file.
 * 
 * @author Poul Henriksen
 */
public interface PackageFile
{
    /**
     * Load the properties from the file into the given properties.
     * 
     * @throws IOException
     */
    public void load(Properties p)
        throws IOException;

    /**
     * Save the given properties to the file.
     * 
     * @return False if it couldn't save it.
     * @throws IOException
     * 
     */
    public void save(Properties p)
        throws IOException;
}
