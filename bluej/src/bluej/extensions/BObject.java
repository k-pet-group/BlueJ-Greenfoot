package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.ObjectReference;

import bluej.debugger.*;

/**
 * This allows you to interacet with objects that are created in the BlueJ environment.
 * This wraps an object so you can put and remove it from the bench.
 * You get Bobjects from BConstructor or BMethods and from BField.
 *
 * @version $Id: BObject.java 1723 2003-03-21 11:19:28Z damiano $
 */
public class BObject
{
    private ObjectWrapper  wrapper;  

    /**
     * Not for use by the extension writer.
     * You should get BObjects from the BPackage, BConstructor, BMethod, BField
     */
    public BObject (ObjectWrapper i_wrapper)
    {
        wrapper = i_wrapper;
    }

    /**
     * Tests if this BObject is still valid in Bluej.
     * 
     * @return true or false
     */
    public boolean isValid()
        {
        if ( wrapper == null ) return ( false);
        // TODO: Possible others checks here
        return true;
        }


    /**
     * Gets the BPackage whose this BObject belongs.
     * 
     * @return the BPackage belonging to this Object
     */
    public BPackage getPackage()
    {
        if ( ! isValid() ) return null;

        return new BPackage(wrapper.getPackage());
    }
        
    /**
     * Removes this object from the Object Bench. 
     * Having done this, it will no longer be accessible in any way, shape or form.
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
     * Is this object a null one, if so you will not be able to put it into the bench.
     * 
     * @return true or false
     */
    public boolean isNullObject()
        {
        // Kind of cheating, but really we can think of it as a null object.
        if ( ! isValid() ) return true;

        return wrapper.getObject().isNullObject();
        }


    /**
     * Puts this object on the Object Bench.
     * If it is a null object you will not be able to put it.
     * If you pass null as instanceName the object will have a predefined name.
     * 
     * @param instanceName  The name you want this object to have on the bench.
     */
    public void putIntoBench(String instanceName)
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
        Debugger.debugger.addObjectToScope(aPackage.getId(),wrapper.getName(), wrapper.getObject());
        }


    /**
     * Used when you need to know the name of the object on the bench.
     * 
     * @return the instance name of the object, can return null if object is invalid
     */
    public String getInstanceName()
        {
        if ( ! isValid() ) return null;

        return wrapper.getName();
        }
    
    /**
     * Similar to Reflection API this gets the object BClass.
     * Trom that you get what you need from it. 
     * BClass can tell you if it is an array, the modifiers and so on.
     * 
     * @return the proxy BClass of this object
     */
    public BClass getBClass()
    {
        if ( ! isValid() ) return null;

        return new BClass (wrapper.getPackage(), wrapper.getClassName());
    } 

    /**
     * FOR bluej.extensions ONLY.
     * This should be visible only from within the bluej.extensions
     * Used by BArray
     */
    Package getBluejPackage ()
    {
        if ( wrapper == null ) return null;
        return wrapper.getPackage();
    }

    /**
     * FOR bluej.extensions ONLY.
     * This should be visible only from within the bluej.extensions
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
   * Returns a reasonable representation of this BObject
   */
  public String toString ()
    {
    return "BObject instanceName="+getInstanceName()+" Class Name="+wrapper.getClassName();
    }


}   