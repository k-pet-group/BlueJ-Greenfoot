/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 

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
package bluej.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.pkgmgr.JavadocResolver;

/**
 * Possible code completion for a method.
 * 
 * @author Davin McCall
 */
public class MethodCompletion extends AssistContent
{
    private MethodReflective method;
    private JavadocResolver javadocResolver;
    private Map<String,GenTypeParameter> typeArgs;
    
    /**
     * Construct a new method completion
     * @param method    The method to represent
     * @param typeArgs   The type arguments applied to the declaring class. For a method
     *                   call on a raw expression, will be null.
     * @param javadocResolver  The javadoc resolver to use
     */
    public MethodCompletion(MethodReflective method,
            Map<String,GenTypeParameter> typeArgs,
            JavadocResolver javadocResolver)
    {
        this.method = method;
        if (typeArgs != null) {
            List<GenTypeDeclTpar> mtpars = method.getTparTypes();
            if (! mtpars.isEmpty()) {
                // The method has its own type parameters - these override the class parameters.
                Map<String,GenTypeParameter> fullArgMap = new HashMap<String,GenTypeParameter>();
                fullArgMap.putAll(typeArgs);
                for (GenTypeDeclTpar mtpar : mtpars) {
                    fullArgMap.put(mtpar.getTparName(), mtpar);
                }
                this.typeArgs = fullArgMap;
            }
            else {
                this.typeArgs = typeArgs;
            }
        }
        this.javadocResolver = javadocResolver;
    }
    
    @Override
    public String getDeclaringClass()
    {
        return method.getDeclaringType().getSimpleName();
    }
    
    @Override
    public String getDisplayMethodName()
    {
        return method.getName();
    }

    @Override
    public String getDisplayMethodParams()
    {
        return getDisplayMethodParams(true);
    }
    
    public String getDisplayMethodParams(boolean includeNames)
    {
        List<String> paramNames = includeNames ? method.getParamNames() : null;
        Iterator<String> nameIterator = paramNames != null ? paramNames.iterator() : null;
        
        String displayName = "(";
        List<JavaType> paramTypes = method.getParamTypes();
        for (Iterator<JavaType> i = paramTypes.iterator(); i.hasNext(); ) {
            JavaType paramType = convertToSolid(i.next());
            displayName += paramType.toString(true);
            if (nameIterator != null) {
                displayName += " " + nameIterator.next();
            }
            if (i.hasNext()) {
                displayName += ", ";
            }
        }
        displayName += ")";
        
        return displayName;
    }

    @Override
    public String getDisplayName()
    {
        return getDisplayMethodName() + getDisplayMethodParams(false);
    }
    
    @Override
    public String getCompletionText()
    {
        return method.getName() + "(";
    }
    
    @Override
    public String getCompletionTextSel()
    {
        List<JavaType> paramTypes = method.getParamTypes();
        if (! paramTypes.isEmpty()) {
            List<String> paramNames = method.getParamNames();
            if (paramNames == null || paramNames.isEmpty()) {
                return buildParam(1, paramTypes.get(0), null);
            }
            else {
                return buildParam(1, paramTypes.get(0), paramNames.get(0));
            }
        }
        return "";
    }
    
    @Override
    public String getCompletionTextPost()
    {
        String r = ")";
        List<JavaType> paramTypes = method.getParamTypes();
        if (paramTypes.size() > 1) {
            String paramStr = "";
            List<String> paramNames = method.getParamNames();
            paramNames = (paramNames == null) ? Collections.<String>emptyList() : paramNames;
            Iterator<JavaType> ti = paramTypes.iterator();
            Iterator<String> ni = paramNames.iterator();
            ti.next();
            if (ni.hasNext()) ni.next();
            int i = 2;
            while (ti.hasNext()) {
                String name = ni.hasNext() ? ni.next() : null;
                paramStr += ", " + buildParam(i++, ti.next(), name);
            }
            r = paramStr + r;
        }
        
        return r;
    }
    
    @Override
    public String getReturnType()
    {
        return convertToSolid(method.getReturnType()).toString(true);
    }

    @Override
    public boolean javadocIsSet()
    {
        return method.getJavaDoc() != null;
    }
    
    @Override
    public String getJavadoc()
    {
        String jd = method.getJavaDoc();
        if (jd == null && javadocResolver != null) {
            javadocResolver.getJavadoc(method);
            jd = method.getJavaDoc();
        }
        return jd;
    }
    
    @Override
    public boolean getJavadocAsync(final JavadocCallback callback, Executor executor)
    {
        String jd = method.getJavaDoc();
        if (jd == null && javadocResolver != null) {
            return javadocResolver.getJavadocAsync(method, new JavadocResolver.AsyncCallback() {
                @Override
                public void gotJavadoc(MethodReflective method)
                {
                    if (method.getJavaDoc() == null) {
                        method.setJavaDoc(""); // prevent repeated attempts to retrieve unavailable doc
                    }
                    callback.gotJavadoc(MethodCompletion.this);
                }
            }, executor);
        }
        else {
            return true;
        }
    }

    @Override
    public boolean hasParameters()
    {
        return !method.getParamTypes().isEmpty();
    }
    
    private JavaType convertToSolid(JavaType type)
    {
        if (! type.isPrimitive()) {
            if (typeArgs != null) {
                type = type.mapTparsToTypes(typeArgs).getUpperBound();
            }
            else {
                // null indicates a raw type.
                type = type.getErasedType();
            }
        }
        return type;
    }
    
    private static String buildParam(int pnum, JavaType paramType, String paramName)
    {
        if (paramName != null) {
            return "_" + paramName + "_";
        }
        else {
            return "_" + paramType.toString(true) + "_";
        }
    }
}
