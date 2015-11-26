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
 * The two subclasses of this class (at the moment) are FrameEditorTab for Stride classes,
 * and WebTab for web browser (for documentation).
 */
@OnThread(Tag.FX)
abstract class FXTab extends Tab
{
    /**
     * Initialises any FX items which need to be done on the FX thread
     * @param scene The scene in which this tab will be placed.
     */
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
     */
    abstract void setParent(FXTabbedEditor parent);

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
}
