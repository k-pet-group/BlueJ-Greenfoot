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

import bluej.debugger.gentype.GenTypeParameter;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A base type for representing type arguments as they occur in a source program.
 * 
 * <p>This type does not extend JavaEntity, as it is more specialised.
 * 
 * @author Davin McCall
 */
public abstract class TypeArgumentEntity
{
    /**
     * Get the type parameter. This requires resolving the bound if not already done,
     * and so may return null if the bound is not a valid type.
     */
    @OnThread(Tag.FXPlatform)
    public abstract GenTypeParameter getType();
}
