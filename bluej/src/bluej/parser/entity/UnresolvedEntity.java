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
 * Represents a java entity whose nature (value or type) is not yet known,
 * and provides a static method (getEntity) to obtain instances.
 * 
 * @author Davin McCall
 */
public class UnresolvedEntity extends JavaEntity
{
    private PackageOrClass pocEntity;
    private JavaEntity valueEntity;
    
    /**
     * Get an entity whose type (value or class) is not yet known. The returned entity
     * can later be resolved to either a value or type.
     */
    public static JavaEntity getEntity(EntityResolver resolver, String name)
    {
        PackageOrClass poc = resolver.resolvePackageOrClass(name, null);
        JavaEntity value = resolver.resolveValueEntity(name, null);
        
        if (poc != null && value != null) {
            return new UnresolvedEntity(poc, value);
        }
        
        return poc != null ? poc : value;
    }
    
    protected UnresolvedEntity(PackageOrClass poc, JavaEntity value)
    {
        pocEntity = poc;
        valueEntity = value;
    }

    @Override
    public String getName()
    {
        if (pocEntity != null) {
            return pocEntity.getName();
        }
        else {
            return valueEntity.getName();
        }
    }

    @Override
    public JavaEntity getSubentity(String name)
    {
        PackageOrClass newPocEntity = null;
        JavaEntity newValueEntity = null;
        
        if (pocEntity != null) {
            newPocEntity = pocEntity.getPackageOrClassMember(name);
        }
        if (newValueEntity != null) {
            newValueEntity = valueEntity.getSubentity(getName());
        }
        
        if (newPocEntity != null && newValueEntity != null) {
            return new UnresolvedEntity(newPocEntity, newValueEntity);
        }
        else {
            return newPocEntity != null ? newPocEntity : newValueEntity; 
        }
    }

    @Override
    public JavaType getType()
    {
        return null;
    }

}
