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
package bluej.debugger.gentype;

import java.util.Iterator;
import java.util.List;

/**
 * A "reflective" is an object representing a java type. This interface
 * provides methods to, for instance, find the superclass/superinterfaces,
 * determine the generic type parameters, etc.
 *  
 * @author Davin McCall
 * @version $Id: Reflective.java 6215 2009-03-30 13:28:25Z polle $
 */
public abstract class Reflective {

    /**
     * Get the name of the class or interface represented by the reflective.
     * The name is such that it can be passed to ClassLoader's loadClass
     * method.
     * 
     * @return The fully qualified class/interface name.
     */
    public abstract String getName();
    
    /**
     * Get the formal type parameters of the class/interface this reflective
     * represents. Note that this does not give the type parameters from
     * outer classes which may still parameterize this reflective's class.
     * 
     * @return  The parameters as a List of GenTypeDeclTpar
     */
    public abstract List getTypeParams();
    
    /**
     * Get the (direct) supertypes of this reflective, as a list of reflectives.
     * Supertypes of an array include the "Object" class as well as arrays whose
     * component type is a supertype of this array's component type.
     * @return A List of Reflectives
     */
    public abstract List getSuperTypesR();
    
    /**
     * Get the supertypes of this reflective, as a list of GenTypes. The type
     * parameter names will refer to the type parameters in the parent type.
     * @return A List of GenTypeClass.
     */
    public abstract List getSuperTypes();
    
    /**
     * Get a reflective which represents an array, whose element type is
     * represented by this reflective.
     * 
     * @return A reflective representing an array
     */
    public abstract Reflective getArrayOf();
    
    /**
     * Return true if a variable of the reference type reflected by this
     * reflective can be assigned a value of the type represented by the given
     * reflective.
     * 
     * @param r  The other reflective
     * @return   True if the other reflective type is assignable to this type
     */
    public abstract boolean isAssignableFrom(Reflective r);
    
    /**
     * Return true if this reflective represents an interface type rather than
     * a class type.
     * @return   True if this reflective represents an interface
     */
    public abstract boolean isInterface();
    
    /**
     * Get a supertype (as a GenTypeClass) by name. The default implementation
     * uses getSuperTypes() and searches the resulting list.
     * 
     * @param rawName   the name of the supertype to find
     * @return          the supertype as a GenTypeClass
     */
    public GenTypeClass superTypeByName(String rawName)
    {
        List superTypes = getSuperTypes();
        Iterator i = superTypes.iterator();
        while( i.hasNext() ) {
            GenTypeClass next = (GenTypeClass)i.next();
            if( next.rawName().equals(rawName) )
                return next;
        }
        return null;
    }
    
    /**
     * Find another class as if it were to be loaded by this one. Ie. use this
     * class's classloader.
     * 
     * @param name  The name of the class to locate
     * @return
     */
    abstract public Reflective getRelativeClass(String name);
    
    /**
     * Determine whether this class is a static inner class.
     */
    abstract public boolean isStatic();
}
