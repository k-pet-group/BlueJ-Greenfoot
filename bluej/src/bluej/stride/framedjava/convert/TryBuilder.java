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
import java.util.Stack;

import bluej.stride.framedjava.elements.CodeElement;

/**
 * A class for keeping track of the try/catch finally being built up.
 */
class TryBuilder
{
    // Code within the initial try {...} part
    final List<CodeElement> tryContent = new ArrayList<>();
    // The types in the catch (type name) {code} parts (one entry per catch)
    final Stack<List<String>> catchTypes = new Stack<>();
    // The names in the catch (type name) {code} parts (one entry per catch)
    final List<String> catchNames = new ArrayList<>();
    // The code in the catch (type name) {code} parts (one entry per catch)
    final List<List<CodeElement>> catchBlocks = new ArrayList<>();
    // The contents of the finally block (null if no finally block)
    List<CodeElement> finallyContents = null;
}
