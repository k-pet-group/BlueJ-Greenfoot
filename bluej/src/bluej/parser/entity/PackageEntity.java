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
package bluej.parser.entity;

import bluej.debugger.gentype.JavaType;

/**
 * An entity representing a package. The entity is only presumed to be a package
 * seeing as no class with the same name could be found.
 * 
 * @author Davin McCall
 */
public class PackageEntity extends PackageOrClass
{
    private String packageName;
    private ClassLoader classLoader;
    
    public PackageEntity(String packageName, ClassLoader classLoader)
    {
        this.packageName = packageName;
        this.classLoader = classLoader;
    }
    
    public JavaType getType()
    {
        return null;
    }
    
//    void setTypeParams(List tparams) throws SemanticException
//    {
//        // a package cannot be parameterized!
//        throw new SemanticException();
//    }
    
    public JavaEntity getSubentity(String name)
    {
        Class<?> c;
        try {
            c = classLoader.loadClass(packageName + '.' + name);
            return new TypeEntity(c);
        }
        catch (ClassNotFoundException cnfe) {
            return new PackageEntity(packageName + '.' + name, classLoader);
        }
    }
    
    public PackageOrClass getPackageOrClassMember(String name)
    {
        return (PackageOrClass) getSubentity(name);
    }
    
    public String getName()
    {
        return packageName;
    }
    
    public boolean isClass()
    {
        return false;
    }
}
