package bluej.prefmgr;

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
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 * 
 * @author Andrew Patterson
 * @version $Id: MiscPrefPanel.java 278 1999-11-16 00:58:12Z ajp $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener {
  
    static final String prefpaneltitle = Config.getString("misc.prefpaneltitle");

    /**
     * Registers the misc preference panel with the preferences
     * dialog
     */
    public static void register() {
        MiscPrefPanel p = new MiscPrefPanel();
        
        PrefMgrDialog.add(p, prefpaneltitle, p);
    }

	/**
	 * Setup the UI for the dialog and event handlers for the buttons.
	 */
	public MiscPrefPanel() {

		// Construct a user editable table of user libraries and add/remove buttons

		JLabel userLibrariesTag = new JLabel(Config.getString("classmgr.userlibraries"));
		{
			userLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
		}

		JCheckBox saveWindowState = new JCheckBox("Save window state");
		{
			saveWindowState.setAlignmentX(LEFT_ALIGNMENT);
		}

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(Config.generalBorder);

		add(userLibrariesTag);
		add(Box.createVerticalStrut(Config.generalSpacingWidth));
		add(saveWindowState);
    }

    public void beginEditing()
    {
    }
    
    public void revertEditing()
    {
    }
    
    public void commitEditing()
    {
    }

}
