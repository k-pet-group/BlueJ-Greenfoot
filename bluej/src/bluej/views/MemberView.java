package bluej.views;

import java.lang.reflect.Modifier;

/**
 ** @version $Id: MemberView.java 36 1999-04-27 04:04:54Z mik $
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
		String desc = null;

		Comment comment = getComment();
		if(comment != null)
			desc = getComment().getShortDesc();

		if(desc == null)
			desc = getSignature();

		return desc;
	}
	
	/**
	 ** Get a longer String describing this member
	 **/
	public String getLongDesc()
	{
		String desc = null;

		Comment comment = getComment();
		if(comment != null)
			desc = getComment().getLongDesc();

		if(desc == null)
			desc = getShortDesc();

		return desc;
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
		print(out, "");
	}

	public void print(FormattedPrintWriter out, String prefix)
	{
		Comment comment = getComment();
		if(comment != null)
			comment.print(out, prefix);

		out.setItalic(false);
		out.setBold(true);
		out.println(prefix + getLongDesc());
	}
}
