/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.Reflective;

/**
 * An entity resolver which resolves classes using a ClassLoader.
 * 
 * @author Davin McCall
 */
public class ClassLoaderResolver implements EntityResolver
{
    private ClassLoader classLoader;
    
    public ClassLoaderResolver(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }
    
    public TypeEntity resolveQualifiedClass(String name)
    {
        try {
            // Try as a fully-qualified name 
            Class<?> cl = classLoader.loadClass(name);
            return new TypeEntity(cl);
        }
        catch (Exception e) {}
        
        return null;
    }
    
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        // Have to assume it's a package
        return new PackageEntity(name, this);
    }
    
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        return resolvePackageOrClass(name, querySource);
    }
}
