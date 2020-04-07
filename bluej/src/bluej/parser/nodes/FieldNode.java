/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.gentype.JavaType;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.TextParser;
import bluej.parser.entity.ErrorEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnresolvedArray;
import bluej.parser.entity.ValueEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * A node representing a parsed field or variable declaration.
 * 
 * @author Davin McCall
 */
public class FieldNode extends JavaParentNode
{
    private String name;
    private JavaEntity fieldType;
    private boolean isVarType;  // declared with "var" (type to be inferred)?
    private FieldNode firstNode;
    private int modifiers;
    /** Number of extra array declarators */
    private int arrayDecls;
    
    /** The document for the source declaring this field, used for inferring "var" type fields */
    private ReparseableDocument document;
    
    /**
     * Construct a field node representing the first declared field in a field
     * declaration. The fieldType may be null if it appears invalid.
     */
    public FieldNode(JavaParentNode parent, String name, JavaEntity fieldType, int arrayDecls,
            int modifiers)
    {
        super(parent);
        this.name = name;
        this.fieldType = fieldType;
        this.arrayDecls = arrayDecls;
        this.modifiers = modifiers;
        this.document = null;
        this.isVarType = false;
    }

    /**
     * Construct a field node representing the first declared variable in a variable declaration
     * where the type is specified as "var" (i.e. the type should be inferred).
     */
    public FieldNode(JavaParentNode parent, String name, int arrayDecls, int modifiers,
            ReparseableDocument document)
    {
        super(parent);
        // Note, we can't infer the type now, since the result may change when other edits are
        // made. Instead, save enough information so that we can infer it on request:
        this.name = name;
        this.fieldType = null;
        this.arrayDecls = arrayDecls;
        this.modifiers = modifiers;
        this.document = document;
        this.isVarType = true;
    }
    
    /**
     * Construct a field node representing the second or a subsequent field
     * declared in a field declaration.
     * @param parent     The parent parsed node (should be a TypeInnerNode)
     * @param firstNode  The node representing the first declared field
     */
    public FieldNode(JavaParentNode parent, String name, FieldNode firstNode, int arrayDecls)
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

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    /**
     * Calculate the absolute offset of this node, by walking up the node tree.
     */
    private int getAbsoluteOffset()
    {
        int offs = 0;
        ParsedNode pnode = getParentNode();
        ParsedNode node = this;
        while (pnode != null) {
            offs += node.getOffsetFromParent();
            node = pnode;
            pnode = node.getParentNode();
        }
        return offs;
    }
    
    /**
     * Get the containing type of this field/variable.
     */
    private JavaEntity getContainingType()
    {
        ParsedNode pnode = getParentNode();
        while (pnode.getNodeType() != ParsedNode.NODETYPE_TYPEDEF)
        {
            pnode = pnode.getParentNode();
        }
        
        return new TypeEntity(new ParsedReflective((ParsedTypeNode) pnode));
    }
    
    /**
     * Get the initialiser-expression node and its position.
     */
    private NodeAndPosition<ParsedNode> getInitExpression()
    {
        int mypos = getAbsoluteOffset();
        NodeAndPosition<ParsedNode> r = findNodeAtOrAfter(0, mypos);
        return r;
    }
    
    /**
     * Get the type of this field (as a JavaEntity, which needs to be resolved as a type).
     */
    public JavaEntity getFieldType()
    {
        // Note that "var" isn't allowed in a compound (multi-variable) declaration. So it should
        // be ok to refer to firstNode.fieldType directly here:
        JavaEntity ftype = firstNode == null ? fieldType : firstNode.fieldType;
        if (ftype == null)
        {
            if (isVarType)
            {
                NodeAndPosition<ParsedNode> initExpr = getInitExpression();
                
                if (document == null)
                {
                    return new ErrorEntity();
                }
                
                TextParser tp = new TextParser(this,
                        document.makeReader(initExpr.getPosition(), initExpr.getEnd()),
                        getContainingType(),
                        false /* static access */);
                tp.parseExpression();
                JavaEntity inferredTypeEnt = tp.getExpressionType();
                if (inferredTypeEnt == null)
                {
                    return new ErrorEntity();
                }
                
                ValueEntity inferredVal = inferredTypeEnt.resolveAsValue();
                if (inferredVal == null)
                {
                    return new ErrorEntity();
                }
                
                JavaType inferredType = inferredVal.getType();
                
                if (inferredType != null)
                {
                    return new TypeEntity(inferredType);
                }
            }
            
            return new ErrorEntity();
        }
        
        for (int i = 0; i < arrayDecls; i++)
        {
            ftype = new UnresolvedArray(ftype);
        }
        return ftype;
    }
    
    /**
     * Get the modifiers of this field 
     * @return
     */
    public int getModifiers()
    {
        if (firstNode != null) {
            return firstNode.getModifiers();
        }
        return modifiers;
    }
    
    @Override
    protected ExpressionTypeInfo getExpressionType(int pos, int nodePos, JavaEntity defaultType, ReparseableDocument document)
    {
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(Math.max(pos - 1, 0), nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document);
        }
        
        // A field node can actually be an expression with a missing semicolon, followed
        // by an identifier (which is actually meant to be part of the next statement).
        //
        //   eg:
        //      some.expr
        //      someMethodCall();
        //
        // So, we'll pretend we're an expression.
        return ExpressionNode.suggestAsExpression(pos, nodePos, this, defaultType, document);
    }

    public boolean isFirstFieldNode()
    {
        return firstNode == null;
    }
}
