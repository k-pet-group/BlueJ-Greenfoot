package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import bluej.views.*;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

/**
 * A wrapper for a BlueJ class. 
 * From this you can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 * 
 * @version $Id: BClass.java 1965 2003-05-20 17:30:25Z damiano $
 */

public class BClass
{
    private Identifier classId;

    /**
     * Constructor for the BClass.
     * It is duty of the caller to guarantee that it is a reasonable classId
     */
    BClass ( Identifier thisClassId )
      {
      classId = thisClassId;
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
      return classId.getJavaClass();
      }

    /**
     * Returns the package this class belongs to.
     * Similar to reflection API.
     */
    public BPackage getPackage()
    {
        Project bluejProject = classId.getBluejProject();
        Package bluejPkg = classId.getBluejPackage();

        return new BPackage (new Identifier (bluejProject, bluejPkg ));
    }
    
      
    /**
     * Checks to see if this class has been compiled. 
     * Returns true if it is compiled and valid.
     * Returns true if it is a virtual class.
     */
    public boolean isCompiled()
    {
        ClassTarget aTarget = classId.getClassTarget();

        if (aTarget == null) return true;

        return aTarget.isCompiled();
    }
    
    /**
     * Compile this class, and any dependents. 
     * 
     * @return true if the compilation was successful, false otherwise.
     */
    public boolean compile()
    {
        ClassTarget aTarget = classId.getClassTarget();
    
        if (aTarget == null) return true;
        
        if (aTarget.isCompiled()) return true;
        
        aTarget.compile (null);

        // Wait for the compilation to finish !
        JobQueue.getJobQueue().waitForEmptyQueue();

        return aTarget.isCompiled();
    }

    /**
     * Returns the superclass of this class.
     * Similar to reflection API.
     * ============ NEEDS TESTING ======================
     */
    public BClass getSuperclass()
    {
        // This method is needed otherwise you cannot get a superclass of this BClass.

        View bluejView = classId.getBluejView();
        
        View superView = bluejView.getSuper();
        
        if ( superView == null ) return null;

        // WARNING: This is most likely wrong !
        Project bluejPrj = classId.getBluejProject();
        // WARNING: This is most likely wrong !
        Package bluejPkg = classId.getBluejPackage();
        String  className = superView.getQualifiedName();
        
        return new BClass (new Identifier (bluejPrj, bluejPkg, className ));
    }
    
    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
     */
    public BConstructor[] getConstructors()
        {
        View bluejView   = classId.getBluejView();
        
        ConstructorView[] constructorViews = bluejView.getConstructors();
        BConstructor[] result = new BConstructor [constructorViews.length];
        for (int index=0; index<constructorViews.length; index++) 
            result[index] = new BConstructor (classId, constructorViews[index]);

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
        View bluejView = classId.getBluejView();
        
        ConstructorView[] constructorViews = bluejView.getConstructors();
        for (int index=0; index<constructorViews.length; index++) 
            {
            BConstructor aConstr = new BConstructor (classId, constructorViews[index]);
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
        View bluejView = classId.getBluejView();
        
        MethodView[] methodView = bluejView.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            methods[index] = new BMethod (classId, methodView[index] );

        return methods;
    }

    /**
     * Returns the declared method of this class with the given signature.
     * Similar to reflection API.
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params )
        {
        View bluejView = classId.getBluejView();
        
        MethodView[] methodView = bluejView.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            {
            BMethod aResul = new BMethod (classId, methodView[index]);
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
        View bluejView = classId.getBluejView();

        FieldView[] fieldView = bluejView.getAllFields();
        BField[] bFields = new BField [fieldView.length];
        for ( int index=0; index<fieldView.length; index++)
            bFields[index] = new BField (classId,fieldView[index]);
            
        return bFields;
        }


    /**
     * Returns the field of this class which has the given name.
     * Similar to Reflection API.
     */
    public BField getField(String fieldName)
        {
        if ( fieldName == null ) return null;

        View bluejView   = classId.getBluejView();
        
        FieldView[] fieldView = bluejView.getAllFields();
        for ( int index=0; index<fieldView.length; index++)
            {
            BField result = new BField (classId,fieldView[index]);
            if ( result.matches(fieldName) ) return result;
            }
            
        return null;
        }


    /**
     * Returns a string representation of the Object
     */
    public String toString ()
      {
      Class javaClass = classId.getJavaClass();
      if (javaClass == null) return "BClass: INVALID";

      return "BClass: "+javaClass.getName();
      }
    }   
