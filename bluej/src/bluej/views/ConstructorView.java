package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

/**
 ** @version $Id: ConstructorView.java 811 2001-03-25 23:11:51Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** A representation of a Java constructor in BlueJ
 **/
public final class ConstructorView extends CallableView
{
    protected Constructor cons;

    /**
     * Constructor.
     */
    public ConstructorView(View view, Constructor cons)
    {
        super(view);
        this.cons = cons;
    }

    /**
     * Returns a string describing this Constructor.
     */
    public String toString()
    {
        return cons.toString();
    }

    public int getModifiers()
    {
        return cons.getModifiers();
    }

    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public boolean hasParameters()
    {
        return (cons.getParameterTypes().length > 0);
    }

    /**
     * Returns a signature string in the format
     *  name(type,type,type)
     */
    public String getSignature()
    {
        Class[] params = cons.getParameterTypes();
        return makeSignature(JavaNames.getBase(cons.getName()), params);
    }

    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc()
    {
        Class[] params = cons.getParameterTypes();
        return makeDescription(cons.getName(), params, false);
    }

    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc()
    {
        Class[] params = cons.getParameterTypes();
        return makeDescription(cons.getName(), params, true);
    }

    /**
     * Get an array of Class objects representing constructor's parameters
     * @returns array of Class objects
     */
    public Class[] getParameters()
    {
        return cons.getParameterTypes();
    }


}
