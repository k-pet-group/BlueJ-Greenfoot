package bluej.views;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 ** @version $Id: CommentList.java 1819 2003-04-10 13:47:50Z fisker $
 ** @author Michael Cahill
 **
 ** CommentList class - maintains a list of BlueJ comments
 **/
public final class CommentList
{
    private List comments;
	
    /**
     * Constructor - create a CommentList with an initial list of comments.
     */
    public CommentList(List comments)
    {
        this.comments = comments;
    }
	
    /**
     * Constructor - create an empty CommentList.
     */
    public CommentList()
    {
        this(new ArrayList());
    }
	
    public void addComment(Comment comment)
    {
        comments.add(comment);
    }
	
    public void removeComment(Comment comment)
    {
        comments.remove(comment);
    }
	
    public Iterator getComments()
    {
        return comments.iterator();
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
            comments.add(comment);
        }
    }
}
