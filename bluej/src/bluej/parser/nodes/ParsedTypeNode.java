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

import java.util.List;
import java.util.Map;

import javax.swing.text.Document;

import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.CodeSuggestions;
import bluej.parser.EditorParser;
import bluej.parser.JavaParser;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;



/**
 * A node representing a parsed type (class, interface, enum)
 * 
 * @author Davin McCall
 */
public class ParsedTypeNode extends IncrementalParsingNode
{
    private String name;
    private String prefix;
    private TypeInnerNode inner;
    private Map<String,JavaEntity> typeParams;
    private List<JavaEntity> extendedTypes;
    private List<JavaEntity> implementedTypes;
    
    private int type; // one of JavaParser.TYPEDEF_CLASS, INTERFACE, ENUM, ANNOTATION
    
    /**
     * Construct a new ParsedTypeNode
     * @param parent  The parent node
     * @param name    The base name of the type
     * @param prefix  The prefix of the name, including the final ".", to make this a full
     *                type name
     */
    public ParsedTypeNode(ParsedNode parent, int type, String prefix)
    {
        super(parent);
        stateMarkers = new int[3];
        marksEnd = new boolean[3];
        stateMarkers[0] = -1;
        stateMarkers[1] = -1;
        stateMarkers[2] = -1;
        this.type = type;
        this.prefix = prefix;
    }
    
    public void setTypeParams(Map<String, JavaEntity> typeParams)
    {
        this.typeParams = typeParams;
    }
    
    public void setImplementedTypes(List<JavaEntity> implementedTypes)
    {
        this.implementedTypes = implementedTypes;
    }
    
    public List<JavaEntity> getImplementedTypes()
    {
        return implementedTypes;
    }
    
    public void setExtendedTypes(List<JavaEntity> extendedTypes)
    {
        this.extendedTypes = extendedTypes;
    }
    
    public List<JavaEntity> getExtendedTypes()
    {
        return extendedTypes;
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
    
    public void setName(String name)
    {
        String oldName = this.name;
        this.name = name;
        getParentNode().childChangedName(this, oldName);
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    public String getPrefix()
    {
        return prefix;
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
    
    public TypeInnerNode getInner()
    {
        return inner;
    }
    
    @Override
    protected void childRemoved(NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        if (child.getNode() == inner) {
            inner = null;
        }
        super.childRemoved(child, listener);
    }
    
    @Override
    protected int doPartialParse(ParseParams params, int state)
    {
        if (state == 0) {
            // [modifiers] {class|interface|enum|@interface} name [<type params>] [extends...]
            int r = params.parser.parseTypeDefBegin();
            if (r == JavaParser.TYPEDEF_EPIC_FAIL) {
                return PP_EPIC_FAIL;
            }
            
            type = r;
            params.parser.initializeTypeExtras();
            
            LocatableToken token = params.tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                last = token;
                return PP_INCOMPLETE;
            }
            setName(token.getText());
            
            token = params.parser.parseTypeDefPart2();
            if (token == null) {
                last = params.tokenStream.LA(1);
                return PP_INCOMPLETE;
            }
            last = token;
            params.tokenStream.pushBack(token);
            return PP_BEGINS_NEXT_STATE;
        }
        else if (state == 1) {
            // '{' and class body
            LocatableToken token = params.tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.LCURLY) {
                last = token;
                return PP_REGRESS_STATE;
            }
            
            if (inner == null) {
                last = params.parser.parseTypeBody(type, params.tokenStream.nextToken());
                if (last.getType() == JavaTokenTypes.RCURLY) {
                    params.tokenStream.pushBack(last);
                    return PP_BEGINS_NEXT_STATE;
                }
                return PP_INCOMPLETE;
            }
            
            // If we already have an inner we pull it into position.
            NodeAndPosition<ParsedNode> nextChild = params.childQueue.peek();
            while (nextChild != null && nextChild.getNode() != inner) {
                childRemoved(nextChild, params.listener);
                params.childQueue.poll();
                nextChild = params.childQueue.peek();
            }
            
            return PP_PULL_UP_CHILD;
        }
        else if (state == 2) {
            // '}'
            last = params.tokenStream.nextToken();
            
            // Extend the inner node up to the token we just pulled.
            int lastPos = lineColToPos(params.document, last.getLine(), last.getColumn());
            int innerPos = inner.getOffsetFromParent() + params.nodePos;
            int innerSize = inner.getSize();
            if ((innerPos + innerSize) != lastPos) {
                inner.setSize(lastPos - innerPos);
                inner.setComplete(true);
                params.document.scheduleReparse(innerPos + innerSize, lastPos - innerPos - innerSize);
            }
            
            if (last.getType() != JavaTokenTypes.RCURLY) {
                if (inner != null) {
                    inner.setComplete(false);
                }
                return PP_INCOMPLETE;
            }
            complete = true;
            return PP_ENDS_STATE;
        }
        else if (state == 3) {
            last = params.tokenStream.LA(1);
            return PP_ENDS_NODE;
        }
        
        return PP_EPIC_FAIL;
    }
    
    @Override
    protected boolean lastPartialCompleted(EditorParser parser,
            LocatableToken token, int state)
    {
        return state == 3;
    }
    
    @Override
    protected boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap)
    {
        return false;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    protected void childResized(MoeSyntaxDocument document, NodeAndPosition<ParsedNode> child)
    {
        if (child.getNode() == inner) {
            stateMarkers[1] = child.getEnd();
        }
    }
    
    @Override
    public CodeSuggestions getExpressionType(int pos, int nodePos, TypeEntity defaultType, Document document)
    {
        // The default type if the expression is not know should be this type
        TypeEntity myType = new TypeEntity(new ParsedReflective(this));
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(pos, nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), myType, document);
        }
        
        // We don't return the specified default type (which must be an outer type). There
        // can be no completions because no completions can occur except in the context
        // of child nodes.
        return null;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        if (typeParams != null) {
            JavaEntity ent = typeParams.get(name);
            if (ent != null) {
                TypeEntity tent = ent.resolveAsType();
                if (tent != null) {
                    return tent;
                }
            }
        }
        return super.resolvePackageOrClass(name, querySource);
    }
}
