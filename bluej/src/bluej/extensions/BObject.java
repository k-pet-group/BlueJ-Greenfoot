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

/**
 * The BlueJ proxy Object object. This represents an object on the Object Bench. It can be 
 * got from {@link bluej.extensions.BPackage#getObject(java.lang.String) getObject}.
 * The Idea is that this behaves much as the normal Java Object but with added properties.
 *
 * @version $Id: BObject.java 1640 2003-03-04 20:26:52Z damiano $
 */
public class BObject
{
    private final BPackage pkg;
    private ObjectWrapper wrapper; // Not final because it can be nullified
    private final String instanceName;

    BObject (BPackage pkg, ObjectWrapper wrapper, String instanceName)
    {
        this.pkg = pkg;
        this.wrapper = wrapper;
        this.instanceName = instanceName;
    }
    
    /**
     * Gets the owning Package of this object
     * @return the originator
     */
    public BPackage getPackage()
    {
        return pkg;
    }
        
    /**
     * Removes this object from the Object Bench. Having done this, it will no longer be accessible in
     * any way, shape or form.
     */
    public void remove()
    {
        if (wrapper != null) {
            PkgMgrFrame.findFrame (pkg.bluej_pkg).getObjectBench().remove (wrapper, pkg.bluej_pkg.getId());
            wrapper = null;
        }
    }
    
    /**
     * Gets the name of the instance of this object
     * @return the name of the object
     */
    public String getName()
    {
        return instanceName;
    }
    
    /**
     * Similar to Reflection API this gets the object BClass and from that you get
     * what you need from it.
     * 
     * @return the proxy BClass of this object
     */
    public BClass getBClass()
    {
        return new BClass (pkg, wrapper.getClassName());
    } 
    
    /**
     * Gets the methods available for this object
     * @param includeSuper if <code>true</code> the methods from all superclasses will
     * also be included
     * @return the methods of this object, or an empty array if none exist
     
    public BMethod[] getMethods (boolean includeSuper)
    {
        String className = wrapper.getClassName();
        if (className.endsWith ("[]")) className = "[L"+className.substring (0, className.length()-2)+";";
        Class cl = pkg.getRealPackage().loadClass (className);
        MethodView[] methodViews = includeSuper ? View.getView (cl).getAllMethods()
                                                : View.getView (cl).getDeclaredMethods();
        BMethod[] methods = new BMethod [methodViews.length];
        for (int i=0; i<methods.length; i++) {
            methods[i] = new BMethod (pkg, methodViews[i], instanceName);
        }
        return methods;
    }
    
    /**
     * Gets a method available in this object complying with the given critera
     * @param name the name of the method
     * @param signature the signature, given as an array of classes of the parameters
     * @return a method of this object, or <code>null</code> if none matched
     
    public BMethod getMethod (String name, Class[] signature)
    {
        Class cl = pkg.getRealPackage().loadClass (wrapper.getClassName());
        MethodView[] methodViews = View.getView (cl).getDeclaredMethods();
        for (int i=0; i<methodViews.length; i++) {
            BMethod method = new BMethod (pkg, methodViews[i], instanceName);
            if (BMethod.matches (method, name, signature)) return method;
        }
        return null;
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
        return new BField (pkg, obj.getObjectReference(), field);
    }
    
    /**
     * Checks if this object is an array
     * @return <code>true</code> if this object is an array
     
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
     
    public int getModifiers()
    {
        return wrapper.getObject().getObjectReference().referenceType().modifiers();
    }

    /**
     * Determines the position of this object, if it is real!
     * @return the location on the screen of the centre of this object,
     * or <code>null</code> if it's a virtual object
    public Point getLocationOnScreen()
    {
        return wrapper == null ? null : wrapper.getLocationOnScreen();
    }
     */
    
    /**
     * Gets a description of this object
     * @return the classname and instance name of this object
     
    public String toString()
    {
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getType().getName() + ": " + getName();
    }
    */
}   