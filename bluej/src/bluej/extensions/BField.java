package bluej.extensions;

import bluej.debugger.ObjectWrapper;
import bluej.debugger.jdi.JdiObject;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.*;

import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * This is similar to the Reflection Field<P>
 * The main reason to have a field coming from a Class and not from an Object is that
 * logically we should be able to get static methods without having objects around.
 * Reflection states that to get a static field we can use a FIled and pass null as the object to work on.<P>
 * Damiano
 */
public class BField
{
    private Package bluej_pkg;
    private FieldView bluej_view;

    /*
    private final ObjectReference ref;
    private final Field field;
    private final BObject obj;
    private final boolean array;
    private final int element;
*/

    BField (Package i_bluej_pkg, FieldView i_bluej_view )
    {
        bluej_pkg = i_bluej_pkg;
        bluej_view = i_bluej_view;
/*
        element = 0;
        array = false;
        obj = null;
        field = null;
        ref = null;
*/        
    }        



  /*
    BField (Package i_bluej_pkg, ObjectReference ref, Field field)
    {
        bluej_pkg = i_bluej_pkg;
        this.ref = ref;
        this.field = field;
        this.array = false;
        this.element = 0;
        this.obj = null;
    }
    
    /**
     * Constructor for an array

    BField (Package i_bluej_pkg, ArrayReference ref, BObject obj, int element)
    {
        bluej_pkg = i_bluej_pkg;
        this.ref = ref;
        this.field = null;
        this.obj = obj;
        this.array = true;
        this.element = element;
    }


    /**
     * Used to see if this field matches with the given criteria
     */
    public boolean matches ( String fieldName )
        {
        // Who is so crazy to give me a null name ?
        if ( fieldName == null ) return false;

        return fieldName.equals(getName());
        }


    /**
     * The name of the Field, as from reflection.
     */
    public String getName()
        {
        return bluej_view.getName();
        }

    /**
     * The type of the field, as from reflection
     */
    public Class getType()
        {
        return bluej_view.getDeclaringView().getClass();
        }

    /**
     * Gets this Filed Value on the given BObject
     */
    public Object get ( BObject onThis )
        {
        if ( onThis == null ) return null;
        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName (bluej_view.getName());
        if ( thisField == null ) return null;
       
        Value val = objRef.getValue (thisField);
        if ( val == null ) return null;
        
        if (val instanceof StringReference) return ((StringReference) val).value();
        if (val instanceof BooleanValue) return new Boolean (((BooleanValue) val).value());
        if (val instanceof ByteValue)    return new Byte (((ByteValue) val).value());
        if (val instanceof CharValue)    return new Character (((CharValue) val).value());
        if (val instanceof DoubleValue)  return new Double (((DoubleValue) val).value());
        if (val instanceof FloatValue)   return new Float (((FloatValue) val).value());
        if (val instanceof IntegerValue) return new Integer (((IntegerValue) val).value());
        if (val instanceof LongValue)    return new Long (((LongValue) val).value());
        if (val instanceof ShortValue)   return new Short (((ShortValue) val).value());

        if (val instanceof ObjectReference)
          {
          PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
          return new BObject ( new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val), getName()));
          }

        return val.toString();
        }
        
    
    /**
     * Gets the value of this field object. 
     * @return an appropriate object. This could be one of:<bl>
     * <li><b>BObject</b> - if the item is an object
     * <li><b>Number</b> - if the item is a Byte, Double, Float, Integer, Long or Short
     * <li><b>Boolean</b>
     * <li><b>Character</b>
     * <li><b>String</b>
     * <li><b>BField[]</b> - if this is an array
     * <code><b>null</b></code> - if the field is null, or if the field does not exist
     * </bl>
    public Object getValue()
    {
        Value val = array ? ((ArrayReference)ref).getValue (element)
                          : ref.getValue (field);
        if (val == null)
        {
            return null;
        }
        else if (val instanceof StringReference)
        {
            return ((StringReference) val).value();
        }
        else if (val instanceof ObjectReference)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
            return new BObject ( new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val), getName()));
        }
        else if (val instanceof BooleanValue)
        {
            return new Boolean (((BooleanValue) val).value());
        }
        else if (val instanceof ByteValue)
        {
            return new Byte (((ByteValue) val).value());
        }
        else if (val instanceof CharValue)
        {
            return new Character (((CharValue) val).value());
        }
        else if (val instanceof DoubleValue)
        {
            return new Double (((DoubleValue) val).value());
        }
        else if (val instanceof FloatValue)
        {
            return new Float (((FloatValue) val).value());
        }
        else if (val instanceof IntegerValue)
        {
            return new Integer (((IntegerValue) val).value());
        }
        else if (val instanceof LongValue)
        {
            return new Long (((LongValue) val).value());
        }
        else if (val instanceof ShortValue)
        {
            return new Short (((ShortValue) val).value());
        }
        else
        {
            return val.toString();
        }
    }
    */
    
}