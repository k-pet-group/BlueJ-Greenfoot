package bluej.utility;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.beans.*;
import bluej.Config;
import bluej.pkgmgr.LibraryBrowserPkgMgrFrame;
					
/**
 * A basic JOptionPane message box with a "don't display this message again"
 * checkbox to allow users to prevent it being shown again.  Best used with
 * simple informational messages that are quickly acknowledged and will get 
 * in the way with repeated viewings.  A callback method is used to tell the
 * parent class whether to show the dialog again or not when the dialog exits
 * 
 * @author $Author: mik $
 * @version $Id: ToggleMessageBox.java 36 1999-04-27 04:04:54Z mik $
 */
public class ToggleMessageBox extends JDialog 

    implements ActionListener, ItemListener 
{
    private static JButton ok = new JButton("Ok");
    private static JCheckBox toggle = new JCheckBox(Config.getString("browser.togglemessagebox.toggle.text"));
    private static JPanel optionPanel = new JPanel(new BorderLayout());
    private static JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private int id = 0;
    private ToggleMessageBoxOwner parent = null;
    private boolean showAgain = true;

    /**
     * @param parent a ToggleMessageBoxOwner implementer which must also be a JFrame subclass
     * @param text the body of the message
     * @param title the dialog title
     * @param type a valid JOptionPane message type (determines Icon type)
     * @param id a unique identifier for this dialog (used in callback)
     */
    public ToggleMessageBox(ToggleMessageBoxOwner parent, String text, 
			    String title, int type, int id) 
    {
	super((JFrame)parent, title);
	
	this.parent = parent;
	this.id = id;
		
	ok.addActionListener(this);
	toggle.addItemListener(this);
		
	okPanel.add(ok);
	optionPanel.add(toggle, BorderLayout.NORTH);
	optionPanel.add(okPanel, BorderLayout.SOUTH);
	JComponent[] options = {optionPanel};

	JOptionPane optionPane = new JOptionPane(text,
						 type,
						 JOptionPane.DEFAULT_OPTION,
						 null,
						 options);
			
		
	setContentPane(optionPane);
	setResizable(false);
	pack();
	setVisible(false);
    }
	
    public void display() 
    {
	setVisible(true);
    }

    /**
     * Set the showAgain flag based on the current state of the checkbox.
     */
    public void itemStateChanged(ItemEvent ie) 
    {
	Object source = ie.getItemSelectable();
	if (source == toggle) {
	    if (ie.getStateChange() == ItemEvent.SELECTED)
		showAgain = false;
	    else
		showAgain = true;
	}
    }
	
    /**
     * Hide the dialog and alert the parent class to the state of the dialog
     * (i.e., show again or not)
     */
    public void actionPerformed(ActionEvent ae) 
    {
	setVisible(false);
	parent.showDialogAgain(id, showAgain);
    }
	
}
