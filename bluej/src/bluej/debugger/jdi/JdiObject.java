/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugger.jdi;

import java.util.ArrayList;
import java.util.List;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

/**
 * Represents an object running on the user (remote) machine, together with an optional generic
 * type of the object.
 *
 * @author  Michael Kolling
 */
public class JdiObject extends DebuggerObject
{
    /**
     *  Factory method that returns instances of JdiObjects.
     *
     *  @param  obj  the remote object this encapsulates (may be null)
     *  @return      a new JdiObject or a new JdiArray object if
     *               remote object is an array
     */
    public static JdiObject getDebuggerObject(ObjectReference obj)
    {
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj);
        }
        else {
            return new JdiObject(obj);
        }
    }
    
    public static JdiObject getDebuggerObject(ObjectReference obj, JavaType expectedType)
    {
        if( obj instanceof ArrayReference ) {
            return new JdiArray((ArrayReference) obj, expectedType);
        }
        else {
            if( expectedType instanceof GenTypeClass ) {
                return new JdiObject(obj, (GenTypeClass)expectedType);
            }
            else {
                return new JdiObject(obj);
            }
        }
    }

    /**
     * Get a JdiObject from a field. 
     * @param obj    Represents the value of the field.
     * @param field  The field.
     * @param parent The parent object containing the field.
     * @return
     */
    public static JdiObject getDebuggerObject(ObjectReference obj, Field field, JdiObject parent)
    {
        JavaType expectedType = JdiReflective.fromField(field, parent);
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj, expectedType);
        }
        
        if (expectedType.asClass() != null) {
            return new JdiObject(obj, expectedType.asClass());
        }
        
        return new JdiObject(obj);
    }
    
    
    // -- instance methods --

    ObjectReference obj;  // the remote object represented
    GenTypeClass genType = null; // the generic type, if known
    List<Field> fields;
    
    // used by JdiArray.
    protected JdiObject()
    {
    }

    /**
     *  Constructor is private so that instances need to use getJdiObject
     *  factory method.
     *
     *  @param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    private JdiObject(ObjectReference obj)
    {
        this.obj = obj;
        if (obj != null) {
            obj.disableCollection();
            getRemoteFields();
        }
    }

    private JdiObject(ObjectReference obj, GenTypeClass expectedType)
    {
        this.obj = obj;
        if (obj != null) {
            obj.disableCollection();
            getRemoteFields();
            Reflective reflective = new JdiReflective(obj.referenceType());
            if( expectedType.isGeneric() ) {
                genType = expectedType.mapToDerived(reflective);
            }
        }
    }
    
    @Override
    protected void finalize()
    {
        if (obj != null) {
            obj.enableCollection();
        }
    }
    
    @Override
    public String toString()
    {
        return JdiUtils.getJdiUtils().getValueString(obj);
    }
    
    /*
     * Get the (raw) name of the class of this object.
     */
    @Override
    public String getClassName()
    {
        if (obj == null) {
            return "";
        }
        else {
            return obj.referenceType().name();
        }
    }

    /*
     * Get the class of this object.
     */
    @Override
    public DebuggerClass getClassRef()
    {
        if (obj == null) {
            return null;
        }
        else {
            return new JdiClass(obj.referenceType());
        }
    }
    
    @Override
    public GenTypeClass getGenType()
    {
        if(genType != null) {
            return genType;
        }
        else if (obj != null) {
            Reflective r = new JdiReflective(obj.referenceType());
            return new GenTypeClass(r);
        }
        else {
            return null;
        }
    }
    
    /**
     *  Return true if this object is an array. This is always false, since
     *  arrays are wropped in the subclass "JdiArray".
     *
     *@return    The Array value
     */
    @Override
    public boolean isArray()
    {
        return false;
    }

    @Override
    public boolean isNullObject()
    {
        return obj == null;
    }

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    @Override
    public int getElementCount()
    {
        return -1;
    }
    
    @Override
    public JavaType getElementType()
    {
        return null;
    }

    @Override
    public DebuggerObject getElementObject(int index)
    {
        return null;
    }
    
    @Override
    public String getElementValueString(int index)
    {
        return null;
    }
    
    @Override
    public ObjectReference getObjectReference()
    {
        return obj;
    }
    
    @Override
    public List<DebuggerField> getFields()
    {
        List<Field> visibleFields = obj.referenceType().visibleFields();
        List<DebuggerField> rlist = new ArrayList<DebuggerField>(fields.size());
        for (Field field : fields) {
            if (! checkIgnoreField(field)) {
                boolean visible = visibleFields.remove(field);
                rlist.add(new JdiField(field, this, !visible));
            }
        }
        return rlist;
    }

    private boolean checkIgnoreField(Field f)
    {
        return (f.name().indexOf('$') >= 0);
    }

    /**
     *  Get the list of fields for this object.
     */
    protected void getRemoteFields()
    {
        if (obj != null) {
            ReferenceType cls = obj.referenceType();
            if (cls != null) {
                fields = cls.allFields();
                return;
            }
        }
        // either null object or unavailable fields
        // lets give them an empty list of fields
        fields = new ArrayList<Field>();
    }

    /**
     * Base our object equality on the object that we are referring
     * to in the remote VM.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this == o) {
            return true;
        }
        if((o == null) || (o.getClass() != this.getClass())) {
            return false;
        }

        // object must be JdiObject at this point
        JdiObject test = (JdiObject)o;
        return this.obj.equals(test.obj);
    }

    /**
     * Base our hashcode on the hashcode of the object that we are
     * referring to in the remote VM.
     */
    @Override
    public int hashCode()
    {
        return obj.hashCode();
    }
}
