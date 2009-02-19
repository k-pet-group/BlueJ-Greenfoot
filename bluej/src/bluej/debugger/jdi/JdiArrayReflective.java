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

import bluej.debugger.gentype.*;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * A proxy-type reflective for arrays.
 * 
 * @author Davin McCall
 * @version $Id: JdiArrayReflective.java 6163 2009-02-19 18:09:55Z polle $
 */
public class JdiArrayReflective extends JdiReflective {

    private JavaType componentType;
    
    public JdiArrayReflective(JavaType t, ReferenceType srctype)
    {
        super(null, srctype);
        componentType = t;
    }
    
    public JdiArrayReflective(JavaType t, ClassLoaderReference classLoader, VirtualMachine vm)
    {
        super(null, classLoader, vm);
    }
    
    public String getName()
    {
        checkLoaded();
        return super.getName();
    }
    
    protected void checkLoaded()
    {
        name = "[" + componentName();
        super.checkLoaded();
    }
    
    /**
     * Get the component name, as it appears in the class name given to a
     * classloader.
     */
    private String componentName()
    {
        if (componentType.typeIs(JavaType.JT_BOOLEAN))
            return "Z";
        if (componentType.typeIs(JavaType.JT_BYTE))
            return "B";
        if (componentType.typeIs(JavaType.JT_CHAR))
            return "C";
        if (componentType.typeIs(JavaType.JT_DOUBLE))
            return "D";
        if (componentType.typeIs(JavaType.JT_FLOAT))
            return "F";
        if (componentType.typeIs(JavaType.JT_INT))
            return "I";
        if (componentType.typeIs(JavaType.JT_LONG))
            return "J";
        if (componentType.typeIs(JavaType.JT_SHORT))
            return "S";

        if (componentType instanceof GenTypeArray) {
            Reflective r = ((GenTypeArray) componentType).getReflective();
            return r.getName();
        }

        // If we get to here, assume it's a class/interface type.
        GenTypeClass gtc = (GenTypeClass) componentType;
        return "L" + gtc.rawName() + ";";
    }
}
