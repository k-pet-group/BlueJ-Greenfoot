package greenfoot.localdebugger;

import java.util.ArrayList;
import java.util.List;

import bluej.debugger.DebuggerObject;

/**
 * A DebuggerObject to represent arrays. This base class is for object arrays;
 * primitive array types are represented in seperate classes which derive from
 * this one.<p>
 * 
 * Subclasses should override:
 * <ul>
 * <li> getValueString(int)
 * <li> instanceFieldIsObject(int)
 * </ul>
 * 
 * @author Davin McCall
 */
public class LocalArray extends LocalObject
{
    private int length;
    
    protected LocalArray(Object [] object)
    {
        super(object);
        length = object.length;
    }
    
    /**
     * Subclasses use this constructor to specifiy the array lenght.
     * 
     * @param object  The array object
     * @param length  The array length
     */
    protected LocalArray(Object object, int length)
    {
        super(object);
        this.length = length;
    }
    
    public int getInstanceFieldCount()
    {
        return length;
    }

    public String getInstanceFieldName(int slot)
    {
        return "[" + String.valueOf(slot) + "]";
    }

    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Object val = ((Object []) object)[slot];
        return getLocalObject(val);
    }

    public List getInstanceFields(boolean includeModifiers)
    {
        List fields = new ArrayList(length);

        for (int i = 0; i < length; i++) {
            String valString = getValueString(i);
            fields.add("[" + i + "]" + " = " + valString);
        }
        return fields;
    }
    
    public String getValueString(int index)
    {
        Object value = ((Object []) object)[index];
        
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        else if (value instanceof Enum) {
            Enum enumv = (Enum) value;
            return enumv.name();
        }
        else {
            return DebuggerObject.OBJECT_REFERENCE;
        }
    }
    
    public boolean instanceFieldIsPublic(int slot)
    {
        return true;
    }

    public boolean instanceFieldIsObject(int slot)
    {
        return ((Object []) object)[slot] != null; 
    }
}
