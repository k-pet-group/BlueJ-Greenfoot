package bluej.utility;

import javax.swing.*;
import java.awt.event.*;
import java.beans.*;

/**
 * A simple wrapper around a JDialog making it modeless.
 * 
 * @author $Author: mik $
 * @version $Id: ModelessMessageBox.java 36 1999-04-27 04:04:54Z mik $
 */
public class ModelessMessageBox extends JDialog 
{
    public ModelessMessageBox(JFrame parent, String text, String title, int type) {
	super(parent, title, false);
	final JOptionPane pane = new JOptionPane(text, type);
	this.setContentPane(pane);
	this.pack();
	this.setVisible(true);
	// copied from Sun Tutorial on Dialogs - 
	// apparently it intercepts the close dialog event
	pane.addPropertyChangeListener(
				       new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible() && (e.getSource() == pane)) {
		    setVisible(false);
		}
	    }
	});	
    }
}
