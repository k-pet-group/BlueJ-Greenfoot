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
package bluej.stride.slots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.editor.fixes.SuggestionList;
import bluej.pkgmgr.target.role.Kind;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.StructuredCompletionCalculator;
import bluej.utility.Utility;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

public class TypeCompletionCalculator implements StructuredCompletionCalculator
{
    private final InteractionManager editor;
    private final Class<?> superType; // null means any
    private final Set<Kind> kinds = new HashSet<>();
    private List<AssistContentThreadSafe> acs;
    
    public TypeCompletionCalculator(InteractionManager editor)
    {
        this(editor, (Class<?>)null);
    }
    
    public TypeCompletionCalculator(InteractionManager editor, Class<?> superType)
    {
        this.editor = editor;
        this.superType = superType;
        if (superType == null)
            kinds.addAll(Arrays.asList(Kind.values()));
        else
            // Leave out enums and primitives:
            kinds.addAll(Arrays.asList(Kind.CLASS_FINAL, Kind.INTERFACE, Kind.CLASS_NON_FINAL));
    }
    
    public TypeCompletionCalculator(InteractionManager editor, Kind kind)
    {
        this.editor = editor;
        this.superType = null;
        this.kinds.add(kind);
    }

    private static final Map<String, List<String>> commonTypes = new HashMap<>();
    private static final Map<String, List<String>> boxedTypes = new HashMap<>();
    static {
        commonTypes.put(null, Arrays.asList("boolean", "char", "double", "int", "void"));
        commonTypes.put("greenfoot", Arrays.asList("Actor", "GreenfootImage", "GreenfootSound", "MouseInfo", "UserInfo", "World"));
        commonTypes.put("java.lang", Arrays.asList("Exception", "Object", "String"));
        commonTypes.put("java.util", Arrays.asList("ArrayList", "HashMap", "HashSet", "LinkedList", "List", "Map", "Set"));
        boxedTypes.put("java.lang", Arrays.asList("Boolean", "Character", "Double", "Float", "Integer"));
    }

    public static SuggestionList.SuggestionShown getRarity(AssistContentThreadSafe ac, boolean boxedAsCommon)
    {
        switch (ac.getKind())
        {
            case TYPE:
                if (boxedAsCommon && boxedTypes.containsKey(ac.getPackage()) && boxedTypes.get(ac.getPackage()).contains(ac.getName()) )
                {
                    return SuggestionList.SuggestionShown.COMMON;
                }
                else if (commonTypes.containsKey(ac.getPackage()))
                {
                    return commonTypes.get(ac.getPackage()).contains(ac.getName()) ? SuggestionList.SuggestionShown.COMMON : SuggestionList.SuggestionShown.RARE;
                }
                else
                {
                    return SuggestionList.SuggestionShown.COMMON;
                }
            default:
                throw new IllegalStateException();
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void withCalculatedSuggestionList(PosInSourceDoc pos, ExpressionSlot<?> completing,
                                             CodeElement codeEl, SuggestionListListener listener, String targetType, boolean completingStartOfSlot, FXPlatformConsumer<SuggestionList> handler) {

        HashSet<Kind> curKinds = new HashSet<>(kinds);
        // We only complete primitives at the start; they can't follow
        // package names or be in generic types
        if (!completingStartOfSlot)
            curKinds.remove(Kind.PRIMITIVE);
        editor.withTypes(superType, true, curKinds, acs -> {
            Platform.runLater(() ->
            {
                this.acs = new ArrayList<>(acs.values());
                this.acs.removeIf(ac -> !ac.accessibleFromPackage(""));
                this.acs.sort(Comparator.comparing(AssistContentThreadSafe::getName));
                List<SuggestionDetailsWithHTMLDoc> suggestions = Utility.mapList(this.acs, ac -> new SuggestionDetailsWithHTMLDoc(ac.getName(), getRarity(ac, !completingStartOfSlot), ac.getDocHTML()));
                SuggestionList suggestionDisplay = new SuggestionList(editor, suggestions, null, SuggestionList.SuggestionShown.COMMON, null, listener);
                handler.accept(suggestionDisplay);
            });
        });
    }
    
    @Override
    public String getName(int selected)
    {
        return acs.get(selected).getName();
    }

    @Override
    public List<String> getParams(int selected)
    {
        //TODO support generics
        return null;
    }

    @Override
    public char getOpening(int selected)
    {
        return '<';
    }
}
