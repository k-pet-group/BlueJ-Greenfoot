/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2015,2016,2017,2019,2020  Michael Kolling and John Rosenberg

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

import bluej.Config;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Objects;


/**
 * An FX Abstract Action to replace the Swing Action class
 *
 * @author Neil Brown
 */
@OnThread(Tag.FXPlatform)
public abstract class FXAbstractAction
{
    private String name;

    private boolean hasMenuItem = false;
    private final BooleanProperty unavailable = new SimpleBooleanProperty(false);
    private final BooleanProperty disabled = new SimpleBooleanProperty(false);
    // Kept as field to avoid bind GC issues:
    private final BooleanBinding disabledOrUnavailable = disabled.or(unavailable);
    protected final ObjectProperty<KeyCombination> accelerator;
    private final Node buttonGraphic;

    protected FXAbstractAction(String name)
    {
        this(name, (KeyCombination)null);
    }

    protected FXAbstractAction(String name, KeyCombination accelerator)
    {
        this.name = name;
        this.accelerator = new SimpleObjectProperty<>(accelerator);
        this.buttonGraphic = null;
    }

    protected FXAbstractAction(String name, Node buttonGraphic)
    {
        this.name = name;
        this.accelerator = new SimpleObjectProperty<>(null);
        this.buttonGraphic = buttonGraphic;
    }

    protected FXAbstractAction(String name, Image buttonImage)
    {
        this(name, new ImageView(buttonImage));
    }

    public abstract void actionPerformed(boolean viaContextMenu);

    public void bindDisabled(BooleanExpression disabled)
    {
        if (disabled != null)
        {
            this.disabled.bind(disabled);
        }
        else
        {
            this.disabled.unbind();
        }
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
        button.disableProperty().bind(disabledOrUnavailable);
        button.setOnAction(e -> actionPerformed(false));
        if (buttonGraphic != null)
            button.setGraphic(buttonGraphic);
        return button;
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

    private void prepareMenuItem(MenuItem menuItem)
    {
        setMenuActionAndDisable(menuItem, false);
        boolean cmdPlusMinusOnMac = Config.isMacOS() && accelerator.get() != null &&
                (accelerator.get().equals(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN))
                    || accelerator.get().equals(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN))
                    || accelerator.get().equals(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.META_DOWN))
                    || accelerator.get().equals(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.META_DOWN)));
        // We don't set Cmd-+ or Cmd-- as a menu accelerator on Mac because a JavaFX bug
        // prevents them working as a menu item.  So we set them as a shortcut on the text pane
        // involved.  (See the caller of hasMenuItem).
        if (!cmdPlusMinusOnMac)
        {
            menuItem.acceleratorProperty().bind(accelerator);
            hasMenuItem = true;
        }
    }

    /**
     * Makes a MenuItem which will run this action, but without an accelerator.
     *
     * @param nameOverride If non-null, will be used as the text on the menu item.  If localised, caller is responsible for calling Config.getString
     */
    public MenuItem makeContextMenuItem(String nameOverride)
    {
        MenuItem menuItem = new MenuItem(nameOverride != null ? nameOverride : name);
        setMenuActionAndDisable(menuItem, true);
        return menuItem;
    }

    private void setMenuActionAndDisable(MenuItem menuItem, boolean contextMenu)
    {
        menuItem.disableProperty().bind(disabledOrUnavailable);
        menuItem.setOnAction(e -> actionPerformed(true));
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

    /**
     * Determines whether this action has a menu item which has been created, and which has the given
     * shortcut as an accelerator (compared using .equals method).  We don't check if the menu item
     * is actually visible, or enabled, etc, that's up to the caller.
     * @param shortcut
     * @return
     */
    public boolean hasMenuItemWithAccelerator(KeyCombination shortcut)
    {
        return hasMenuItem && Objects.equals(accelerator.get(), shortcut);
    }

    /**
     * changes the name of the action.
     * @param name the new name to be assigned
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
