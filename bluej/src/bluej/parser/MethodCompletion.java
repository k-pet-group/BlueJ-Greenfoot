/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2017  Michael Kolling and John Rosenberg 

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.JavaUtils;

/**
 * Possible code completion for a method.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class MethodCompletion extends AssistContent
{
    @OnThread(Tag.Any) private final MethodReflective method;
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
    @OnThread(Tag.Any)
    public String getName()
    {
        return method.getName();
    }
        
    @Override
    public String getType()
    {
        return convertToSolid(method.getReturnType()).toString(true);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public String getJavadoc()
    {
        String jd = method.getJavaDoc();
        if (jd == null && javadocResolver != null) {
            javadocResolver.getJavadoc(method.getDeclaringType(), Collections.singletonList(method));
            jd = method.getJavaDoc();
            if (jd == null) {
                method.setJavaDoc(""); // prevent repeated attempts to retrieve unavailable doc
            }
        }
        return jd;
    }

    private JavaType convertToSolid(JavaType type)
    {
        if (! type.isPrimitive()) {
            if (typeArgs != null) {
                type = type.mapTparsToTypes(typeArgs).getTparCapture();
            }
            else {
                // null indicates a raw type.
                type = type.getErasedType();
            }
        }
        return type;
    }

    //package-visible
    static String buildDummyName(JavaType paramType, String paramName)
    {
        if (paramName != null) {
            return "_" + paramName + "_";
        }
        else {
            return "_" + paramType.toString(true) + "_";
        }
    }
    
    public CompletionKind getKind()
    {
        return CompletionKind.METHOD;
    }
    
    /**
     * Gets a String that is the method's unique signature
     */
    @OnThread(Tag.FXPlatform)
    public String getSignature()
    {
        StringBuilder sig = new StringBuilder();
        sig.append(getType()).append(" ").append(getName()).append("(")
           .append(getParams().stream().map(ParamInfo::getQualifiedType).collect(Collectors.joining(",")))
           .append(")");
        return sig.toString();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<ParamInfo> getParams()
    {
        // We must get Javadoc before asking for parameter names, as it is this method call that sets the parameter names:
        getJavadoc();
        ArrayList<ParamInfo> r = new ArrayList<>();
        List<JavaType> paramTypes = method.getParamTypes();
        List<String> paramNames = method.getParamNames();
        for (int i = 0; i < paramTypes.size(); i++)
        {
            JavaType t = convertToSolid(paramTypes.get(i));
            String paramName = paramNames == null ? null : paramNames.get(i);
            r.add(new ParamInfo(t.toString(), paramName, buildDummyName(t, paramName), javadocForParam(paramName)));
        }
        return r;
    }

    @OnThread(Tag.FXPlatform)
    private Supplier<String> javadocForParam(String paramName)
    {
        final String javadocSrc = getJavadoc();
        return () -> {
            JavaUtils.Javadoc javadoc = JavaUtils.parseJavadoc(javadocSrc);

            if (javadoc == null)
                return null;

            String target = "param " + paramName;
            for (String block : javadoc.getBlocks())
            {
                if (block.startsWith(target) && Character.isWhitespace(block.charAt(target.length())))
                {
                    return block.substring(target.length() + 1).trim();
                }
            }
            return null;
        };
    }

    @Override
    public Access getAccessPermission()
    {
        return fromModifiers(method.getModifiers());
    }
}
