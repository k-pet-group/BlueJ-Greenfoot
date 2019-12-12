/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016,2019  Michael Kolling and John Rosenberg 

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
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.FlowActions;
import bluej.editor.flow.FlowActions.Category;
import bluej.editor.flow.FlowActions.FlowAbstractAction;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.DialogManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.ModifierValue;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * KeyBindingsPanel panel for the key bindings in preferences
 * @author Marion Zalk
 *
 */
@OnThread(Tag.FXPlatform)
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
    // The functions in the current category:
    private final ListView<String> functionList;
    // The keys for the current function:
    private final ListView<KeyCodeCombination> keyList;
    private final Text helpLabel;

    private FlowActions actions;     // The Flow action manager

    private Properties help;
    private List<ActionInfo> functions;     // all user functions
    private static Label errorLabel;

    private static class ActionInfo
    {
        private final String name;
        private final Category category;

        public ActionInfo(FlowAbstractAction action)
        {
            name = action.getName();
            category = action.getCategory();
        }
    }

    public void categoryMenuChanged()
    {
        Category selected = categoryMenu.getSelectionModel().getSelectedItem();

        functionList.getItems().setAll(functions.stream().filter(a -> a.category == selected).map(a -> a.name).collect(Collectors.toList()));
        clearKeyList();
        clearHelpText();
        addKeyButton.setDisable(true);
        delKeyButton.setDisable(true);
    }

    public KeyBindingsPanel(FXPlatformSupplier<Window> parent)
    {
        this.parent = parent;
        actions = new FlowActions(null);
        functions = Utility.mapList(actions.getAllActions(), ActionInfo::new);

        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50.0);
        getColumnConstraints().setAll(c, c);

        JavaFXUtil.addStyleClass(this, "prefmgr-key-panel");

        // create function list area
        BorderPane funcPanel = new BorderPane();
        functionList = new ListView<>();
        functionList.setEditable(false);
        funcPanel.setCenter(functionList);

        Label label = new Label(categoriesLabel + " ");
        categoryMenu = new ComboBox<>();
        HBox categoryHBox = new HBox(label, categoryMenu);
        categoryHBox.setAlignment(Pos.BASELINE_CENTER);
        BorderPane.setAlignment(categoryHBox, Pos.BASELINE_CENTER);
        funcPanel.setTop(categoryHBox);

        // create control area on right (key bindings and buttons)
        BorderPane controlPanel = new BorderPane();
        // create area for key bindings
        BorderPane keyPanel = new BorderPane();
        Label kLabel = new Label(keyLabel);
        JavaFXUtil.addStyleClass(kLabel, "key-header-label");
        keyPanel.setTop(kLabel);
        keyList = new ListView<>();
        keyList.setCellFactory(lv -> new TextFieldListCell<>(new StringConverter<KeyCodeCombination>()
        {
            @Override
            public String toString(KeyCodeCombination object)
            {
                return object.getDisplayText();
            }

            @Override
            public KeyCodeCombination fromString(String string)
            {
                // Won't be used as no editing:
                return null;
            }
        }));
        keyList.setEditable(false);
        keyPanel.setCenter(keyList);

        HBox keyButtonPanel = new HBox();
        keyButtonPanel.setAlignment(Pos.BASELINE_CENTER);
        JavaFXUtil.addStyleClass(keyButtonPanel, "key-bindings-buttons");
        addKeyButton = new Button(addKeyLabel);
        keyButtonPanel.getChildren().add(addKeyButton);

        delKeyButton = new Button(delKeyLabel);
        keyButtonPanel.getChildren().add(delKeyButton);

        defaultsButton = new Button(defaultsLabel);
        controlPanel.setTop(defaultsButton);
        BorderPane.setAlignment(defaultsButton, Pos.BASELINE_RIGHT);

        keyPanel.setBottom(keyButtonPanel);
        controlPanel.setCenter(keyPanel);

        // create help text area at bottom
        helpLabel=new Text();
        TextFlow textFlow = new TextFlow(helpLabel);
        textFlow.setMinHeight(80);
        controlPanel.setBottom(textFlow);

        JavaFXUtil.addStyleClass(funcPanel, "key-bindings-column");
        JavaFXUtil.addStyleClass(controlPanel, "key-bindings-column");

        add(funcPanel, 0, 0);
        add(controlPanel, 1, 0);
        updateDisplay();
    }

    @OnThread(Tag.FXPlatform)
    public void beginEditing(Project project) {
       
    }

    @OnThread(Tag.FXPlatform)
    public void commitEditing(Project project) {
       handleClose();
    }

    @OnThread(Tag.FXPlatform)
    public void revertEditing(Project project) {
        
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

    /**
     * Handle click on Defaults button.
     */
    private void handleDefaults()
    {
        int answer = DialogManager.askQuestionFX(parent.get(), "default-keys");
        if (answer == 0)
        {
            actions.setDefaultKeyBindings();
            handleFuncListSelect();
        }
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

        String currentAction = functionList.getSelectionModel().getSelectedItem();

        // display keys and help text

        updateKeyList(currentAction);
        String helpText = getHelpText(currentAction);
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
        if(!actions.save())
            DialogManager.showErrorFX(parent.get(), "cannot-save-keys");
    }
    /**
     * Handle click on Add Key button.
     */
    private void handleAddKey()
    {
        Optional<KeyCodeCombination> newKey = new KeyCaptureDialog().showAndWait();
        if (newKey.equals(Optional.of(ButtonType.CANCEL)))
        {
            return; 
        }
        if (newKey.isPresent())
        {
            String action = functionList.getSelectionModel().getSelectedItem();
            if (action != null)
            {
                FlowActions.addKeyCombinationForActionToAllEditors(newKey.get(), action);
                updateKeyList(action);
            }
        }
    }

    /**
     * Handle click on Delete Key button.
     */
    private void handleDelKey()
    {
        int index = keyList.getSelectionModel().getSelectedIndex();
        if(index == -1)
            return;             // deselection event - ignore

        actions.removeKeyStrokeBinding(keyList.getSelectionModel().getSelectedItem());
        updateKeyList(functionList.getSelectionModel().getSelectedItem());
    }

    /**
     * Display key bindings in the key list
     */
    private void updateKeyList(String action)
    {
        List<KeyCodeCombination> currentKeys = actions.getKeyStrokesForAction(action);
        if(currentKeys == null)
            clearKeyList();
        else {
            keyList.getItems().setAll(currentKeys);
            delKeyButton.setDisable(true);
        }
        addKeyButton.setDisable(false);
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
        help = Config.getFlowHelp();
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

    @OnThread(Tag.FXPlatform)
    private static class KeyCaptureDialog extends Dialog<KeyCodeCombination>
    {
        public KeyCaptureDialog()
        {
            TextField textField = new TextField();
            errorLabel = JavaFXUtil.withStyleClass(new Label(), "dialog-error-label");
            errorLabel.setText("");

            textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, (javafx.scene.input.KeyEvent e) -> {
                // Let escape trigger the cancel button
                // Also, ignore presses of modifier kyes
                if (e.getCode() != KeyCode.ESCAPE && e.getCode() != KeyCode.SHIFT && e.getCode() != KeyCode.CONTROL &&
                        e.getCode() != KeyCode.ALT && e.getCode() != KeyCode.ALT_GRAPH && e.getCode() != KeyCode.META &&
                        e.getCode() != KeyCode.COMMAND)
                {
                    if ( e.getCode().isLetterKey())
                    {
                        if (!(e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()))
                        {
                            errorLabel.setText("Single Character is not allowed");
                            RotateTransition animation = null;
                            animation = new RotateTransition(Duration.millis(70), errorLabel);
                            animation.setByAngle(5);
                            // Rotate, rotate back, rotate, rotate back:
                            animation.setAutoReverse(true);
                            animation.setCycleCount(4);
                            animation.play();
                            return;
                        }
                    }

                    // So, on Mac it seems that if we directly call setResult,
                    // the JVM crashes!  It seems this can be avoided by doing the setResult
                    // and hide in a runLater, so that's what we'll have to go with.
                    // Do not remove unless you've tested on Mac using the application bundle
                    // (JVM crash does not occur when running from IntelliJ, only from final built
                    // and signed RC .app bundle):
                    JavaFXUtil.runAfterCurrent(() -> {
                        setResult(new KeyCodeCombination(e.getCode(), mod(e.isShiftDown()),
                                mod(e.isControlDown()), mod(e.isAltDown()), mod(e.isMetaDown()), ModifierValue.ANY));
                        hide();
                    });
                    e.consume();
                }
            });
            VBox mainPanel=new VBox(new Label("Press a key, or Escape to cancel"), textField);
            getDialogPane().setContent(mainPanel);
            setOnShown(e -> textField.requestFocus());
            // Cancel button on the dialog, or otherwise can't close:
            Config.addDialogStylesheets(getDialogPane());
            getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
            mainPanel.getChildren().add(errorLabel);
        }

        private static ModifierValue mod(boolean down)
        {
            // We use up here, not any, because e.g. pressing Ctrl-Shift-S shouldn't trigger the Ctrl-S accelerator:
            return down ? ModifierValue.DOWN : ModifierValue.UP;
        }
    }
}
