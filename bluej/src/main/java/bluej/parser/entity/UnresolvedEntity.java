/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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
 * Represents a java entity whose nature (value or type) is not yet known,
 * and provides a static method (getEntity) to obtain instances.
 * 
 * @author Davin McCall
 */
public class UnresolvedEntity extends JavaEntity
{
    private EntityResolver resolver;
    private String name;
    private List<TypeArgumentEntity> typeArguments;
    private Reflective querySource;
    
    /**
     * Get an entity whose type (value or class) is not yet known. The returned entity
     * can later be resolved to either a value or type.
     */
    public static JavaEntity getEntity(EntityResolver resolver, String name, Reflective querySource)
    {
        return new UnresolvedEntity(resolver, name, querySource, null);
    }
    
    protected UnresolvedEntity(EntityResolver resolver, String name, Reflective querySource,
            List<TypeArgumentEntity> typeArgs)
    {
        this.resolver = resolver;
        this.name = name;
        this.querySource = querySource;
        this.typeArguments = typeArgs;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return new UnresolvedSubEntity(this, name, accessSource);
    }

    @Override
    public JavaType getType()
    {
        return null;
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return new UnresolvedEntity(resolver, name, querySource, tparams);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public ValueEntity resolveAsValue()
    {
        if (typeArguments != null) {
            return null;
        }
        
        JavaEntity entity = resolver.getValueEntity(name, querySource);
        if (entity != null) {
            return entity.resolveAsValue();
        }
        return null;
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        PackageOrClass entity = resolver.resolvePackageOrClass(name, querySource);
        if (entity != null) {
            TypeEntity tentity = entity.resolveAsType();
            if (tentity != null) {
                if (typeArguments != null) {
                    return tentity.setTypeArgs(typeArguments);
                }
                return tentity;
            }
        }
        return null;
    }
    
    @Override
    public PackageOrClass resolveAsPackageOrClass()
    {
        PackageOrClass ent = resolver.resolvePackageOrClass(name, querySource);
        if (typeArguments != null) {
            TypeEntity tent = ent.resolveAsType();
            if (tent != null) {
                return tent.setTypeArgs(typeArguments);
            }
            return null;
        }
        return ent;
    }
}
