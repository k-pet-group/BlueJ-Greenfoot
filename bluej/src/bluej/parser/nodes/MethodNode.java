/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TparEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A node representing a parsed method or constructor.
 * 
 * @author Davin McCall
 */
public class MethodNode extends JavaParentNode
{
    private String name;
    private String javadoc;
    private JavaEntity returnType;
    private List<String> paramNames = new ArrayList<String>();
    private List<JavaEntity> paramTypes = new ArrayList<JavaEntity>();
    private List<TparEntity> typeParams = null;
    private boolean isVarArgs = false;
    private int modifiers = 0;
    
    /**
     * Construct a MethodNode representing a constructor or method.
     * @param parent  The parent node (containing this node)
     * @param name    The constructor/method name
     * @param javadoc The javadoc comment text (or null)
     */
    public MethodNode(JavaParentNode parent, String name, String javadoc)
    {
        super(parent);
        this.name = name;
        this.javadoc = javadoc;
        setCommentAttached(javadoc != null);
    }

    /**
     * Set the return type of this method.
     * 
     * <p>(If the returnType is unresolved, it must resolve against type parameters for
     * this actual method).
     */
    public void setReturnType(JavaEntity returnType)
    {
        this.returnType = returnType;
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
     * Set the type parameters for this method.
     */
    public void setTypeParams(List<TparEntity> typeParams)
    {
        this.typeParams = typeParams;
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
     * Returns a possibly unresolved JavaEntity. For a constructor,
     * returns null.
     */
    public JavaEntity getReturnType()
    {
        return returnType;
    }
    
    /**
     * Get the type parameters for this method.
     */
    public List<GenTypeDeclTpar> getTypeParams()
    {
        if (typeParams == null) {
            return null;
        }
        
        List<GenTypeDeclTpar> tparList = new ArrayList<GenTypeDeclTpar>(typeParams.size());
        for (TparEntity tparEnt : typeParams) {
            GenTypeDeclTpar tparType = tparEnt.getType();
            if (tparType != null) {
                tparList.add(tparType);
            }
        }
        return tparList;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        JavaEntity paramEntity = getParameterEntity(name, querySource);
        if (paramEntity != null) {
            return paramEntity;
        }
        return super.getValueEntity(name, querySource);
    }
    
    /**
     * Look for a value entity in the method parameters.
     */
    private JavaEntity getParameterEntity(String name, Reflective querySource)
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
        return null;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name,
            Reflective querySource)
    {
        if (typeParams != null) {
            for (TparEntity tpar : typeParams) {
                if (tpar.getName().equals(name)) {
                    return tpar.resolveAsType();
                }
            }
        }
        return super.resolvePackageOrClass(name, querySource);
    }
    
    @Override
    protected ExpressionTypeInfo getExpressionType(int pos, int nodePos,
            JavaEntity defaultType, ReparseableDocument document)
    {
        if (Modifier.isStatic(modifiers)) {
            JavaType dtype = defaultType.getType();
            if (dtype != null) {
                defaultType = new TypeEntity(dtype);
            }
        }
        return super.getExpressionType(pos, nodePos, defaultType, document);
    }

    /**
     * Gets the local variables nodes of a method.
     *
     * @return a Map object containing the local variables nodes of that method.
     */
    public Map<String, Set<FieldNode>> getLocVarNodes()
    {
        Iterator<NodeAndPosition<ParsedNode>> children = getChildren(0);
        while (children.hasNext())
        {
            NodeAndPosition<ParsedNode> child = children.next();
            if (child.getNode() instanceof MethodBodyNode)
            {
                return ((MethodBodyNode) child.getNode()).variables;
            }
        }
        return null;
    }
}
