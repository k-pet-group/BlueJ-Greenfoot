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

import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An entity representing a package. The entity is only presumed to be a package
 * seeing as no class with the same name could be found.
 * 
 * @author Davin McCall
 */
public class PackageEntity extends PackageOrClass
{
    private String name;
    private EntityResolver resolver;
    
    public PackageEntity(String name, EntityResolver resolver)
    {
        this.name = name;
        this.resolver = resolver;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public PackageOrClass getPackageOrClassMember(String name)
    {
        String nname = this.name + "." + name;
        PackageOrClass rval = resolver.resolveQualifiedClass(nname);
        if (rval != null) {
            return rval;
        }
        return new PackageEntity(nname, resolver);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return getPackageOrClassMember(name);
    }

    @Override
    public JavaType getType()
    {
        return null;
    }

    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
}
