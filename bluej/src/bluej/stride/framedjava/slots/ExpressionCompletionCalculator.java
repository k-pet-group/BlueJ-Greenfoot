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
package bluej.stride.framedjava.slots;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.editor.fixes.SuggestionList;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

public class ExpressionCompletionCalculator implements StructuredCompletionCalculator
{
    private final InteractionManager editor;
    private List<AssistContentThreadSafe> completions = Collections.emptyList();
    private SuggestionList suggestionDisplay;
    
    public ExpressionCompletionCalculator(InteractionManager editor)
    {
        this.editor = editor;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withCalculatedSuggestionList(PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, SuggestionListListener clickListener, String targetType, boolean completingStartOfSlot, FXPlatformConsumer<SuggestionList> handler)
    {
        editor.withCompletions(pos, completing, codeEl, assists -> {
            completions = assists.stream()
                            .filter(a -> !Parser.isDummyName(a.getName()))
                            .sorted(AssistContentThreadSafe.getComparator(targetType))
                            .collect(Collectors.toList());
            suggestionDisplay = new SuggestionList(editor, Utility.mapList(completions, ac -> new SuggestionDetailsWithHTMLDoc(ac.getName(), ExpressionCompletionCalculator.getParamsCompletionDisplay(ac), ac.getType(), getRarity(ac), ac.getDocHTML())), targetType, SuggestionList.SuggestionShown.COMMON, null, clickListener);
            handler.accept(suggestionDisplay);
        });
    }
    
    public static String getParamsCompletionDisplay(AssistContentThreadSafe a)
    {
        if (a.getParams() == null)
            return ""; // Variable
        else
            return "(" + a.getParams().stream().map(ParamInfo::getUnqualifiedType).collect(Collectors.joining(", ")) + ")";
    }
    
    public String getName(int selected)
    {
        if (selected == -1)
            throw new IllegalStateException();
        
        AssistContentThreadSafe a = completions.get(selected);
        
        return a.getName();
    }
    
    // null if variable (no parameters at all), empty if method call but no parameters
    public List<String> getParams(int selected)
    {
        if (selected == -1)
            throw new IllegalStateException();
        
        AssistContentThreadSafe a = completions.get(selected);
        
        return Utility.orNull(a.getParams(), params -> params.stream().map(p -> p.getDummyName()).collect(Collectors.toList()));
    }

    @OnThread(Tag.FXPlatform)
    public void withConstructorParamNames(SuperThis constructorKind, FXPlatformConsumer<List<List<String>>> handler)
    {
        if (constructorKind == SuperThis.THIS)
        {
            handler.accept(Utility.mapList(editor.getThisConstructors(), con -> Utility.mapList(con.getParams(), p -> p.getFormalName())));
        }
        else
        {
            // Assume super in other cases:
            editor.withSuperConstructors(assists -> handler.accept(assists.stream()
                    .filter(a -> a.getParams() != null)
                    .map(a -> a.getParams().stream().map(p -> p.getFormalName()).collect(Collectors.toList()))
                    .collect(Collectors.toList())));
        }
    }

    @OnThread(Tag.FXPlatform)
    public void withParamNames(PosInSourceDoc pos, ExpressionSlot<?> completing, String methodName, CodeElement codeEl, FXPlatformConsumer<List<List<String>>> handler)
    {
        editor.withCompletions(pos, completing, codeEl, assists ->
            handler.accept(assists.stream()
            .filter(a -> !Parser.isDummyName(a.getName()))
            .filter(a -> a.getName().equals(methodName))
            .filter(a -> a.getParams() != null)
            .map(a -> a.getParams().stream().map(p -> p.getFormalName()).collect(Collectors.toList()))
            .collect(Collectors.toList())));
    }

    private String makeHint(ParamInfo p)
    {
        return p.getUnqualifiedType() + " " + p.getFormalName() + "\n\nDescription: " + p.getJavadocDescription();
    }

    @OnThread(Tag.FXPlatform)
    public void withParamHints(PosInSourceDoc pos, ExpressionSlot<?> completing, String methodName, CodeElement codeEl, FXPlatformConsumer<List<List<String>>> handler)
    {
        editor.withCompletions(pos, completing, codeEl, assists ->
            handler.accept(assists.stream()
            .filter(a -> !Parser.isDummyName(a.getName()))
            .filter(a -> a.getName().equals(methodName))
            .filter(a -> a.getParams() != null)
            .map(a -> a.getParams().stream().map(this::makeHint).collect(Collectors.toList()))
                .collect(Collectors.toList())));
    }

    @OnThread(Tag.FXPlatform)
    public void withConstructorParamHints(SuperThis constructorKind, int totalParams, FXPlatformConsumer<List<List<String>>> handler)
    {
        Function<List<AssistContentThreadSafe>, List<List<String>>> asHints = assists -> assists.stream()
            .filter(a -> a.getParams() != null && a.getParams().size() == totalParams)
            .map(a -> a.getParams().stream().map(this::makeHint).collect(Collectors.toList()))
            .collect(Collectors.toList());

        if (constructorKind == SuperThis.THIS)
        {
            handler.accept(asHints.apply(editor.getThisConstructors()));

        }
        else
        {
            // Assume super in other cases:
            editor.withSuperConstructors(assists -> handler.accept(asHints.apply(assists)));
        }
    }

    @OnThread(Tag.FXPlatform)
    public void withMethodHints(PosInSourceDoc pos, ExpressionSlot<?> completing, String methodName, CodeElement codeEl, FXPlatformConsumer<List<String>> handler)
    {
        editor.withCompletions(pos, completing, codeEl, assists ->
            handler.accept(assists.stream()
            .filter(a -> !Parser.isDummyName(a.getName()))
            .filter(a -> a.getName().equals(methodName))
            .filter(a -> a.getParams() != null)
            .map(a -> a.getName() + "(" + a.getParams().stream().map(p -> p.getUnqualifiedType() + " " + p.getFormalName()).collect(Collectors.joining(", ")) + ")"
                    + "\n\n" + JavaUtils.parseJavadoc(a.getJavadoc()).getHeader().trim())
            .collect(Collectors.toList())));
    }

    private static SuggestionList.SuggestionShown getRarity(AssistContentThreadSafe ac)
    {
        switch (ac.getKind())
        {
            case METHOD:
                if (ac.getDeclaringClass().equals("java.lang.Object"))
                {
                    // We mark most Object methods as rare:
                    switch (ac.getName())
                    {
                        case "equals":
                        case "toString":
                            return SuggestionList.SuggestionShown.COMMON;
                        default:
                            return SuggestionList.SuggestionShown.RARE;
                    }
                }
                // If not Object, mark as common:
                return SuggestionList.SuggestionShown.COMMON;
            default:
                return SuggestionList.SuggestionShown.COMMON;
        }
    }

    @Override
    public char getOpening(int selected)
    {
        return '(';
    }
}
