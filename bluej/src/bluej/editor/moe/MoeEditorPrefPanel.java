package bluej.editor.moe;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.Vector;

import bluej.Config;
import bluej.utility.Debug;
import bluej.prefmgr.*;

/**
 * A PrefPanel subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as a file (ZIP or JAR
 * archive) with an associated description.
 * 
 * @author Andrew Patterson
 * @version $Id: MoeEditorPrefPanel.java 265 1999-11-05 04:31:07Z ajp $
 */
public class MoeEditorPrefPanel extends JPanel implements PrefPanelListener {

    private JTextField sizeField;

	/**
	 * Setup the UI for the dialog and event handlers for the dialog's buttons.
	 * 
	 * @param title the title of the dialog
	 */
	public MoeEditorPrefPanel() {

        JLabel fontsizeTag = new JLabel("Font size");
	    {
			fontsizeTag.setAlignmentX(LEFT_ALIGNMENT);
	    }

        sizeField = new JTextField(4);
	    {
			sizeField.setAlignmentX(LEFT_ALIGNMENT);
	    }

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(Config.generalBorder);

		add(fontsizeTag);
		add(sizeField);
		add(Box.createGlue());
    }

    public void beginEditing()
    {
        sizeField.setText("10");            
    }
    
    public void revertEditing()
    {
    }
    
    public void commitEditing()
    {
    }
}

