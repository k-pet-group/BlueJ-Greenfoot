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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;


/**
 * A node representing a parsed method or constructor.
 * 
 * @author Davin McCall
 */
public class MethodNode extends ParentParsedNode
{
    private String name;
    private String javadoc;
    private JavaEntity returnType;
    private List<String> paramNames = new ArrayList<String>();
    private List<JavaEntity> paramTypes = new ArrayList<JavaEntity>();
    private boolean isVarArgs = false;
    private int modifiers = 0;
    
    /**
     * Construct a MethodNode representing a constructor or method.
     * @param parent  The parent node (containing this node)
     * @param name    The constructor/method name
     * @param javadoc The javadoc comment text (or null)
     */
    public MethodNode(ParsedNode parent, String name, String javadoc)
    {
        super(parent);
        this.name = name;
        this.javadoc = javadoc;
    }

    /**
     * Construct a MethodNode representing a method.
     * @param parent   The parent node (containing this node)
     * @param name     The method name
     * @param returnType   The method return type
     * @param javadoc  The javadoc comment text (or null)
     */
    public MethodNode(ParsedNode parent, String name, JavaEntity returnType, String javadoc)
    {
        super(parent);
        this.name = name;
        this.returnType = returnType;
        this.javadoc = javadoc;
    }
    
    /**
     * Add a method parameter
     * @param name  The parameter name
     * @param type  The parameter type
     */
    public void addParameter(String name, JavaEntity type)
    {
        paramNames.add(name);
        paramTypes.add(type);
    }
    
    /**
     * Mark this method as a varargs method (or not).
     * @param isVarArgs  Whether this method is a varargs method.
     */
    public void setVarArgs(boolean isVarArgs)
    {
        this.isVarArgs = isVarArgs;
    }
    
    /**
     * Check whether this method is a varargs method.
     */
    public boolean isVarArgs()
    {
        return isVarArgs;
    }
    
    /**
     * Set the modifiers on this method (as per java.lang.reflect.Modifier)
     */
    public void setModifiers(int modifiers)
    {
        this.modifiers = modifiers;
    }
    
    /**
     * Get the modifiers on this method (as per java.lang.reflect.Modifier)
     */
    public int getModifiers()
    {
        return modifiers;
    }
    
    /**
     * Get the parameter names of this method
     * @return
     */
    public List<String> getParamNames()
    {
        return paramNames;
    }
    
    /**
     * Get the parameter types of this method.
     */
    public List<JavaEntity> getParamTypes()
    {
        return paramTypes;
    }
    
    /**
     * Get the javadoc comment text for this node. May return null.
     */
    public String getJavadoc()
    {
        return javadoc;
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
    
    @Override
    public int getNodeType()
    {
        return ParsedNode.NODETYPE_METHODDEF;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    /**
     * Get the return type of the method represented by this node.
     * Returns a possibly unresolved JavaEntity.
     */
    public JavaEntity getReturnType()
    {
        return returnType;
    }
    
    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        Iterator<String> i = paramNames.iterator();
        Iterator<JavaEntity> j = paramTypes.iterator();
        while (i.hasNext()) {
            if (i.next().equals(name)) {
                TypeEntity tent = j.next().resolveAsType();
                if (tent != null) {
                    return new ValueEntity(name, tent.getType());
                }
                return null;
            }
            j.next();
        }
        return super.getValueEntity(name, querySource);
    }
}
