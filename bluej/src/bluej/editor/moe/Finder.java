// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@monash.edu.au

package bluej.editor.moe;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;		// all the GUI components

import bluej.Config;
import bluej.utility.Debug;

/**
 ** @author Michael Kolling
 **
 **/

public final class Finder extends JDialog

    implements ActionListener
{
    static final String title = Config.getString("editor.find.title");
    static final String textfieldLabel = Config.getString("editor.find.textfield.label");
    static final String cancel = Config.getString("cancel");
    static final String forwardText = Config.getString("editor.find.forward");
    static final String backwardText = Config.getString("editor.find.backward");

    // -------- CONSTANTS --------

    // search direction for the finder
    static final int FORWARD = 0; 
    static final int BACKWARD = 1;
  
  // -------- INSTANCE VARIABLES --------

    private String searchString;	// the last search string used
    private boolean searchFound;	// true if last find was successfull
    private int searchDirection;	// direction of search
    private boolean cancelled;		// last dialog cancelled

    JButton forwardButton;
    JButton backwardButton;
    JButton cancelButton;
    JTextField textField;

  // ------------- METHODS --------------

    public Finder()
    {
	super((Frame)null, title, true);

	searchString = null;
	searchFound = true;
	searchDirection = FORWARD;

	makeDialog();
    }

    /**
     * set the search string
     */
    public void setSearchString(String s)
    {
	searchString = s;
    }

    /**
     * Ask the user for input of search details via a dialogue.
     *  Returns null if operation was cancelled.
     *
     * @param
     * @param direction  either FORWARD or BACKWARD
     */
    public String getNewSearchString(JFrame parent, int direction)
    {
	if(direction == FORWARD)
	    getRootPane().setDefaultButton(forwardButton);
	if(direction == BACKWARD)
	    getRootPane().setDefaultButton(backwardButton);

	textField.selectAll();
	textField.requestFocus();
	setVisible(true);

	// the dialog is modal, so when we get here it was closed.
	if(cancelled)
	    return null;
	else
	    return textField.getText();
    }

    /**
     * return the last search string
     */
    public String getLastSearchString()
    {
	return searchString;
    }

    /**
     * return the direction chosen in the last dialog
     */
    public int getDirection()
    {
	return searchDirection;
    }

    /**
     * set last search found
     */
    public void setSearchFound(boolean found)
    {
	searchFound = found;
    }

    /**
     * return info whether the last search was successful
     */
    public boolean lastSearchFound()
    {
	return searchFound;
    }

    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
	Object src = evt.getSource();
	if(src == forwardButton) {
	    searchDirection = FORWARD;
	    cancelled = false;
	}
	else if(src == backwardButton) {
	    searchDirection = BACKWARD;
	    cancelled = false;
	}
	else if(src == cancelButton)
	    cancelled = true;

	setVisible(false);
    }

    private void makeDialog()
    {
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		cancelled = true;
		setVisible(false);
	    }
	});

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());

	forwardButton = new JButton(forwardText);
	buttonPanel.add(forwardButton);
	forwardButton.addActionListener(this);

	backwardButton = new JButton(backwardText);
	buttonPanel.add(backwardButton);
	backwardButton.addActionListener(this);

	cancelButton = new JButton(cancel);
	buttonPanel.add(cancelButton);
	cancelButton.addActionListener(this);

	getContentPane().add("South", buttonPanel);

	JPanel textPanel = new JPanel();
	textPanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));
	textPanel.setLayout(new GridLayout(0,1));

	textPanel.add(new JLabel(textfieldLabel));
	textField = new JTextField(16);
	textPanel.add(textField);
	
	getContentPane().add("Center", textPanel);
	pack();
    }

}  // end class Finder
