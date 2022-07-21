/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.nodes;

import bluej.parser.entity.JavaEntity;

/**
 * An interface for a variable that has been declared in the source code,
 * either as a "full" variable declaration (local variable, field, parameter),
 * or using the new pattern-matching instanceof feature.
 */
public interface VariableDeclaration
{
    /**
     * The name of the variable
     */
    String getName();
    /**
     * The type of the variable, as a JavaEntity
     */
    JavaEntity getFieldType();

    /**
     * The offset of this node compared to its parent node in the parse tree.
     */
    int getOffsetFromParent();

    /**
     * The OR-ed together modifiers for the variable (final, volatile, etc)
     */
    int getModifiers();
}
