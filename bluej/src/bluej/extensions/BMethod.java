package bluej.extensions;

import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.MethodView;
import bluej.views.ConstructorView;
import bluej.views.CallableView;
import java.lang.reflect.Modifier;

/**
 * The BlueJ proxy Method object. This represents a method of a class or object. 
 * This could be a method, or constructor.
 *
 * @author Clive Miller
 * @version $Id: BMethod.java 1547 2002-11-29 14:10:17Z damiano $
 * @see bluej.extensions.BObject#getMethod(java.lang.String,java.lang.Class[])
 * @see bluej.extensions.BObject#getMethods(boolean)
 * @see bluej.extensions.BClass#getConstructor(java.lang.Class[])
 * @see bluej.extensions.BClass#getConstructors()
 * @see bluej.extensions.BClass#getStaticMethod(java.lang.String,java.lang.Class[])
 * @see bluej.extensions.BClass#getStaticMethods()
 */
public class BMethod
{
    /**
     * Evaluates whether the given parameters match with a given method.
     * The name and signature of the method is matched against the name and
     * signature given as parameters.
     * @param method the method to test
     * @param name the name of the method being looked for
     * @param signature the signature of the method being looked for
     * @return <code>true</code> if the name matches, and the signature is assignable correctly
     */
    public static boolean matches (BMethod method, String name, Class[] signature)
    {
        Class[] thisargs = method.view.getParameters();
        if (thisargs != null && thisargs.length == 0) thisargs = null;
        if (thisargs == null || signature == null) {
            if (thisargs != null || signature != null) return false;
        } else {
            if (thisargs.length != signature.length) return false;
            for (int ii=0; ii<thisargs.length; ii++) {
                if (!thisargs[ii].isAssignableFrom (signature[ii])) return false;
            }
        }
        if (method.view instanceof MethodView) {
            if (!((MethodView)method.view).getName().equals(name)) return false;
        }
        return true;
    }

    private final BPackage pkg;
    private final CallableView view;
    private final String instanceName;
    private DirectInvoker invoker;
    
    /**
     * @param instanceName should be <code>null</code> for constructors and static methods
     */
    BMethod (BPackage pkg, CallableView view, String instanceName)
    {
        this.pkg = pkg;
        this.view = view;
        this.instanceName = instanceName;
    }
    
    /**
     * Gets the name of this method
     * @return the name of the method. If it is a constructor, an empty string is returned
     */
    public String getName()
    {
        if (view instanceof MethodView) {
            return ((MethodView)view).getName();
        } else {
            return "";
        }
    }
    
    /**
     * Gets the signature of this method
     * @return the signature of this method
     */
    public Class[] getSignature()
    {
        return view.getParameters();
    }

    /**
     * Gets the return type of this method
     * @return a string describing the return type, eg <code>int</code>, <code>Object</code>
     */
    public String getReturnType()
    {
        if (view instanceof MethodView) {
            return ((MethodView)view).getReturnType().getTypeName();
        } else {
            return view.getClassName();
        }
    }
    
    /**
     * Invokes the method. It is performed synchronously, so this method
     * blocks until the invocation is complete.
     * <P>The returning value is returned as a BField. If this is a constructor,
     * the new object can be extracted from BField and placed on the object
     * bench, or have further methods called on it.
     * @param args an array containing the arguments which are parsed in the same way that BlueJ
     * parses input from the user. These should be exactly
     * as they would appear in Java code, for example <BL>
     * <LI><CODE>5</CODE>
     * <LI><CODE>"Fred"</CODE>
     * <LI><CODE>3.5f</CODE>
     * <LI><CODE>person1</CODE>
     * </BL>They can refer to other objects on the object bench too. If there are no parameters,
     * this should be set to <CODE>null</CODE>.
     * @return the resulting BField
     */
    public BField invoke (String[] args)
    {
        return invoke (args, null);
    }

    /**
     * Invokes the method and places the result on the object bench, with
     * the given name. It is performed synchronously, so this method
     * blocks until the invocation is complete.
     * <P>The returning value is returned as a BField. If this is a constructor,
     * the new object can be extracted from BField and placed on the object
     * bench, or have further methods called on it.
     * @param args an array containing the arguments which are parsed in the same way that BlueJ
     * parses input from the user. These should be exactly
     * as they would appear in Java code, for example <BL>
     * <LI><CODE>5</CODE>
     * <LI><CODE>"Fred"</CODE>
     * <LI><CODE>3.5f</CODE>
     * <LI><CODE>person1</CODE>
     * </BL>They can refer to other objects on the object bench too. If there are no parameters,
     * this should be set to <CODE>null</CODE>.
     * @param resultName the name that the result should take when placed on the Object Bench
     * @return the resulting BField
     */
    public BField invoke (String[] args, String newObjectName)
    {
        invoker = new DirectInvoker (pkg.getRealPackage(), view, instanceName);
        DebuggerObject result = invoker.invoke (args);

        if (result == null) return null;

        String resultName = invoker.getResultName();
        if ( view instanceof ConstructorView) {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame (pkg.getRealPackage());
            ObjectWrapper wrapper =
                    new ObjectWrapper(pmf, pmf.getObjectBench(), result.getInstanceFieldObject(0), resultName);
            pmf.getObjectBench().add(wrapper);
            /* XXX To be fixed
            if (!newObjectName.equals (resultName)) {
                Debugger.debugger.addObjectToScope(pmf.getPackage().getId(), newObjectName,
                                                    result );
            }
*/
        }
        return new BField (pkg, result.getObjectReference(), result.getObjectReference().referenceType().fieldByName (resultName));
    }
    
    /**
     * Gets the last error that occurred. This should be called after receiving a <code>null</code> back from an
     * invoke
     * @return any error as a String
     */
    public String getLastError()
    {
        if (invoker == null) return null;
        return invoker.getError();
    }
    
    /**
     * Determines the modifiers for this method.
     * Use <code>java.lang.reflect.Modifiers</code> to
     * decode the meaning of this integer
     * @return The modifiers of this method, encoded in
     * a standard Java language integer.
     */
    public int getModifiers()
    {
        return view.getModifiers();
    }

    /**
     * Gets a description of this method
     * @return the return type, name and signature of the method
     */
    public String toString()
    {
        Class[] signature = getSignature();
        String sig = "";
        for (int i=0; i<signature.length; i++) {
            sig += signature[i].getName() + (i==signature.length-1?"":", ");
        }
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getReturnType()+" "+getName()+"("+sig+")";
    }
}