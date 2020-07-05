/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2015,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A "reflective" is an object representing a java type. This interface
 * provides methods to, for instance, find the superclass/superinterfaces,
 * determine the generic type parameters, etc.
 *  
 * @author Davin McCall
 */
public abstract class Reflective
{
    /**
     * Get the name of the class or interface represented by the reflective.
     * The name is such that it can be passed to ClassLoader's loadClass
     * method.
     * 
     * @return The fully qualified class/interface name.
     */
    public abstract String getName();
    
    /**
     * Get the name of the class or interface represented by the reflective.
     * The name is in a form that can be presented nicely to the user.
     */
    public String getSimpleName()
    {
        return getName();
    }
    
    /**
     * Get the formal type parameters of the class/interface this reflective
     * represents. Note that this does not give the type parameters from
     * outer classes which may still parameterize this reflective's class.
     * 
     * @return  The parameters as a List of GenTypeDeclTpar
     */
    @OnThread(Tag.FXPlatform)
    public abstract List<GenTypeDeclTpar> getTypeParams();
    
    /**
     * Get the (direct) supertypes of this reflective, as a list of reflectives.
     * Supertypes of an array include the "Object" class as well as arrays whose
     * component type is a supertype of this array's component type.
     * @return A List of Reflectives
     */
    @OnThread(Tag.FXPlatform)
    public abstract List<Reflective> getSuperTypesR();
    
    /**
     * Get the supertypes of this reflective, as a list of GenTypes. The type
     * parameter names will refer to the type parameters in the parent type.
     * @return A List of GenTypeClass.
     */
    @OnThread(Tag.FXPlatform)
    public abstract List<GenTypeClass> getSuperTypes();
    
    /**
     * Get a reflective which represents an array, whose element type is
     * represented by this reflective.
     * 
     * @return A reflective representing an array
     */
    @OnThread(Tag.FXPlatform)
    public abstract Reflective getArrayOf();
    
    /**
     * Return true if a variable of the reference type reflected by this
     * reflective can be assigned a value of the type represented by the given
     * reflective.
     * 
     * @param r  The other reflective
     * @return   True if the other reflective type is assignable to this type
     */
    @OnThread(Tag.FXPlatform)
    public abstract boolean isAssignableFrom(Reflective r);
    
    /**
     * Return true if this reflective represents an interface type rather than
     * a class type.
     * @return   True if this reflective represents an interface
     */
    @OnThread(Tag.FXPlatform)
    public abstract boolean isInterface();
    
    /**
     * Get a supertype (as a GenTypeClass) by name. The default implementation
     * uses getSuperTypes() and searches the resulting list.
     * 
     * @param rawName   the name of the supertype to find
     * @return          the supertype as a GenTypeClass
     */
    @OnThread(Tag.FXPlatform)
    public GenTypeClass superTypeByName(String rawName)
    {
        List<GenTypeClass> superTypes = getSuperTypes();
        Iterator<GenTypeClass> i = superTypes.iterator();
        while( i.hasNext() ) {
            GenTypeClass next = i.next();
            if( next.classloaderName().equals(rawName) )
                return next;
        }
        return null;
    }
    
    /**
     * Find another class as if it were to be loaded by this one. Ie. use this
     * class's classloader.
     * 
     * @param name  The name of the class to locate
     */
    @OnThread(Tag.FXPlatform)
    abstract public Reflective getRelativeClass(String name);
    
    /**
     * Get the outer class of this one, if there is one.
     */
    @OnThread(Tag.FXPlatform)
    public Reflective getOuterClass()
    {
        int dollarIndex = getName().lastIndexOf('$');
        if (dollarIndex != -1) {
            // Note that package names can have '$' in them, so
            // we need to check for that case:
            int dotIndex = getName().indexOf('.', dollarIndex);
            if (dotIndex == -1) {
                String outerName = getName().substring(0, dollarIndex);
                return getRelativeClass(outerName);
            }
        }
        return null;
    }

    /**
     * Get a reference to a named inner class of this class. Returns null If
     * the named inner class doesn't exist.
     */
    @OnThread(Tag.FXPlatform)
    abstract public Reflective getInnerClass(String name);

    /**
     * Determine whether this class is a static inner class.
     */
    @OnThread(Tag.FXPlatform)
    abstract public boolean isStatic();
    
    /**
     * Determine whether this class is declared public.
     */
    @OnThread(Tag.FXPlatform)
    abstract public boolean isPublic();
    
    /**
     * Determine whether this class is declared final.
     */
    @OnThread(Tag.FXPlatform)
    abstract public boolean isFinal();
    
    /**
     * Get the methods declared in the type represented by this Reflective.
     * This does not include methods declared in the superclass(es), nor does
     * it include synthetic methods.
     * 
     * @return a map which maps method names to a set of methods
     *    (represented by MethodReflective objects) 
     */
    @OnThread(Tag.FXPlatform)
    abstract public Map<String,Set<MethodReflective>> getDeclaredMethods();

    /**
     * Gets the constructors declared in the type represented by this Reflective.
     */
    @OnThread(Tag.FXPlatform)
    abstract public List<ConstructorReflective> getDeclaredConstructors();

    /**
     * Get the fields declared in the type represented by this Reflective.
     * This does not include fields declared in the superclass(es).
     */
    @OnThread(Tag.FXPlatform)
    abstract public Map<String,FieldReflective> getDeclaredFields();

    /**
     * Get the module name of this type.  Returns null if not known or non-applicable.
     */
    abstract public String getModuleName();
}
