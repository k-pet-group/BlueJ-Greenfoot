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
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.entity;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeUnbounded;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A TypeArgumentEntity for representing unbounded wildcards ("?").
 * 
 * @author Davin McCall
 */
public class UnboundedWildcardEntity extends TypeArgumentEntity
{
    private GenTypeClass objClass;

    @OnThread(Tag.FXPlatform)
    public UnboundedWildcardEntity(EntityResolver resolver)
    {
        TypeEntity objEntity = resolver.resolveQualifiedClass("java.lang.Object");
        if (objEntity != null) {
            objClass = objEntity.getClassType();
        }
    }
    
    @Override
    public GenTypeParameter getType()
    {
        return new GenTypeUnbounded(objClass);
    }
}
