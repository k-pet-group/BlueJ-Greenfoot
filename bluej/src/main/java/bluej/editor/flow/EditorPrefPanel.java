/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2015,2016,2019,2020,2021  Michael Kolling and John Rosenberg

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
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * editor settings
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class EditorPrefPanel extends VBox implements PrefPanelListener
{
    private TextField editorFontField;
    private CheckBox highlightingBox;
    private CheckBox autoIndentBox;
    private CheckBox closeCurlyBox;
    private CheckBox lineNumbersBox;
    private CheckBox matchBracketsBox;
    private CheckBox checkFileChangedOnDiskBox;
    private ScopeHighlightingPrefDisplay scopeHighlightingPrefDisplay;
    private TextField textFileExtensionsField; // stores the file extension list of files to be open as plain text by the editor
    private Button textFileExtensionsDefaultButton; // button to reset the list to the default value
    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public EditorPrefPanel()
    {
        JavaFXUtil.addStyleClass(this, "prefmgr-pref-panel");
        
        scopeHighlightingPrefDisplay=new ScopeHighlightingPrefDisplay();

        // Components related to the Java editor preferences
        List<Node> javaEditorPanel = new ArrayList<>();
        {
            GridPane topPanel=new GridPane();
            JavaFXUtil.addStyleClass(topPanel, "prefmgr-java-editor-top");
            
            editorFontField = new TextField();
            editorFontField.setPrefColumnCount(4);
            topPanel.add(PrefMgrDialog.labelledItem("prefmgr.edit.editorfontsize", editorFontField), 0, 0);
            autoIndentBox = new CheckBox(Config.getString("prefmgr.edit.autoindent"));
            topPanel.add(autoIndentBox, 1, 0);

            closeCurlyBox = new CheckBox(Config.getString("prefmgr.edit.closecurly"));
            topPanel.add(closeCurlyBox, 1, 1);
            
            highlightingBox = new CheckBox(Config.getString("prefmgr.edit.usesyntaxhilighting"));
            topPanel.add(highlightingBox, 0, 1);
            
            lineNumbersBox = new CheckBox(Config.getString("prefmgr.edit.displaylinenumbers"));
            topPanel.add(lineNumbersBox, 0, 2);
            
            matchBracketsBox= new CheckBox(Config.getString("prefmgr.edit.matchBrackets"));
            topPanel.add(matchBracketsBox, 1, 2);

            checkFileChangedOnDiskBox = new CheckBox(Config.getString("prefmgr.edit.checkfilechangeondisk"));
            topPanel.add(checkFileChangedOnDiskBox, 1, 3);
            
            //colour scope highlighter slider
            Pane bottomPanel = new HBox();
            JavaFXUtil.addStyleClass(bottomPanel, "prefmgr-java-scope-hbox");
            bottomPanel.getChildren().add(scopeHighlightingPrefDisplay.getHighlightStrengthSlider());            
            bottomPanel.getChildren().add(scopeHighlightingPrefDisplay.getColourPalette());
                        
            javaEditorPanel.add(topPanel);
            javaEditorPanel.add(PrefMgrDialog.headedVBox("prefmgr.edit.colortransparency", Arrays.asList(bottomPanel)));
        }
        getChildren().add(PrefMgrDialog.headedVBox("prefmgr.edit.editor.title", javaEditorPanel));

        // Components related to the text file editor
        List<Node> textEditorPanel = new ArrayList<>();
        {
            GridPane textTopPanel = new GridPane();
            JavaFXUtil.addStyleClass(textTopPanel, "prefmgr-text-editor-top");

            textFileExtensionsField = new TextField();
            textFileExtensionsField.setPrefColumnCount(20);
            textTopPanel.add(PrefMgrDialog.labelledItem("prefmgr.edit.textfileextensions", textFileExtensionsField), 0, 0);

            textFileExtensionsDefaultButton = new Button(Config.getString("prefmgr.edit.texteditor.defaultFileExtensions"));
            textFileExtensionsDefaultButton.setOnAction((event) -> {
                // Reset the default value of the text extensions
                textFileExtensionsField.setText(PrefMgr.DEFAULT_TEXTFILE_EXTENSIONS);
            });
            textTopPanel.add(textFileExtensionsDefaultButton,1,0);

            Label t = new Label(Config.getString("prefmgr.interface.text.restart"));
            textTopPanel.add(t, 0,1);
            textEditorPanel.add(textTopPanel);
        }
        getChildren().add(PrefMgrDialog.headedVBox("prefmgr.edit.editor.texteditor.title", textEditorPanel));
    }

    public void beginEditing(Project project)
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize().get()));
        highlightingBox.setSelected(PrefMgr.getFlag(PrefMgr.HIGHLIGHTING));
        closeCurlyBox.setSelected(PrefMgr.getFlag(PrefMgr.CLOSE_CURLY));
        autoIndentBox.setSelected(PrefMgr.getFlag(PrefMgr.AUTO_INDENT));
        lineNumbersBox.setSelected(PrefMgr.getFlag(PrefMgr.LINENUMBERS));
        matchBracketsBox.setSelected(PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS));
        checkFileChangedOnDiskBox.setSelected(PrefMgr.getFlag(PrefMgr.CHECK_DISKFILECHANGES));
        textFileExtensionsField.setText(PrefMgr.getEditorTextFileExtensionsString());
    }

    public void revertEditing(Project project)
    {
    }

    public void commitEditing(Project project)
    {
        String fontText = editorFontField.getText();

        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, highlightingBox.isSelected());
        PrefMgr.setFlag(PrefMgr.AUTO_INDENT, autoIndentBox.isSelected());
        PrefMgr.setFlag(PrefMgr.CLOSE_CURLY, closeCurlyBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINENUMBERS, lineNumbersBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MATCH_BRACKETS, matchBracketsBox.isSelected());
        PrefMgr.setFlag(PrefMgr.CHECK_DISKFILECHANGES, checkFileChangedOnDiskBox.isSelected());
        int strength = scopeHighlightingPrefDisplay.getStrengthValue();
        try {
            PrefMgr.setEditorFontSize(Integer.parseInt(fontText));
        }
        catch (NumberFormatException nfe) { }
        PrefMgr.setScopeHighlightStrength(strength);

        PrefMgr.setEditorTextFileExtensions(textFileExtensionsField.getText());
    }
}
