package bluej.debugger.jdi;

import bluej.debugger.DebuggerObject;

import bluej.utility.Debug;

import java.util.List;
import java.util.ArrayList;

import com.sun.jdi.*;

/**
 * Represents an array object running on the user (remote) machine.
 *
 * @author     Michael Kolling
 * @created    December 26, 2000
 * @version    $Id: JdiArray.java 1561 2002-12-06 05:05:06Z ajp $
 */
public class JdiArray extends JdiObject
{
    protected JdiArray(ArrayReference obj)
    {
        this.obj = obj;
    }

    private JdiArray()
    {
    }

    public ObjectReference getObjectReference()
    {
        return obj;
    }

    /**
     *  Get the name of the class of this object.
     *
     *@return    String representing the Class name.
     */
    public String getClassName()
    {
        return obj.referenceType().name();
    }

    /**
     *  Return true if this object is an array.
     *
     *@return    The Array value
     */
    public boolean isArray()
    {
        return true;
    }

    public boolean isNullObject()
    {
        return obj == null;
    }

    /**
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return 0;
    }

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    public int getInstanceFieldCount()
    {
        return ((ArrayReference) obj).length();
    }


    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldName");
    }

    /**
     * Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public String getInstanceFieldName(int slot)
    {
        return "[" + String.valueOf(slot) + "]";
    }

    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot or null if slot does not exist
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     *  Return the object in object field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Value val = ((ArrayReference) obj).getValue(slot);
        return JdiObject.getDebuggerObject((ObjectReference) val);
    }


    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List getStaticFields(boolean includeModifiers)
    {
        throw new UnsupportedOperationException("getStaticFields");
        //        return new ArrayList(0);
    }

    /**
     *  Return an array of strings with the description of each field in the
     *  format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The InstanceFields value
     */
    public List getInstanceFields(boolean includeModifiers)
    {
        List values;

        if (((ArrayReference) obj).length() > 0) {
            values = ((ArrayReference) obj).getValues();
        } else {
            values = new ArrayList();
        }
        List fields = new ArrayList(values.size());

        String typeName = null;

        for (int i = 0; i < values.size(); i++) {
            Value val = (Value) values.get(i);
            String valString;

            if (val == null) {
                valString = "<null>";
            } else if ((val instanceof ObjectReference) &&
                        !(val instanceof StringReference)) {
                valString = "<object reference>";
            } else {
                valString = val.toString();
            }

            fields.add("[" + i + "]" + " = " + valString);
        }
        return fields;
    }

    /**
     * Is an object of this class assignable to the given fully qualified type?
     *
     * @param   type    Description of Parameter
     * @return          The AssignableTo value
     */
    public boolean isAssignableTo(String type)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj.referenceType() == null)
        {
            return false;
        }
        if (obj.referenceType().name() != null
                 && type.equals(obj.referenceType().name()))
        {
            return true;
        }
        Type ct = obj.referenceType();
        String t = type;
        if (t.equals("java.lang.Object"))
        {
            return true;
        }
        while ((ct instanceof ArrayType) && t.endsWith("[]"))
        {
            try
            {
                ct = ((ArrayType) ct).componentType();
            }
            catch (com.sun.jdi.ClassNotLoadedException cnle)
            {
                return false;
            }
            t = t.substring(0, t.length() - 2);
        }
        if (t.equals(ct.name()))
        {
            return true;
        }
        if (ct instanceof ClassType)
        {
            ClassType clst = ((ClassType) ct);
            InterfaceType[] intt = ((InterfaceType[]) clst.allInterfaces().toArray(new InterfaceType[0]));
            for (int i = 0; i < intt.length; i++)
            {
                if (t.equals(intt[i].name()))
                {
                    return true;
                }
            }
            clst = clst.superclass();
            while (clst != null)
            {
                if (t.equals(clst.name()))
                {
                    return true;
                }
                clst = clst.superclass();
            }
        }
        return false;
    }


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  Description of Parameter
     *@return       Description of the Returned Value
     *@arg          slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        return true;
    }


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsObject(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param   slot    The slot number to be checked
     * @return          true if the object in slot is an object
     */
    public boolean instanceFieldIsObject(int slot)
    {
        Value val = ((ArrayReference) obj).getValue(slot);

        if (val == null) {
            return false;
        }
        else {
            return (val instanceof ObjectReference);
        }
    }
}
