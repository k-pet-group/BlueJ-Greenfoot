/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017  Michael Kolling and John Rosenberg 
 
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
package bluej.utility.javafx.dialog;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;

/**
 * A base-class for dialogs which ask for a single text-field input from the user.
 * 
 * You will need to subclass this class and implement convert, which converts
 * the user's String input into R, the type returned.  (If you just want the user's
 * raw input, just implement convert as returning the passed parameter.)
 * 
 * You may also want to override validate to either prevent invalid inputs
 * being allowed in the field, or to disable/enable the OK button (see setOKEnabled).
 */
public abstract class InputDialog<R>
{
    // The actual GUI dialog.  Encapsulated to give better control of its properties
    private final Dialog<R> dialog;
    // The input text field
    protected final TextField field;
    // The label above the text field, telling the user what is expected
    private final Label prompt;
    // The optional (invisible unless addErrorTextLabel is called) label below the field
    // showing some error text.
    private final Label error;
    // The dialog pane:
    private final DialogPaneAnimateError dialogPane;

    /**
     * Creates an InputDialog.
     * @param title The title of the dialog (shown in window title bar)
     * @param label The text shown in the label above the text field.
     * @param prompt The prompt shown in the text field
     * @param styleClass The style-class to apply to the dialog.
     * @param labelAfterField The label (null if none) to show after the text field (e.g. a file extension suffix) 
     */
    protected InputDialog(String title, String label, String prompt, String styleClass, String labelAfterField)
    {
        dialog = new Dialog<>();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);
        VBox content = new VBox();
        this.prompt = new Label(label);
        field = new TextField();
        field.setPromptText(prompt);
        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && dialog.getDialogPane().getButtonTypes().contains(ButtonType.CANCEL))
            {
                dialog.hide();
                e.consume();
            }
        });
        error = new Label();
        if (labelAfterField == null)
        {
            // By default, error label is shown
            content.getChildren().addAll(this.prompt, field, error);
        }
        else
        {
            HBox hbox = new HBox(field, new Label(labelAfterField));
            content.getChildren().addAll(this.prompt, hbox, error);
        }
        dialogPane = new DialogPaneAnimateError(error, () -> validate(field.getText(), field.getText()));
        dialog.setDialogPane(dialogPane);
        dialog.getDialogPane().setContent(content);
        // By default, we have an OK and Cancel button:
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Config.addDialogStylesheets(dialog.getDialogPane());
        
        if (styleClass != null)
            JavaFXUtil.addStyleClass(content, styleClass);
        JavaFXUtil.addStyleClass(content, "input-dialog-content");
        JavaFXUtil.addStyleClass(this.prompt, "input-dialog-prompt");
        JavaFXUtil.addStyleClass(field, "input-dialog-field");
        JavaFXUtil.addStyleClass(error, "dialog-error-label");
        
        /* Scenic view:
        dialog.initModality(Modality.NONE);
        dialog.setOnShown(e -> org.scenicview.ScenicView.show(dialogPane));
        */
        field.setTextFormatter(new TextFormatter<Object>((TextFormatter.Change change) -> {
            if (!change.getControlText().equals(change.getControlNewText()) && !validate(change.getControlText(), change.getControlNewText()))
            {
                // I believe this is the right code to revert to the old content:
                change.setRange(0, change.getControlText().length());
                change.setText(change.getControlText());
            }
            return change;
        }));

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK)
                return convert(field.getText());
            else
                return null; // This will turn into Optional.empty in the showAndWait return.
        });
    }

    /**
     * Gets either the title (pass true) or message (pass false) for a dialog
     * from the DialogManager.
     */
    private static String getDetail(String msgID, boolean getTitle)
    {
        String message = DialogManager.getMessage(msgID);
        if (message != null)
        {
            int defaultTextIndex = message.lastIndexOf("\n");
            int titleIndex = message.lastIndexOf("\n", defaultTextIndex - 1);
            String defaultText = message.substring(defaultTextIndex + 1);
            String title = message.substring(titleIndex + 1, defaultTextIndex);
            message = message.substring(0, titleIndex);
            if ("null".equals(defaultText))
            {
                defaultText = null;
            }
            return getTitle ? title : message;
        }
        return "";
    }

    protected InputDialog(String title, String label, String prompt, String styleClass)
    {
        this(title, label, prompt, styleClass, null);
    }
    
    protected InputDialog(String dialogMsgID, String prompt, String styleClass)
    {
        // Calling same method twice is messy, but no easy way I can see in Java
        // of getting one object and passing to two different parameters of
        // delegate constructor:
        this(getDetail(dialogMsgID, true), getDetail(dialogMsgID, false).replace("\n", " "), prompt, styleClass);
    }

    /**
     * Sets the owner of the dialog.  Should be called before dialog is shown.
     */
    public void initOwner(Window parent)
    {
        dialog.initOwner(parent);
    }

    /**
     * Sets the prompt text.  Can be called any time, including while the dialog
     * is showing.  Although if the text changes length a lot, the layout may be disturbed.
     */
    protected void setPrompt(String s)
    {
        prompt.setText(s);
    }

    /**
     * Shows the dialog and waits for it to be dismissed (e.g. by closing
     * the window, pressing escape, clicking OK or Cancel).  The returned
     * value is Optional.of(X) if OK was pressed, and convert(..) returned
     * X, where X != null.  In any other case (window closed by cancelled or
     * closing, or convert returns null), Optional.empty will be returned.
     * 
     * If you want null to be an acceptable return from convert, and treated
     * differently to cancelled, consider wrapping it an Optional, so that
     * R itself is Optional, and you get back an Optional<Optional<..>> from this method.
     */
    public Optional<R> showAndWait()
    {
        Platform.runLater(field::requestFocus);
        return dialog.showAndWait();
    }

    /**
     * Sets the error text in the dialog.  Will only be visible if you have previously
     * called addErrorTextLabel.
     */
    protected void setErrorText(String errorText)
    {
        error.setText(errorText);
        JavaFXUtil.setPseudoclass("bj-dialog-error", !errorText.equals(""), field);
    }
    
    /**
     * Sets the OK button of the dialog to be enabled (pass true) or not (pass false)
     */
    protected void setOKEnabled(boolean okEnabled)
    {
        dialogPane.getOKButton().setDisable(!okEnabled);
    }

    /**
     * Convert the user's text input into a return value of the desired type,
     * e.g. Integer or perhaps String.
     * @param input The user text to convert.  Will not be null.
     * @return The converted value.  See showAndWait for details of the consequences
     *         of returning null here.
     */
    protected abstract R convert(String input);

    /**
     * Given a new input, checks whether to allow it as text in the text field
     * (disallowing pressing OK is another matter, which should be handled by setOKEnabled).
     * Note that you should always allow blank input, and new input which is identical to old output,
     * so make sure to return true when newInput is the empty String.
     * 
     * You may also want to call setErrorText or setOKEnabled during your implementation of this method,
     * to show an error or disable the OK button if the input is invalid.  (But if you do, make
     * sure to blank the error and enable the OK button when the input becomes valid again.) 
     * 
     * This method gets called with the current content for oldInput and newInput if the user hovers over
     * the OK button while the field is blank, to allow you to set the error text (which
     * will then be animated).
     * 
     * @param oldInput The previous text
     * @param newInput The potential new input
     * @return Whether to allow the new input (true) or revert to previous (false)
     */
    protected boolean validate(String oldInput, String newInput)
    {
        return true;
    }
}
