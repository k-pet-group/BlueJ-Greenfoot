package bluej.views;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.GenType;
import bluej.utility.JavaUtils;

/**
 *
 *  A representation of a Java method in BlueJ
 * 
 *  @version $Id: MethodView.java 2951 2004-08-27 01:47:46Z davmac $
 * @author Michael Cahill
 * @author Michael Kolling
 */
public class MethodView extends CallableView implements Comparable
{
    protected Method method;
    protected View returnType;

    /**
     * Constructor.
     */
    public MethodView(View view, Method method) {
        super(view);
        this.method = method;
    }

    public Method getMethod()
    {
        return method;
    }
    
    /**
     * Returns a string describing this Method.
     */
    public String toString() {
        return method.toString();
    }

    public int getModifiers() {
        return method.getModifiers();
    }

    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public boolean hasParameters() {
        return (method.getParameterTypes().length > 0);
    }

    /**
     * Returns a signature string in the format
     * "type name(type,type,type)".
     */
    public String getSignature() {
        return JavaUtils.getJavaUtils().getSignature(method);
    }
    
    /**
     * Get the "call signature", ie. the signature without the return type.
     * This should not be made user visible, it is for internal purposes only.
     * It is useful for locating methods which override a method in a super
     * class, without having to worry about covariant returns and generic
     * methods etc.
     */
    public String getCallSignature() {
        StringBuffer name = new StringBuffer();
        name.append(method.getName());
        name.append('(');
        Class[] params = method.getParameterTypes();
        for(int i = 0; i < params.length; i++) {
            name.append(params[i].getName());
            if (i != params.length - 1)
                name.append(',');
        }
        name.append(')');
        return name.toString();
    }
    
    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc() {
        return JavaUtils.getJavaUtils().getShortDesc(method, getParamNames());
    }

    /**
     * Return a short description string, mapping type parameters from the
     * class to the corresponding instantiation type. Type parameters not
     * contained in the map are mapped to their erasure type; type parameters
     * from a generic method are left unmapped.
     *  
     * @param genericParams  The map of String -> GenType
     * @return  the signature string with type parameters mapped
     */
    public String getShortDesc(Map genericParams)
    {
        return JavaUtils.getJavaUtils().getShortDesc(method, getParamNames(), genericParams);
    }
    
    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc() {
        return JavaUtils.getJavaUtils().getLongDesc(method, getParamNames());
    }

    /**
     * Get an array of Class objects representing method's parameters
     * @returns array of Class objects
     */
    public Class[] getParameters() {
        return method.getParameterTypes();
    }
    
    public GenType[] getParamTypes()
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
        GenType [] ptypes = jutils.getParamGenTypes(method);
        
        // Now map the generic method type parameters to their base types
        List tparams = jutils.getTypeParams(method);
        Map tpmap = JavaUtils.TParamsToMap(tparams);
        for(int i=0; i < ptypes.length; i++) {
            ptypes[i] = ptypes[i].mapTparsToTypes(tpmap);
        }
        return ptypes;
    }
    
    public String[] getParamTypeStrings() 
    {
        return JavaUtils.getJavaUtils().getParameterTypes(method);
    }

    /**
     * Returns the name of this method as a String
     */
    public String getName() {
        return method.getName();
    }

    /**
     * @returns a boolean indicating whether this method has no return value
     */
    public boolean isVoid() {
        String resultName = getReturnType().getQualifiedName();
        return "void".equals(resultName);
    }

    /**
     * @returns if this method is the main method (a static void returning
     * function called main with a string array as an argument)
     */
    public boolean isMain() {
        if (!isVoid())
            return false;
        if ("main".equals(getName())) {
            Class[] c = getParameters();
            if (c.length != 1)
                return false;
            if (c[0].isArray() && String.class.equals(c[0].getComponentType())) {
                if (Modifier.isStatic(getModifiers()) && Modifier.isPublic(getModifiers()))
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Whether this method has a var arg.
     *
     */
    public boolean isVarArgs() {
        return JavaUtils.getJavaUtils().isVarArgs(method);
    }

    /**
     * Returns a Class object that represents the formal return type
     * of the method represented by this Method object.
     */
    public View getReturnType() {
        if (returnType == null)
            returnType = View.getView(method.getReturnType());
        return returnType;
    }
    
    

    // ==== Comparable interface ====
    /**
     * Compare operation to provide alphabetical sorting by method name.
     */
    public int compareTo(Object other) {
        MethodView otherView = (MethodView) other;
        return method.getName().compareTo(otherView.method.getName());
    }
}