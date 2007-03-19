package bluej.groupwork;

import java.util.Map;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.connection.Connection;

/**
 * Provide some additional mechanism over the standard CVS library "Client" class.
 * Specifically we need to track binary conflicts.
 * 
 * @author Davin McCall
 */
public class BlueJCvsClient extends Client
{
    BlueJFileHandler fileHandler;
    
    public BlueJCvsClient(Connection connection, AdminHandler adminHandler)
    {
        super(connection, adminHandler);
        fileHandler = new BlueJFileHandler();
        setUncompressedFileHandler(fileHandler);
    }
    
    /**
     * Get the map of conflicting files. The return maps (File to File) the
     * original file name (repository version) to its backup (local version).
     */
    public Map getConflictFiles()
    {
        return fileHandler.getConflicts();
    }
    
    /**
     * Inform the BlueJCvsClient that the next conflict detected is a non-binary
     * conflict.
     */
    public void nextConflictNonBinary()
    {
        fileHandler.nextConflictNonBinary();
    }
}
