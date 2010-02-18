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
package bluej.parser.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.nodes.FieldNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.utility.JavaUtils;

/**
 * A Reflective implementation for classes which are parsed, but not necessarily compiled.
 * 
 * @author Davin McCall
 */
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reflective getRelativeClass(String name)
    {
        // TODO Auto-generated method stub
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

        for (JavaEntity etype : pnode.getImplementedTypes()) {
            TypeEntity tent = etype.resolveAsType();
            if (tent != null) {
                GenTypeClass ct = tent.getType().asClass();
                if (ct != null) {
                    rval.add(ct);
                }
            }
        }
        
        if (rval.size() == 0 && !isInterface()) {
            TypeEntity tent = pnode.resolveQualifiedClass("java.lang.Object");
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
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        // TODO complete
        return Collections.emptyList();
    }

    @Override
    public boolean isAssignableFrom(Reflective r)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInterface()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStatic()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean isPublic()
    {
        // TODO Auto-generated method stub
        return true;
    }
    
    @Override
    public Map<String,JavaType> getDeclaredFields()
    {
        Map<String,FieldNode> fields = pnode.getInner().getFields();
        Map<String,JavaType> rmap = new HashMap<String, JavaType>();
        for (Iterator<String> i = fields.keySet().iterator(); i.hasNext(); ) {
            String fieldName = i.next();
            JavaEntity ftypeEnt = fields.get(fieldName).getFieldType().resolveAsType();
            if (ftypeEnt != null) {
                rmap.put(fieldName, ftypeEnt.getType());
            }
        }
        return rmap;
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        Map<String,Set<MethodNode>> methods = pnode.getInner().getMethods();
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
                JavaType rtype = rtypeEnt.getType().getCapture();
                List<JavaType> paramTypes = new ArrayList<JavaType>();
                List<JavaEntity> mparamTypes = method.getParamTypes();
                for (JavaEntity mparam : mparamTypes) {
                    TypeEntity mtent = mparam.resolveAsType();
                    if (mtent == null) continue methodLoop;
                    paramTypes.add(mtent.getType());
                }
                // DAV need to set the type parameters properly
                MethodReflective mref = new MethodReflective(name, rtype, null,
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
    public List<GenTypeClass> getInners()
    {
        // DAV fix
        return Collections.emptyList();
    }
}
