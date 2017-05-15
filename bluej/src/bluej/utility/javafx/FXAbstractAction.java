/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2015,2016,2017  Michael Kolling and John Rosenberg

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
package bluej.utility.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;

import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * An FX Abstract Action to replace the Swing Action class
 *
 * @author Neil Brown
 */
@OnThread(Tag.FXPlatform)
public abstract class FXAbstractAction
{
    protected String name;
    private boolean hasMenuItem = false;
    private final BooleanProperty unavailable = new SimpleBooleanProperty(false);
    private final BooleanProperty disabled = new SimpleBooleanProperty(false);
    private final ObjectBinding<KeyCombination> accelerator;

    public FXAbstractAction(String name, KeyCombination accelerator)
    {
        this.name = name;
        this.accelerator = Bindings.createObjectBinding(() -> accelerator);
    }

    public abstract void actionPerformed();

    public FXAbstractAction bindEnabled(BooleanExpression enabled)
    {
        if (enabled != null)
            disabled.bind(enabled.not());
        return this;
    }

    public void setEnabled(boolean enabled)
    {
        if (disabled.isBound())
            disabled.unbind();
        disabled.set(!enabled);
    }

    public boolean isDisabled()
    {
        return disabled.get();
    }

    /**
     * There's two ways in which actions become disabled.  One is that their inherent
     * state in the editor changes, e.g. no undo if you haven't made any changes.
     * The other is that they become what we call unavailable, which happens when
     * the documentation view is showing, for example.  So their GUI enabled status
     * is actually the conjunction of being enabled and available.
     */
    public void setAvailable(boolean available)
    {
        unavailable.set(!available);
    }

    public String getName()
    {
        return name;
    }

    public KeyCombination getAccelerator()
    {
        return accelerator.get();
    }

    public Button makeButton()
    {
        Button button = new Button(name);
        setButtonAction(button);
        return button;
    }

    public void setButtonAction(ButtonBase button)
    {
        button.disableProperty().bind(disabled.or(unavailable));
        button.setOnAction(e -> actionPerformed());
    }

    /**
     * Note: calling this method indicates that the item will have
     * a menu item with the accelerator available for use.  Actions with a menu item
     * don't set a separate entry in the key map.  So don't call this
     * unless you're actually going to have the menu item available on the menu.
     */
    public MenuItem makeMenuItem()
    {
        MenuItem menuItem = new MenuItem(name);
        prepareMenuItem(menuItem);
        return menuItem;
    }

    public void prepareMenuItem(MenuItem menuItem)
    {
        prepareContextMenuItem(menuItem);
        menuItem.acceleratorProperty().bind(accelerator);
        hasMenuItem = true;
    }

    /**
     * Makes a MenuItem which will run this action, but without an accelerator.
     */
    public MenuItem makeContextMenuItem()
    {
        MenuItem menuItem = new MenuItem(name);
        prepareContextMenuItem(menuItem);
        return menuItem;
    }

    private void prepareContextMenuItem(MenuItem menuItem)
    {
        menuItem.disableProperty().bind(disabled.or(unavailable));
        menuItem.setOnAction(e -> actionPerformed());
    }

    public String toString()
    {
        return name;
    }

    /*public Category getCategory()
    {
        return category;
    }*/

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FXAbstractAction that = (FXAbstractAction) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public boolean hasMenuItem()
    {
        return hasMenuItem;
    }
}