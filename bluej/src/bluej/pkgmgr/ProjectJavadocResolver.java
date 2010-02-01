/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.views.Comment;
import bluej.views.MethodView;
import bluej.views.View;

/**
 * Resolves javadoc from classes within a project.
 * 
 * @author Davin McCall
 */
public class ProjectJavadocResolver implements JavadocResolver
{
    private Project project;
    
    public ProjectJavadocResolver(Project project)
    {
        this.project = project;
    }
    
    public void getJavadoc(MethodReflective method)
    {
        Reflective declaring = method.getDeclaringType();
        String declName = declaring.getName();
        String methodSig = buildSig(method);
        
        try {
            Class<?> cl = project.getClassLoader().loadClass(declName);
            View clView = View.getView(cl);
            MethodView [] methods = clView.getAllMethods();
            
            for (int i = 0; i < methods.length; i++) {
                if (methodSig.equals(methods[i].getSignature())) {
                    Comment comment = methods[i].getComment();
                    if (comment != null) {
                        method.setJavaDoc(comment.getText());
                        List<String> paramNames = new ArrayList<String>(comment.getParamCount());
                        for (int j = 0; j < comment.getParamCount(); j++) {
                            paramNames.add(comment.getParamName(j));
                        }
                        method.setParamNames(paramNames);
                    }
                    return;
                }
            }
        }
        catch (ClassNotFoundException cnfe) {}
        catch (LinkageError e) {}
    }

    /**
     * Build a method signature from a MethodReflective.
     */
    private static String buildSig(MethodReflective method)
    {
        String sig = method.getReturnType().getErasedType().toString();
        sig = sig.replace('$', '.');
        sig += ' ';
        
        sig += method.getName() + '(';
        Iterator<JavaType> i = method.getParamTypes().iterator();
        while (i.hasNext()) {
            JavaType ptype = i.next();
            sig += ptype.getErasedType().toString().replace('$', '.');
            if (i.hasNext()) {
                sig += ", ";
            }
        }
        sig += ')';
        
        return sig;
    }
}
