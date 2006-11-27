package bluej.views;

import java.lang.reflect.Constructor;
import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.utility.JavaUtils;

/**
 * A representation of a Java constructor in BlueJ
 * 
 * @version $Id: ConstructorView.java 4708 2006-11-27 00:47:57Z bquig $
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

    public boolean isGeneric()
    {
        return !JavaUtils.getJavaUtils().getTypeParams(cons).isEmpty();
    }
    
    public boolean isConstructor()
    {
        return true;
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
    
    public JavaType[] getParamTypes(boolean raw)
    {
        return JavaUtils.getJavaUtils().getParamGenTypes(cons);
    }
    
    public GenTypeDeclTpar[] getTypeParams()
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
        List tparams = jutils.getTypeParams(cons);
        return (GenTypeDeclTpar[]) tparams.toArray(new GenTypeDeclTpar[0]);
    }

    /**
     * Whether this method has a var arg.
     */
    public boolean isVarArgs() {
        return JavaUtils.getJavaUtils().isVarArgs(cons);
    }
}
