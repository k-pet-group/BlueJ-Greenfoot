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
 ** @version $Id: CommentList.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** CommentList class - maintains a list of BlueJ comments
 **/
public final class CommentList
{
    private Vector comments;
	
    /**
     ** Constructor - create a CommentList with an initial list of comments.
     **/
    public CommentList(Vector comments)
    {
	this.comments = comments;
    }
	
    /**
     ** Constructor - create an empty CommentList.
     **/
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
	
    public void save(String filename) throws IOException
    {
	FileOutputStream out = new FileOutputStream(filename);
	save(out);
	out.close();
    }
		
    public void save(OutputStream out) throws IOException
    {
	Properties props = new Properties();
	props.put("numComments", String.valueOf(comments.size()));
	for(int i = comments.size(); i > 0; i--)
	    {
		Comment comment = (Comment)comments.elementAt(i - 1);
		comment.save(props, "comment" + i);
	    }
		
	props.save(out, "BlueJ class context");
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
	for(int i = numComments; i > 0; i--)
	    {
		Comment comment = new Comment();
		comment.load(props, "comment" + i);
		comments.addElement(comment);
	    }
    }
}
