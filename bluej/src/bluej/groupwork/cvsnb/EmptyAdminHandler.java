package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.GlobalOptions;

/**
 * An admin handler which pretends that nothing exists.
 * 
 * @author Davin McCall
 * @version $Id: EmptyAdminHandler.java 5069 2007-05-28 05:18:17Z davmac $
 */
public class EmptyAdminHandler
    implements AdminHandler
{
    public EmptyAdminHandler()
    {
        // do nothing.
    }
    
    public void updateAdminData(String localDirectory, String repositoryPath, Entry entry, GlobalOptions globalOptions)
        throws IOException
    {
    }

    public Entry getEntry(File file)
        throws IOException
    {
        return null;
    }

    public Iterator getEntries(File directory)
        throws IOException
    {
        return Collections.EMPTY_LIST.iterator();
    }

    public void setEntry(File file, Entry entry)
        throws IOException
    {
    }

    public String getRepositoryForDirectory(String directory, String repository)
        throws IOException
    {
        return null;
    }

    public void removeEntry(File file)
        throws IOException
    {
    }

    public Set getAllFiles(File directory)
        throws IOException
    {
        return new HashSet();
    }

    public String getStickyTagForDirectory(File directory)
    {
        return null;
    }

    public boolean exists(File file)
    {
        return false;
    }

}
