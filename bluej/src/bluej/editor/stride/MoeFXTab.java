/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg 
 
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

import bluej.editor.moe.MoeEditor;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SwingNodeFixed;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Created by neil on 13/04/2016.
 */
public @OnThread(Tag.FX) class MoeFXTab extends FXTab
{
    private final MoeEditor moeEditor;
    private FXTabbedEditor parent = null;
    private final StringProperty windowTitleProperty = new SimpleStringProperty();
    private boolean initialised = false;
    private final TabMenuManager menuManager;
    private SwingNode swingNode;

    @OnThread(Tag.FXPlatform)
    public MoeFXTab(MoeEditor moeEditor, String windowTitle)
    {
        super(false);
        this.moeEditor = moeEditor;
        this.windowTitleProperty.set(windowTitle);
        menuManager = new TabMenuManager(this)
        {
            @Override
            List<Menu> getMenus()
            {
                updateMoveMenus();
                List<Menu> moeFXMenu = moeEditor.getFXMenu();
                if (moeFXMenu.get(0).getItems().get(0) != this.mainMoveMenu)
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
        swingNode.requestFocus();
        moeEditor.requestEditorFocus();
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
        swingNode = new SwingNodeFixed();
        //JPanel panel = new JPanel();
        //panel.add(moeEditor);
        swingNode.setContent(moeEditor);
        setContent(swingNode);

        setText("");
        Label titleLabel = new Label(windowTitleProperty.get());
        titleLabel.textProperty().bind(windowTitleProperty); // Is this right?
        HBox tabHeader = new HBox(titleLabel);
        tabHeader.setAlignment(Pos.CENTER);
        tabHeader.setSpacing(3.0);
        tabHeader.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.MIDDLE)
            {
                SwingUtilities.invokeLater(() ->
                    moeEditor.setVisible(false)
                );
            }
        });
        setGraphic(tabHeader);
    }

    @Override
    public void notifySelected()
    {
        SwingUtilities.invokeLater(() -> moeEditor.notifyVisibleTab(true));
    }

    @Override
    public void notifyUnselected()
    {
        SwingUtilities.invokeLater(() -> {
            moeEditor.notifyVisibleTab(false);
            moeEditor.cancelFreshState();
        });
    }

    @Override
    void setParent(FXTabbedEditor parent, boolean partOfMove)
    {
        this.parent = parent;
        moeEditor.setParent(parent, partOfMove);
        SwingUtilities.invokeLater(() -> moeEditor.notifyVisibleTab(false));
    }

    @Override
    ObservableStringValue windowTitleProperty()
    {
        return windowTitleProperty;
    }

    public MoeEditor getMoeEditor()
    {
        return moeEditor;
    }
}
