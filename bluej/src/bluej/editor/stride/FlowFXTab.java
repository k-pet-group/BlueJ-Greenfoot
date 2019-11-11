/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017,2018,2019 Michael Kölling and John Rosenberg
 
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

import bluej.editor.flow.FlowEditor;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;


/**
 * Created by neil on 13/04/2016.
 */
public @OnThread(Tag.FXPlatform) class FlowFXTab extends FXTab
{
    // -------- INSTANCE VARIABLES --------
    private boolean initialised = false;
    private final FlowEditor flowEditor;
    private final TabMenuManager menuManager;
    private final StringProperty windowTitleProperty = new SimpleStringProperty();
    private final SimpleObjectProperty<Image> classIcon;
    private FXTabbedEditor parent;

    /**
     * Make a Tab to contain a FlowEditor
     * @param flowEditor The FlowEditor to put in the tab
     * @param windowTitle The title of the tab
     */
    @OnThread(Tag.FXPlatform)
    public FlowFXTab(FlowEditor flowEditor, String windowTitle)
    {
        super(false);
        this.flowEditor = flowEditor;
        this.windowTitleProperty.set(windowTitle);
        this.classIcon = new SimpleObjectProperty<>();
        menuManager = new TabMenuManager(this)
        {
            @Override
            @OnThread(Tag.FXPlatform)
            List<Menu> getMenus()
            {
                updateMoveMenus();
                List<Menu> moeFXMenu = FlowFXTab.this.flowEditor.getFXMenu();
                if (moeFXMenu.size() > 0 && moeFXMenu.get(0).getItems().get(0) != this.mainMoveMenu)
                {
                    moeFXMenu.get(0).getItems().add(0, this.mainMoveMenu);
                    moeFXMenu.get(0).getItems().add(1, new SeparatorMenuItem());
                }
                return moeFXMenu;
            }
        };
        JavaFXUtil.addStyleClass(this, "moe-tab");
    }

    public void setWindowTitle(String title)
    {
        this.windowTitleProperty.set(title);
    }

    public void setErrorStatus(boolean errorStatus)
    {
        // We can't use pseudoclasses because Tab doesn't allow them to be changed,
        // so we must use full classes:
        if (errorStatus)
            JavaFXUtil.addStyleClass(this, "bj-tab-error");
        else
            getStyleClass().removeAll("bj-tab-error");
    }

    @Override
    void focusWhenShown()
    {
        flowEditor.requestEditorFocus();
    }

    @Override
    List<Menu> getMenus()
    {
        return menuManager.getMenus();
    }

    @Override
    FXTabbedEditor getParent()
    {
        return parent;
    }

    @Override
    String getWebAddress()
    {
        return null;
    }

    @Override
    void initialiseFX()
    {
        if (!initialised)
            initialised = true;
        setContent(flowEditor);

        setText("");
        Label titleLabel = new Label(windowTitleProperty.get());
        titleLabel.textProperty().bind(windowTitleProperty); // Is this right?
        HBox tabHeader = new HBox(titleLabel);
        tabHeader.getChildren().add(makeClassGraphicIcon(classIcon, 16, false));
        tabHeader.setAlignment(Pos.CENTER);
        tabHeader.setSpacing(3.0);
        tabHeader.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.MIDDLE)
            {
                flowEditor.setEditorVisible(false, false);
            }
        });
        setGraphic(tabHeader);
    }

    @Override
    public void notifySelected()
    {
        flowEditor.notifyVisibleTab(true);
    }

    @Override
    public void notifyUnselected()
    {
        flowEditor.notifyVisibleTab(false);
        flowEditor.cancelFreshState();
    }

    @Override
    void setParent(FXTabbedEditor parent, boolean partOfMove)
    {
        this.parent = parent;
        flowEditor.setParent(parent, partOfMove);
        flowEditor.notifyVisibleTab(false);
    }

    @Override
    ObservableStringValue windowTitleProperty()
    {
        return windowTitleProperty;
    }

    public FlowEditor getFlowEditor()
    {
        return flowEditor;
    }

    /**
     * Set the header image (in the tab header) for this editor
     * @param image The image to use (any size).
     */
    public void setHeaderImage(Image image)
    {
        classIcon.set(image);
    }
}
