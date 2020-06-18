/*
 This file is part of the BlueJ program. 
 Copyright (C) 2000-2009,2010,2011,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeArrayClass;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents an array object running on the user (remote) machine.
 *
 * @author     Michael Kolling
 * @created    December 26, 2000
 */
public class JdiArray extends JdiObject
{
    private JavaType componentType;

    @OnThread(Tag.Any)
    protected JdiArray(ArrayReference obj)
    {
        this.obj = obj;
        obj.disableCollection();
        calcComponentType();
    }

    /**
     * Constructor for when the array type is known.
     * @param obj           The reference to the the remote object
     * @param expectedType  The known type of the object
     */
    @OnThread(Tag.FXPlatform)
    protected JdiArray(ArrayReference obj, JavaType expectedType)
    {
        this.obj = obj;
        obj.disableCollection();
        // All arrays extend java.lang.Object - so it's possible that the
        // expected type is java.lang.Object and not an array type at all.
        if(expectedType instanceof GenTypeArray) {
            String ctypestr = obj.referenceType().signature();
            JavaType genericType = expectedType;
            int level = 0;
            
            // Go downwards until we find the base component type
            while(genericType instanceof GenTypeArray) {
                GenTypeArray genericArray = (GenTypeArray)genericType;
                genericType = genericArray.getArrayComponent();
                ctypestr = ctypestr.substring(1);
                level++;
            }
            
            // If the arrays are of different depths, no inference is possible
            // (this is possible because all arrays extend Object)
            if(ctypestr.charAt(0) == '[') {
                calcComponentType();
                return;
            }

            // The array may be of a primitive type.
            if(genericType.isPrimitive()) {
                calcComponentType();
                return;
            }

            // It's not really possible for an array to have a component type
            // that is a wildcard, but this type is inferred in some cases so
            // it must be handled here.
            
            JavaType component;
            
            if (genericType instanceof GenTypeClass) {
                // the sig looks like "Lpackage/package/class;". Strip the 'L'
                // and the ';'
                String compName = ctypestr.substring(1, ctypestr.length() - 1);
                compName = compName.replace('/', '.');

                Reflective compReflective = new JdiReflective(compName, obj.referenceType());
                component = ((GenTypeClass) genericType).mapToDerived(compReflective);

                while (level > 1) {
                    component = component.getArray();
                    level--;
                }
                componentType = component;
            }
        }
        
        if (componentType == null) {
            calcComponentType();
        }
    }

    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    private void calcComponentType()
    {
        ArrayType ar = (ArrayType) obj.referenceType();
        String componentSig = ar.componentSignature();
        JdiReflective.StringIterator i = new JdiReflective.StringIterator(componentSig);
        componentType = JdiReflective.typeFromSignature(i, null, ar).asType();
    }

    /**
     * Get the name of the class of this object.
     * 
     * @return String representing the Class name.
     */
    @Override
    @OnThread(Tag.Any)
    public String getClassName()
    {
        return obj.referenceType().name();
    }

    /**
     * Get the GenType object representing the type of this array.
     * 
     * @return   GenType representing the type of the array.
     */
    @Override
    public GenTypeClass getGenType()
    {
        Reflective r = new JdiArrayReflective(componentType, obj.referenceType());
        return new GenTypeArrayClass(r, componentType);
    }
    
    /**
     * Return true if this object is an array.
     *
     * @return    The Array value
     */
    @Override
    @OnThread(Tag.Any)
    public boolean isArray()
    {
        return true;
    }

    @Override
    public int getElementCount()
    {
        return ((ArrayReference) obj).length();
    }
    
    @Override
    public JavaType getElementType()
    {
        return componentType;
    }
    
    @Override
    public String getElementValueString(int index)
    {
        Value val = ((ArrayReference) obj).getValue(index);
        return JdiUtils.getJdiUtils().getValueString(val);
    }

    /*
     * Return the object in object field 'slot'.
     *
     * @param  slot  The slot number to be returned
     * @return       The InstanceFieldObject value
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public DebuggerObject getElementObject(int index)
    {
        Value val = ((ArrayReference) obj).getValue(index);
        return JdiObject.getDebuggerObject((ObjectReference) val, componentType);
    }
}
