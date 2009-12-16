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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;

/**
 * Node for the inner part of a type definition. This contains the declarations inside
 * the type.
 * 
 * @author Davin McCall
 */
public class TypeInnerNode extends ParentParsedNode
{
    private Map<String,FieldNode> fields = new HashMap<String,FieldNode>();
    private Map<String,Set<MethodNode>> methods = new HashMap<String,Set<MethodNode>>();

    public TypeInnerNode(ParsedNode parent)
    {
        super(parent);
    }
    
    @Override
    public boolean isInner()
    {
        return true;
    }
    
    /**
     * Insert a field child.
     */
    public void insertField(FieldNode child, int position, int size)
    {
        super.insertNode(child, position, size);
        fields.put(child.getName(), child);
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
    
    /**
     * Get the fields defined in this type.
     */
    public Map<String, FieldNode> getFields()
    {
        return fields;
    }
    
    /**
     * Get the methods defined in this type.
     */
    public Map<String,Set<MethodNode>> getMethods()
    {
        return methods;
    }
    
    // =================== EntityResolver interface ====================

    public JavaEntity getValueEntity(String name, String querySource)
    {
        FieldNode field = fields.get(name);
        if (field != null) {
            JavaEntity fieldType = field.getFieldType().resolveAsType();
            if (fieldType != null) {
                return new ValueEntity(fieldType.getType());
            }
        }
        return super.getValueEntity(name, querySource);
    }

}
