package bluej.editor.stride;

import java.util.Collections;
import java.util.List;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.web.WebView;

import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A tab in the FXTabbedEditor which just contains a WebView.
 */
@OnThread(Tag.FX)
public class WebTab extends FXTab
{
    private final WebView browser;

    /**
     * Constructs a WebTab with a WebView in it
     * @param url The initial URL to display in the WebView.
     */
    public WebTab(String url)
    {
        browser = new WebView();
        browser.getEngine().load(url);
        setGraphic(getWebIcon());
        setContent(browser);
        textProperty().bind(browser.getEngine().titleProperty());
    }

    /**
     * Gets an icon to display next to web view tabs
     */
    private Node getWebIcon()
    {
        Label j = new Label("W");
        JavaFXUtil.addStyleClass(j, "icon-label");
        return j;
    }

    @Override
    void focusWhenShown()
    {
        // Nothing to do
    }

    @Override
    List<Menu> getMenus()
    {
        return Collections.emptyList(); //TODO
    }

    @Override
    String getWebAddress()
    {
        return browser.getEngine().getLocation();
    }

    @Override
    void initialiseFX()
    {
        //We do our initialisation in the constructor
    }

    @Override
    void setParent(FXTabbedEditor parent)
    {
        //We don't mind who our parent is
    }

    @Override
    ObservableStringValue windowTitleProperty()
    {
        // Take it from the tab title:
        return textProperty();
    }
}
