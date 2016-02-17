/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bluej.utility.Utility;
import javafx.scene.control.TextField;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.slots.ExpressionCompletionCalculator;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.stride.slots.SuggestionList.SuggestionListListener;
import bluej.utility.javafx.FXConsumer;

public class TypeCompletionCalculator implements CompletionCalculator
{
    private final InteractionManager editor;
    private final Class<?> superType; // null means any
    private final Set<InteractionManager.Kind> kinds = new HashSet<>();
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
            kinds.addAll(Arrays.asList(InteractionManager.Kind.values()));
        else
            // Leave out enums and primitives:
            kinds.addAll(Arrays.asList(InteractionManager.Kind.CLASS_FINAL, InteractionManager.Kind.INTERFACE, InteractionManager.Kind.CLASS_NON_FINAL));
    }
    
    public TypeCompletionCalculator(InteractionManager editor, InteractionManager.Kind kind)
    {
        this.editor = editor;
        this.superType = null;
        this.kinds.add(kind);
    }

    @Override
    public void withCalculatedSuggestionList(PosInSourceDoc pos,
                                             CodeElement codeEl, SuggestionListListener listener, FXConsumer<SuggestionList> handler) {
        
        editor.withTypes(superType, true, kinds, acs -> {
            this.acs = new ArrayList<>(acs);
            this.acs.removeIf(ac -> !ac.accessibleFromPackage(""));
            List<SuggestionDetailsWithHTMLDoc> suggestions = Utility.mapList(this.acs, ac -> new SuggestionDetailsWithHTMLDoc(ac.getName(), ExpressionCompletionCalculator.getRarity(ac), ac.getDocHTML()));
            SuggestionList suggestionDisplay = new SuggestionList(editor, suggestions, null, SuggestionList.SuggestionShown.COMMON, null, listener);
            handler.accept(suggestionDisplay);
        });
    }

    @Override
    public boolean execute(TextField field, int selected, int startOfCurWord)
    {
        if (selected == -1)
            return false;
        
        AssistContentThreadSafe a = acs.get(selected);
        field.setText(a.getName());
        
        return true;
    }

}
