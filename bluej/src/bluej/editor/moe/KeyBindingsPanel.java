/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.awt.Component;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.FocusManager;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import bluej.editor.moe.MoeActions.Category;
import bluej.editor.moe.MoeActions.MoeAbstractAction;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import bluej.Config;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXPlatformSupplier;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * KeyBindingsPanel panel for the key bindings in preferences
 * @author Marion Zalk
 *
 */
public class KeyBindingsPanel extends GridPane implements PrefPanelListener
{

    // -------- CONSTANTS --------
    static final String title = Config.getString("editor.functions.title");
    static final String close = Config.getString("close");
    static final String defaultsLabel = Config.getString("editor.functions.defaults");
    static final String categoriesLabel = Config.getString("editor.functions.categories");
    static final String keyLabel = Config.getString("editor.functions.keys");
    static final String addKeyLabel = Config.getString("editor.functions.addkey");
    static final String delKeyLabel = Config.getString("editor.functions.delkey");
    
    // -------- INSTANCE VARIABLES --------

    private final FXPlatformSupplier<Window> parent;
    private final Button defaultsButton;
    private final Button addKeyButton;
    private final Button delKeyButton;
    private final ComboBox<Category> categoryMenu;
    private final ListView<MoeAbstractAction> functionList;
    private final ListView<String> keyList;
    private final Text helpLabel;

    private MoeActions actions;     // The Moe action manager
    private MoeAbstractAction currentAction;       // the action currently selected
    private List<KeyCombination> currentKeys;    // key strokes currently displayed

    private Properties help;
    private List<MoeAbstractAction> functions;     // all user functions

    public void categoryMenuChanged()
    {
        Category selected = categoryMenu.getSelectionModel().getSelectedItem();

        functionList.getItems().setAll(functions.stream().filter(a -> a.getCategory() == selected).collect(Collectors.toList()));
        clearKeyList();
        clearHelpText();
        addKeyButton.setDisable(true);
        delKeyButton.setDisable(true);
        currentAction = null;
        currentKeys = null;

    }

    public KeyBindingsPanel(FXPlatformSupplier<Window> parent)
    {
        this.parent = parent;
        actions = MoeActions.getActions(null);
        functions = actions.getAllActions();

        // create function list area
        BorderPane funcPanel = new BorderPane();
        functionList = new ListView<>();
        funcPanel.setCenter(functionList);

        Label label = new Label(categoriesLabel);
        categoryMenu = new ComboBox<>();
        funcPanel.setTop(new VBox(label, categoryMenu));

        // create control area on right (key bindings and buttons)
        BorderPane controlPanel = new BorderPane();
        // create area for key bindings
        BorderPane keyPanel = new BorderPane();
        Label kLabel = new Label(keyLabel);
        keyPanel.setTop(kLabel);
        keyList = new ListView<>();
        //MOEFX
        //keyList.setPrototypeCellValue("shift-ctrl-delete");
        keyPanel.setCenter(keyList);

        VBox keyButtonPanel = new VBox();
        addKeyButton = new Button(addKeyLabel);
        keyButtonPanel.getChildren().add(addKeyButton);

        delKeyButton = new Button(delKeyLabel);
        keyButtonPanel.getChildren().add(delKeyButton);

        defaultsButton = new Button(defaultsLabel);
        keyButtonPanel.getChildren().add(defaultsButton);
        keyPanel.setBottom(keyButtonPanel);
        controlPanel.setCenter(keyPanel);

        // create help text area at bottom
        helpLabel=new Text();
        controlPanel.setBottom(new TextFlow(helpLabel));

        add(funcPanel, 0, 0);
        add(controlPanel, 1, 0);
        updateDisplay();
    }

    @OnThread(Tag.FXPlatform)
    public void beginEditing() {
       
    }

    @OnThread(Tag.FXPlatform)
    public void commitEditing() {
       SwingUtilities.invokeLater(() -> handleClose());
    }

    @OnThread(Tag.FXPlatform)
    public void revertEditing() {
        
    }

    /**
     * 
     */
    public void updateDisplay()
    {
        JavaFXUtil.addChangeListenerPlatform(categoryMenu.getSelectionModel().selectedIndexProperty(), i -> categoryMenuChanged());
        JavaFXUtil.addChangeListenerPlatform(functionList.getSelectionModel().selectedIndexProperty(), i -> handleFuncListSelect());
        JavaFXUtil.addChangeListenerPlatform(keyList.getSelectionModel().selectedIndexProperty(), i -> handleKeyListSelect());

        defaultsButton.setOnAction(e -> handleDefaults());
        addKeyButton.setOnAction(e -> handleAddKey());
        delKeyButton.setOnAction(e -> handleDelKey());

        openHelpFile();
        categoryMenu.getItems().setAll(Category.values());
        categoryMenu.getSelectionModel().selectFirst();
    }

    class KeyCatcher extends FocusManager {

