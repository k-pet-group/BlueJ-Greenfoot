package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.View;
import bluej.views.MethodView;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.ListIterator;
import java.lang.reflect.Modifier;
import java.awt.Point;
import bluej.debugger.*;

/**
 * The BlueJ proxy Object object. 
 *
 * @version $Id: BObject.java 1660 2003-03-06 09:44:15Z damiano $
 */
public class BObject
{
    private ObjectWrapper  wrapper;  

    /**
     * Do NOT use: You should get BObjects from the BPackage or by the BConstructors
     */
    BObject (ObjectWrapper i_wrapper)
    {
        wrapper = i_wrapper;
    }

    /**
     * Tests if this BObject is still valid. It may happens for various reasons
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
     * Gets the owning Package of this object
     * @return the originator
     */
    public BPackage getPackage()
    {
        if ( ! isValid() ) return null;
        
        return new BPackage(wrapper.getPackage());
    }
        
    /**
     * Removes this object from the Object Bench. Having done this, it will no longer be accessible in
     * any way, shape or form.
     */
    public void removeFromBench()
        {
        if ( ! isValid() ) return;

        // This should really always exists, no need to check
        bluej.pkgmgr.Package aPackage = wrapper.getPackage();

        // This may reasonably fail
        PkgMgrFrame aFrame = PkgMgrFrame.findFrame ( aPackage );
        if ( aFrame == null ) return;

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.remove(wrapper, aPackage.getId());

        wrapper = null;
        }
    
    /**
     * puts this object on the Object Bench
     */
    public void putIntoBench()
        {
        if ( ! isValid() ) return;

        // This should really always exists, no need to check
        bluej.pkgmgr.Package aPackage = wrapper.getPackage();

        // This may reasonably fail
        PkgMgrFrame aFrame = PkgMgrFrame.findFrame ( aPackage );
        if ( aFrame == null ) return;

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.add(wrapper);
        }



    /**
     * Gets the name of the instance of this object
     * @return the instance name of the object, can return null if object is invalid
     */
    public String getInstanceName()
        {
        if ( ! isValid() ) return null;

        return wrapper.getName();
        }
    
    /**
     * Similar to Reflection API this gets the object BClass and from that you get
     * what you need from it.
     * 
     * @return the proxy BClass of this object
     */
    public BClass getBClass()
    {
        if ( ! isValid() ) return null;
        
        return new BClass (wrapper.getPackage(), wrapper.getClassName());
    } 
    
    
    
    /**
     * Gets the fields of this object
     * @param includeSuper if <code>true</code> the fields from all superclasses will
     * also be included
     * @return the fields belonging to this object, or an empty array if none exist
    public BField[] getFields (boolean includeSuper)
    {
        DebuggerObject obj = wrapper.getObject();
        ObjectReference ref = obj.getObjectReference();
        ReferenceType type = ref.referenceType();

        return null;
    }
        /*
        BField[] bFields;
        
        if (ref instanceof ArrayReference) {
            ArrayReference array = (ArrayReference)ref;
            ReferenceType type = ref.referenceType();
            bFields = new BField [array.length()];
            for (int i=0; i<bFields.length; i++) {
//                bFields[i] = new BField (pkg, array, array.type(), i, instanceName);
                bFields[i] = new BField (pkg, array, this, i);
            }
        } else {
            ReferenceType type = ref.referenceType();
            List fields = includeSuper ? type.allFields() 
                                       : type.fields();
            bFields = new BField [fields.size()];
            for (ListIterator li=fields.listIterator(); li.hasNext();) {
                int i=li.nextIndex();
                Field field = (Field)li.next();
                bFields[i] = new BField (pkg, obj.getObjectReference(), field);
            }
        }
        return bFields;
    }
    */


    /**
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
     * Gets the field in this object of a given name
     * @param name the name of the field to get
     * @return the field belonging to this object, or an empty array if none exist
    public BField getField (String name)
    {
        DebuggerObject obj = wrapper.getObject();
        ObjectReference ref = obj.getObjectReference();
        ReferenceType type = ref.referenceType();
        Field field = type.fieldByName (name);
        if (field == null) return null;
        return new BField (wrapper.getPackage(), obj.getObjectReference(), field);
    }
    
    /**
     * Checks if this object is an array
     * @return <code>true</code> if this object is an array
     */     
    public boolean isArray()
    {
        return wrapper.getObject().isArray();
    }
    
    /**
     * Determines the modifiers for this object.
     * Use <code>java.lang.reflect.Modifiers</code> to
     * decode the meaning of this integer
     * @return The modifiers of this method, encoded in
     * a standard Java language integer. If this object
     * represents an array, this value will probably
     * be meaningless.
     */
    public int getModifiers()
    {
        return wrapper.getObject().getObjectReference().referenceType().modifiers();
    }

    /**
     * Gets a description of this object
     * @return the classname and instance name of this object
     */     
    public String toString()
    {
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+": " + getInstanceName();
    }
}   