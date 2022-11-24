/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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
 * An unresolved subentity (an entity of form xxx.yyy, where yyy is the subentity name).
 * 
 * @author Davin McCall
 */
public class UnresolvedSubEntity extends JavaEntity
{
    private JavaEntity parent;
    private String name;
    private Reflective accessSource;
    private List<TypeArgumentEntity> typeArgs;
    
    /**
     * Construct a new unresolved sub entity.
     * @param parent   The parent entity
     * @param name     The subentity name
     * @param accessSource  The access source (used for access control)
     */
    public UnresolvedSubEntity(JavaEntity parent, String name, Reflective accessSource)
    {
        this.parent = parent;
        this.name = name;
        this.accessSource = accessSource;
    }
    
    @Override
    public JavaType getType()
    {
        return null;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return new UnresolvedSubEntity(this, name, accessSource);
    }

    @Override
    public String getName()
    {
        return parent.getName() + "." + name;
    }

    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        UnresolvedSubEntity newEnt = new UnresolvedSubEntity(parent, name, accessSource);
        newEnt.typeArgs = tparams;
        return newEnt;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public ValueEntity resolveAsValue()
    {
        if (typeArgs != null) {
            // A value can't have type arguments.
            return null;
        }
        
        JavaEntity pent = parent.resolveAsValue();
        if (pent == null) {
            pent = parent.resolveAsType();
        }
        
        if (pent == null) {
            return null;
        }
        
        JavaEntity subEnt = pent.getSubentity(name, accessSource);
        if (subEnt == null) {
            return null;
        }
        
        return subEnt.resolveAsValue();
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        PackageOrClass pent = parent.resolveAsPackageOrClass();
        if (pent == null) {
            return null;
        }
        
        JavaEntity subEnt = pent.getPackageOrClassMember(name);
        if (subEnt == null) {
            return null;
        }
        
        TypeEntity tentity = subEnt.resolveAsType();
        if (tentity != null && typeArgs != null) {
            tentity = tentity.setTypeArgs(typeArgs);
        }
        
        return tentity;
    }
    
    @Override
    public PackageOrClass resolveAsPackageOrClass()
    {
        if (typeArgs != null) {
            return resolveAsType();
        }
        
        PackageOrClass pent = parent.resolveAsPackageOrClass();
        if (pent == null) {
            return null;
        }
        
        JavaEntity subEnt = pent.getPackageOrClassMember(name);
        if (subEnt == null) {
            return null;
        }
        
        PackageOrClass pocEnt = subEnt.resolveAsPackageOrClass();
        if (pocEnt != null && typeArgs != null) {
            TypeEntity tent = pocEnt.resolveAsType();
            if (tent != null) {
                return tent.setTypeArgs(typeArgs);
            }
            return null;
        }
        return pocEnt;
    }
}
