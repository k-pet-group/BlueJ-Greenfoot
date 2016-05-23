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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;

import javafx.beans.binding.StringExpression;
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
import threadchecker.OnThread;
import threadchecker.Tag;

abstract class TypeTextSlot extends TextSlot//<TypeSlotFragment>
{
    public TypeTextSlot(InteractionManager editor, Frame frameParent, CodeFrame codeFrameParent, FrameContentRow row, CompletionCalculator completionCalculator, String stylePrefix, List list)
    {
        super(editor, frameParent, codeFrameParent, row, completionCalculator, stylePrefix, list);
    }
    /*TODOTYPESLOT

    private static final List<FrameCatalogue.Hint> HINTS = Arrays.asList(
        new FrameCatalogue.Hint("int", "An integer (whole number)"),
        new FrameCatalogue.Hint("double", "A number value"),
        new FrameCatalogue.Hint("String", "Some text"),
        new FrameCatalogue.Hint("Actor", "A Greenfoot actor")
    );

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
    */
}
