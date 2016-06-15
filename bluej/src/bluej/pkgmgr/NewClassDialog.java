/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.Config;
import bluej.extensions.SourceType;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.javafx.HorizontalRadio;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.DialogPaneAnimateError;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
class NewClassDialog extends Dialog<NewClassDialog.NewClassInfo>
{
    /** The radio buttons for class, interface, enum, etc */
    private final ToggleGroup templateButtons;
    /** Which radio button is associated with which template */
    private final Map<RadioButton, TemplateInfo> templates = new IdentityHashMap<>();
    
    /** The buttons for the source language (Java/Stride) */
    private final HorizontalRadio<SourceType> language;

    /** stores restricted windows class filenames */
    private static List<String> windowsRestrictedWords;
    /** The field with the class name */
    private final TextField nameField;
    /** The dialog pane */
    private final DialogPaneAnimateError dialogPane;
    /** Keeps track of whether the field has yet been non-blank.
     *  We don't show an error for empty class name if the class name
     *  has always been empty (unless the user mouses over OK)
     */
    private boolean fieldHasHadContent = false;
    /**
     * The label with the error message.
     */
    private final Label errorLabel;

    /**
     * The information selected in the dialog: class name,
     * template name and source type.
     */
    @OnThread(Tag.Any)
    public static class NewClassInfo
    {
        public final String className;
        public final String templateName;
        public final SourceType sourceType;

