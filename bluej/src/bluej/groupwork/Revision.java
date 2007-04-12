package bluej.groupwork;

/**
 * A revision number with associated information - author, date, comment.
 * 
 * @author Davin McCall
 */
public class Revision
{
    private String author;
    private String number;
    private String date;
    private String comment;
    
    public Revision(String author, String number, String date, String comment)
    {
        this.author = author;
        this.number = number;
        this.date = date;
        this.comment = comment;
    }
    
    public String getAuthor()
    {
        return author;
    }
    
    public String getNumber()
    {
        return number;
    }
    
    public String getDateString()
    {
        return date;
    }
    
    public String getMessage()
    {
        return comment;
    }
}
