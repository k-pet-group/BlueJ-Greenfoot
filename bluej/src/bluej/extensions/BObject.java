package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.ObjectReference;

import bluej.debugger.*;

/**
 * A Wrapper for an Object in the BlueJ object bench.
 * This wraps an object so you can put and remove it from the bench.
 * You get Bobjects from BConstructor or BMethods and from BField.
 *
 * @version $Id: BObject.java 1726 2003-03-24 13:33:06Z damiano $
 */
public class BObject
{
    private ObjectWrapper  wrapper;  

    /**
     * NOT to be used by Extension writer.
     * Get BObjects from the BPackage, BConstructor, BMethod, BField
     */
    public BObject (ObjectWrapper i_wrapper)
    {
        wrapper = i_wrapper;
    }

    /**
     * Tests if this BObject is still valid in Bluej.
     * Return true if it is false othervise.
     */
    public boolean isValid()
        {
        if ( wrapper == null ) return ( false);
        // TODO: Possible others checks here
        return true;
        }


    /**
     * Return the BPackage whose this BObject belongs.
     */
    public BPackage getPackage()
    {
        if ( ! isValid() ) return null;

        return new BPackage(wrapper.getPackage());
    }
        
    /**
     * Removes this object from the Object Bench. 
     * This will also remove it from the visible part of the bench.
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
     * Test is this object a null one, if so you will not be able to put it into the bench.
     * The reasoning is that you may have a representation of a null object and this BObject may
     * be just that.
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
     * Return the name of the object on the bench.
     * It can return null if the object is in an invalid state.
     */
    public String getInstanceName()
        {
        if ( ! isValid() ) return null;

        return wrapper.getName();
        }
    
    /**
     * Return the BClass of this BObject.
     * Similar to Reflection API.
     * It can return null if the object is invalid.
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
   * Return a reasonable representation of this BObject.
   */
  public String toString ()
    {
    return "BObject instanceName="+getInstanceName()+" Class Name="+wrapper.getClassName();
    }


}   