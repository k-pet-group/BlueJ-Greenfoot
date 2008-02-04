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
    private File [] files;
    private List revisionList;
    
    /**
     * Create a log information entry for the given file and list of revisions
     * @param file  The file to which the entry applies
     * @param revisionList  The list of revisions for this file (List of Revision)
     */
    public LogInformation(File [] files, List revisionList)
    {
        this.files = files;
        this.revisionList = revisionList;
    }
    
    public File [] getFiles()
    {
        return files;
    }
    
    public List getRevisionList()
    {
        return revisionList;
    }
}
