package bluej.groupwork;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;

/**
 * This is an admin handler which doesn't complain (i.e. throw an IOException)
 * when a local file or directory doesn't exist. This allows querying the
 * status of remote files in directories which don't even exist locally.
 *
 * Mostly, this involves delegating to the supeclass and catching IOException,
 * replacing it with a meaningful return value.
 * 
 * @author davmac
 * @version $Id: MildManneredAdminHandler.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class MildManneredAdminHandler extends StandardAdminHandler
{
    private boolean mode = false;
    
    public MildManneredAdminHandler()
    {
        super();
    }

    /**
     * Switch the admin handler between mild-mannered mode and
     * standard mode.
     * 
     * @param mode  true for mild-mannered mode
     */
    public void setMildManneredMode(boolean mode)
    {
        this.mode = mode;
    }
    
    public Entry getEntry(File file) throws IOException
    {
        try {
            return super.getEntry(file);
        }
        catch (IOException ioe) {
            if (mode) {
                return null;
            }
            else {
                throw ioe;
            }
        }
    }

    public Iterator getEntries(File directory) throws IOException
    {
        try {
            return super.getEntries(directory);
        }
        catch (IOException ioe) {
            if (mode) {
                return Collections.EMPTY_LIST.iterator();
            }
            else {
                throw ioe;
            }
        }
    }

    public String getRepositoryForDirectory(String directory, String repository)
            throws IOException
    {
        try {
            return super.getRepositoryForDirectory(directory, repository);
        }
        catch (IOException ioe) {
            if (mode) {
                File f = new File(directory);
                String parentFile = f.getParent();
                if (parentFile == null) {
                    throw ioe;
                }
                String parentRepository = getRepositoryForDirectory(f.getParent(), repository);
                return parentRepository + "/" + f.getName();
            }
            else {
                throw ioe;
            }
        }
    }

    public Set getAllFiles(File directory) throws IOException
    {
        if (mode && ! directory.exists()) {
            return Collections.EMPTY_SET;
        }

        return super.getAllFiles(directory);
    }
}
