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
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.graph.Graph;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 *
 * @author  Andrew Patterson
 * @version $Id: MiscPrefPanel.java 762 2001-02-07 04:21:14Z mik $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    static final String prefpaneltitle = Config.getString("prefmgr.misc.prefpaneltitle");
    static final String jdkURLPropertyName = "bluej.url.javaStdLib";


    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JTextField jdkURLField;
    private JCheckBox linkToLibBox;

    ButtonGroup notationStyleGroup;
    private JRadioButton umlRadioButton;
    private JRadioButton blueRadioButton;

    //private

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

        JLabel editorFontTag = new JLabel(Config.getString("prefmgr.misc.editorfontsize"));
        {
            editorFontTag.setAlignmentX(LEFT_ALIGNMENT);
        }

        editorFontField = new SingleLineTextField(8);
        {
            editorFontField.setAlignmentX(LEFT_ALIGNMENT);
        }

        hilightingBox = new JCheckBox(Config.getString("prefmgr.misc.usesyntaxhilighting"));

        JLabel jdkURLTag = new JLabel(Config.getString("prefmgr.misc.jdkurlpath"));
        {
            jdkURLTag.setAlignmentX(LEFT_ALIGNMENT);
        }

        jdkURLField = new SingleLineTextField(8);
        {
            jdkURLField.setAlignmentX(LEFT_ALIGNMENT);
        }

        linkToLibBox = new JCheckBox(Config.getString("prefmgr.misc.linkToLib"));

        JLabel linkToLibNoteLine1 = new JLabel(
                            Config.getString("prefmgr.misc.linkToLibNoteLine1"));
        Font smallFont = linkToLibNoteLine1.getFont().deriveFont(10);
        {
            //linkToLibNote.setAlignmentX(LEFT_ALIGNMENT);
            linkToLibNoteLine1.setFont(smallFont);
        }

        JLabel linkToLibNoteLine2 = new JLabel(
                            Config.getString("prefmgr.misc.linkToLibNoteLine2"));
        {
            //linkToLibNote.setAlignmentX(LEFT_ALIGNMENT);
            linkToLibNoteLine2.setFont(smallFont);
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
        add(linkToLibBox);
        add(linkToLibNoteLine1);
        add(linkToLibNoteLine2);
        add(Box.createVerticalStrut(Config.generalSpacingWidth));
        add(Box.createVerticalStrut(Config.generalSpacingWidth));
        add(Box.createVerticalStrut(Config.generalSpacingWidth));

        JPanel notationPanel = new JPanel();
        {
            notationPanel.setLayout(new BoxLayout(notationPanel, BoxLayout.Y_AXIS));
            notationPanel.setBorder(BorderFactory.createCompoundBorder(
                                                                       BorderFactory.createTitledBorder("Notation Style"),
                                                                       Config.generalBorder));
            notationPanel.setAlignmentX(LEFT_ALIGNMENT);

            notationStyleGroup = new ButtonGroup();
            umlRadioButton = new JRadioButton("Unified Modeling Language (UML)");
            blueRadioButton = new JRadioButton("Blue");
            umlRadioButton.setActionCommand(Graph.UML);
            blueRadioButton.setActionCommand(Graph.BLUE);
            notationStyleGroup.add(umlRadioButton);
            notationStyleGroup.add(blueRadioButton);

            notationPanel.add(umlRadioButton);
            notationPanel.add(blueRadioButton);
        }

        add(notationPanel);


        add(Box.createGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.useSyntaxHilighting());
        linkToLibBox.setSelected(PrefMgr.linkDocToLibrary());
        jdkURLField.setText(Config.getPropString(jdkURLPropertyName));

        if(!PrefMgr.isUML())
            blueRadioButton.setSelected(true);
        else
            umlRadioButton.setSelected(true);
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
        PrefMgr.setDocumentationLinking(linkToLibBox.isSelected());

        Package.editorManager.refreshAll();

        String jdkURL = jdkURLField.getText();

        if (Config.getDefaultPropString(jdkURLPropertyName, "") == jdkURL)
            Config.removeProperty(jdkURLPropertyName);
        else
            Config.putPropString(jdkURLPropertyName, jdkURL);

        String notationStyle = notationStyleGroup.getSelection().getActionCommand();
        PrefMgr.setNotationStyle(notationStyle);
        PkgMgrFrame.refreshAllFrames();
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
