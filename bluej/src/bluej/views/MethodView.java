package bluej.views;

import java.lang.reflect.*;
import bluej.Config;

/**
 *
 *  A representation of a Java method in BlueJ
 * 
 *  @version $Id: MethodView.java 2564 2004-06-01 13:07:17Z polle $
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
     *  name(type,type,type)
     */
    public String getSignature() {
        String name = View.getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeSignature(name, params);
    }

    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc() {
        String name = View.getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeDescription(name, params, false);
    }

    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc() {
        String name = View.getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeDescription(name, params, true);
    }

    /**
     * Get an array of Class objects representing method's parameters
     * @returns array of Class objects
     */
    public Class[] getParameters() {
        return method.getParameterTypes();
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
        return (Config.isJava15() && method.isVarArgs());
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