package bluej.editor.moe;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;		// all the GUI components

/**
 ** Moe preference settings. This class manages the dialog and the settings.
 **
 ** @author Michael Kolling
 **/

public final class Preferences extends JDialog

    implements ActionListener
{
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");

  // -------- CONSTANTS --------

    // 
  
  // -------- INSTANCE VARIABLES --------

    private int fontSize;		// editor font size
    private boolean cancelled;		// last dialog cancelled

    JButton okayButton;
    JButton cancelButton;
    JTextField sizeField;

  // ------------- METHODS --------------

    public Preferences()
    {
	super((Frame)null, "Editor Preferences", true);
	fontSize = 12;

	makeDialog();
    }

    public int getFontSize()
    {
	return fontSize;
    }

    /**
     *  Show dialog and return true if confirmed, false if cancelled.
     */
    public boolean showDialog(JFrame parent)
    {
	if(parent != null)
	    DialogManager.centreWindow(this, parent);

	sizeField.setText("" + fontSize);
	sizeField.selectAll();
	sizeField.requestFocus();
	setVisible(true);

	// the dialog is modal, so when we get here it was closed.
	return !cancelled;
    }

    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
	Object src = evt.getSource();
	if(src == okayButton) {
	    readSettings();
	    cancelled = false;
	}
	else if(src == cancelButton)
	    cancelled = true;

	setVisible(false);
    }

    /**
     * Read the settings from the dialog into the current settings.
     */
    private void readSettings()
    {
	String sizeText = sizeField.getText();
	try {
	    int size = Integer.parseInt(sizeText);
	    if(size>=9 && size<=72)
		fontSize = size;
	}
	catch(NumberFormatException e) {
	    // ignore
	}
    }

    private void makeDialog()
    {
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		cancelled = true;
		setVisible(false);
	    }
	});

	// button panel 

	JPanel buttonPanel = new JPanel(new FlowLayout());

	okayButton = new JButton(okay);
	buttonPanel.add(okayButton);
	okayButton.addActionListener(this);
	getRootPane().setDefaultButton(okayButton);

	cancelButton = new JButton(cancel);
	buttonPanel.add(cancelButton);
	cancelButton.addActionListener(this);

	getContentPane().add(buttonPanel, BorderLayout.SOUTH);

	// panel for preference options

	JPanel optionsPanel = new JPanel();
	optionsPanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));
	optionsPanel.setLayout(new GridLayout(0,1));

	    JPanel sizePanel = new JPanel(new FlowLayout());
	    sizePanel.add(new JLabel("Font size:"));
	    sizeField = new JTextField(4);
	    sizePanel.add(sizeField);
	
	optionsPanel.add(sizePanel);

	getContentPane().add(optionsPanel, BorderLayout.CENTER);
	pack();
    }

}  // end class Preferences
