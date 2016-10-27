/*
 This file is part of the BlueJ program.
 Copyright (C) 2015,2016  Michael Kolling and John Rosenberg

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
package bluej.editor.stride;

import java.util.List;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * There's not really a good name for this, but essentially it is a subclass of Tab
 * which adds some methods which FXTabbedEditor needs to call on its contained tabs.
 *
 * The three subclasses of this class (at the moment) are FrameEditorTab for Stride classes,
 * MoeFXTab for Java classes, and WebTab for web browser (for documentation).
 */
@OnThread(Tag.FX)
abstract class FXTab extends Tab
{
    private final boolean showCatalogue;

    public FXTab(boolean showCatalogue)
    {
        this.showCatalogue = showCatalogue;
    }

    /**
     * Initialises any FX items which need to be done on the FX thread.
     */
    @OnThread(Tag.FXPlatform)
    abstract void initialiseFX();

    /**
     * When this tab gets shown as the selected tab, focus an appropriate item
     * within the tab.
     */
    abstract void focusWhenShown();

    /**
     * When the tab gets selected, this method is called to find out the menus whic
     * should be shown in the menubar (which is per-window, and thus shared between all
     * tabs)
     * @return The list of menus to use for the top menubar.
     */
    abstract List<Menu> getMenus();

    /**
     * Called to notify the tab of a new parent (null means tab has been closed)
     * @param parent The new window containing this tab, or null if the tab was closed.
     * @param partOfMove Whether this change is part of a move to another window
     */
    abstract void setParent(FXTabbedEditor parent, boolean partOfMove);

    /**
     * Called to get the parent window of this tab (as set by previous setParent call)
     */
    abstract FXTabbedEditor getParent();

    /**
     * Gets the web address showing in this tab.
     * @return The URL of the web address showing, or null if this is not a web tab.
     */
    abstract String getWebAddress();

    /**
     * Gets the Window title to show when this is the currently selected tab.
     */
    abstract ObservableStringValue windowTitleProperty();

    /**
     * Called when the tab has been selected, and the window has been focused.
     */
    public abstract void notifySelected();

    /**
     * Called when the tab was selected, but now is no longer selected.
     */
    public abstract void notifyUnselected();

    /**
     * Specifies whether the tab should show the frame catalogue.
     * Will not change over a tab's lifetime.
     */
    public final boolean shouldShowCatalogue()
    {
        return showCatalogue;
    }
}
