/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.LocatableToken;

/**
 * A simple class to keep track of the details of a field/local var declaration set being parsed
 */
class FieldOrVarBuilder
{
    // The type of the variable
    final String type;
    // The modifiers
    final List<Modifier> modifiers = new ArrayList<>();

    FieldOrVarBuilder(String type, List<Modifier> modifiers)
    {
        this.type = type;
        this.modifiers.addAll(modifiers);
    }
}
