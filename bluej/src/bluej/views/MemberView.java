package bluej.views;

import java.lang.reflect.Modifier;

/**
 * A representation of a Java class member in BlueJ.
 *
 * @author  Michael Cahill
 * @version $Id: MemberView.java 2016 2003-06-04 05:55:54Z ajp $
 */
public abstract class MemberView
{
    private View view;
    private Comment comment;

    protected MemberView(View view)
    {
        if (view == null)
            throw new NullPointerException();

        this.view = view;
    }

    /**
     * @return the View of the class or interface that declares this member.
     */
    public View getDeclaringView()
    {
        return view;
    }

    /**
     * @return the name of the class or interface that declares this member.
     */
    public String getClassName()
    {
        return view.getQualifiedName();
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
        if (view != null)
            view.loadComments();

        return comment;
    }

    /**
     ** Get a short String describing this member
     **/
    public abstract String getShortDesc();

    /**
     ** Get a longer String describing this member
     **/
    public abstract String getLongDesc();

    /**
     * @return a boolean indicating whether this member is static
     */
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

    public String toString()
    {
        return view.toString();
    }
}
