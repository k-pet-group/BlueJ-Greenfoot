/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012,2014,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej.utility;

import bluej.BlueJTheme;
import bluej.Config;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The dialog manager is a utility class to simplify communication with
 * the user via dialogs. It provides convenience methods to display
 * message, choice or question dialogs. Messages are properly
 * internationalised, using BlueJ's language library system.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class DialogManager
{
    private static final String DLG_FILE_NAME = "dialogues";
    private static final String GREENFOOT_DLG_FILE_NAME = "greenfoot/dialogues";

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). Then replacing variables with subs.
     * 
     * @param parent  the parent window for the information dialog; may be null
     * @param msgId   the message ID. The dialogs texts will be searched for a message with the given ID.
     * @param subs    any string substitutions for the message. Dollar symbols in the message text
     *                will be substituted with these strings..
     */
    public static void showMessageFX(javafx.stage.Window parent, String msgID, String... subs)
    {
        String message = getMessage(msgID);
        if (message != null) {
            for (String sub : subs) {
                message = message.replace("$", sub);
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setHeaderText(message);
            alert.setTitle(Config.getApplicationName() + ":  " +
                Config.getString("dialogmgr.message"));
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.showAndWait();
        }
    }

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * to the message.
     */
    public static void showMessageWithTextFX(javafx.stage.Window parent, String msgID,
                                             String text)
    {
        String message = getMessage(msgID);
        if (message != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
            alert.setHeaderText(message);
            alert.setTitle(Config.getApplicationName() + ":  " +
                Config.getString("dialogmgr.message"));
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.showAndWait();
        }
    }

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * as a prefix to the message. Some text (given as a parameter -
     * innerText) is inserted within the message itself. 
     */
    public static void showMessageWithPrefixTextFX(javafx.stage.Window parent, String msgID,
                                                   String text, String innerText)
    {
        String message = getMessage(msgID);
        String messageDialog = Utility.mergeStrings(message, innerText);
        if (message != null)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setHeaderText("");
            alert.setTitle(Config.getApplicationName() + ":  " +
                Config.getString("dialogmgr.message"));
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setContentText(messageDialog);
            alert.showAndWait();
        }
    }

    public static void showTextWithCopyButtonFX(javafx.stage.Window parent, String text, String title)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK, ButtonType.APPLY);
        ((Button)alert.getDialogPane().lookupButton(ButtonType.APPLY)).setText(Config.getString("editor.copy-to-clipboardLabel"));
        alert.setTitle(title);
        alert.initOwner(parent);
        alert.setHeaderText("");
        alert.initModality(Modality.WINDOW_MODAL);
        if (alert.showAndWait().orElse(ButtonType.OK) == ButtonType.APPLY)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }

    public static void showTextFX(javafx.stage.Window parent, String text)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setHeaderText("");
        alert.showAndWait();
    }

    /**
     * Show an info dialog with an already-localized message and "Continue" button.
     *
     * @param parent   The component to position the dialog over
     * @param title    The title of the dialog
     * @param message  The message text to display (should be localized)
     * @param cancelButton  The true/false value to indicate if the dialog includes "Cancel" button
     * @return The button's index selected by the user
     */
    public static int showInfoTextFX(javafx.stage.Window parent, String title,
                                     String message, boolean cancelButton)
    {
        ButtonType CONTINUE = new ButtonType(BlueJTheme.getContinueLabel(),
                ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dialogPane = new DialogPane() {
            @Override
            protected Node createButtonBar()
            {
                ButtonBar buttonBar = (ButtonBar) super.createButtonBar();
                buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
                return buttonBar;
            }
        };
        dialog.setDialogPane(dialogPane);
        dialog.setTitle(title);
        if (cancelButton)
        {
            dialogPane.getButtonTypes().addAll(ButtonType.CANCEL,CONTINUE);
        }
        else
        {
            dialogPane.getButtonTypes().addAll(CONTINUE);
        }

        dialogPane.setContentText(message);
        //The following code is used as a hack for button central alignment
        Region spacer = new Region();
        ButtonBar.setButtonData(spacer, ButtonBar.ButtonData.BIG_GAP);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dialogPane.applyCss();
        HBox hbox = (HBox) dialogPane.lookup(".container");
        hbox.getChildren().add(spacer);
        return dialog.showAndWait().map(dialogPane.getButtonTypes()::indexOf).
                orElse(dialogPane.getButtonTypes().size() - 1);
    }

    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showErrorFX(javafx.stage.Window parent, String msgID)
    {
        String message = getMessage(msgID);
        if (message != null) {
            showErrorTextFX(parent, message);
        }
    }

    /**
     * Show an error dialog with an already-localized message and "OK" button.
     *
     * @param parent   The component to position the dialog over
     * @param message  The message text to display (should be localized)
     */
    public static void showErrorTextFX(javafx.stage.Window parent, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(Config.getApplicationName() + ":  " + Config.getString("dialogmgr.error"));
        alert.initOwner(parent);
        Label label = new Label(message);
        alert.getDialogPane().setContent(label);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setHeaderText("");
        alert.showAndWait();
    }

    public static void showErrorWithTextFX(javafx.stage.Window parent, String msgID,
                                           String text)
    {
        String message = getMessage(msgID);
        if (message != null) {
            showErrorTextFX(parent, message + "\n" + text);
        }
    }

    /**
     * Brings up a two or three button question dialog. The text for the
     * question and the buttons is read from the dialogues file. If the third
     * button text is "null", it is not shown. Returns the button index that
     * was selected (0..2).
     * 
     * FX button types/ordering:
     * With two buttons, the first button is assumed to be a YES button,
     * the second is assumed to be NO.  With three buttons, the first two
     * are assumed to be yes, the third is NO.
     */
    public static int askQuestionFX(javafx.stage.Window parent, String msgID)
    {
        MessageAndButtons messageAndButtons = new MessageAndButtons(getMessage(msgID));
        if (messageAndButtons.getMessage() != null) {
            List<ButtonType> buttons = new ArrayList<>();
            for (int i = 0; i < messageAndButtons.getOptions().size(); i++)
            {
                buttons.add(new ButtonType(messageAndButtons.getOptions().get(i), i == messageAndButtons.getOptions().size() - 1 ? ButtonBar.ButtonData.NO : ButtonBar.ButtonData.YES));
            }
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messageAndButtons.getMessage(), buttons.toArray(new ButtonType[0]));
            alert.setHeaderText("");
            alert.initOwner(parent);
            alert.initModality(parent == null ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL);
            alert.setTitle(Config.getApplicationName() + ":  " +
                Config.getString("dialogmgr.question"));
            return alert.showAndWait().map(buttons::indexOf).orElse(buttons.size() - 1);
        }
        return 0;
    }

    /**
     * Brings up a two or three button question dialog. The text for the
     * question and the buttons is read from the dialogues file; '$'
     * characters in the message are replaced one-by-one with the specified
     * strings.
     * 
     * <p>If the third button text is "null", it is not shown. Returns the button
     * index that was selected (0..2).
     */
    public static int askQuestionFX(javafx.stage.Window parent, String msgID, String [] subs)
    {
        String message = getMessage(msgID);
        if (message != null) {
            int button3Index = message.lastIndexOf("\n");
            int button2Index = message.lastIndexOf("\n", button3Index-1);
            int button1Index = message.lastIndexOf("\n", button2Index-1);
            String button3 = message.substring(button3Index+1);
            String button2 = message.substring(button2Index+1, button3Index);
            String button1 = message.substring(button1Index+1, button2Index);
            message = message.substring(0, button1Index);
            message = Utility.mergeStrings(message, subs);
            List<ButtonType> buttons = new ArrayList<>();
            boolean hasThirdButton = !"null".equals(button3);
            buttons.add(new ButtonType(button1));
            buttons.add(new ButtonType(button2));
            if (hasThirdButton)
            {
                buttons.add(new ButtonType(button3));
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, buttons.toArray(new ButtonType[0]));
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setHeaderText("");
            alert.setTitle(Config.getApplicationName() + ":  " +
                    Config.getString("dialogmgr.question"));
            return alert.showAndWait().map(buttons::indexOf).orElse(buttons.size() - 1);
        }
        return 0;
    }

    /**
     * Brings up a two or three button question dialog. The question message and
     * the buttons are read from the dialogues file; the information text is passed
     * to be appended to the question message.<p>
     * 
     * The first option should be the affirmative, the second should be negative,
     * and the third (if present) should be the cancellation option. 
     *
     * <p>If the third button text is "null", it is not shown. Returns the button
     * index that was selected (0..2).
     */
    public static int askQuestionFX(javafx.stage.Window parent, String msgID, String  infoText)
    {
        String message = getMessage(msgID);
        if (message != null)
        {
            int button3Index = message.lastIndexOf("\n");
            int button2Index = message.lastIndexOf("\n", button3Index-1);
            int button1Index = message.lastIndexOf("\n", button2Index-1);
            String button3 = message.substring(button3Index+1);
            String button2 = message.substring(button2Index+1, button3Index);
            String button1 = message.substring(button1Index+1, button2Index);
            message = message.substring(0, button1Index);
            message = infoText + message;

            List<ButtonType> buttons = new ArrayList<>();
            boolean hasThirdButton = !"null".equals(button3);
            buttons.add(new ButtonType(button1, ButtonBar.ButtonData.YES));
            buttons.add(new ButtonType(button2,
                    hasThirdButton ?  ButtonBar.ButtonData.NO : ButtonBar.ButtonData.CANCEL_CLOSE));
            if (hasThirdButton)
            {
                buttons.add(new ButtonType(button3, ButtonBar.ButtonData.CANCEL_CLOSE));
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                    buttons.toArray(new ButtonType[0]));

            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setHeaderText("");
            alert.setTitle(Config.getApplicationName() + ":  " +
                    Config.getString("dialogmgr.question"));
            
            return alert.showAndWait().map(buttons::indexOf).orElse(buttons.size() - 1);
        }
        return 0;
    }

    /**
     * Bring up a dialog which asks the user for a response in the form of a string.
     * 
     * @param parent The parent component of the dialog
     * @param msgId  the identifier of the message in the dialogs file. The first line
     *               is used as the dialog title, and the last line is used as the
     *               default response ("null" means no default response).
     * 
     * @return The string supplied by the user, or null if the dialog was cancelled.
     */
    public static String askStringFX(javafx.stage.Window parent, String msgID)
    {
        String response = "";
        String message = getMessage(msgID);
        if (message != null) {
            int defaultTextIndex = message.lastIndexOf("\n");
            int titleIndex = message.lastIndexOf("\n", defaultTextIndex-1);
            String defaultText = message.substring(defaultTextIndex+1);
            String title = message.substring(titleIndex+1, defaultTextIndex);
            message = message.substring(0, titleIndex);
            if ("null".equals(defaultText)) {
                defaultText = null;
            }
            TextInputDialog dialog = new TextInputDialog(defaultText);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(parent);
            dialog.setTitle(title);
            dialog.setHeaderText(message);
            return dialog.showAndWait().orElse(null);
        }
        return response;
    }

    /**
     * Bring up a dialog which asks the user for a response in the form of a string,
     * using a specified default response.
     * 
     * @param parent The parent component of the dialog
     * @param msgId  the identifier of the message in the dialogs file. The first line
     *               is used as the dialog title. The last line is ignored.
     * @param defaultText The default response, which may be null for no default.
     * 
     * @return The string supplied by the user, or null if the dialog was cancelled.
     */
    public static String askStringFX(javafx.stage.Window parent, String msgID, String defaultText)
    {
        String response = "";
        String message = getMessage(msgID);
        if (message != null) {
            int defaultTextIndex = message.lastIndexOf("\n");
            int titleIndex = message.lastIndexOf("\n", defaultTextIndex - 1);
            String title = message.substring(titleIndex + 1, defaultTextIndex);
            message = message.substring(0, titleIndex);
            TextInputDialog dialog = new TextInputDialog(defaultText);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(parent);
            dialog.setTitle(title);
            dialog.setHeaderText(message);
            return dialog.showAndWait().orElse(null);
        }
        return response;
    }

    /**
     * Support routine for dialogues. Read the message text out of the
     * dialogue text file (language dependent).
     */
    @OnThread(Tag.Any)
    public static String getMessage(String msgID, String... subs)
    {
        String message = null;
        
        if (Config.isGreenfoot()) {
            File filename = Config.getLanguageFile(GREENFOOT_DLG_FILE_NAME);
            message = BlueJFileReader.readHelpText(filename, msgID, true);
            if (message == null) {
                // Might not be available in the chosen language; try English:
                filename = Config.getDefaultLanguageFile(GREENFOOT_DLG_FILE_NAME);
                message = BlueJFileReader.readHelpText(filename, msgID, true);
            }
        }
        
        if (message == null) {
            File filename = Config.getLanguageFile(DLG_FILE_NAME);
            message = BlueJFileReader.readHelpText(filename, msgID, true);
        }
        
        // check that message has been found, some messages may be missing
        // in non-default language resource files.  If not found and not using
        // English, then use the default English message
        if (message == null && (!Config.language.equals(Config.DEFAULT_LANGUAGE))) {
            File filename = Config.getDefaultLanguageFile(DLG_FILE_NAME);
            message = BlueJFileReader.readHelpText(filename, msgID, true);
        }
        // if we still can't find it, there's something wrong...
        if (message == null) {
            message = "BlueJ configuration problem:\n" + "text not found for message ID\n" + msgID;
            Debug.message(message);
        }
        else
        {
            // Replace single ':' with a blank line; this allows messages to include blank lines:
            message = message.replace("\n:\n", "\n\n");
            message = message.replace("\r\n:\r\n", "\r\n\r\n");
            
            for (String sub : subs) {
                message = message.replace("$", sub);
            }
        }
        return message;
    }

    // --- utility methods to position dialogues and other windows ---

    public static void centreDialog(Dialog<?> dialog)
    {
        dialog.setOnShown(event -> centreWindow(dialog, dialog.getOwner()));
    }

    /**
     * Centre a dialog over another window. The dialog's position and size must be available,
     * which generally requires it to have been shown.
     * 
     * @param dialog  the dialog to position
     * @param owner   the window over which to centre the dialog. If null, nothing is done.
     */
    private static void centreWindow(Dialog<?> dialog, javafx.stage.Window owner)
    {
        if (owner != null)
        {
            dialog.setX(owner.getX() + owner.getWidth()/2d - dialog.getWidth()/2d);
            dialog.setY(owner.getY() + owner.getHeight()/2d - dialog.getHeight()/2d);
        }
    }

    private static class MessageAndButtons
    {
        private final String message;
        private final List<String> options;

        public MessageAndButtons(String message)
        {
            if (message == null)
            {
                this.message = null;
                this.options = null;
                return;
            }
            
            int button3Index = message.lastIndexOf("\n");
            int button2Index = message.lastIndexOf("\n", button3Index-1);
            int button1Index = message.lastIndexOf("\n", button2Index-1);
            String button3 = message.substring(button3Index+1);
            String button2 = message.substring(button2Index+1, button3Index);
            String button1 = message.substring(button1Index+1, button2Index);
            this.message = message.substring(0, button1Index);
            if ("null".equals(button3)) {
                options = Arrays.asList(button1, button2);
            }
            else {
                options = Arrays.asList(button1, button2, button3);
            }
        }

        public String getMessage()
        {
            return message;
        }

        public List<String> getOptions()
        {
            return options;
        }
    }
}
