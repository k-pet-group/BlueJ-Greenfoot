package bluej.views;

import java.lang.reflect.Modifier;

/**
 ** @version $Id: MemberView.java 187 1999-07-17 02:32:38Z ajp $
 ** @author Michael Cahill
 **
 ** A representation of a Java class member in BlueJ
 **/
public abstract class MemberView
{
	protected View view;
	protected Comment comment;
	
	protected MemberView(View view)
	{
	    if (view == null)
	        throw new NullPointerException();

		this.view = view;
	}
	
	/**
	 ** Returns the View of the class or interface that declares this member.
	 **/
	public View getDeclaringView()
	{
		return view;
	}
	
	/**
	 ** Returns the name of the class or interface that declares this member.
	 **/
	public String getClassName()
	{
		return view.getName();
	}

	/**
	 ** Returns the Java language modifiers for the member or
	 ** constructor represented by this Member, as an integer.  The
	 ** Modifier class should be used to decode the modifiers in
	 ** the integer.
	 ** @see Modifier
	 **/
	public abstract int getModifiers();
	
	/**
	 ** Returns a string describing this member in a human-readable format
	 **/
	public abstract String getSignature();
	
	/**
	 ** Sets the (javadoc) comment for this Member
	 **/
	void setComment(Comment comment)
	{
		this.comment = comment;
	}
	
	/**
	 ** Returns the (javadoc) comment for this Member
	 **/
	public Comment getComment()
	{
		view.loadComments();
		
		return comment;
	}
	
	/**
	 ** Get a short String describing this member
	 **/
	public String getShortDesc()
	{
		return getSignature();
	}
	
	/**
	 ** Get a longer String describing this member
	 **/
	public String getLongDesc()
	{
		return getSignature();
	}
	
	/**
	 ** @returns a boolean indicating whether this member is static
	 **/
	public boolean isStatic()
	{
		return Modifier.isStatic(getModifiers());
	}
	
	public void print(FormattedPrintWriter out)
	{
		print(out, 0);
	}

	public void print(FormattedPrintWriter out, int indents)
	{
		Comment comment = getComment();
		if(comment != null)
			comment.print(out, indents);

		out.setItalic(false);
		out.setBold(true);
		for(int i=0; i<indents; i++)
		    out.indentLine();
		out.println(getLongDesc());
	}
}
