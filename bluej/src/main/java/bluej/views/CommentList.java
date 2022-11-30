/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.views;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * CommentList class - maintains a list of BlueJ comments
 *
 * @author Michael Cahill
 */
public final class CommentList
{
    private List<Comment> comments;
	
    /**
     * Constructor - create a CommentList with an initial list of comments.
     */
    public CommentList(List<Comment> comments)
    {
        this.comments = comments;
    }
	
    /**
     * Constructor - create an empty CommentList.
     */
    public CommentList()
    {
        this(new ArrayList<Comment>());
    }
	
    public void addComment(Comment comment)
    {
        comments.add(comment);
    }
	
    public void removeComment(Comment comment)
    {
        comments.remove(comment);
    }
	
    public Iterator<Comment> getComments()
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
