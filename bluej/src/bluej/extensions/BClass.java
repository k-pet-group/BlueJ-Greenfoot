package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import bluej.views.*;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import java.util.*;

/**
 * A wrapper for a BlueJ class. 
 * From this you can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 * 
 * @version $Id: BClass.java 1982 2003-05-23 08:08:34Z damiano $
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public Class getJavaClass () 
      throws ProjectNotOpenException, ClassNotFoundException
      {
      return classId.getJavaClass();
      }

    /**
     * Returns the package this class belongs to.
     * Similar to reflection API.
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this class belongs has been deleted by the user.
     */
    public BPackage getPackage() throws ProjectNotOpenException, PackageNotFoundException
    {
        Project bluejProject = classId.getBluejProject();
        Package bluejPkg = classId.getBluejPackage();

        return new BPackage (new Identifier (bluejProject, bluejPkg ));
    }
    
      
    /**
     * Checks to see if this class has been compiled. 
     * @return true if it is compiled false othervise.
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this class belongs has been deleted by the user.
     * @throws ClassNotInteractiveException if the class is not compileable, i.e. it is a non-interactive (source only) class.
     */
    public boolean isCompiled() 
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotInteractiveException
        {
        ClassTarget aTarget = classId.getClassTarget();

        return aTarget.isCompiled();
        }
    
    /**
     * Compile this class, and any dependents. 
     * @return true if the compilation was successful, false otherwise.
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this class belongs has been deleted by the user.
     * @throws ClassNotInteractiveException if an attempt is made to compile a non-interactive (source only) class.
     */
    public boolean compile() 
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotInteractiveException
        {
        ClassTarget aTarget = classId.getClassTarget();
    
        if (aTarget.isCompiled()) return true;
        
        aTarget.compile (null);

        // Wait for the compilation to finish !
        JobQueue.getJobQueue().waitForEmptyQueue();

        return aTarget.isCompiled();
    }


    /**
     * Utility. Finds the package name given a fully qualified name
     * If no package exist then an EMPTY string is retrned.
     */
    private String findPkgName ( String fullyQualifiedName )
      {
      if ( fullyQualifiedName == null ) return "";

      int dotIndex = fullyQualifiedName.lastIndexOf(".");
      // If there is no package name to be found return an empty one.
      if ( dotIndex <  0 ) return "";

      return fullyQualifiedName.substring(0,dotIndex);
      }

    /**
     * Returns the superclass of this class.
     * Similar to reflection API.
     * If this class represents either the Object class, an interface, 
     * a primitive type, or void, then null is returned.
     * If the superclass is not part of a package in the current BlueJ project then 
     * null is returned.
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this class belongs has been deleted by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BClass getSuperclass() 
      throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
      // Tested 22 may 2003, Damiano
      {
      Project bluejPrj = classId.getBluejProject();

      View bluejView = classId.getBluejView();
      View superView = bluejView.getSuper();

      // If this <code>Class</code> represents either the Object class, an interface, 
      // a primitive type, or void, then null is returned
      if ( superView == null ) return null;

      // The class exists, is it part of this project ?
      Class aTest = bluejPrj.loadClass(superView.getQualifiedName());
      // Really strange, a superclass  that is not part of this project classloader...
      if ( aTest == null ) return null;

      String classPkgName = findPkgName ( superView.getQualifiedName());
//      System.out.println ("Parent="+classPkgName);
      
      // Now I need to find out to what package it belongs to...
      boolean foundPackageMatch=false;
      List pkgList = bluejPrj.getPackageNames();
      for ( Iterator iter=pkgList.iterator(); iter.hasNext(); )
        {
        if ( ! classPkgName.equals(iter.next()) ) continue;
        // Fount it, remembar that we found it and get out.
        foundPackageMatch = true;
        break;
        }

      // There is no point to return a BClass whose package does not match..
      // Things would just fall here and there...
      if ( ! foundPackageMatch ) return null;

      // Let me get the package I want now...
      Package bluejPkg = bluejPrj.getPackage(classPkgName);
      
      return new BClass (new Identifier (bluejPrj, bluejPkg, superView.getQualifiedName() ));
      }
    
    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BConstructor[] getConstructors() 
        throws ProjectNotOpenException, ClassNotFoundException
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BConstructor getConstructor (Class[] signature) 
        throws ProjectNotOpenException, ClassNotFoundException
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BMethod[] getDeclaredMethods() 
        throws ProjectNotOpenException, ClassNotFoundException
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params ) 
        throws ProjectNotOpenException, ClassNotFoundException
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException
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
     * @throws ProjectNotOpenException if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException
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
      try
        {
        Class javaClass = classId.getJavaClass();
        return "BClass: "+javaClass.getName();
        }
      catch ( ExtensionException exc )
        {
        return "BClass: INVALID";  
        }
      }
    }   
