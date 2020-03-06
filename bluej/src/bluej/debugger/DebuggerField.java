/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugger;

import bluej.debugger.gentype.JavaType;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Representation of a class or object field, together with its value.
 * 
 * @author Davin McCall
 */
public abstract class DebuggerField
{
    /**
     * Get the field name
     */
    @OnThread(Tag.Any)
    public abstract String getName();
    
    /**
     * Get the field type
     */
    @OnThread(Tag.FXPlatform)
    public abstract JavaType getType();
    
    /**
     * Get the field modifiers. see {@link java.lang.reflect.Modifier}.
     */
    public abstract int getModifiers();
    
    /**
     * Get a string representation of the value of the field.
     * For null, the string "null" will be returned.
     * For a primitive, a string representation of the value will be returned.
     * For a string, the return will be a quoted Java literal string expression.
     * For any other reference type, the return will be DebuggerObject.OBJECT_REFERENCE.
     */
    @OnThread(Tag.Any)
    public abstract String getValueString();
    
    /**
     * If the field value is an object (or null), return it as a DebuggerObject.
     * 
     * @param expectedType   the known type of the field, which may be more precise than the declared type.
     *                       May be null.
     */
    @OnThread(Tag.FXPlatform)
    public abstract DebuggerObject getValueObject(JavaType expectedType);

    /**
     * If the field value is an object (or null), return it as a DebuggerObject.
     * 
     * JavaType is difficult to tag because its subclasses may use Parsed*Reflective
     * classes, which are only safe for use on the FX thread.  However, when null is
     * passed to the method it is thread-safe, so we make a delegate method here which
     * we tag as Any, then suppress the thread checker.
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    public DebuggerObject getValueObject()
    {
        return getValueObject(null);
    }

    /**
     * Get the class which declares this field.
     */
    public abstract DebuggerClass getDeclaringClass();
    
    /**
     * Get the qualified name of the class which declares this field.
     */
    public String getDeclaringClassName()
    {
        return getDeclaringClass().getName();
    }
    
    /**
     * Check whether this field is hidden - redefined in a subclass or ambiguously multiply inherited
     */
    public abstract boolean isHidden();
    
    /**
     * Check whether the field type is a reference type.
     */
    @OnThread(Tag.FXPlatform)
    public boolean isReferenceType()
    {
        return ! getType().isPrimitive();
    }
    
    /**
     * Check whether the field value is null.
     */
    public boolean isNull()
    {
        return getValueString().equals("null");
    }
}
