/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012,2014  Michael Kolling and John Rosenberg 
 
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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import bluej.Config;

/**
 * The dialog manager is a utility class to simplyfy communication with 
 * the user via dialogs. It provides convinience methods to display
 * message, choice or question dialogs. Messages are properly
 * internationalised, using BlueJ's langauage library system.
 *
 * @author Michael Kolling
 */
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
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * to the message.
     */
    public static void showMessageWithText(Component parent, String msgID,
                                           String text)
    {
        String message = getMessage(msgID);
        if (message != null) {
            JOptionPane.showMessageDialog(parent, message + "\n" + text);
        }
    }
    
    /**
     * Show an information dialog with message (including and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * to the message.
     */
    public static void showMessageWithText(Component parent, String msgID, String[] subs)
    {
        String message = getMessage(msgID);
        message = Utility.mergeStrings(message, subs);
        
        // Replace single ':' with a blank line:
        message = message.replace("\n:\n", "\n\n");
        message = message.replace("\r\n:\r\n", "\r\n\r\n");
        
        if (message != null) {
            JOptionPane.showMessageDialog(parent, message,
                    Config.getApplicationName() + ":  " +
                    Config.getString("dialogmgr.message"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * as a prefix to the message. Use showMessageWithText in order to
     * append to the suffix of the message
     */
    public static void showMessageWithPrefixText(Component parent, String msgID,
                                           String text)
    {
        String message = getMessage(msgID);
        if (message != null)
            JOptionPane.showMessageDialog(parent, text+ "\n"+message);
    }
    
    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * as a prefix to the message. Some text (given as a parameter -
     * innerText) is inserted within the message itself. 
     */
    public static void showMessageWithPrefixText(Component parent, String msgID,
                                           String text, String innerText)
    {
        String message = getMessage(msgID);
        String messageDialog=Utility.mergeStrings(message, innerText);
        if (message != null)
            JOptionPane.showMessageDialog(parent, text+ "\n"+messageDialog);
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
    
    public static void showTextWithCopyButton(Component parent, String text, String title)
    {
        if (JOptionPane.showOptionDialog(parent, text, title, 0, 0, null, new String [] {Config.getString("okay"), Config.getString("editor.copy-to-clipboardLabel")}, null) == 1)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
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
     * Show an error dialog with a message, some additional text, and "OK" button.
     */
    public static void showErrorWithText(Component parent, String msgID,
                                         String text)
    {
        String message = getMessage(msgID);
        if (message != null) {
            showErrorText(parent, message + "\n" + text);
        }
    }

    /**
     * Brings up a two or three button question dialog. The text for the
     * question and the buttons is read from the dialogues file. If the third
     * button text is "null", it is not shown. Returns the button index that
     * was selected (0..2).
     */
    public static int askQuestion(Component parent, String msgID)
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
            Object[] options;
            if ("null".equals(button3)) {
                options = new Object[] { button1, button2 };
            }
            else {
                options = new Object[] { button1, button2, button3 };
            }

            return JOptionPane.showOptionDialog(parent, message,
                                                Config.getApplicationName() + ":  " +
                                                Config.getString("dialogmgr.question"),
                                                JOptionPane.DEFAULT_OPTION,
                                                JOptionPane.WARNING_MESSAGE,
                                                null, options, options[0]);
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
    public static int askQuestion(Component parent, String msgID, String [] subs)
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
            Object[] options;
            if ("null".equals(button3)) {
                options = new Object[] { button1, button2 };
            }
            else {
                options = new Object[] { button1, button2, button3 };
            }

            return JOptionPane.showOptionDialog(parent, message,
                                                Config.getApplicationName() + ":  " +
                                                Config.getString("dialogmgr.question"),
                                                JOptionPane.DEFAULT_OPTION,
                                                JOptionPane.WARNING_MESSAGE,
                                                null, options, options[0]);
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
    public static String askString(Component parent, String msgID)
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
            response = (String)JOptionPane.showInputDialog(parent,
                                                           message,
                                                           title,
                                                           JOptionPane.PLAIN_MESSAGE,
                                                           null,
                                                           null,
                                                           defaultText);
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
    public static String askString(Component parent, String msgID, String defaultText)
    {
        String response = "";
        String message = getMessage(msgID);
        if (message != null) {
            int defaultTextIndex = message.lastIndexOf("\n");
            int titleIndex = message.lastIndexOf("\n", defaultTextIndex - 1);
            String title = message.substring(titleIndex + 1, defaultTextIndex);
            message = message.substring(0, titleIndex);
            response = (String) JOptionPane.showInputDialog(parent,
                                                            message,
                                                            title,
                                                            JOptionPane.PLAIN_MESSAGE,
                                                            null,
                                                            null,
                                                            defaultText);
        }
        return response;
    }

    /**
     * Support routine for dialogues. Read the message text out of the
     * dialogue text file (language dependent).
     */
    public static String getMessage(String msgID)
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
        return message;
    }

    /**
     * Show a "Not Yet Implemented" message.
     */
    public static void NYI(Component frame)
    {
        showMessage(frame, "not-yet-implemented");
    }


    // --- utility methods to position dialogues and other windows ---

    /**
     * centreDialog - try to center a dialog within its parent frame
     */
    public static void centreDialog(JDialog dialog)
    {
        centreWindow(dialog, (Window)dialog.getParent());
    }


    /**
     * centreWindow - try to center a window within a parent window
     */
    public static void centreWindow(Window child, Window parent)
    {
        child.setLocationRelativeTo(parent);
    }


    /**
     * tileWindow - position the child at 20, 20 offset of parent
     *  location
     */
    public static void tileWindow(Window child, Window parent)
    {
        if(parent.isShowing()) {
            Point p_topleft = parent.getLocationOnScreen();
            child.setLocation(p_topleft.x + 20, p_topleft.y + 20);
        }
    }
    
    /**
     * Allows the user to specify the number of buttons in question dialog. 
     * The text for the question and the buttons is read from the dialogues file. 
     */
    public static int askQuestion(Component parent, String msgID, int numOptions)
    {
        String message = getMessage(msgID);
        if(message != null) {
            String buttonName;
            int btnIndex=message.length()+1;
            int prevBtnIndex=message.length(); 
            String[] options=new String[numOptions];
            for (int i=0; i < numOptions; i++) {
                btnIndex=message.lastIndexOf("\n", btnIndex-1);
                buttonName=message.substring(btnIndex+1, prevBtnIndex);
                options[numOptions-i-1]=buttonName; //just to ensure they go in, in the correct order
                prevBtnIndex=btnIndex;
            }
            message = message.substring(0, btnIndex);

            return JOptionPane.showOptionDialog(parent, message,
                    Config.getApplicationName() + ":  " +
                    Config.getString("dialogmgr.question"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
        }
        return 0;
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

}
