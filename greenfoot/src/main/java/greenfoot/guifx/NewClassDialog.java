/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013,2014,2015,2016,2018,2019,2022  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.Config;
import bluej.extensions2.SourceType;
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
public class NewClassDialog extends Dialog<NewClassDialog.NewClassInfo>
{
    /** The buttons for the source language (Java/Stride) */
    private final HorizontalRadio<SourceType> language;

    /** stores restricted windows class filenames */
    private static List<String> windowsRestrictedWords;
    /** The field with the class name */
    private final TextField nameField;
    /** The dialog pane */
    private final DialogPaneAnimateError dialogPane;
    /* Keeps track of whether the field has yet been non-blank.
     *  We don't show an error for empty class name if the class name
     *  has always been empty (unless the user mouses over OK)
     */
    private boolean fieldHasHadContent = false;
    /**
     * The label with the error message.
     */
    private final Label errorLabel;

    /**
     * The information selected in the dialog: class name and source type.
     */
    @OnThread(Tag.Any)
    public static class NewClassInfo
    {
        public final String className;
        public final SourceType sourceType;

        private NewClassInfo(String className, SourceType sourceType)
        {
            this.className = className;
            this.sourceType = sourceType;
        }
    }


    /**
     * Construct a NewClassDialog.
     */
    public NewClassDialog(Window parent, SourceType defaultSourceType)
    {
        setTitle(Config.getString("newclass.dialog.title"));
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
        nameField.setMinWidth(250);
        nameField.setPromptText(Config.getString("pkgmgr.newClass.prompt"));
        JavaFXUtil.addChangeListenerPlatform(nameField.textProperty(), s -> {
            hideError();
            updateOKButton(false);
        });
        HBox nameBox = new HBox(new Label(Config.getString("pkgmgr.newClass.label")), nameField);
        JavaFXUtil.addStyleClass(nameBox, "new-class-dialog-hbox");
        nameBox.setAlignment(Pos.BASELINE_LEFT);

        mainPanel.getChildren().add(nameBox);

        language = new HorizontalRadio<>(Arrays.asList(SourceType.Java, SourceType.Stride));
        language.select(defaultSourceType);

        HBox langBox = new HBox();
        JavaFXUtil.addStyleClass(langBox, "new-class-dialog-hbox");
        langBox.getChildren().add(new Label(Config.getString("pkgmgr.newClass.lang")));
        langBox.getChildren().addAll(language.getButtons());
        langBox.setAlignment(Pos.BASELINE_LEFT);
        mainPanel.getChildren().add(langBox);

        mainPanel.getChildren().add(errorLabel);
        JavaFXUtil.addChangeListenerPlatform(language.selectedProperty(), language -> {
            hideError();
            updateOKButton(false);
        });

        getDialogPane().setContent(mainPanel);
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK)
            {
                return new NewClassInfo(nameField.getText().trim(), language.selectedProperty().get());
            }
            else {
                return null;
            }
        });

        updateOKButton(false);
        setOnShown(e -> Platform.runLater(nameField::requestFocus));
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
        boolean enable = false;

        SourceType sourceType = language.selectedProperty().get();
        Properties localProperties = new Properties();
        localProperties.put("LANGUAGE", sourceType.toString());

        if (!JavaNames.isIdentifier(newClassName))
        {
            if (fieldHasHadContent || force)
                showError(Config.getString("pkgmgr.newClass.error.notValidClassName", null,  localProperties, false), true);
        }
        else if (isWindowsRestrictedWord(newClassName))
        {
            if (fieldHasHadContent || force)
                showError(Config.getString("pkgmgr.newClass.error.windowsRestricted"), true);
        }
        else
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
     * Sets the suggested class name to the nameFiled in the dialog
     */
    public void setSuggestedClassName(String suggestedClassName)
    {
        nameField.setText(suggestedClassName);
    }

    /**
     * Disables/Enables the language box in the dialog
     */
    public void disableLanguageBox(boolean value){
        language.setDisable(value);
    };

    /**
     * Get the selected language of the class.
     */
    public SourceType getSelectedLanguage()
    {
        return (SourceType) language.selectedProperty().get();
    }

    /**
     * select the language of the class.
     */
    public void setSelectedLanguage(SourceType type)
    {
        language.select(type);
    }


}
