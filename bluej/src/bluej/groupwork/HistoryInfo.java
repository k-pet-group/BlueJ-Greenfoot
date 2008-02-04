package bluej.groupwork;

/**
 * Represents a single element of history information. This includes:
 * Filename, revision, date, user, comment
 * 
 * @author Davin McCall
 * @version $Id: HistoryInfo.java 5529 2008-02-04 04:39:56Z davmac $
 */
public class HistoryInfo
{
    private String [] files;
    private String revision;
    private String date;
    private String user;
    private String comment;
    
    public HistoryInfo(String [] files, String revision, String date, String user, String comment)
    {
        this.files = files;
        this.revision = revision;
        this.date = date;
        this.user = user;
        this.comment = comment;
    }
    
    public String [] getFiles()
    {
        return files;
    }
    
    public String getRevision()
    {
        return revision;
    }
    
    public String getDate()
    {
        return date;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public String getComment()
    {
        return comment;
    }
}
