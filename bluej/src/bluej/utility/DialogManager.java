package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.io.*;
import java.util.Random;
import java.util.Vector;
import java.awt.event.*;
import javax.swing.*;

/**
 **
 **
 **
 **
 ** @author Michael Kolling
 **/
public class DialogManager
{
    private static final String DLG_FILE_NAME = "dialogues";

    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english").
     */
    public static void showMessage(Component parent, String msgID)
    {
	String message = getMessage(msgID);
	if(message != null)
	    JOptionPane.showMessageDialog(parent, message,
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
	if(message != null)
	    JOptionPane.showMessageDialog(parent, message + "\n" + text);
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


    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showError(Component parent, String msgID)
    {
	String message = getMessage(msgID);
	if(message != null)
	    JOptionPane.showMessageDialog(parent, message,
                                          Config.getString("dialogmgr.error"),
					  JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showErrorWithText(Component parent, String msgID,
					 String text)
    {
	String message = getMessage(msgID);
	if(message != null)
	    JOptionPane.showMessageDialog(parent, message + "\n" + text,
					  "Error", JOptionPane.ERROR_MESSAGE);
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
	if(message != null) {
	    int button3Index = message.lastIndexOf("\n");
	    int button2Index = message.lastIndexOf("\n", button3Index-1);
	    int button1Index = message.lastIndexOf("\n", button2Index-1);
	    String button3 = message.substring(button3Index+1);
	    String button2 = message.substring(button2Index+1, button3Index);
	    String button1 = message.substring(button1Index+1, button2Index);
	    message = message.substring(0, button1Index);
	    Object[] options;
	    if ("null".equals(button3))
		options = new Object[] { button1, button2 };
	    else
		options = new Object[] { button1, button2, button3 };

	    return JOptionPane.showOptionDialog(parent, message,
                                                Config.getString("dialogmgr.question"),
                                                JOptionPane.DEFAULT_OPTION,
                                                JOptionPane.WARNING_MESSAGE,
                                                null, options, options[0]);
	}
	return 0;
    }


    /**
     *
     */
    public static String askString(Component parent, String msgID)
    {
	String response = "";
	String message = getMessage(msgID);
	if(message != null) {
	    int defaultTextIndex = message.lastIndexOf("\n");
	    int titleIndex = message.lastIndexOf("\n", defaultTextIndex-1);
	    String defaultText = message.substring(defaultTextIndex+1);
	    String title = message.substring(titleIndex+1, defaultTextIndex);
	    message = message.substring(0, titleIndex);
	    if("null".equals(defaultText))
		defaultText = null;
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
     * Support routine for dialogues. Read the message text out of the
     * dialogue text file (language dependent).
     */
    public static String getMessage(String msgID) {
	String filename = Config.getLanguageFilename(DLG_FILE_NAME);
	String message = BlueJFileReader.readHelpText(filename, msgID, true);
	if(message == null)
	    JOptionPane.showMessageDialog(null,
				"BlueJ configuration problem:\n" +
				"text not found for message ID\n" +
				msgID);
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
	child.pack();

	Point p_topleft = parent.getLocationOnScreen();
	Dimension p_size = parent.getSize();
	Dimension d_size = child.getSize();

	child.setLocation(p_topleft.x + (p_size.width - d_size.width) / 2,
			  p_topleft.y + (p_size.height - d_size.height) / 2);
    }


    /**
     * tileWindow - position the child at 20, 20 offset of parent
     *  location
     */
    public static void tileWindow(Window child, Window parent)
    {
	Point p_topleft = parent.getLocationOnScreen();
	child.setLocation(p_topleft.x + 20, p_topleft.y + 20);
    }
}
