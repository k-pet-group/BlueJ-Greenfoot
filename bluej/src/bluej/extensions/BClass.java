package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.ClassTarget;
import bluej.utility.Debug;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Modifier;
import java.awt.Point;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * This should behave as much as possible like the Java reflection API.
 * It will not be the same thing but every effort is made to avoid differences.
 * The reasoning behind it is that is is no good to create a new standard when there 
 * is already one that can be used.
 * 
 * @version $Id: BClass.java 1664 2003-03-07 12:10:55Z damiano $
 */
public class BClass
{
    private static Map primitiveArrayNameMap;
    static {
        primitiveArrayNameMap = new HashMap();
        primitiveArrayNameMap.put ("boolean", "Z");
        primitiveArrayNameMap.put ("byte", "B");
        primitiveArrayNameMap.put ("short", "S");
        primitiveArrayNameMap.put ("char", "C");
        primitiveArrayNameMap.put ("int", "I");
        primitiveArrayNameMap.put ("long", "J");
        primitiveArrayNameMap.put ("float", "F");
        primitiveArrayNameMap.put ("double", "D");
    }

    private String transJavaToClass (final String javaStyle)
    {
        String className = javaStyle;
        int array = 0;
        while (className.endsWith ("[]")) {
            array++;
            className = className.substring (0, className.length()-2);
        }
        if (array > 0) {
            String replace = (String)BClass.primitiveArrayNameMap.get(className);
            if (replace != null) {
                className = replace;
            } else {
                className = "L"+className+";";
            }
            while (array-- > 0) className = "["+className;
        }
        return className;
    }
    
    private Package  bluej_pkg;
    private final ClassTarget classTarget;
    private final Class loadedClass;
    private final View  bluej_view;


