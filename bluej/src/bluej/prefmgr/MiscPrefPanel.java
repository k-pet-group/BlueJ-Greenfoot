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
 * @version $Id: MiscPrefPanel.java 352 2000-01-12 05:53:41Z ajp $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    static final String prefpaneltitle = Config.getString("prefmgr.misc.prefpaneltitle");
    static final String jdkURLPropertyName = "bluej.url.javaStdLib";


    private JTextField editorFontField;
    private JTextField jdkURLField;
    private JCheckBox hilightingBox;

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

		JLabel editorFontTag = new JLabel(Config.getString("Editor font size"));
		{
			editorFontTag.setAlignmentX(LEFT_ALIGNMENT);
		}

        editorFontField = new SingleLineTextField(8);
        {
            editorFontField.setAlignmentX(LEFT_ALIGNMENT);
        }

	    hilightingBox = new JCheckBox(Config.getString("Use syntax hilighting in editor"));

		JLabel jdkURLTag = new JLabel(Config.getString("JDK documentation URL"));
		{
			jdkURLTag.setAlignmentX(LEFT_ALIGNMENT);
		}
        jdkURLField = new SingleLineTextField(8);
        {
            jdkURLField.setAlignmentX(LEFT_ALIGNMENT);
        }


/*        JPanel compilerPanel = new JPanel();
        {
		    compilerPanel.setLayout(new BoxLayout(compilerPanel, BoxLayout.Y_AXIS));
            compilerPanel.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createTitledBorder("Compiler"),
                                    Config.generalBorder));
            compilerPanel.setAlignmentX(LEFT_ALIGNMENT);

            compilerPanel.add(new JRadioButton("internal"));
            compilerPanel.add(new JRadioButton("javac"));
            compilerPanel.add(new JRadioButton("jikes"));

            JLabel executableTag = new JLabel(Config.getString("Compiler Executable"));

		    compilerPanel.add(Box.createVerticalStrut(Config.generalSpacingWidth));
            compilerPanel.add(executableTag);
            compilerPanel.add(new SingleLineTextField(8));
        }
*/

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(Config.generalBorder);

		add(editorFontTag);
		add(editorFontField);
		add(Box.createVerticalStrut(Config.generalSpacingWidth));
		add(hilightingBox);
		add(Box.createVerticalStrut(Config.generalSpacingWidth));
		add(Box.createVerticalStrut(Config.generalSpacingWidth));
        add(jdkURLTag);
		add(jdkURLField);

		add(Box.createGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.useSyntaxHilighting());
        jdkURLField.setText(Config.getPropString(jdkURLPropertyName));
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

        PrefMgr.setSyntaxHilighting(hilightingBox.isSelected());

        String jdkURL = jdkURLField.getText();

        if (Config.getDefaultPropString(jdkURLPropertyName, "") == jdkURL)
            Config.removeProperty(jdkURLPropertyName);
        else
            Config.putPropString(jdkURLPropertyName, jdkURL);
    }

    class SingleLineTextField extends JTextField
    {
        public SingleLineTextField(int col)
        {
            super(col);
        }

        public Dimension getMaximumSize()
        {
            Dimension d = super.getPreferredSize();

            d.width = Integer.MAX_VALUE;

            return d;
        }
    }
}
