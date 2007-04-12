package bluej.groupwork;

import java.io.File;
import java.util.List;

/**
 * Class for objects representing log entries from commit operations.
 * 
 * @author Davin McCall
 */
public class LogInformation
{
    private File file;
    private List revisionList;
    
    /**
     * Create a log information entry for the given file and list of revisions
     * @param file  The file to which the entry applies
     * @param revisionList  The list of revisions for this file (List of Revision)
     */
    public LogInformation(File file, List revisionList)
    {
        this.file = file;
        this.revisionList = revisionList;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public List getRevisionList()
    {
        return revisionList;
    }
}
