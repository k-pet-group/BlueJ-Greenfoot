package bluej.prefmgr;

import javax.swing.*;
import java.awt.*;
import bluej.Config;
import bluej.pkgmgr.Package;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 *
 * @author  Andrew Patterson
 * @version $Id: MiscPrefPanel.java 1819 2003-04-10 13:47:50Z fisker $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    static final String prefpaneltitle = Config.getString("prefmgr.misc.prefpaneltitle");
    static final String jdkURLPropertyName = "bluej.url.javaStdLib";

    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JCheckBox autoIndentBox;
    private JCheckBox lineNumbersBox;
    private JCheckBox makeBackupBox;
    private JCheckBox useJdk14Box;
    private JCheckBox matchBracketsBox;

    private JTextField jdkURLField;
    private JCheckBox linkToLibBox;

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

        JPanel editorPanel = new JPanel(new GridLayout(4,2,0,0));
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
            editorPanel.add(new JLabel(" "));
            autoIndentBox = new JCheckBox(Config.getString("prefmgr.misc.autoindent"));
            editorPanel.add(autoIndentBox);
            lineNumbersBox = new JCheckBox(Config.getString("prefmgr.misc.displaylinenumbers"));
            editorPanel.add(lineNumbersBox);
            hilightingBox = new JCheckBox(Config.getString("prefmgr.misc.usesyntaxhilighting"));
            editorPanel.add(hilightingBox);
            makeBackupBox = new JCheckBox(Config.getString("prefmgr.misc.makeBackup"));
            editorPanel.add(makeBackupBox);
            matchBracketsBox= new JCheckBox(Config.getString("prefmgr.misc.matchBrackets"));
            editorPanel.add(matchBracketsBox);
        }
        add(editorPanel);

        add(Box.createVerticalStrut(Config.generalSpacingWidth));

        JPanel compilerPanel = new JPanel(new GridLayout(1,2,0,0));
        {
            compilerPanel.setBorder(BorderFactory.createCompoundBorder(
                                          BorderFactory.createTitledBorder(
                                                 Config.getString("prefmgr.misc.compiler.title")),
                                          Config.generalBorder));
            compilerPanel.setAlignmentX(LEFT_ALIGNMENT);

            useJdk14Box = new JCheckBox(Config.getString("prefmgr.misc.usejdk14"));
            compilerPanel.add(useJdk14Box);
            if(System.getProperty("java.vm.version").startsWith("1.3"))
                useJdk14Box.setEnabled(false);
        }
        add(compilerPanel);

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

        add(Box.createVerticalGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.getFlag(PrefMgr.HILIGHTING));
        autoIndentBox.setSelected(PrefMgr.getFlag(PrefMgr.AUTO_INDENT));
        lineNumbersBox.setSelected(PrefMgr.getFlag(PrefMgr.LINENUMBERS));
        makeBackupBox.setSelected(PrefMgr.getFlag(PrefMgr.MAKE_BACKUP));
        matchBracketsBox.setSelected(PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS));
        useJdk14Box.setSelected(PrefMgr.getFlag(PrefMgr.ENABLE_JDK14));
        linkToLibBox.setSelected(PrefMgr.getFlag(PrefMgr.LINK_LIB));
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
        catch (NumberFormatException nfe) { }

        PrefMgr.setFlag(PrefMgr.HILIGHTING, hilightingBox.isSelected());
        PrefMgr.setFlag(PrefMgr.AUTO_INDENT, autoIndentBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINENUMBERS, lineNumbersBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MAKE_BACKUP, makeBackupBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MATCH_BRACKETS, matchBracketsBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINK_LIB, linkToLibBox.isSelected());
        PrefMgr.setFlag(PrefMgr.ENABLE_JDK14, useJdk14Box.isSelected());

        Package.editorManager.refreshAll();

        String jdkURL = jdkURLField.getText();

        if (Config.getDefaultPropString(jdkURLPropertyName, "") == jdkURL)
            Config.removeProperty(jdkURLPropertyName);
        else
            Config.putPropString(jdkURLPropertyName, jdkURL);
    }
}
