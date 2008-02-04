package bluej.groupwork;

/**
 * A revision number with associated information - author, date, comment.
 * 
 * @author Davin McCall
 */
public class Revision
{
    private String author;
    private String date;
    private String comment;
    
    public Revision(String author, String date, String comment)
    {
        this.author = author;
        this.date = date;
        this.comment = comment;
    }
    
    public String getAuthor()
    {
        return author;
    }
    
    public String getDateString()
    {
        return date;
    }
    
    public String getMessage()
    {
        return comment;
    }
    
    public int hashCode()
    {
        return author.hashCode() + date.hashCode() + comment.hashCode();
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof Revision) {
            Revision rother = (Revision) other;
            return rother.author.equals(author) && rother.date.equals(date)
                && rother.comment.equals(comment);
        }
        return false;
    }
}
