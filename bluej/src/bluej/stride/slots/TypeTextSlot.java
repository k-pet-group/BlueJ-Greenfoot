/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import javafx.collections.FXCollections;
import javafx.scene.control.MenuItem;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SuggestedFollowUpDisplay;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

public class TypeTextSlot extends TextSlot<TypeSlotFragment>
{
    private boolean isReturnType;

    // We don't differentiate start and part, as that just gets too fiddly during editing:
    private SlotValueListener validCharactersListener = (slot, oldValue, newValue, parent) ->
        // Valid identifier, or the characters for generics, or dot for compound names:
        newValue.chars().allMatch(c -> Character.isJavaIdentifierPart(c) || c == '.' || c == '<' || c == '>' || c == ',' || c == '[' || c == ']');

    private static final List<FrameCatalogue.Hint> HINTS = Arrays.asList(
        new FrameCatalogue.Hint("int", "An integer (whole number)"),
        new FrameCatalogue.Hint("double", "A number value"),
        new FrameCatalogue.Hint("String", "Some text"),
        new FrameCatalogue.Hint("Actor", "A Greenfoot actor")
    );

    public <T extends Frame & CodeFrame<? extends CodeElement>>
    TypeTextSlot(InteractionManager editor, T frameParent, FrameContentRow row,
            CompletionCalculator completionCalculator, String stylePrefix)
    {
        super(editor, frameParent, frameParent, row, completionCalculator, stylePrefix, HINTS);
        addValueListener(validCharactersListener);
    }
    
    public TypeTextSlot(InteractionManager editor, Frame frameParent,
            CodeFrame<? extends CodeElement> codeFrameParent, FrameContentRow row,
            CompletionCalculator completionCalculator, String stylePrefix)
    {
        super(editor, frameParent, codeFrameParent, row, completionCalculator, stylePrefix, HINTS);
        addValueListener(validCharactersListener);
    }

    @Override
    public TypeSlotFragment createFragment(String content)
    {
        return new TypeSlotFragment(content, this);
    }

    @Override
    public void valueChangedLostFocus(String oldValue, String newValue)
    {
        // When return type, perform action to add/remove values from return items
        // Prompt for removal, don't prompt for add
        if (isReturnType)
        {
            if ((oldValue.equals("void") || oldValue.equals("")) && !(newValue.equals("void") || newValue.equals("")))
            {
                // Added a return type; need to go through and add empty slots for all returns that don't have them:
                for (Frame f : Utility.iterableStream(getParentFrame().getAllFrames()))
                {
                    if (f instanceof ReturnFrame)
                    {
                        ReturnFrame rf = (ReturnFrame) f;
                        rf.showValue();
                    }
                }
            }
            else if (!oldValue.equals("void") && newValue.equals("void"))
            {
                // Removed a return type; prompt about removing return values from all returns
                List<FXRunnable> removeActions = getParentFrame().getAllFrames()
                       .filter(f -> f instanceof ReturnFrame)
                       .map(f -> (ReturnFrame)f)
                       .map(rf -> rf.getRemoveFilledValueAction())
                       .filter(a -> a != null)
                       .collect(Collectors.toList());
                
                if (!removeActions.isEmpty())
                {
                    SuggestedFollowUpDisplay disp = new SuggestedFollowUpDisplay(editor, "Return type changed to void.  Would you like to remove return values from all return frames in this method?", () -> removeActions.forEach(FXRunnable::run));
                    disp.showBefore(getNode());
                }
            }
        }
    }
    

    public void markReturnType()
    {
        isReturnType = true;
    }
    
    @Override
    protected Map<TopLevelMenu, MenuItems> getExtraContextMenuItems()
    {
        final SortedMenuItem scanningItem = MenuItemOrder.GOTO_DEFINITION.item(new MenuItem("Scanning..."));
        scanningItem.getItem().setDisable(true);
                        
        return Collections.singletonMap(TopLevelMenu.VIEW, new MenuItems(FXCollections.observableArrayList()) {
            
            public void removeScanning()
            {
                if (items.size() == 1 && items.get(0) == scanningItem)
                    items.clear();
            }
            
            public void onShowing()
            {
                items.setAll(scanningItem);

                findLinks().forEach(l -> editor.searchLink(l, optLink -> {
                    removeScanning();
                    optLink.ifPresent(defLink -> {
                        items.add(MenuItemOrder.GOTO_DEFINITION.item(JavaFXUtil.makeMenuItem("Go to definition of \"" + defLink.getName() + "\"", defLink.getOnClick(), null)));
                    });
                }));
            }
            
            public void onHidden()
            {
                items.clear();
            }
        });
    }

    @Override
    public List<PossibleTypeLink> findLinks()
    {
        return Collections.singletonList(new PossibleTypeLink(getText(), 0, getText().length(), this));
    }
}
