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
package bluej.parser.nodes;

import javax.swing.text.Document;

import bluej.parser.CodeSuggestions;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;



/**
 * A node representing a parsed type (class, interface, enum)
 * 
 * @author Davin McCall
 */
public class ParsedTypeNode extends ParentParsedNode
{
    private String name;
    private TypeInnerNode inner;
    
    public ParsedTypeNode(ParsedNode parent, String name)
    {
        super(parent);
        this.name = name;
    }
    
    @Override
    public int getNodeType()
    {
        return NODETYPE_TYPEDEF;
    }
    
    public boolean isContainer()
    {
        return true;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    /**
     * Insert the inner node for the type definition.
     * The inner node will hold the field definitions etc.
     */
    public void insertInner(TypeInnerNode child, int position, int size)
    {
        super.insertNode(child, position, size);
        inner = child;
    }
    
    /**
     * Get the fields in this node. (A map of field name to its node).
     * The returned map is live and should not be modified.
     */
    public TypeInnerNode getInner()
    {
        return inner;
    }
    
    @Override
    public CodeSuggestions getExpressionType(int pos, int nodePos, TypeEntity defaultType, Document document)
    {
        // The default type if the expression is not know should be this type
        TypeEntity myType = new TypeEntity(new ParsedReflective(this));
        NodeAndPosition child = getNodeTree().findNode(pos, nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), myType, document);
        }
        
        // We don't return the specified default type (which must be an outer type). There
        // can be no completions because no completions can occur except in the context
        // of child nodes.
        return null;
    }
}
