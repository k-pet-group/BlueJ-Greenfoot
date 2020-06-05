/*
 This file is part of the BlueJ program.
 Copyright (C) 2015,2016,2018,2020  Michael Kolling and John Rosenberg

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

import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * There's not really a good name for this, but essentially it is a subclass of Tab
 * which adds some methods which FXTabbedEditor needs to call on its contained tabs.
 *
 * The three subclasses of this class (at the moment) are FrameEditorTab for Stride classes,
 * FlowFXTab for Java classes, and WebTab for web browser (for documentation).
 */
@OnThread(Tag.FXPlatform)
abstract class FXTab extends Tab
{
    private final boolean showCatalogue;

    /**
     * Make an FXTab.
     * @param showCatalogue Whether to keep showing the frame cheat sheet (catalogue) if it is showing and we switch to this tab.
     *                      The catalogue is per-window, but it is hidden if this tab does not support it. 
     */
    public FXTab(boolean showCatalogue)
    {
        this.showCatalogue = showCatalogue;
        setOnSelectionChanged(e -> {
            JavaFXUtil.runAfterCurrent(this::focusWhenShown);
        });
    }

    /**
     * A helper method used by subclasses to create an ImageView for a given 
     * observable image expression (which may have a null Image in it)
     * @param imageExpression The image expression to watch for changes
     * @param maxSize The maximum width and height of the image
     * @param scaleUp If true, scale the image up to the max image width/height (while preserving aspect ratio).
     *                If false and the image is smaller than max size, do not scale it up.
     * @return An imageView which displays the latest image.
     */
    @OnThread(Tag.FX)
    protected static ImageView makeClassGraphicIcon(ObjectExpression<Image> imageExpression, int maxSize, boolean scaleUp)
    {
        ImageView imageView = new ImageView();
        FXConsumer<Image> imageChanged = image -> {
            if (image == null)
            {
                imageView.setFitWidth(0);
                imageView.setFitHeight(0);
            }
            else if (scaleUp)
            {
                imageView.setFitHeight(maxSize);
                imageView.setFitWidth(maxSize);
            }
            else
            {
                imageView.setFitHeight(Math.min(image.getHeight(), maxSize));
                imageView.setFitWidth(Math.min(image.getWidth(), maxSize));
            }
            
            imageView.setImage(image);
        };
        imageView.setPreserveRatio(true);
        imageChanged.accept(imageExpression.get());
        JavaFXUtil.addChangeListener(imageExpression, imageChanged);
        return imageView;
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
    @OnThread(Tag.FXPlatform)
    abstract void setParent(FXTabbedEditor parent, boolean partOfMove);

    /**
     * Called to get the parent window of this tab (as set by previous setParent call)
     */
    abstract FXTabbedEditor getParent();

    /**
     * Gets the web address showing in this tab.
     * @return The URL of the web address showing, or null if this is not a web tab.
     */
    @OnThread(Tag.FXPlatform)
    abstract String getWebAddress();

    /**
     * Gets the Window title to show when this is the currently selected tab.
     */
    abstract ObservableStringValue windowTitleProperty();

    /**
     * Called when the tab has been selected, and the window has been focused.
     */
    @OnThread(Tag.FXPlatform)
    public abstract void notifySelected();

    /**
     * Called when the tab was selected, but now is no longer selected.
     */
    @OnThread(Tag.FXPlatform)
    public abstract void notifyUnselected();

    /**
     * Specifies whether the tab supports showing the frame catalogue.
     * Will not change over a tab's lifetime.
     */
    public final boolean shouldShowCatalogue()
    {
        return showCatalogue;
    }

    /**
     * Is this tab showing a web view with a tutorial?
     */
    @OnThread(Tag.FX)
    public boolean isTutorial()
    {
        // By default this is false; it's overridden by tabs which do show the tutorial:
        return false;
    }
}
