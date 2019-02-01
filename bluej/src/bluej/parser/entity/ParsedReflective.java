/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2014,2015,2016,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.parser.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.ConstructorReflective;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.JavaParser;
import bluej.parser.nodes.FieldNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.parser.nodes.TypeInnerNode;
import bluej.utility.JavaUtils;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Reflective implementation for classes which are parsed, but not necessarily compiled.
 * 
 * @author Davin McCall
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class ParsedReflective extends Reflective
{
    private ParsedTypeNode pnode;
    
    public ParsedReflective(ParsedTypeNode pnode)
    {
        this.pnode = pnode;
    }
    
    @Override
    public String getName()
    {
        return pnode.getPrefix() + pnode.getName();
    }

    @Override
    public Reflective getArrayOf()
    {
        return new ParsedArrayReflective(this, "L" + getName() + ";");
    }

    @Override
    public Reflective getRelativeClass(String name)
    {
        TypeEntity tent = pnode.resolveQualifiedClass(name);
        if (tent != null) {
            GenTypeClass ctype = tent.getType().asClass();
            if (ctype != null) {
                return ctype.getReflective();
            }
        }
        return null;
    }

    @Override
    public List<GenTypeClass> getSuperTypes()
    {
        List<GenTypeClass> rval = new LinkedList<GenTypeClass>();
        
        for (JavaEntity etype : pnode.getExtendedTypes()) {
            TypeEntity tent = etype.resolveAsType();
            if (tent != null) {
                GenTypeClass ct = tent.getType().asClass();
                if (ct != null) {
                    rval.add(ct);
                }
            }
        }
        
        if (! isInterface()) {
            if (rval.isEmpty()) {
                // All classes extend Object implicitly
                TypeEntity tent = pnode.resolveQualifiedClass("java.lang.Object");
                if (tent != null) {
                    GenTypeClass ct = tent.getType().asClass();
                    if (ct != null) {
                        rval.add(ct);
                    }
                }
            }
        }

        for (JavaEntity etype : pnode.getImplementedTypes()) {
            TypeEntity tent = etype.resolveAsType();
            if (tent != null) {
                GenTypeClass ct = tent.getType().asClass();
                if (ct != null) {
                    rval.add(ct);
                }
            }
        }
        
        return rval;
    }

    @Override
    public List<Reflective> getSuperTypesR()
    {
        List<Reflective> rlist = new ArrayList<Reflective>();
        List<JavaEntity> extendedTypes = pnode.getExtendedTypes();
        if (extendedTypes != null && ! extendedTypes.isEmpty()) {
            for (JavaEntity etype : extendedTypes) {
                TypeEntity etypeTEnt = etype.resolveAsType();
                if (etypeTEnt != null) {
                    GenTypeClass superGTC = etypeTEnt.getType().asClass();
                    if (superGTC != null) {
                        rlist.add(superGTC.getReflective());
                    }
                }
            }
        }
        
        if (rlist.isEmpty() && ! isInterface()) {
            // Object is always a supertype
            TypeEntity objEntity = pnode.resolveQualifiedClass("java.lang.Object");
            if (objEntity != null) {
                GenTypeClass superGTC = objEntity.getType().asClass();
                if (superGTC != null) {
                    rlist.add(superGTC.getReflective());
                }
            }
        }
        
        extendedTypes = pnode.getImplementedTypes();
        if (extendedTypes != null && ! extendedTypes.isEmpty()) {
            for (JavaEntity etype : extendedTypes) {
                TypeEntity etypeTEnt = etype.resolveAsType();
                if (etypeTEnt != null) {
                    GenTypeClass superGTC = etypeTEnt.getType().asClass();
                    if (superGTC != null) {
                        rlist.add(superGTC.getReflective());
                    }
                }
            }
        }
        
        return rlist;
    }

    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        List<TparEntity> tparEntList = pnode.getTypeParams();
        if (tparEntList == null) {
            return Collections.emptyList();
        }
        
        List<GenTypeDeclTpar> tparList = new ArrayList<GenTypeDeclTpar>(tparEntList.size());
        for (TparEntity tpar : tparEntList) {
            GenTypeDeclTpar tparType = tpar.getType();
            if (tparType != null) {
                tparList.add(tparType);
            }
        }
        
        return tparList;
    }

    @Override
    public boolean isAssignableFrom(Reflective r)
    {
        Set<String> done = new HashSet<String>();
        LinkedList<Reflective> todo = new LinkedList<Reflective>();
        
        while (r != null) {
            String rname = r.getName();
            if (rname.equals(getName())) {
                return true;
            }
            if (done.add(r.getName())) {
                todo.addAll(r.getSuperTypesR());
            }
            r = todo.poll();
        }
        
        return false;
    }

    @Override
    public boolean isInterface()
    {
        return pnode.getTypeKind() == JavaParser.TYPEDEF_INTERFACE;
    }

    @Override
    public boolean isStatic()
    {
        return Modifier.isStatic(pnode.getModifiers());
    }
    
    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(pnode.getModifiers());
    }
    
    @Override
    public boolean isFinal()
    {
        return Modifier.isFinal(pnode.getModifiers());
    }
    
    @Override
    public Map<String,FieldReflective> getDeclaredFields()
    {
        Map<String,Set<FieldNode>> allfields = pnode.getInner().getFields();
        
        // Filter out duplicates:
        Map<String, FieldNode> fields = new HashMap<>();
        for (String name : allfields.keySet()) {
            fields.put(name, allfields.get(name).iterator().next());
        }
        
        Map<String,FieldReflective> rmap = new HashMap<String,FieldReflective>();
        for (Iterator<String> i = fields.keySet().iterator(); i.hasNext(); ) {
            String fieldName = i.next();
            FieldNode fieldNode = fields.get(fieldName);
            JavaEntity ftypeEnt = fieldNode.getFieldType().resolveAsType();
            if (ftypeEnt != null) {
                FieldReflective fref = new FieldReflective(fieldName, ftypeEnt.getType(),
                        fieldNode.getModifiers(), this);
                rmap.put(fieldName, fref);
            }
        }
        return rmap;
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        TypeInnerNode pnodeInner = pnode.getInner();
        if (pnodeInner == null) {
            return Collections.emptyMap();
        }
        
        Map<String,Set<MethodNode>> methods = pnodeInner.getMethods();
        Map<String,Set<MethodReflective>> rmap = new HashMap<String,Set<MethodReflective>>();
        
        for (Iterator<String> i = methods.keySet().iterator(); i.hasNext(); ) {
            String name = i.next();
            Set<MethodNode> mset = methods.get(name);
            Set<MethodReflective> rset = new HashSet<MethodReflective>();
            
            methodLoop:
            for (Iterator<MethodNode> j = mset.iterator(); j.hasNext(); ) {
                MethodNode method = j.next();
                JavaEntity rtypeEnt = method.getReturnType();
                if (rtypeEnt == null) continue; // constructor
                rtypeEnt = rtypeEnt.resolveAsType();
                if (rtypeEnt == null) continue;
                JavaType rtype = rtypeEnt.getType();
                List<JavaType> paramTypes = new ArrayList<JavaType>();
                List<JavaEntity> mparamTypes = method.getParamTypes();
                for (JavaEntity mparam : mparamTypes) {
                    TypeEntity mtent = mparam.resolveAsType();
                    if (mtent == null) continue methodLoop;
                    paramTypes.add(mtent.getType());
                }
                List<GenTypeDeclTpar> tparTypes = method.getTypeParams();
                MethodReflective mref = new MethodReflective(name, rtype, tparTypes,
                        paramTypes, this, method.isVarArgs(), method.getModifiers());
                mref.setJavaDoc(JavaUtils.javadocToString(method.getJavadoc()));
                mref.setParamNames(method.getParamNames());
                rset.add(mref);
            }
            if (! rset.isEmpty()) {
                rmap.put(name, rset);
            }
        }
        return rmap;
    }

    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        // TODO actually pick out the constructors:
        return Collections.emptyList();
    }
    
    @Override
    public Reflective getOuterClass()
    {
        ParsedTypeNode containing = pnode.getContainingClass();
        if (containing != null) {
            return new ParsedReflective(containing);
        }
        return null;
    }
    
    @Override
    public ParsedReflective getInnerClass(String name)
    {
        Map<String,ParsedNode> contained = pnode.getInner().getContainedClasses();
        ParsedNode innerParsedNode = contained.get(name);
        if (innerParsedNode instanceof ParsedTypeNode) {
            return new ParsedReflective((ParsedTypeNode) innerParsedNode);
        }
        return null;
    }

    @Override
    public String getModuleName()
    {
        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        
        if (obj instanceof ParsedReflective) {
            ParsedReflective other = (ParsedReflective) obj;
            return pnode == other.pnode;
        }
        
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return pnode.hashCode();
    }
}
