/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2020  Michael Kolling and John Rosenberg
 
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
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A proxy-type reflective for arrays.
 * 
 * @author Davin McCall
 */
public class JdiArrayReflective extends JdiReflective
{
    private JavaType componentType;
    
    public JdiArrayReflective(JavaType t, ReferenceType srctype)
    {
        super(null, srctype);
        componentType = t;
    }
    
    /**
     * Create a new JdiArrayReflective representing an array with a certain component type.
     * @param t            The component type
     * @param classLoader  The classloader used to load the component type (or the array)
     * @param vm           The virtual machine holding the type
     */
    @OnThread(Tag.FXPlatform)
    public JdiArrayReflective(JavaType t, ClassLoaderReference classLoader, VirtualMachine vm)
    {
        super("[" + t.arrayComponentName(), classLoader, vm);
        componentType = t;
    }
    
    @Override
    public String getName()
    {
        return super.getName();
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected void checkLoaded()
    {
        name = "[" + componentType.arrayComponentName();
        super.checkLoaded();
    }
}
