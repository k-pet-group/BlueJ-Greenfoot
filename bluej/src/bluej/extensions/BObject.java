package bluej.extensions;

import bluej.debugger.*;
import bluej.debugmgr.objectbench.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import com.sun.jdi.*;
import java.util.*;

/**
 * A wrapper for an object on the BlueJ object bench.
 * This wraps an object so you can add and remove it from the bench.
 *
 * @see BConstructor
 * @see BMethod
 * @see BField
 * @version $Id: BObject.java 2714 2004-07-01 15:55:03Z mik $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury 2003
 */
public class BObject
{
    private ObjectWrapper  wrapper;     // I wish I could get rid of this...
    private Identifier     wrapperId;
    
    /**
     * Constructor for BObject.
     */
    BObject (ObjectWrapper aWrapper)
    {
        wrapper = aWrapper;

        Package bluejPkg  = wrapper.getPackage();
        Project bluejProj = bluejPkg.getProject();

        // It really seems that the translation between Java naming and Class is needed.
        // Also tryng to get the Class instead of just the name is a mess...
        String  className = transJavaToClass(wrapper.getClassName());

        wrapperId = new Identifier (bluejProj, bluejPkg, className );
    }

    /**
     * Returns the package this object belongs to.
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this object belongs has been deleted by the user.
     */
    public BPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Project bluejPrj = wrapperId.getBluejProject();
        Package bluejPkg = wrapperId.getBluejPackage();
        return new BPackage(new Identifier(bluejPrj, bluejPkg ));
        }
        
    /**
     * Removes this object from the object bench. 
     * This will also remove it from the view of the object bench.
     * Once the object is removed from the bench it will not be available again.
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this object belongs has been deleted by the user.
     */
    public void removeFromBench()
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package aPackage = wrapperId.getBluejPackage();
        PkgMgrFrame aFrame = wrapperId.getPackageFrame();

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.removeObject(wrapper, aPackage.getId());

        wrapper = null;
        }
    

    /**
     * Adds this object on the object bench.
     * If you pass null as instanceName the object will have a predefined name.
     * If the object is not a valid one nothing will happen.
     * 
     * @param instanceName  The name you want this object to have on the bench.
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this object belongs has been deleted by the user.
     */
    public void addToBench(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException
        {
        if ( wrapper == null ) return;
        
        // No reational to add a null object, isn't it ?
        if (wrapper.getObject().isNullObject()) return;

        // If you want you may set the instance name here. Othervise accept default
        if ( instanceName != null ) wrapper.setName(instanceName);
        
        // This should really always exists, no need to check
        Package aPackage = wrapperId.getBluejPackage();
        PkgMgrFrame aFrame = wrapperId.getPackageFrame();

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.addObject(wrapper);

        // load the object into runtime scope
        aPackage.getDebugger().addObject(wrapper.getName(), wrapper.getObject());
        }


    /**
     * Return the name of this object on the object bench.
     * @return The instance name if the object can be put into bench, null othervise
     */
    public String getInstanceName()
        {
        if ( wrapper == null ) return null;

        return wrapper.getName();
        }
    
    /**
     * Return the class of this object.
     * Similar to Reflection API. Note the naming inconsistency, which
     * avoids a clash with <code>java.lang.Object.getClass()</code>
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws ClassNotFoundException if the class has been deleted by the user.
     */
    public BClass getBClass()
        throws ProjectNotOpenException, ClassNotFoundException
        {
        // Tis is to test if the Bobject is till valid
        wrapperId.getJavaClass();
        
        // Tested also with string array. 20 may 2003, Damiano
        return new BClass ( wrapperId );
        } 

    /**
     * Returns the underlying BlueJ package.
     * Should remain visible only to package members.
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this object belongs has been deleted by the user.
     */
    PkgMgrFrame getPackageFrame ()
        throws ProjectNotOpenException, PackageNotFoundException
        {
        return wrapperId.getPackageFrame();
        }

    /**
     * Used by BField to get hold of the real Object
     */
    ObjectReference getObjectReference()
        {
        if ( wrapper == null ) return null;
        DebuggerObject obj = wrapper.getObject();

        if ( obj == null ) return null;
        return obj.getObjectReference();
        }


    /**
     * Returns a string representation of the Object
     */
    public String toString ()
      {
      return "BObject instanceName="+getInstanceName()+" Class Name="+wrapper.getClassName();
      }


// ============================ UTILITY ========================================

   private static HashMap primiMap;

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
   * From: java.lang.String[]
   * To:   [Ljava.lang.String;
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
        
    String replace = (String)primiMap.get(className);

    // If I can substitute the name I will do it
    if (replace != null)  className = replace;
    else                  className = "L"+className+";";
            
    while (arrayCount-- > 0) className = "["+className;
          
    return className;
    }
  }   
