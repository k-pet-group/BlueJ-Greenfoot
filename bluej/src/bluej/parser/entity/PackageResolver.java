/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2016,2019  Michael Kolling and John Rosenberg
 
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
 * A resolver for a package scope level. Classes within the package are found
 * first; otherwise resolution is delegated to the parent resolver.
 * 
 * @author Davin McCall
 */
public class PackageResolver implements EntityResolver
{
    private EntityResolver parentResolver;
    private final String pkg;
    
    /**
     * Constructor a PackageResolver for the specified package and with the specified parent resolver.
     */
    public PackageResolver(EntityResolver parentResolver, String pkg)
    {
        this.parentResolver = parentResolver;
        this.pkg = pkg;
    }
    
    /* (non-Javadoc)
     * @see bluej.parser.entity.EntityResolver#getValueEntity(java.lang.String, java.lang.String)
     */
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        return resolvePackageOrClass(name, querySource);
    }

    /* (non-Javadoc)
     * @see bluej.parser.entity.EntityResolver#resolvePackageOrClass(java.lang.String, java.lang.String)
     */
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        String fqName = (pkg.length() == 0) ? name : pkg + "." + name;
        TypeEntity tent = parentResolver.resolveQualifiedClass(fqName);
        if (tent != null) {
            // TODO check the returned class isn't private
            return tent;
        }
        else {
            return new PackageEntity(name, this);
        }
    }

    /* (non-Javadoc)
     * @see bluej.parser.entity.EntityResolver#resolveQualifiedClass(java.lang.String)
     */
    public TypeEntity resolveQualifiedClass(String name)
    {
        return parentResolver.resolveQualifiedClass(name);
    }
}