    /**
     * For use only by the bluej.extensions package
     */
    BClass (Package i_bluej_pkg, ClassTarget classTarget)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = classTarget;
        this.loadedClass = bluej_pkg.loadClass (classTarget.getQualifiedName());
        bluej_view = View.getView (loadedClass);
    }

    BClass (Package i_bluej_pkg, Class systemClass)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = systemClass;
        bluej_view = View.getView (loadedClass);
    }


    private BClass (Package i_bluej_pkg, View view)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = bluej_pkg.loadClass (view.getQualifiedName());
        bluej_view = view;
    }

    /**
     * @param className the Java style className, eg int[][]
     */    
    BClass (Package i_bluej_pkg, String className)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = bluej_pkg.loadClass (transJavaToClass (className));
        bluej_view = View.getView (loadedClass);
    }


    /**
     * TODO:
     */
    public boolean isValid()
      {
      return true;
      }

    /**
     * As From Reflection API
     */
    public boolean isArray ()
      {
      return loadedClass.isArray();
      }
      
    /**
     * Gets the owning Package of this class
     * @return the originator
     */
    public BPackage getPackage()
    {
        return new BPackage (bluej_pkg);
    }
    
   
    /**
     * Gets the name of this class
     * @return the simple name of the class, ie no package name
     */
    public String getName()
    {
        String name = getFullName();
        int i=name.lastIndexOf ('.');
        return  name.substring (i+1);
    }
    
    /**
     * Gets the fully-qualified name of this class
     * @return the full name of the class, ie with package name
     */
    public String getFullName()
    {
        Class cl = loadedClass;
        String postfix = "";
        while (cl.isArray()) {
            postfix += "[]";
            cl = cl.getComponentType();
        }
        return cl.getName()+postfix;
    }
    
    /**
     * Checks to see if a class has been compiled. Ignored and returns <code>true</code> if it
     * is a virtual class.
     * @return <code>true</code> if the class has been compiled and has not been modified since.
     */
    public boolean isCompiled()
    {
        if ( ! isValid () ) return false;
        
        if (classTarget == null) return true;

        return classTarget.isCompiled();
    }
    
    /**
     * Compile this class, and any dependants. Performed synchronously.  Ignored and returns 
     * <code>true</code> if it is a virtual class.
     * @return <code>true</code> if the compilation was successful. 
     */
    public boolean compile()
    {
        if (classTarget == null) return true;
        if (isCompiled()) return true;
        classTarget.compile (null);
        JobQueue.getJobQueue().waitForEmptyQueue();
        return isCompiled();
    }

    /**
     * Gets the superclass to this one
     * @return the immediate superclass to this one, or <code>null</code> if it has none
     */
    public BClass getSuper()
    {
        View sup = bluej_view.getSuper();

        if ( sup == null ) return null;
        
        return new BClass (bluej_pkg, sup);
    }
    
    /** Gets the array type of this array
     * @return the type of each element of this array
     * @throws IllegalArgumentException if this is not an array
     */
    public BClass getArrayType()
    {
        if (!loadedClass.isArray()) throw new IllegalArgumentException ("Not an array");
        return new BClass (bluej_pkg, loadedClass.getComponentType());
    }
    
    /**
     * As From reflection: gets all constructors of this class
     * NOTE: If the class is NOT compiled it WILL return a zero len constructors array
     * 
     * @return an array of constructors, zero len array if none or invalid
     */
    public BConstructor[] getConstructors()
        {
        if ( ! isCompiled() ) return new BConstructor[0];

        ConstructorView[] constructorViews = bluej_view.getConstructors();
        BConstructor[] result = new BConstructor [constructorViews.length];
        for (int index=0; index<constructorViews.length; index++) 
            result[index] = new BConstructor (bluej_pkg, constructorViews[index]);

        return result;
        }
     
    /**
     * Gets a constructor for this class complying with the given criteria
     * @param signature the signature of the required constructor
     * @return the requested constructor of this class, or <code>null</code> if
     * the class has not been compiled or the constructor cannot be found.
     */
    public BConstructor getConstructor (Class[] signature)
        {
        if (!isCompiled()) return null;
        
        ConstructorView[] constructorViews = bluej_view.getConstructors();
        for (int index=0; index<constructorViews.length; index++) 
            {
            BConstructor aConstr = new BConstructor (bluej_pkg, constructorViews[index]);
            if (aConstr.matches (signature)) return aConstr;
            }
        return null;
        }


    /**
     * Gets the declared method of this class
     * @return the methods of this class
     */
    public BMethod[] getDeclaredMethods()
    {
        if (!isCompiled()) return new BMethod[0];
        
        MethodView[] methodView = bluej_view.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            methods[index] = new BMethod (bluej_pkg, methodView[index] );

        return methods;
    }

    /**
     * Gets the declared method of this class with the given signature
     * @return the methods of this class
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params )
        {
        if (!isCompiled()) return null;
        
        MethodView[] methodView = bluej_view.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            {
            BMethod aResul = new BMethod (bluej_pkg, methodView[index]);
            if ( aResul.matches(methodName, params) ) return aResul;
            }

        return null;
        }


    /**
     * Returns all BFields of this Class
     */
    public BField[] getFields()
        {
        if (!isCompiled()) return null;

        FieldView[] fieldView = bluej_view.getAllFields();
        BField[] bFields = new BField [fieldView.length];
        for ( int index=0; index<fieldView.length; index++)
            bFields[index] = new BField (fieldView[index]);
            
        return bFields;
        }


    /**
     * Returns a specific Field of this Class
     */
    public BField getField(String fieldName)
        {
        if (!isCompiled()) return null;
        if ( fieldName == null ) return null;
        
        FieldView[] fieldView = bluej_view.getAllFields();
        for ( int index=0; index<fieldView.length; index++)
            {
            BField result = new BField (fieldView[index]);
            if ( result.matches(fieldName) ) return result;
            }
            
        return null;
        }

    /**
     * See Reflection API
     */
    public int getModifiers()
    {
        return loadedClass.getModifiers();
    }

    /**
     * Gets a description of this object
     * @return the classname of this class
     */
    public String toString()
    {
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getFullName();
    }
}   