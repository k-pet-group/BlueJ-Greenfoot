package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.pkgmgr.ClassTarget;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

import java.util.Map;
import java.util.HashMap;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * A wrapper for a BlueJ class. 
 * From this you can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 * For all methods the return value is null if the class is not compiled.
 * 
 * @version $Id: BClass.java 1855 2003-04-16 10:07:36Z damiano $
 */
public class BClass
{
    private Package  bluej_pkg;
    private final ClassTarget classTarget;
    private final Class loadedClass;
    private final View  bluej_view;

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

    BClass (Package i_bluej_pkg, String className)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = bluej_pkg.loadClass (transJavaToClass(className));
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
     * Returns the Java class being wrapped by this BClass.
     * Use this method when you need more information about the class than 
     * is provided by the BClass interface. E.g.:
     * What is the real class being hidden?
     * Is it an array?
     * What is the type of the array element?
     * 
     * Note that this is for information only. If you want to interact with BlueJ you must
     * use the methods provided in BClass.
     */
    public Class getJavaClass ()
      {
      return loadedClass;
      }

    /**
     * Returns the package this class belongs to.
     */
    public BPackage getPackage()
    {
        return new BPackage (bluej_pkg);
    }
    
      
    /**
     * Checks to see if this class has been compiled. 
     * Returns true if it is compiled and valid.
     * Returns true if it is a virtual class.
     */
    public boolean isCompiled()
    {
        if (classTarget == null) return true;

        return classTarget.isCompiled();
    }
    
    /**
     * Compile this class, and any dependents. 
     * 
     * @return true if the compilation was successful, false otherwise.
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
     * Returns the superclass of this class.
     * Similar to reflection API.
     */
    public BClass getSuperclass()
    {
        // This method is needed otherwise you cannot get a superclass of this BClass.
        View sup = bluej_view.getSuper();
        if ( sup == null ) return null;
        
        return new BClass (bluej_pkg, sup);
    }
    
    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
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
     * Returns the constructor for this class which has the given signature.
     * Similar to reflection API.
     * 
     * @param signature the signature of the required constructor.
     * @return the requested constructor of this class, or null if
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
     * Returns the declared methods of this class.
     * Similar to reflection API.
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
     * Returns the declared method of this class with the given signature.
     * Similar to reflection API.
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
     * Returns all the fields of this class.
     * Similar to reflection API.
     */
    public BField[] getFields()
        {
        if (!isCompiled()) return null;

        FieldView[] fieldView = bluej_view.getAllFields();
        BField[] bFields = new BField [fieldView.length];
        for ( int index=0; index<fieldView.length; index++)
            bFields[index] = new BField (bluej_pkg,fieldView[index]);
            
        return bFields;
        }


    /**
     * Returns the field of this class which has the given name.
     * Similar to Reflection API.
     */
    public BField getField(String fieldName)
        {
        if (!isCompiled()) return null;
        if ( fieldName == null ) return null;
        
        FieldView[] fieldView = bluej_view.getAllFields();
        for ( int index=0; index<fieldView.length; index++)
            {
            BField result = new BField (bluej_pkg,fieldView[index]);
            if ( result.matches(fieldName) ) return result;
            }
            
        return null;
        }


     // ====================== UTILITY AREA ====================================
     private static Map primiMap;

     static
        {
        // This will be executed once when this class is loaded
        primiMap = new HashMap();
        primiMap.put ("boolean", "Z");
        primiMap.put ("byte", "B");
        primiMap.put ("short", "S");
        primiMap.put ("char", "C");
        primiMap.put ("int", "I");
        primiMap.put ("long", "J");
        primiMap.put ("float", "F");
        primiMap.put ("double", "D");
        }

    /**
     * Needed to convert java style class names to classloaded class names.
     */
    private String transJavaToClass ( String javaStyle )
        {
        String className = javaStyle;

        int arrayCount = 0;
        while (className.endsWith ("[]")) 
          {
          // Counts how may arrays are in this class name
          arrayCount++;
          className = className.substring (0, className.length()-2);
          }

        // No array around, nothing to do.  
        if (arrayCount <= 0) return className;
        
        String replace = (String)BClass.primiMap.get(className);

        // If I can substitute the name I will do it
        if (replace != null)  className = replace;
        else                  className = "L"+className+";";
            
        while (arrayCount-- > 0) className = "["+className;
          
        return className;
        }

    }   