        public void processKeyEvent(Component focusedComponent, KeyEvent e) 
        { 
            if(e.getID() != KeyEvent.KEY_PRESSED)
                return;

            int keyCode = e.getKeyCode();

            if(keyCode == KeyEvent.VK_CAPS_LOCK ||    // the keys we want to ignore...
                    keyCode == KeyEvent.VK_SHIFT ||
                    keyCode == KeyEvent.VK_CONTROL ||
                    keyCode == KeyEvent.VK_META ||
                    keyCode == KeyEvent.VK_ALT ||
                    keyCode == KeyEvent.VK_ALT_GRAPH ||
                    keyCode == KeyEvent.VK_COMPOSE ||
                    keyCode == KeyEvent.VK_NUM_LOCK ||
                    keyCode == KeyEvent.VK_SCROLL_LOCK ||
                    keyCode == KeyEvent.VK_UNDEFINED
            )
                return;

            if(currentAction == null)
                Debug.message("FunctionDialog: currentAction is null...");
            else {
                KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
                if(isPrintable(key, e))
                    helpLabel.setText(getHelpText("cannot-redefine"));
                else {
                    //MOEFX
                    //actions.setKeyCombinationForAction(key, currentAction);
                    handleFuncListSelect();
                }
            }
            e.consume();
            removeKeyListener();
        }

        private boolean isPrintable(KeyStroke key, KeyEvent e)
        {
            // all control and alt keys are non-printable
            int modifiers = key.getModifiers();
            if(modifiers != 0 && modifiers != Event.SHIFT_MASK)
                return false;

            // action keys are non-printable
            if(e.isActionKey())
                return false;

            // some action keys that the above function not recognises
            int keyCode = e.getKeyCode();
            if(keyCode == KeyEvent.VK_BACK_SPACE ||
                    keyCode == KeyEvent.VK_DELETE ||
                    keyCode == KeyEvent.VK_ENTER ||
                    keyCode == KeyEvent.VK_TAB ||
                    keyCode == KeyEvent.VK_ESCAPE)
                return false;

            // otherwise it's printable
            return true;
        }

        public void focusNextComponent(Component c) {}
        public void focusPreviousComponent(Component c) {}
    }

    /**
     * Handle click on Defaults button.
     */
    private void handleDefaults()
    {
        Platform.runLater(() -> {
            int answer = DialogManager.askQuestionFX(parent.get(), "default-keys");
            if (answer == 0)
            {
                SwingUtilities.invokeLater(() -> {
                    actions.setDefaultKeyBindings();
                    handleFuncListSelect();
                });
            }
        });
    }

    /**
     * Handle click in functions list.
     */
    private void handleFuncListSelect()
    {
        int index = functionList.getSelectionModel().getSelectedIndex();
        if(index == -1)
            return; // deselection event - ignore

        // find selected action

        currentAction = functionList.getSelectionModel().getSelectedItem();

        // display keys and help text

        updateKeyList(currentAction);
        String helpText = getHelpText(currentAction.getName());
        helpLabel.setText(helpText);
    }

    /**
     * Handle click in key bindings list.
     */
    private void handleKeyListSelect()
    {
        delKeyButton.setDisable(false);
    }

    /**
     * Handle click on Close button.
     */
    private void handleClose()
    {
        removeKeyListener();
        if(!actions.save())
            Platform.runLater(() -> DialogManager.showErrorFX(parent.get(), "cannot-save-keys"));
        setVisible(false);
    }
    /**
     * Handle click on Add Key button.
     */
    private void handleAddKey()
    {
        helpLabel.setText(getHelpText("press-key"));
        addKeyListener();
    }

    /**
     * Handle click on Delete Key button.
     */
    private void handleDelKey()
    {
        if(currentKeys == null)
            return;             // something went wrong here...

        int index = keyList.getSelectionModel().getSelectedIndex();
        if(index == -1)
            return;             // deselection event - ignore

        //MOEFX
        //actions.removeKeyStrokeBinding(currentKeys[index]);
        updateKeyList(currentAction);
    }

    /**
     * Display key bindings in the key list
     */
    private void updateKeyList(MoeAbstractAction action)
    {
        //MOEFX
        currentKeys = actions.getKeyStrokesForAction(action);
        if(currentKeys == null)
            clearKeyList();
        else {
            List<String> keyStrings = getKeyStrings(currentKeys);
            keyList.getItems().setAll(keyStrings);
            delKeyButton.setDisable(true);
        }
        addKeyButton.setDisable(false);
    }

    /**
     * Translate KeyStrokes into String representation.
     */
    private List<String> getKeyStrings(List<KeyCombination> keys)
    {
        return Utility.mapList(keys, KeyCombination::getDisplayText);
    }

    private void clearKeyList()
    {
        keyList.getItems().clear();
    }

    private void clearHelpText()
    {
        helpLabel.setText(null);
    }

    private void openHelpFile()
    {
        help = Config.getMoeHelp();
    }

    private String getHelpText(String function)
    {
        if(help == null)
            return null;
        String helpText=help.getProperty(function);
        //need to check if null first as some items do not have helpText/helpText is empty
        if (helpText!=null && helpText.length()>0) {
            //remove the /n from the text 
            helpText=helpText.replaceAll("\n", "");
            helpText=helpText.trim();
        }
        return helpText;
    }

    private void addKeyListener()
    {
        FocusManager.setCurrentManager(new KeyCatcher());
    }

    private void removeKeyListener()
    {
    }

}
