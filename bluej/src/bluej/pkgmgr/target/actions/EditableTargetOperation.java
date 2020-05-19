/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020 Michael Kölling and John Rosenberg 
 
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
package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.EditableTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.javafx.AbstractOperation;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.input.KeyCombination;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.List;

@OnThread(Tag.FXPlatform)
public abstract class EditableTargetOperation extends AbstractOperation<Target>
{
    private final MenuItemOrder menuItemOrder;
    private final List<String> styleClasses;
    private final String label;

    public EditableTargetOperation(String identifier, Combine combine, KeyCombination shortcut, String label, MenuItemOrder menuItemOrder, String... styleClasses)
    {
        super(identifier, combine, shortcut);
        this.label = label;
        this.menuItemOrder = menuItemOrder;
        this.styleClasses = Arrays.asList(styleClasses);
    }

    @Override
    public final void activate(List<Target> targets)
    {
        for (Target target : targets)
        {
            if (target instanceof EditableTarget)
            {
                executeEditable((EditableTarget) target);
            }
        }
    }

    protected abstract void executeEditable(EditableTarget target);

    @Override
    public final List<ItemLabel> getLabels()
    {
        return List.of(new ItemLabel(new ReadOnlyStringWrapper(label), menuItemOrder));
    }

    @Override
    public List<String> getStyleClasses()
    {
        return styleClasses;
    }
}
