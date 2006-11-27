package bluej.groupwork;

/**
 * Represents a single element of history information. This includes:
 * Filename, revision, date, user, comment
 * 
 * @author Davin McCall
 * @version $Id: HistoryInfo.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class HistoryInfo
{
    private String file;
    private String revision;
    private String date;
    private String user;
    private String comment;
    
    public HistoryInfo(String file, String revision, String date, String user, String comment)
    {
        this.file = file;
        this.revision = revision;
        this.date = date;
        this.user = user;
        this.comment = comment;
    }
    
    public String getFile()
    {
        return file;
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