        private NewClassInfo(String className, String templateName, SourceType sourceType)
        {
            this.templateName = templateName;
            this.className = className;
            this.sourceType = sourceType;
        }
    }
    
    
    /**
     * Construct a NewClassDialog.
     */
    public NewClassDialog(Window parent, SourceType defaultSourceType)
    {
        setTitle(Config.getString("pkgmgr.newClass.title"));
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        errorLabel = JavaFXUtil.withStyleClass(new Label(), "dialog-error-label");
        dialogPane = new DialogPaneAnimateError(errorLabel, () -> updateOKButton(true));
        setDialogPane(dialogPane);
        Config.addDialogStylesheets(getDialogPane());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        VBox mainPanel = new VBox();
        JavaFXUtil.addStyleClass(mainPanel, "new-class-dialog");
        
        nameField = new TextField();
        nameField.setPromptText(Config.getString("pkgmgr.newClass.prompt"));
        JavaFXUtil.addChangeListenerPlatform(nameField.textProperty(), s -> {
            hideError();
            updateOKButton(false);
        });
        
        HBox nameBox = new HBox(new Label(Config.getString("pkgmgr.newClass.label")), nameField);
        JavaFXUtil.addStyleClass(nameBox, "new-class-dialog-hbox");
        nameBox.setAlignment(Pos.BASELINE_LEFT);

        mainPanel.getChildren().add(nameBox);

        language = new HorizontalRadio(Arrays.asList(SourceType.Java, SourceType.Stride));
        language.select(defaultSourceType);
        
        HBox langBox = new HBox();
        JavaFXUtil.addStyleClass(langBox, "new-class-dialog-hbox");
        langBox.getChildren().add(new Label(Config.getString("pkgmgr.newClass.lang")));
        langBox.getChildren().addAll(language.getButtons());
        langBox.setAlignment(Pos.BASELINE_LEFT);
        mainPanel.getChildren().add(langBox);
        
        mainPanel.getChildren().add(new Label(Config.getString("pkgmgr.newClass.classType")));

        templateButtons = new ToggleGroup();
        addClassTypeButtons(parent, mainPanel);
        mainPanel.getChildren().add(errorLabel);

        JavaFXUtil.addChangeListenerPlatform(language.selectedProperty(), language -> {
            templates.forEach((radio, info) -> radio.setDisable(!info.sourceTypes.contains(language)));
            hideError();
            updateOKButton(false);
        });
        
        getDialogPane().setContent(mainPanel);
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK)
            {
                return new NewClassInfo(nameField.getText().trim(), templates.get(templateButtons.getSelectedToggle()).name, language.selectedProperty().get());
            }
            else
                return null;
        });
        
        updateOKButton(false);
        setOnShown(e -> Platform.runLater(nameField::requestFocus));
    }

    /**
     * Each template has a name, and a set of source types for which that
     * template is available.
     */
    @OnThread(Tag.Any)
    private static class TemplateInfo
    {
        private final String name;
        private final Set<SourceType> sourceTypes = new HashSet<>();

        public TemplateInfo(String name)
        {
            this.name = name;
        }
        public TemplateInfo(String name, SourceType sourceType)
        {
            this.name = name;
            sourceTypes.add(sourceType);
        }
    }

    /**
     * Add the class type buttons (defining the class template to be used
     * to the panel. The templates are defined in the "defs" file.
     */
    private void addClassTypeButtons(Window parent, Pane panel)
    {
        String templateSuffix = ".tmpl";
        int suffixLength = templateSuffix.length();

        // first, get templates out of defined templates from bluej.defs
        // (we do this rather than usign the directory only to be able to
        // force an order on the templates.)

        String templateString = Config.getPropString("bluej.classTemplates");

        StringTokenizer tokenizer = new StringTokenizer(templateString);

        List<TemplateInfo> templates = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            templates.add(new TemplateInfo(tokenizer.nextToken()));
        }

        // next, get templates from files in template directory and
        // merge them in

        File templateDir = Config.getClassTemplateDir();
        if (!templateDir.exists())
        {
            DialogManager.showErrorFX(parent, "error-no-templates");
        }
        else
        {
            for (String file : templateDir.list())
            {
                if (file.endsWith(templateSuffix))
                {
                    String templateName = file.substring(0, file.length() - suffixLength);
                    SourceType sourceType = SourceType.Java;
                    if (templateName.endsWith("Stride"))
                    {
                        templateName = templateName.substring(0, templateName.length() - "Stride".length());
                        sourceType = SourceType.Stride;
                    }
                    String finalTemplateName = templateName;
                    TemplateInfo template = templates.stream().filter(t -> t.name.equals(finalTemplateName)).findFirst().orElse(null);
                    if (template != null)
                    {
                        template.sourceTypes.add(sourceType);
                    }
                    else
                    {
                        templates.add(new TemplateInfo(templateName, sourceType));
                    }
                }
            }
        }        

        // Create a radio button for each template found
        boolean first = true;
        for (TemplateInfo template : templates)
        {
            String label = Config.getString("pkgmgr.newClass." + template.name, template.name);
            RadioButton button = new RadioButton(label);
            if (first)
                button.setSelected(true); // select first
            button.setToggleGroup(templateButtons);
            this.templates.put(button, template);
            panel.getChildren().add(button);
            first = false;
        }
        
        JavaFXUtil.addChangeListenerPlatform(templateButtons.selectedToggleProperty(), selected -> updateOKButton(false));
    }

    /**
     * Enable/disable the OK button, and set the error label
     * 
     * @param force True if we want to display a message for the blank class name,
     *              even if it has been blank since the dialog was shown (we do
     *              this when the user mouses over OK).
     */
    private void updateOKButton(boolean force)
    {
        String newClassName = nameField.getText().trim();
        fieldHasHadContent |= !newClassName.equals("");
        Toggle selectedToggle = templateButtons.getSelectedToggle();
        TemplateInfo info = this.templates.get(selectedToggle);
        boolean enable = false;
        if (info == null)
            showError("No class type selected", false);
        else if (((RadioButton)selectedToggle).isDisabled() || !info.sourceTypes.contains(language.selectedProperty().get()))
            showError("Type not available for " + language.selectedProperty().get(), false);
        else if (!JavaNames.isIdentifier(newClassName))
        {
            if (fieldHasHadContent || force)
                showError("Not valid Java class name", true);
        }
        else if (isWindowsRestrictedWord(newClassName))
        {
            if (fieldHasHadContent || force)
                showError("Windows restricted word", true);
        }else
        {
            hideError();
            enable = true;
        }
        setOKEnabled(enable);
    }
    
    private void hideError()
    {
        errorLabel.setText("");
        JavaFXUtil.setPseudoclass("bj-dialog-error", false, nameField);
    }
    
    private void showError(String error, boolean problemIsName)
    {
        // show error, highlight field red if problem is name:
        errorLabel.setText(error);
        JavaFXUtil.setPseudoclass("bj-dialog-error", problemIsName, nameField);
    }

    /**
     * Sets the OK button of the dialog to be enabled (pass true) or not (pass false)
     */
    private void setOKEnabled(boolean okEnabled)
    {
        dialogPane.getOKButton().setDisable(!okEnabled);
    }

    
    /**
     * Tests for restricted class names (case insensitive)
     * @param fileName potential class name
     * @return true if restricted word
     */
    private boolean isWindowsRestrictedWord(String fileName)
    {
        initialiseRestrictedWordList();
        if (windowsRestrictedWords.contains(fileName.toUpperCase())){
            return true;
        }
        return false;
    }
    
    /**
     * Initialises the list of restricted words
     */
    private void initialiseRestrictedWordList()
    {
        if (windowsRestrictedWords==null){
            windowsRestrictedWords=new ArrayList<String>();
            windowsRestrictedWords.add("CON");
            windowsRestrictedWords.add("PRN");
            windowsRestrictedWords.add("AUX");
            windowsRestrictedWords.add("NUL");
            windowsRestrictedWords.add("COM1");
            windowsRestrictedWords.add("COM2");
            windowsRestrictedWords.add("COM3");
            windowsRestrictedWords.add("COM4");
            windowsRestrictedWords.add("COM5");
            windowsRestrictedWords.add("COM6");
            windowsRestrictedWords.add("COM7");
            windowsRestrictedWords.add("COM8");
            windowsRestrictedWords.add("COM9");
            windowsRestrictedWords.add("LPT1");
            windowsRestrictedWords.add("LPT2");
            windowsRestrictedWords.add("LPT3");
            windowsRestrictedWords.add("LPT4");
            windowsRestrictedWords.add("LPT5");
            windowsRestrictedWords.add("LPT6");
            windowsRestrictedWords.add("LPT7");
            windowsRestrictedWords.add("LPT8");
            windowsRestrictedWords.add("LPT9");
        }
    }

}
