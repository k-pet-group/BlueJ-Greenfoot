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
 * @author  Andrew Patterson
 * @version $Id: MiscPrefPanel.java 304 1999-12-09 23:48:13Z ajp $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{

    static final String prefpaneltitle = Config.getString("prefmgr.misc.prefpaneltitle");

    private JTextField editorFontField;

    /**
     * Registers the misc preference panel with the preferences
     * dialog
     */
    public static void register()
    {
        MiscPrefPanel p = new MiscPrefPanel();

        PrefMgrDialog.add(p, prefpaneltitle, p);
    }

	/**
	 * Setup the UI for the dialog and event handlers for the buttons.
	 */
	private MiscPrefPanel()
	{

		JLabel userLibrariesTag = new JLabel(Config.getString("Editor font size"));
		{
			userLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
		}

        editorFontField = new JTextField(16);
        {
            editorFontField.setAlignmentX(LEFT_ALIGNMENT);
        }

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(Config.generalBorder);

		add(userLibrariesTag);
		add(Box.createVerticalStrut(Config.generalSpacingWidth));
		add(editorFontField);
		add(Box.createGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        int newFontSize = 0;

        try {
            newFontSize = Integer.parseInt(editorFontField.getText());

            PrefMgr.setEditorFontSize(newFontSize);
        }
        catch (NumberFormatException nfe)
        {
        }
    }

}
