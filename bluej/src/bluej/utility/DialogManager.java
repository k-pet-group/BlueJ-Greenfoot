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
    public static void showMessage(JFrame parent, String msgID)
    {
	String filename = Config.getLanguageFilename(DLG_FILE_NAME);
	String message = BlueJFileReader.readHelpText(filename, msgID, true);
	if(message == null)
	    JOptionPane.showMessageDialog(parent, 
				"BlueJ configuration problem:\n" +
				"text not found for message ID\n" +
				msgID);
	else
	    JOptionPane.showMessageDialog(parent, message);
    }


    /**
     * Show an information dialog with message and "OK" button. The
     * message itself is identified by a message ID (a short string)
     * which is looked up in the language specific dialogue text file
     * (eg. "dialogues.english"). A text (given in a parameter) is appended
     * to the message.
     */
    public static void showMessageWithText(JFrame parent, String msgID, 
					   String text)
    {
	String filename = Config.getLanguageFilename(DLG_FILE_NAME);
	String message = BlueJFileReader.readHelpText(filename, msgID, true);
	if(message == null)
	    JOptionPane.showMessageDialog(parent, 
				"BlueJ configuration problem:\n" +
				"text not found for message ID\n" +
				msgID + "\n" + text);
	else
	    JOptionPane.showMessageDialog(parent, message + "\n" + text);
    }


    /**
     * Show an information dialog with a text and "OK" button. The text
     * is shown as it is passed in here. This method should only be used
     * if the text has already been localised (translated into the local
     * language). Most of the time "showMessage" (above) should be used.
     */
    public static void showText(JFrame parent, String text)
    {
	JOptionPane.showMessageDialog(parent, text);
    }


    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showError(JFrame parent, String msgID)
    {
	String filename = Config.getLanguageFilename(DLG_FILE_NAME);
	String message = BlueJFileReader.readHelpText(filename, msgID, true);
	if(message == null)
	    JOptionPane.showMessageDialog(parent, 
				"BlueJ configuration problem:\n" +
				"text not found for message ID\n" +
				msgID);
	else
	    JOptionPane.showMessageDialog(parent, message, "Error",
					  JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Show an error dialog with message and "OK" button.
     */
    public static void showErrorWithText(JFrame parent, String msgID, 
					 String text)
    {
	String filename = Config.getLanguageFilename(DLG_FILE_NAME);
	String message = BlueJFileReader.readHelpText(filename, msgID, true);
	if(message == null)
	    JOptionPane.showMessageDialog(parent, 
				"BlueJ configuration problem:\n" +
				"text not found for message ID\n" +
				msgID + "\n" + text);
	else
	    JOptionPane.showMessageDialog(parent, message + "\n" + text, 
					  "Error", JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Brings up a two or three button question dialog. If button3 is null
     * only the first two buttons are shown. Returns the button index that
     * was selected (0..2).
     */
    public static int askQuestion(JFrame parent, String text, 
				  String button1, String button2, String button3)
    {
	Object[] options;
	if (button3 == null)
	    options = new Object[] { button1, button2 };
	else
	    options = new Object[] { button1, button2, button3 };

	return JOptionPane.showOptionDialog(parent, text, "Question", 
					    JOptionPane.DEFAULT_OPTION, 
					    JOptionPane.WARNING_MESSAGE, 
					    null, options, options[0]);
    }


    /**
     *
     */
    public static String askString(JFrame parent, String prompt, String title,
				   String defaultText)
    {
	String response = (String)JOptionPane.showInputDialog(parent, 
						prompt, 
						title, 
						JOptionPane.PLAIN_MESSAGE, 
						null, 
						null,
						defaultText);
	return response;
    }


    public static void NYI(JFrame frame)
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
