package bluej.extensions;

import bluej.debugger.*;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import com.sun.jdi.ObjectReference;
import java.util.*;

/**
 * A wrapper for an object on the BlueJ object bench.
 * This wraps an object so you can add and remove it from the bench.
 *
 * @see BConstructor
 * @see BMethod
 * @see BField
 * @version $Id: BObject.java 1965 2003-05-20 17:30:25Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury 2003
 */
public class BObject
{
    private ObjectWrapper  wrapper;  

    /**
     * Constructor for BObject.
     */
    BObject (ObjectWrapper aWrapper)
    {
        wrapper = aWrapper;
    }

    /**
     * Tests if this object is still valid in BlueJ.
     * This object may not be valid since what it represents has been modified or deleted
     * from the main BlueJ graphical user interface.
     * Return true if it is valid, false otherwise.
     */
    public boolean isValid()
        {
        if ( wrapper == null ) return ( false);
        // TODO: Possible others checks here
        return true;
        }


    /**
     * Returns the package this object belongs to.
     * It returns null if this is not a valid object anymore.
     */
    public BPackage getPackage()
    {
        if ( ! isValid() ) return null;

        Package aPkg = wrapper.getPackage();
        Project bluejProject = aPkg.getProject();
        return new BPackage(new Identifier(bluejProject, aPkg ));
    }
        
    /**
     * Removes this object from the object bench. 
     * This will also remove it from the view of the object bench.
     * Once the object is removed from the bench it will not be available again.
     */
    public void removeFromBench()
        {
        if ( ! isValid() ) return;

        // This should really always exists, no need to check
        Package aPackage = wrapper.getPackage();

        // This may reasonably fail
        PkgMgrFrame aFrame = PkgMgrFrame.findFrame ( aPackage );
        if ( aFrame == null ) return;

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.remove(wrapper, aPackage.getId());

        wrapper = null;
        }
    

    /**
     * Adds this object on the object bench.
     * If you pass null as instanceName the object will have a predefined name.
     * If the object is not a valid one nothing will happen.
     * 
     * @param instanceName  The name you want this object to have on the bench.
     */
    public void addToBench(String instanceName)
    {
        if ( ! isValid() ) return;

        // No reational to add a null object, isn't it ?
        if (wrapper.getObject().isNullObject()) return;

        // If you want you may set the instance name here. Othervise accept default
        if ( instanceName != null ) wrapper.setName(instanceName);
        
        // This should really always exists, no need to check
        Package aPackage = wrapper.getPackage();

        // This may reasonably fail
        PkgMgrFrame aFrame = PkgMgrFrame.findFrame ( aPackage );
        if ( aFrame == null ) return;

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.add(wrapper);

        // load the object into runtime scope
        aPackage.getDebugger().addObjectToScope(aPackage.getId(),wrapper.getName(), wrapper.getObject());
    }


    /**
     * Return the name of this object on the object bench.
     * It can return null if the object is invalid.
     */
    public String getInstanceName()
        {
        if ( ! isValid() ) return null;

        return wrapper.getName();
        }
    
    /**
     * Return the class of this object.
     * Similar to Reflection API.
     * It can return null if the object is invalid.
     */
    public BClass getBClass()
    {
        // Tested also with string array. 20 may 2003, Damiano
        if ( ! isValid() ) return null;

        Package bluejPkg  = wrapper.getPackage();
        Project bluejProj = bluejPkg.getProject();

        // It really seems that the translation between Java naming and Class is needed.
        // Also tryng to get the Class instead of just the name is a mess...
        String  className = transJavaToClass(wrapper.getClassName());

        return new BClass ( new Identifier (bluejProj,bluejPkg,className));
    } 

    /**
     * Returns the underlying BlueJ package.
     * Should remain visible only to package members.
     */
    Package getBluejPackage ()
    {
        if ( wrapper == null ) return null;
        return wrapper.getPackage();
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
     * Returns a reasonable representation of this object.
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
