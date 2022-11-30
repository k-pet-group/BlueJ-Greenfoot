/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg 
 
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

import java.util.LinkedList;

import bluej.parser.EditorParser;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * Parameter block for certain parsing methods.
 * 
 * <p>The sole use of objects of this class are to pass around a large number of
 * parameters. For this reason fields are publicly accessible.
 * 
 * @author Davin McCall
 */
public class ParseParams
{
    // Parameters passed to the subclass partial parse method
    
    public NodeStructureListener listener;
    public EditorParser parser;
    public JavaTokenFilter tokenStream;
    public ReparseableDocument document;
    public int nodePos;
    public LinkedList<NodeAndPosition<ParsedNode>> childQueue;
    
    // Parameters returned from the partial parse method
    
    /** Where exactly parsing got to, when partial parse returns PP_ABORT */
    public int abortPos;
}
