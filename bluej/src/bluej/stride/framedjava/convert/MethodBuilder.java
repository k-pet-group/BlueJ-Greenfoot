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
import bluej.stride.framedjava.ast.ParamFragment;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class to record the details of a method/constructor currently being parsed.
 */
class MethodBuilder
{
    // Type of the method (null if constructor)
    final String type;
    // Name of the method (null if constructor)
    final String name;
    // Modifiers of the method/constructor
    final List<Modifier> modifiers = new ArrayList<>();
    // Parameters of the method/constructor
    final List<ParamFragment> parameters = new ArrayList<>();
    // Types in throws declaration
    final List<String> throwsTypes = new ArrayList<>();
    // The Javadoc comment (may be null)
    final String comment;
    // Either super, this or null for constructors (the delegate call just inside
    // the method).  Always null for methods.
    String constructorCall;
    // The arguments to the delegate call.  Always null if constructorCall is null.
    List<Expression> constructorArgs;
    // Whether the method has a body or not.
    boolean hasBody = false;

    MethodBuilder(String type, String name, List<Modifier> modifiers, String comment)
    {
        this.type = type;
        this.name = name;
        this.modifiers.addAll(modifiers);
        this.comment = comment;
    }
}
