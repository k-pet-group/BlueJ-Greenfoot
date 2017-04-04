/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017 Michael Kölling and John Rosenberg
 
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

import bluej.Config;
import bluej.editor.moe.MoeActions;
import bluej.editor.moe.MoeEditor;
import bluej.editor.moe.ToolbarAction;
import bluej.utility.Debug;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SwingNodeFixed;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Created by neil on 13/04/2016.
 */
public @OnThread(Tag.FX) class MoeFXTab extends FXTab
{
    // suffixes for resources
    private final static String LabelSuffix = "Label";
    private final static String ActionSuffix = "Action";

    // -------- INSTANCE VARIABLES --------
    private boolean initialised = false;
    private final MoeEditor moeEditor;
    private MoeActions actions;
    private final TabMenuManager menuManager;
    private final StringProperty windowTitleProperty = new SimpleStringProperty();
    private SwingNode swingNode;
    private FXTabbedEditor parent = null;

    private Pane toolBar;
    private ComboBox<String> interfaceToggle;

    // Strings
    private final String implementationString = Config.getString("editor.implementationLabel");
    private final String interfaceString = Config.getString("editor.interfaceLabel");

    @OnThread(Tag.FXPlatform)
    public MoeFXTab(MoeEditor moeEditor, String windowTitle)
    {
        super(false);
        this.moeEditor = moeEditor;
        this.windowTitleProperty.set(windowTitle);
        actions = MoeActions.getActions(moeEditor, moeEditor.getSourcePane());

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
        initialiseFX();
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
        if (!initialised) {
            initialised = true;
            toolBar = createToolbar();
        }

        swingNode = new SwingNodeFixed();
        //JPanel panel = new JPanel();
        //panel.add(moeEditor);
        swingNode.setContent(moeEditor);

        VBox wrapper = new VBox();
        wrapper.getChildren().addAll(toolBar, swingNode);
        setContent(wrapper);

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

    // --------------------------------------------------------------------

    /**
     * Create the toolbar.
     *
     * @return The toolbar component, ready made.
     */
    private Pane createToolbar()
    {
        HBox toolbar = new HBox();

        String[] toolGroups = getResource("toolbar").split(" ");
        for (String group : toolGroups) {
            addToolbarGroup(toolbar, group);
        }

        toolbar.getChildren().add(new Separator());
        ComboBox<String> interfaceSelector = createInterfaceSelector();
        toolbar.getChildren().add(interfaceSelector);
        HBox.setHgrow(interfaceSelector, Priority.ALWAYS);

        return toolbar;
    }
    /**
     * Create the toolbar.
     */
    private void addToolbarGroup(Pane toolbar, String group)
    {
        String[] toolKeys = group.split(":");
        for (String toolKey : toolKeys) {
            toolbar.getChildren().add(createToolbarButton(toolKey));
//            if(!Config.isMacOSLeopard()) toolbar.add(Box.createHorizontalStrut(3));
        }
    }

    /**
     * Create a button on the toolbar.
     *
     * @param key  The internal key identifying the action and label
     * @return The button created
     */
    private Button createToolbarButton(String key)
    {
        final String label = Config.getString("editor." + key + LabelSuffix);
        Button button;

        String actionName = getResource(key + ActionSuffix);
        if (actionName == null) {
            actionName = key;
        }
        Action action = actions.getActionByName(actionName);

        if (action != null) {
            Action tbAction = new ToolbarAction(action, label);
            button = new Button(label);
            // The source of ActionEvent is not used in any of the buttons actions
            button.setOnAction(event -> SwingUtilities.invokeLater(() -> tbAction.actionPerformed(null)));
        }
        else {
            button = new Button("Unknown");
        }

        button.setText(actionName);

        if (action == null) {
            button.setDisable(true);
            Debug.message("Moe: action not found for button " + label);
        }

        if (MoeEditor.isNonReadmeAction(actionName) && !moeEditor.containsSourceCode()) {
            button.setDisable(true);
        }

        button.setFocusTraversable(false);
        // never get keyboard focus

        if (!Config.isMacOS()) {
            // on all other platforms than MacOS, the default insets needs to
            // be changed to make the buttons smaller
            //Insets margin = button.getMargin(); //////
            //button.setMargin(new Insets(margin.top, 3, margin.bottom, 3)); //////
        }
        else {
//            Utility.changeToMacButton(button);//////
        }
//        button.setFont(PrefMgr.getStandardFont());//////
        return button;
    }

    /**
     * Create a combo box for the toolbar
     */
    private ComboBox<String> createInterfaceSelector()
    {
        String[] choiceStrings = {implementationString, interfaceString};
        interfaceToggle = new ComboBox<>(FXCollections.observableArrayList(choiceStrings));

        interfaceToggle.setFocusTraversable(false);
//        interfaceToggle.setFont(PrefMgr.getStandardFont());//
//        interfaceToggle.setBorder(new EmptyBorder(2, 2, 2, 2)); ////
//        interfaceToggle.setForeground(envOpColour); ////
//        if (!Config.isRaspberryPi()) interfaceToggle.setOpaque(false); ////

        String actionName = "toggle-interface-view";
        Action action = actions.getActionByName(actionName);
        if (action != null) {           // should never be null...
            interfaceToggle.setOnAction(event -> SwingUtilities.invokeLater(() -> action.actionPerformed(null)));
        }
        else {
            interfaceToggle.setDisable(true);
            Debug.message("Moe: action not found: " + actionName);
        }
        if (!moeEditor.containsSourceCode()) {
            interfaceToggle.setDisable(true);
        }
        return interfaceToggle;
    }

    /**
     * Gets the resource attribute of the MoeEditor object
     */
    private String getResource(String name)
    {
        return Config.getPropString(name, null, Config.moeUserProps);
    }

    /**
     * Set the editor to display either the interface or the source code.
     *
     * @param interfaceStatus  If true, display class interface, otherwise source.
     */
    @OnThread(Tag.FXPlatform)
    public void showInterface(boolean interfaceStatus)
    {
        interfaceToggle.getSelectionModel().select(interfaceStatus ? 1 : 0);
    }

    /**
     * Check if the interface option is selected in the view toggle.
     *
     * @return true if interface is selected, false otherwise.
     */
    public boolean isInterfaceSelected()
    {
        return interfaceToggle.getSelectionModel().getSelectedItem().equals(interfaceString);
    }

    /**
     * Toggle the interface toggle, selecting the other option.
     *
     */
    public void toggleInterfaceMenu()
    {
        SingleSelectionModel<String> selection = interfaceToggle.getSelectionModel();
        selection.select(1 - selection.getSelectedIndex());
    }

    /**
     * Set the interface toggle as disabled.
     */
    public void disableInterfaceToggle()
    {
        interfaceToggle.setDisable(true);
    }
}
