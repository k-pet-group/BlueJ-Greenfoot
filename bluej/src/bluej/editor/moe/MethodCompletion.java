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
package bluej.editor.moe;

import java.util.Iterator;
import java.util.List;

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
    
    public MethodCompletion(MethodReflective method,
            JavadocResolver javadocResolver)
    {
        this.method = method;
        this.javadocResolver = javadocResolver;
    }
    
    @Override
    public String getDeclaringClass()
    {
        String dname = method.getDeclaringType().getName();
        dname = dname.replace('$', '.');
        return dname;
    }

    @Override
    public String getDisplayName()
    {
        String displayName = method.getName() + "(";
        List<JavaType> paramTypes = method.getParamTypes();
        for (Iterator<JavaType> i = paramTypes.iterator(); i.hasNext(); ) {
            displayName += i.next().toString(true);
            if (i.hasNext()) {
                displayName += ", ";
            }
        }
        displayName += ")";
        
        return displayName;
        
    }

    @Override
    public String getCompletionText()
    {
        return method.getName() + "(";
    }
    
    @Override
    public String getCompletionTextPost()
    {
        return ")";
    }
    
    @Override
    public String getReturnType()
    {
        return method.getReturnType().toString(true);
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

}
