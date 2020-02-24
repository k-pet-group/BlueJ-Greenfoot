/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.AssistContent.CompletionKind;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CodeElement.LocalParamInfo;
import bluej.stride.framedjava.elements.ContainerCodeElement;
import bluej.stride.framedjava.elements.MethodWithBodyElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.parser.AssistContentThreadSafe;

public class ASTUtility
{
    // Finds the variables in scope at this element (excluding any that are defined by the element itself, unless includeCurDecl is true)
    // includeCurDecl is only needed for a special case with constructors and super/this
    public static List<LocalParamInfo> findLocalsAndParamsInScopeAt(CodeElement orig, boolean includeFields, boolean includeCurDecl)
    {
        ArrayList<LocalParamInfo> vars = new ArrayList<>();
        CodeElement cur = orig;
        ContainerCodeElement parent = cur.getParent();

        if (includeCurDecl && cur instanceof ContainerCodeElement)
            vars.addAll(((ContainerCodeElement)cur).getDeclaredVariablesWithin(cur));

        // We don't go through classes because we are not interested in fields, only locals and params:
        while (parent != null && (includeFields || parent.getTopLevelElement() == null))
        {
            for (CodeElement c : parent.childrenUpTo(cur))
            {
                vars.addAll(c.getDeclaredVariablesAfter());
            }
            vars.addAll(parent.getDeclaredVariablesWithin(cur));
            
            cur = parent;
            parent = parent.getParent();
        }
        return vars;
    }

    public static TopLevelCodeElement getTopLevelElement(CodeElement orig)
    {
        ContainerCodeElement c = orig.getParent();
        while (c.getTopLevelElement() == null)
            c = c.getParent();
        return c.getTopLevelElement();
    }
    
    public static MethodWithBodyElement getMethodElement(CodeElement orig)
    {
        ContainerCodeElement c = orig.getParent();
        while (c != null && c.getMethodElement() == null)
            c = c.getParent();
        return c == null ? null : c.getMethodElement();
    }
    
    // CodeElement is optional; may be null if we don't know where declaration was:
    // includeCurDecl is only needed for a special case with constructors and super/this
    @OnThread(Tag.FXPlatform)
    public static void withLocalsParamsAndFields(CodeElement el, InteractionManager editor, PosInSourceDoc pos, boolean includeCurDecl, FXPlatformConsumer<Map<String, CodeElement>> handler)
    {
        editor.withAccessibleMembers(pos, Collections.singleton(CompletionKind.FIELD), false,
                x ->
        {
            Stream<String> fieldStream = x.stream().map(AssistContentThreadSafe::getName);
            
            List<LocalParamInfo> localsAndParams = findLocalsAndParamsInScopeAt(el, true, includeCurDecl);
            
            Map<String, CodeElement> r = new HashMap<>();
            
            fieldStream.forEach(s -> r.put(s,  null));
            r.putAll(localsAndParams.stream().collect(Collectors.toMap(LocalParamInfo::getName, LocalParamInfo::getDeclarer, (early, late) -> late)));
            
            handler.accept(r);
        });
    }

    @OnThread(Tag.FXPlatform)
    public static void withMethods(CodeElement el, InteractionManager editor, PosInSourceDoc pos, boolean includeCurDecl, FXPlatformConsumer<List<String>> handler)
    {
        editor.withAccessibleMembers(pos, Collections.singleton(CompletionKind.METHOD), false,
            x ->
            {
                Stream<String> methodStream = x.stream().map(AssistContentThreadSafe::getName);
                handler.accept(methodStream.collect(Collectors.toList()));
            });
    }
}
