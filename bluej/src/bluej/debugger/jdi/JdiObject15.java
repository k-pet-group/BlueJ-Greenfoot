package bluej.debugger.jdi;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaNames;

import com.sun.jdi.*;

/**
 * A DebuggerObject with support for java 1.5 - generics etc.
 * @author Davin McCall
 * @version $Id: JdiObject15.java 2581 2004-06-10 01:09:01Z davmac $
 */
public class JdiObject15 extends JdiObject {

    private Map genericParams = null; // Map of parameter names to types
    
    /**
     *  Factory method that returns instances of JdiObjects.
     *
     *  @param  obj  the remote object this encapsulates.
     *  @return      a new JdiObject or a new JdiArray object if
     *               remote object is an array
     */
    public static JdiObject getDebuggerObject(ObjectReference obj)
    {
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj);
        } else {
            return new JdiObject15(obj);
        }
    }
    
    /**
     * Get a JdiObject from a field. 
     * @param obj    Represents the value of the field.
     * @param field  The field.
     * @param parent The parent object containing the field.
     * @return
     */
    public static JdiObject getDebuggerObject(ObjectReference obj, Field field, JdiObject15 parent)
    {
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj);
        } else {
            if( jvmSupportsGenerics )
                return new JdiObject15(obj, field, (JdiObject15)parent);
            else
                return new JdiObject15(obj);
        }
    }
    
    /**
     * Constructor. 
     */
    protected JdiObject15() {
        super();
    }

    /**
     *  Constructor is private so that instances need to use getJdiObject
     *  factory method.
     *
     *  @param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    private JdiObject15(ObjectReference obj)
    {
        this.obj = obj;
        getRemoteFields();
    }

    /**
     * Private constructor. Construct from a given object reference using the
     * generic signature of a field and the parent object.
     * @param obj     The object to represent
     * @param field   The field to extract the generic signature from
     * @param parent  The parent object to get type information from
     */
    private JdiObject15(ObjectReference obj, Field field, JdiObject15 parent)
    {
        this.obj = obj;
        getRemoteFields();
        if( field.genericSignature() != null ) {
            GenTypeClass genericType = (GenTypeClass)JdiReflective.fromField(field, parent);
            genericParams = genericType.mapToDerived(new JdiReflective(obj.referenceType()));
        }
    }
    
    /**
     * Get a mapping of the type parameter names for this objects class to the
     * actual type, for all parameters where some information is known. May
     * return null.
     * 
     * @return a Map (String:JdiGenType) of type parameter names to types
     */
    public Map getGenericParams()
    {
        Map r = null;
        if( genericParams != null ) {
            r = new HashMap();
            r.putAll(genericParams);
        }
        return r;
    }
    
    public GenTypeClass getGenType()
    {
        Stack arrays = new Stack();
        Type rt = obj.referenceType();
        Reflective r;
        
        // Determine the number of array dimensions, if any
        try {
            while( rt instanceof ArrayType ) {
                arrays.push(rt);
                rt = ((ArrayType)rt).componentType();
            }
            r = new JdiReflective(obj.referenceType());
        }
        catch(ClassNotLoadedException cnle) {
            r = new JdiReflective(((ArrayType)rt).componentTypeName(), obj.referenceType());
        }
        
        // get the component type
        GenTypeClass retval = new GenTypeClass(r, genericParams);
        
        // construct GenArrayType (array of...) as necessary
        while( ! arrays.empty() ) {
            ArrayType at = (ArrayType)arrays.pop();
            retval = new GenTypeArray(retval, new JdiReflective(at));
        }
        return retval;
    }
    
    public String getGenClassName()
    {
        if (obj == null)
            return "";
        if( genericParams != null )
            return new GenTypeClass(new JdiReflective(obj.referenceType()),
                    genericParams).toString();
            // return JdiGenType.fromClassSignature((ClassType)obj.referenceType(), genericParams).toString();
        else
            return getClassName();
    }
    
    public String getStrippedGenClassName()
    {
        if( obj == null )
            return "";
        if( genericParams != null )
            // return JdiGenType.fromClassSignature((ClassType)obj.referenceType(), genericParams).toString(true);
            return new GenTypeClass(new JdiReflective(obj.referenceType()),
                    genericParams).toString(true);
        else
            return JavaNames.stripPrefix(getClassName());
    }
    
    /**
     * Get the ClassType (com.sun.jdi.ClassType) object representing the class
     * of this remote object.
     * @return  the remote object's class type
     */
    public ClassType getClassType()
    {
        return (ClassType)obj.referenceType();
    }
}
