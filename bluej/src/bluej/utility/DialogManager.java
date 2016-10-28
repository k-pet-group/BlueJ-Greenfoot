/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012,2014,2016  Michael Kolling and John Rosenberg 
 
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

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;

import bluej.utility.javafx.SwingNodeDialog;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;

/**
 * The dialog manager is a utility class to simplyfy communication with 
 * the user via dialogs. It provides convinience methods to display
 * message, choice or question dialogs. Messages are properly
 * internationalised, using BlueJ's langauage library system.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.Swing)
public class DialogManager
{
    private static final String DLG_FILE_NAME = "dialogues";
    private static final String GREENFOOT_DLG_FILE_NAME = "greenfoot/dialogues";

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english").
     */
    public static void showMessage(Component parent, String msgID)
    {
        String message = getMessage(msgID);
        if (message != null)
            JOptionPane.showMessageDialog(parent, message,
                                          Config.getApplicationName() + ":  " +
                                          Config.getString("dialogmgr.message"),
                                          JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english").
     */
    @OnThread(Tag.FXPlatform)
    public static void showMessageFX(javafx.stage.Window parent, String msgID)
    {
        showMessageFX(parent, msgID, new String[0]);
    }

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). Then replacing variables with subs.
     */
    @OnThread(Tag.FXPlatform)
    public static void showMessageFX(javafx.stage.Window parent, String msgID, String[] subs)
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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


    /**
     * Show an information dialog with a text and "OK" button. The text
     * is shown as it is passed in here. This method should only be used
     * if the text has already been localised (translated into the local
     * language). Most of the time "showMessage" (above) should be used.
     */
    public static void showText(Component parent, String text)
    {
        JOptionPane.showMessageDialog(parent, text);
    }

    @OnThread(Tag.FXPlatform)
    public static void showTextWithCopyButtonFX(javafx.stage.Window parent, String text, String title)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK, ButtonType.APPLY);
        ((Button)alert.getDialogPane().lookupButton(ButtonType.APPLY)).setText(Config.getString("editor.copy-to-clipboardLabel"));
        alert.setTitle(title);
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        if (alert.showAndWait().orElse(ButtonType.OK) == ButtonType.APPLY)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }

    @OnThread(Tag.FXPlatform)
    public static void showTextFX(javafx.stage.Window parent, String text)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }

    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showError(Component parent, String msgID)
    {
        String message = getMessage(msgID);
        if (message != null) {
            showErrorText(parent, message);
        }
    }

    /**
     * Show an error dialog with message and "OK" button.
     */
    @OnThread(Tag.FXPlatform)
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
    public static void showErrorText(Component parent, String message)
    {
        JOptionPane.showMessageDialog(parent, message,
                Config.getApplicationName() + ":  " +
                Config.getString("dialogmgr.error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show an error dialog with an already-localized message and "OK" button.
     *
     * @param parent   The component to position the dialog over
     * @param message  The message text to display (should be localized)
     */
    @OnThread(Tag.FXPlatform)
    public static void showErrorTextFX(javafx.stage.Window parent, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(Config.getApplicationName() + ":  " +
            Config.getString("dialogmgr.error"));
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }

    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
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
    @OnThread(Tag.FXPlatform)
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
            boolean hasThirdButton = "null".equals(button3);
            buttons.add(new ButtonType(button1, hasThirdButton ? ButtonBar.ButtonData.CANCEL_CLOSE : ButtonBar.ButtonData.NO));
            buttons.add(new ButtonType(button2, hasThirdButton ? ButtonBar.ButtonData.NO : ButtonBar.ButtonData.YES));
            if (hasThirdButton)
                buttons.add(new ButtonType(button3, ButtonBar.ButtonData.YES));

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, buttons.toArray(new ButtonType[0]));
            alert.initOwner(parent);
            alert.initModality(Modality.WINDOW_MODAL);
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
            for (String sub : subs) {
                message = message.replace("$", sub);
            }
        }
        return message;
    }

    // --- utility methods to position dialogues and other windows ---

    /**
     * centreDialog - try to center a dialog within its parent frame
     */
    public static void centreDialog(JDialog dialog)
    {
        centreWindow(dialog, (Window)dialog.getParent());
    }

    public static void centreDialog(SwingNodeDialog dialog)
    {
        
    }


    /**
     * centreWindow - try to center a window within a parent window
     */
    public static void centreWindow(Window child, Window parent)
    {
        child.setLocationRelativeTo(parent);
    }

    public static void addOKCancelButtons(JPanel panel, JButton okButton, JButton cancelButton) 
    {
        if (Config.isMacOS()) {
            panel.add(cancelButton);
            panel.add(okButton);
        } else {
            panel.add(okButton);
            panel.add(cancelButton);
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
