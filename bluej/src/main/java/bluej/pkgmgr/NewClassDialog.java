/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2016,2017,2019,2022,2025  Michael Kolling and John Rosenberg
 
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
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javafx.collections.ObservableList;

import bluej.prefmgr.PrefMgr;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.javafx.HorizontalRadio;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.DialogPaneAnimateError;
import javafx.util.StringConverter;
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

    private final ComboBox<ClassContent> classContent;

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
    public static record NewClassInfo(String className, String templateName, SourceType sourceType, ClassContent classContent)
    {
    }

    /**
     * Construct a NewClassDialog.
     */
    public NewClassDialog(Window parent, SourceType defaultSourceType)
    {
        setTitle(Config.getString("pkgmgr.newClass.title"));
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
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

        language = new HorizontalRadio(Arrays.asList(SourceType.Java, SourceType.Stride, SourceType.Kotlin));
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

        classContent = new ComboBox<>();
        classContent.getItems().addAll(ClassContent.values());
        classContent.getSelectionModel().select(PrefMgr.getFlag(PrefMgr.NEW_CLASS_FULL_CONTENT) ? ClassContent.FULL : ClassContent.EMPTY);
        classContent.setConverter(new StringConverter<ClassContent>() {
            @Override
            public String toString(ClassContent classContent) {
                return switch (classContent) {
                    case FULL -> Config.getString("pkgmgr.newClass.content.full");
                    case EMPTY -> Config.getString("pkgmgr.newClass.content.empty");
                };
            }

            @Override
            public ClassContent fromString(String s) {
                // Won't be used because it's not an editable text field:
                return null;
            }
        });
        HBox classContentBox = new HBox(new Label(Config.getString("pkgmgr.newClass.content")), classContent);
        classContentBox.setSpacing(10);
        classContentBox.setAlignment(Pos.BASELINE_LEFT);
        mainPanel.getChildren().add(classContentBox);


        mainPanel.getChildren().add(errorLabel);
        JavaFXUtil.addChangeListenerPlatform(templateButtons.selectedToggleProperty(), toggle -> {
            hideError();
            updateOKButton(false);
        });

        JavaFXUtil.addChangeListenerPlatform(language.selectedProperty(), language -> {
            hideError();
            updateOKButton(false);
            resortTemplateButtons(templateButtons);
        });
        
        getDialogPane().setContent(mainPanel);
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK)
            {
                // Save the classContent preference:
                PrefMgr.setFlag(PrefMgr.NEW_CLASS_FULL_CONTENT, classContent.getSelectionModel().getSelectedItem() == ClassContent.FULL);

                return new NewClassInfo(nameField.getText().trim(), templates.get(templateButtons.getSelectedToggle()).name, language.selectedProperty().get(), classContent.getSelectionModel().getSelectedItem());
            }
            else {
                return null;
            }
        });
        
        updateOKButton(false);
        resortTemplateButtons(templateButtons);
        setOnShown(e -> Platform.runLater(nameField::requestFocus));
        if (Config.makeDialogsResizable())
        {
            setResizable(true);
        }
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

        public TemplateInfo(String name, SourceType sourceType)
        {
            this.name = name;
            sourceTypes.add(sourceType);
        }
    }

    /**
     * A list of Templates guarantee uniqueness and order
     */
    @OnThread(Tag.Any)
    private static class TemplatesList
    {
        private final List<TemplateInfo> templates = new ArrayList<>();

        public void addTemplate(String name, SourceType sourceType)
        {
            TemplateInfo template = templates.stream().filter(t -> t.name.equals(name)).findFirst().orElse(null);
            if (template != null) {
                template.sourceTypes.add(sourceType);
            }
            else {
                templates.add(new TemplateInfo(name, sourceType));
            }
        }

        public List<TemplateInfo> getTemplates()
        {
            return templates;
        }
    }


    /**
     * Add the class type buttons (defining the class template to be used
     * to the panel. The templates are defined in the "defs" file.
     */
    private void addClassTypeButtons(Window parent, Pane panel)
    {
        TemplatesList templates = new TemplatesList();

        // first, get templates out of defined templates from bluej.defs
        // (we do this rather than using the directory only to be able to force an order on the templates.)
        addDEFsTemplates(templates, SourceType.Java);
        addDEFsTemplates(templates, SourceType.Stride);
        addDEFsTemplates(templates, SourceType.Kotlin);

        // next, get templates from files in template directory and merge them in
        addDirectoryTemplates(templates, SourceType.Java, parent);
        addDirectoryTemplates(templates, SourceType.Kotlin, parent);

        // Create a radio button for each template found
        boolean first = true;
        for (TemplateInfo template : templates.getTemplates())
        {
            String label = Config.getString("pkgmgr.newClass." + template.name, template.name);
            RadioButton button = new RadioButton(label);
            if (first)
                button.setSelected(true); // select first
            button.setToggleGroup(templateButtons);
            button.setUserData(template.name);
            this.templates.put(button, template);
            panel.getChildren().add(button);
            first = false;
        }
        JavaFXUtil.addChangeListenerPlatform(templateButtons.selectedToggleProperty(), selected -> updateOKButton(false));
    }

    private void addDEFsTemplates(TemplatesList templates, SourceType sourceType)
    {
        String templateString = Config.getPropString("bluej.classTemplates." + sourceType.toString().toLowerCase());
        StringTokenizer tokenizer = new StringTokenizer(templateString);
        while (tokenizer.hasMoreTokens()) {
            templates.addTemplate(tokenizer.nextToken(), sourceType);
        }
    }

    private void addDirectoryTemplates(TemplatesList templates, SourceType sourceType, Window parent)
    {
        File templateDir = Config.getClassTemplateDir(sourceType.getConfigSourceType());
        if ( !templateDir.exists() ) {
            DialogManager.showErrorFX(parent, "error-no-templates");
        }
        else {
            String templateSuffix = ".tmpl";
            int suffixLength = templateSuffix.length();

            Arrays.asList(templateDir.list()).forEach(file -> {
                if(file.endsWith(templateSuffix)) {
                    String templateName = file.substring(0, file.length() - suffixLength);
                    templates.addTemplate(templateName, sourceType);
                }
            });
        }
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

        SourceType sourceType = language.selectedProperty().get();
        Properties localProperties = new Properties();
        localProperties.put("LANGUAGE", sourceType.toString());

        if (info == null)
            showError(Config.getString("pkgmgr.newClass.error.noType"), false);
        else if (((RadioButton)selectedToggle).isDisabled() || !info.sourceTypes.contains(sourceType))
            showError(Config.getString("pkgmgr.newClass.error.typeNotAvailable", null,  localProperties, false), false);
        else if (!JavaNames.isIdentifier(newClassName))
        {
            if (fieldHasHadContent || force)
                showError(Config.getString("pkgmgr.newClass.error.notValidClassName", null,  localProperties, false), true);
        }
        else if (isWindowsRestrictedWord(newClassName))
        {
            if (fieldHasHadContent || force)
                showError(Config.getString("pkgmgr.newClass.error.windowsRestricted"), true);
        }
        else if (sourceType == SourceType.Kotlin && selectedToggle.getUserData().equals("facade") && !newClassName.endsWith("Kt")) {
            if (fieldHasHadContent || force)
                showError(Config.getString("pkgmgr.newClass.error.notFacadeName"), true);
        }
        else
        {
            hideError();
            enable = true;
        }

        templates.forEach((radio, templateInfo) -> radio.setVisible(templateInfo.sourceTypes.contains(language.selectedProperty().get())));
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
        return windowsRestrictedWords.contains(fileName.toUpperCase());
    }
    
    /**
     * Initialises the list of restricted words
     */
    private void initialiseRestrictedWordList()
    {
        if (windowsRestrictedWords==null){
            windowsRestrictedWords = Arrays.asList("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
                    "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");
        }
    }

    /**
     * Resorts the template buttons so that all visible buttons are at the top,
     * while preserving the original order of visible items.
     */
    private void resortTemplateButtons(ToggleGroup templateButtons)
    {
        // Get all toggles
        ObservableList<Toggle> toggles = templateButtons.getToggles();

        // If there are no toggles or only one toggle, no need to resort
        if (toggles == null || toggles.isEmpty() || toggles.size() <= 1) {
            return;
        }

        // Create separate lists for visible and invisible buttons, preserving the original order
        List<RadioButton> visibleButtons = new ArrayList<>();
        List<RadioButton> invisibleButtons = new ArrayList<>();

        for (Toggle toggle : toggles) {
            RadioButton button = (RadioButton) toggle;
            if (button.isVisible()) {
                visibleButtons.add(button);
            } else {
                invisibleButtons.add(button);
            }
        }

        // Get the parent container of the buttons
        Pane parent = (Pane) visibleButtons.get(0).getParent();
        if (parent == null) {
            return;
        }

        // Create a list of all buttons for removal
        List<RadioButton> allButtons = new ArrayList<>();
        allButtons.addAll(visibleButtons);
        allButtons.addAll(invisibleButtons);

        // Remove all buttons from the parent
        parent.getChildren().removeAll(allButtons);

        // Add visible buttons first, then invisible buttons, preserving original order within each group
        parent.getChildren().addAll(visibleButtons);
        parent.getChildren().addAll(invisibleButtons);
    }
}
