package bluej.views;

import java.lang.reflect.Constructor;

import bluej.debugger.gentype.GenType;
import bluej.utility.JavaUtils;

/**
 * A representation of a Java constructor in BlueJ
 * 
 * @version $Id: ConstructorView.java 2969 2004-09-01 05:07:49Z davmac $
 * @author Michael Cahill
 * @author Michael Kolling
 */
public final class ConstructorView extends CallableView
{
    protected Constructor cons;

    /**
     * Constructor.
     */
    public ConstructorView(View view, Constructor cons) {
        super(view);
        this.cons = cons;
    }

    /**
     * Returns a string describing this Constructor.
     */
    public String toString() {
        return cons.toString();
    }

    public int getModifiers() {
        return cons.getModifiers();
    }

    /**
     * Returns a boolean indicating whether this method has parameters
     */
    public boolean hasParameters() {
        return (cons.getParameterTypes().length > 0);
    }

    /**
     * Returns a signature string in the format
     *  name(type,type,type)
     */
    public String getSignature() {
        return JavaUtils.getJavaUtils().getSignature(cons);
    }

    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc() 
    {
        return JavaUtils.getJavaUtils().getShortDesc(cons, getParamNames());       
    }

    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc() 
    {
        return JavaUtils.getJavaUtils().getLongDesc(cons, getParamNames());
    }
    
    /**
     * Get an array of Class objects representing constructor's parameters
     * @returns array of Class objects
     */
    public Class[] getParameters() {
        return cons.getParameterTypes();
    }
    
    public String[] getParamTypeStrings() 
    {
        return JavaUtils.getJavaUtils().getParameterTypes(cons);
    }
    
    public GenType[] getParamTypes(boolean raw)
    {
        return JavaUtils.getJavaUtils().getParamGenTypes(cons);
    }

    /**
     * Whether this method has a var arg.
     */
    public boolean isVarArgs() {
        return JavaUtils.getJavaUtils().isVarArgs(cons);
    }
}