package bluej.views;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 ** @version $Id: CommentList.java 187 1999-07-17 02:32:38Z ajp $
 ** @author Michael Cahill
 **
 ** CommentList class - maintains a list of BlueJ comments
 **/
public final class CommentList
{
    private Vector comments;
	
    /**
     * Constructor - create a CommentList with an initial list of comments.
     */
    public CommentList(Vector comments)
    {
        this.comments = comments;
    }
	
    /**
     * Constructor - create an empty CommentList.
     */
    public CommentList()
    {
        this(new Vector());
    }
	
    public void addComment(Comment comment)
    {
        comments.addElement(comment);
    }
	
    public void removeComment(Comment comment)
    {
        comments.removeElement(comment);
    }
	
    public Enumeration getComments()
    {
        return comments.elements();
    }
	
    public int numComments()
    {
        return comments.size();
    }
	
    public void load(String filename) throws IOException
    {
        FileInputStream input = new FileInputStream(filename);
        load(input);
        input.close();
    }
	
    public void load(InputStream input) throws IOException
    {
        Properties props = new Properties();
        props.load(input);
    		
        int numComments = Integer.parseInt(props.getProperty("numComments", "0"));
        for(int i = numComments-1; i >= 0; i--)
        {
            Comment comment = new Comment();
            comment.load(props, "comment" + i);
            comments.addElement(comment);
        }
    }
}
