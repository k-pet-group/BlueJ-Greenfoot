package bluej.views;

import java.lang.reflect.Modifier;

/**
 * A representation of a Java class member in BlueJ.
 *
 * @author  Michael Cahill
 * @version $Id: MemberView.java 2955 2004-08-30 06:15:11Z davmac $
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

    public String toString()
    {
        return view.toString();
    }
}
