/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.utility.Debug;

import com.sun.jdi.*;

/**
 *  Represents an class running on the user (remote) machine.
 *
 *@author     Michael Kolling
 *@created    December 26, 2000
 *@version    $Id: JdiClass.java 6163 2009-02-19 18:09:55Z polle $
 */
public class JdiClass extends DebuggerClass
{
    ReferenceType remoteClass;  // the remote class represented
    List<Field> staticFields;

    // -- instance methods --

    /**
     *  Create a remote class object.
     *
     *@param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    public JdiClass(ReferenceType remoteClass)
    {
        this.remoteClass = remoteClass;
        getRemoteFields();
    }


    /**
     *  Return the name of this class (fully qualified).
     *
     *@return    The class name
     */
    public String getName()
    {
        return remoteClass.name();
    }


    /**
     *  Return the number of static fields (including inherited fields).
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return staticFields.size();
    }


    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        return ((Field)staticFields.get(slot)).name();
    }

    /**
     * Return the type of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the static field
     */
    @Override
    public String getStaticFieldType(int slot)
    {
        Field field = (Field) staticFields.get(slot);
        return JdiReflective.fromField(field, remoteClass).toString(false);
    }

    /**
     *  Return the object in static field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = (Field)staticFields.get(slot);
        ObjectReference val = (ObjectReference) remoteClass.getValue(field);
        GenTypeClass expectedType = (GenTypeClass)JdiReflective.fromField(field, remoteClass);
        return JdiObject.getDebuggerObject(val, expectedType);
    }

    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List<String> getStaticFields(boolean includeModifiers)
    {
        return getFields(includeModifiers);
    }


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsPublic(int slot)
    {
        return ((Field)staticFields.get(slot)).isPublic();
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
        Field field = (Field) staticFields.get(slot);
        Value val = remoteClass.getValue(field);
        return (val instanceof ObjectReference);
    }

    /**
     * Returns true if this represents a Java interface
     *  
     */
    public boolean isInterface()
    {
        return remoteClass instanceof InterfaceType;
    }

    /**
     * Returns true if this represents an enum
     *  
     */
    public boolean isEnum()
    {
        if (remoteClass instanceof ClassType) {
            return JdiUtils.getJdiUtils().isEnum((ClassType) remoteClass);
        }
        return false;
    }

    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     */
    private List<String> getFields(boolean includeModifiers)
    {
        List<String> fieldStrings = new ArrayList<String>(staticFields.size());
        List<Field> visible = remoteClass.visibleFields();

        for (int i = 0; i < staticFields.size(); i++) {
            Field field = (Field) staticFields.get(i);

            Value val = remoteClass.getValue(field);

            String valString = JdiUtils.getJdiUtils().getValueString(val);
            String fieldString = "";

            if (includeModifiers) {
                if (field.isPrivate()) {
                    fieldString = "private ";
                }
                if (field.isProtected()) {
                    fieldString = "protected ";
                }
                if (field.isPublic()) {
                    fieldString = "public ";
                }
            }

            fieldString += JdiReflective.fromField(field, remoteClass).toString(true) 
                + " " + field.name()
                + " = " + valString;

            if (!visible.contains(field)) {
                fieldString += " (hidden)";
            }
            fieldStrings.add(fieldString);
        }
        return fieldStrings;
    }

    /**
     *  Get the list of fields for this object.
     */
    private void getRemoteFields()
    {
        staticFields = new ArrayList<Field>();

        if (remoteClass != null) {
            List<Field> allFields = remoteClass.allFields();
            for (int i = 0; i < allFields.size(); i++) {
                Field field = (Field) allFields.get(i);
                if (field.isStatic())
                    staticFields.add(field);
            }
        }
        else {
            Debug.reportError("cannot get fields for remote class");
        }
    }

}
