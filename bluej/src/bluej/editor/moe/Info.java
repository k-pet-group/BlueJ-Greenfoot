package bluej.editor.moe;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import javax.swing.*;		// all the GUI components
import java.io.*;

/**
 ** @author Michael Kolling
 **
 **/

public final class Info extends JPanel

    implements ActionListener 
{
    static final ImageIcon helpImage = 
	new ImageIcon(Config.getImageFilename("image.help"));

    public static Font infoFont = new Font("SansSerif", Font.BOLD, 10);

    // -------- INSTANCE VARIABLES --------

    private JLabel line1;
    private JLabel line2;
    boolean isClear;
    JButton helpButton;
    String helpGroup;

    // ------------- METHODS --------------

    public Info()
    {
	super();
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createLineBorder(Color.black));
	setFont(infoFont);

	JPanel body = new JPanel(new GridLayout(0, 1));	// one col, many rows
	body.setBackground(MoeEditor.infoColor);
	line1 = new JLabel();
	line2 = new JLabel();
	body.add(line1);
	body.add(line2);
	add(body, BorderLayout.CENTER);

	helpButton = new JButton(helpImage);
	helpButton.setMargin(new Insets(0,0,0,0));
	helpButton.addActionListener(this);
	helpButton.setRequestFocusEnabled(false);   // never get focus
	add(helpButton, BorderLayout.EAST);
	helpButton.setVisible(false);

	isClear = true;
	helpGroup = "";
    }

    /**
     * display a one line message
     */
    public void message(String msg)
    {
	int newline = msg.indexOf('\n');
	if (newline == -1)
	    message (msg, "");
	else
	    message (msg.substring(0, newline), msg.substring(newline+1));
    }


    /**
     * display a two line message
     */
    public void message(String msg1, String msg2)
    {
	line1.setText(msg1);
	line2.setText(msg2);
	isClear = false;

	hideHelp();
    }


    /**
     * display a one line warning (message with beep)
     */
    public void warning(String msg)
    {
	message (msg);
	MoeEditorManager.editorManager.beep();
    }


    /**
     * display a two line warning (message with beep)
     */
    public void warning(String msg1, String msg2)
    {
	message (msg1, msg2);
	MoeEditorManager.editorManager.beep();
    }

 
    /**
     * clear the display
     */
    public void clear()
    {
	if (!isClear) {
	    message (" ", " ");
	    isClear = true;
	}
    }


    /**
     * 
     */
    public void setHelp(String helpGroup)
    {
	this.helpGroup = helpGroup;
	helpButton.setVisible(true);
    }

    /**
     * 
     */
    public void hideHelp()
    {
	helpButton.setVisible(false);
    }

    // ---- ActionListener interface ----

    public void actionPerformed(ActionEvent evt)
    {
	displayHelp(helpGroup);
    }

    private void displayHelp(String helpGroup)
    {
	String fileName = Config.getHelpFilename(helpGroup);
	BufferedReader in = null; 
	boolean found = false;

	try {
	    //Debug.message("message: #"+line1.getText()+"#");
	    in = new BufferedReader(new FileReader(fileName));
	    String msg;
	    String line;
	    String helptext = "";
	    while ((msg = in.readLine()) != null) {
		if(line1.getText().startsWith(msg)) {
		    // found it - read help text
		    line = in.readLine();
		    while ((line != null) && (line.length() > 0)) {
			helptext += "\n" + line;
			line = in.readLine();
		    }
		    Utility.showMessage(null, helptext);
		    found = true;
		    break;
		}
		else {
		    // skip help text
		    line = in.readLine();
		    while ((line != null) && (line.length() > 0))
			line = in.readLine();
		}
	    }
	    in.close();
	    if(! found)
		Utility.showMessage(null, 
				    "No help available for this message.");
	}
	catch(IOException e) {
	    Utility.showError(null, "Cannot read help file:\n" + fileName);
	}
    }


}  // end class Info
