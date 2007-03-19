package bluej.groupwork;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.netbeans.lib.cvsclient.file.DefaultFileHandler;

/**
 * A file handler which captures file rename operations requested by the server.
 * Renames are used to make backups of conflicting files; if we capture them
 * we can give the user the option of keeping one or the other (the local
 * version, or the repository version).
 * 
 * @author Davin McCall
 */
public class BlueJFileHandler extends DefaultFileHandler 
{
    /** Map a file name to it's backed-up local version */
    private Map conflicts = new HashMap();
    
    private boolean ignoreNextConflict = false;
    
    /**
     * Inform the file handler that the next conflict is a non-binary
     * conflict (it doesn't need to be tracked).
     */
    public void nextConflictNonBinary()
    {
        ignoreNextConflict = true;
    }
    
    /**
     * Get the conflicts map. This is a map (File to File) which maps the
     * original file name to the backup file name for each file for which
     * a backup was created.
     */
    public Map getConflicts()
    {
        return conflicts;
    }
    
    public void renameLocalFile(String pathname, String newName)
        throws IOException
    {
        File path = new File(pathname);
        File parent = path.getParentFile();
        File backup = new File(parent, newName);
        
        // The backup shouldn't exist; the cvs library explicitly deletes it if
        // it does, before calling this method. But we'll check for safety.
        if (! backup.exists()) {
            if (! ignoreNextConflict) {
                conflicts.put(path, backup);
            }
            super.renameLocalFile(pathname, newName);
        }
        
        ignoreNextConflict = false;
    }
}
