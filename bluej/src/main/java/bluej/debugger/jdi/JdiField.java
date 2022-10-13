/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An implement of DebuggerField using the Java Debug Interface (JDI).
 * 
 * @author Davin McCall
 */
public class JdiField extends DebuggerField
{
    @OnThread(Tag.Any)
    private final Field field;
    @OnThread(Tag.Any)
    private final JdiObject object;
    private boolean hidden;
    
    @OnThread(Tag.Any)
    public JdiField(Field field, JdiObject object, boolean hidden)
    {
        this.field = field;
        this.object = object;
        this.hidden = hidden;
    }

    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return field.name();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getType()
    {
        if (object != null) {
            return JdiReflective.fromField(field, object);
        }
        else {
            return JdiReflective.fromField(field);
        }
    }

    @Override
    public int getModifiers()
    {
        return field.modifiers();
    }

    @Override
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    public String getValueString()
    {
        Value value;
        if (object != null) {
            value = object.obj.getValue(field);
        }
        else {
            value = field.declaringType().getValue(field);
        }

        return JdiUtils.getJdiUtils().getValueString(value);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public DebuggerObject getValueObject(JavaType expectedType)
    {
        Value value;
        if (object != null) {
            value = object.obj.getValue(field);
        }
        else {
            value = field.declaringType().getValue(field);
        }
        
        if (value == null) {
            return JdiObject.getDebuggerObject(null);
        }
        
        if (value instanceof ObjectReference) {
            ObjectReference or = (ObjectReference) value;
            if (expectedType == null) {
                expectedType = getType();
            }
            return JdiObject.getDebuggerObject(or, expectedType);
        }
        
        return null;
    }

    @Override
    public DebuggerClass getDeclaringClass()
    {
        return new JdiClass(field.declaringType());
    }
    
    @Override
    public boolean isHidden()
    {
        return hidden;
    }
}
