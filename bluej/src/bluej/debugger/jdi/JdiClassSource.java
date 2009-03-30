/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * This class is intended to represent a virtual machine/class loader combo,
 * which can be used to locate a class (and its generic signature information)
 * using the class name. 
 * @author Davin McCall
 * @version $Id: JdiClassSource.java 6215 2009-03-30 13:28:25Z polle $
 */
public class JdiClassSource {

    private ClassLoaderReference cl;
    private VirtualMachine vm;
    
    /**
     * 
     */
    public JdiClassSource(VirtualMachine vm, ClassLoaderReference cl)
    {
        this.cl = cl;
        this.vm = vm;
    }
    
    public ReferenceType classByName(String name)
    {
        List l = vm.classesByName(name);
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            ReferenceType rt = (ReferenceType)i.next();
            if( rt.classLoader() == cl )
                return rt;
        }
        return null;
    }
}
