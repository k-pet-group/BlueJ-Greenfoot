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
 * @version $Id: MiscPrefPanel.java 1096 2002-01-15 11:15:33Z mik $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    static final String prefpaneltitle = Config.getString("prefmgr.misc.prefpaneltitle");
    static final String jdkURLPropertyName = "bluej.url.javaStdLib";

    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JCheckBox lineNumbersBox;
    private JCheckBox makeBackupBox;
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

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Config.generalBorder);

        add(Box.createVerticalGlue());

        JPanel editorPanel = new JPanel(new GridLayout(2,2,0,0));
        {
            String editorTitle = Config.getString("prefmgr.misc.editor.title");
            editorPanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(editorTitle),
                                        Config.generalBorder));
            editorPanel.setAlignmentX(LEFT_ALIGNMENT);

            JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            {
                fontPanel.add(new JLabel(Config.getString("prefmgr.misc.editorfontsize")));
                editorFontField = new JTextField(4);
                fontPanel.add(editorFontField);
            }
            editorPanel.add(fontPanel);

            lineNumbersBox = new JCheckBox(Config.getString("prefmgr.misc.displaylinenumbers"));
            editorPanel.add(lineNumbersBox);
            hilightingBox = new JCheckBox(Config.getString("prefmgr.misc.usesyntaxhilighting"));
            editorPanel.add(hilightingBox);
            makeBackupBox = new JCheckBox(Config.getString("prefmgr.misc.makeBackup"));
            editorPanel.add(makeBackupBox);
        }
        add(editorPanel);

        add(Box.createVerticalStrut(Config.generalSpacingWidth));

        JPanel docPanel = new JPanel();
        {
            docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
            String docTitle = Config.getString("prefmgr.misc.documentation.title");
            docPanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(docTitle),
                                        Config.generalBorder));
            docPanel.setAlignmentX(LEFT_ALIGNMENT);

            JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
            {
                urlPanel.add(new JLabel(Config.getString("prefmgr.misc.jdkurlpath")), 
                             BorderLayout.WEST);
                jdkURLField = new JTextField(32);
                urlPanel.add(jdkURLField, BorderLayout.CENTER);
            }
            urlPanel.setAlignmentX(LEFT_ALIGNMENT);
            docPanel.add(urlPanel);

            docPanel.add(Box.createVerticalStrut(Config.generalSpacingWidth));

            linkToLibBox = new JCheckBox(Config.getString("prefmgr.misc.linkToLib"));
            linkToLibBox.setAlignmentX(LEFT_ALIGNMENT);
            docPanel.add(linkToLibBox);

            docPanel.add(Box.createVerticalStrut(Config.generalSpacingWidth));

            JLabel linkToLibNoteLine1 = new JLabel(
                              Config.getString("prefmgr.misc.linkToLibNoteLine1"));
            Font smallFont = linkToLibNoteLine1.getFont().deriveFont(10);
            linkToLibNoteLine1.setFont(smallFont);
            linkToLibNoteLine1.setAlignmentX(LEFT_ALIGNMENT);
            docPanel.add(linkToLibNoteLine1);

            JLabel linkToLibNoteLine2 = new JLabel(
                              Config.getString("prefmgr.misc.linkToLibNoteLine2"));
            linkToLibNoteLine2.setFont(smallFont);
            linkToLibNoteLine2.setAlignmentX(LEFT_ALIGNMENT);
            docPanel.add(linkToLibNoteLine2);
        }
        add(docPanel);

        add(Box.createVerticalStrut(Config.generalSpacingWidth));

        JPanel notationPanel = new JPanel();
        {
            notationPanel.setLayout(new GridLayout(0,1));
            String notationTitle = Config.getString("prefmgr.misc.notation.title");
            notationPanel.setBorder(BorderFactory.createCompoundBorder(
                                          BorderFactory.createTitledBorder(notationTitle),
                                          Config.generalBorder));
            notationPanel.setAlignmentX(LEFT_ALIGNMENT);

            notationStyleGroup = new ButtonGroup();
            umlRadioButton = 
                new JRadioButton(Config.getString("prefmgr.misc.notation.uml"));
            blueRadioButton = 
                new JRadioButton(Config.getString("prefmgr.misc.notation.blue"));
            umlRadioButton.setActionCommand(Graph.UML);
            blueRadioButton.setActionCommand(Graph.BLUE);
            notationStyleGroup.add(umlRadioButton);
            notationStyleGroup.add(blueRadioButton);

            notationPanel.add(umlRadioButton);
            notationPanel.add(blueRadioButton);
        }
        add(notationPanel);

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
                  compilerPanel.add(new JTextField(8));
                  }
        */

        add(Box.createVerticalGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.useSyntaxHilighting());
        lineNumbersBox.setSelected(PrefMgr.displayLineNumbers());
        makeBackupBox.setSelected(PrefMgr.makeBackup());
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
        catch (NumberFormatException nfe) { }

        PrefMgr.setSyntaxHilighting(hilightingBox.isSelected());
        PrefMgr.setDisplayLineNumbers(lineNumbersBox.isSelected());
        PrefMgr.setMakeBackup(makeBackupBox.isSelected());
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
}
