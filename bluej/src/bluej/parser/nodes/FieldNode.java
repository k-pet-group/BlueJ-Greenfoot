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

import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.UnresolvedArray;

/**
 * A node representing a parsed field or variable declaration.
 * 
 * @author Davin McCall
 */
public class FieldNode extends ParentParsedNode
{
    private String name;
    private JavaEntity fieldType;
    private FieldNode firstNode;
    /** Number of extra array declarators */
    private int arrayDecls;
    
    /**
     * Construct a field node representing the first declared field in a field
     * declaration. The fieldType may be null if it appears invalid.
     */
    public FieldNode(ParsedNode parent, String name, JavaEntity fieldType, int arrayDecls)
    {
        super(parent);
        this.name = name;
        this.fieldType = fieldType;
        this.arrayDecls = arrayDecls;
    }
    
    /**
     * Construct a field node representing the second or a subsequent field
     * declared in a field declaration.
     * @param parent     The parent parsed node (should be a TypeInnerNode)
     * @param firstNode  The node representing the first declared field
     */
    public FieldNode(ParsedNode parent, String name, FieldNode firstNode, int arrayDecls)
    {
        super(parent);
        this.name = name;
        this.firstNode = firstNode;
        this.arrayDecls = arrayDecls;
    }
    
    @Override
    public int getNodeType()
    {
        return ParsedNode.NODETYPE_FIELD;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Get the type of this field (as a JavaEntity, which needs to be resolved as a type).
     */
    public JavaEntity getFieldType()
    {
        JavaEntity ftype = firstNode == null ? fieldType : firstNode.fieldType;
        for (int i = 0; i < arrayDecls; i++) {
            ftype = new UnresolvedArray(ftype);
        }
        return ftype;
    }
    
}
