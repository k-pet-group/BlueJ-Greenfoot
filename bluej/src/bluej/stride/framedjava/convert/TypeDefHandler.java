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

import java.util.List;

import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.CodeElement;

/**
 * An interface for building up a type definition (e.g. class or interface declaration)
 */
interface TypeDefHandler
{
    // Called when the type definition has begun
    public void typeDefBegun(LocatableToken start);

    // Called when the type definition has ended
    public void typeDefEnd(LocatableToken end);

    // Called with the name of the type definition
    public void gotName(String name);

    // Called when we know we have started a class declaration
    public void startedClass(List<Modifier> modifiers, String doc);

    // Called when we know we have started an interface declaration
    public void startedInterface(List<Modifier> modifiers, String doc);

    // Called with the content of the type definition body
    public void gotContent(List<CodeElement> content);

    // Called when we get a type after the extends.
    // If there are several in a type list, this is called once for each type.
    public void typeDefExtends(String type);

    // Called when we get a type after the implements.
    // If there are several in a type list, this is called once for each type.
    public void typeDefImplements(String type);
}
