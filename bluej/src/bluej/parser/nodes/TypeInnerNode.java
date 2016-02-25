/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016  Michael Kolling and John Rosenberg 
 
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * Node for the inner part of a type definition. This contains the declarations inside
 * the type.
 * 
 * @author Davin McCall
 */
public class TypeInnerNode extends IncrementalParsingNode
{
    private Map<String,Set<MethodNode>> methods = new HashMap<String,Set<MethodNode>>();

    public TypeInnerNode(JavaParentNode parent)
    {
        super(parent);
    }
    
    @Override
    public boolean isInner()
    {
        return true;
    }
        
    /**
     * A method was added.
     */
    public void methodAdded(MethodNode method)
    {
        String name = method.getName();
        Set<MethodNode> methodSet = methods.get(name);
        if (methodSet == null) {
            methodSet = new HashSet<MethodNode>();
            methods.put(name, methodSet);
        }
        methodSet.add(method);
    }
    
    @Override
    public void childChangedName(ParsedNode child, String oldName)
    {
        super.childChangedName(child, oldName);
        if (child.getNodeType() == NODETYPE_METHODDEF) {
            Set<MethodNode> methodSet = methods.get(oldName);
            methodSet.remove(child);
            
            methodAdded((MethodNode) child);
        }
    }
    
    @Override
    protected void childRemoved(NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        super.childRemoved(child, listener);
        if (child.getNode().getNodeType() == NODETYPE_METHODDEF) {
            Set<MethodNode> methodSet = methods.get(child.getNode().getName());
            methodSet.remove(child.getNode());
        }
    }
    
    /**
     * Get the fields defined in this type.
     */
    public Map<String, Set<FieldNode>> getFields()
    {
        return variables;
    }
    
    /**
     * Get the methods defined in this type.
     */
    public Map<String,Set<MethodNode>> getMethods()
    {
        return methods;
    }
    
    @Override
    protected int doPartialParse(ParseParams params, int state)
    {
        last = null;
        LocatableToken nextToken = params.tokenStream.nextToken();
                
        if (nextToken.getType() == JavaTokenTypes.RCURLY) {
            last = nextToken;
            return PP_ENDS_NODE;
        }
        if (nextToken.getType() == JavaTokenTypes.EOF) {
            // We don't really know if the last partial was successful or not.
            last = nextToken;
            return PP_INCOMPLETE;
        }
        
        if (checkBoundary(params, nextToken)) {
            last = nextToken;
            return PP_PULL_UP_CHILD;
        }
        
        params.parser.parseClassElement(nextToken);
        complete = false;
        return PP_OK;
    }
    
    @Override
    protected boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap)
    {
        return nap.getNode().isContainer();
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return false;
    }
    
    @Override
    public boolean growsForward()
    {
        return true;
    }
    
    /**
     * Get a reference to the map of inner classes of this node (live; should not be mutated).
     */
    public Map<String,ParsedNode> getContainedClasses()
    {
        return classNodes;
    }
}
