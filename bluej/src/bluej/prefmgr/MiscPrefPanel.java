package bluej.prefmgr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import bluej.Config;
import bluej.BlueJTheme;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 *
 * @author  Andrew Patterson
 * @version $Id: MiscPrefPanel.java 2281 2003-11-05 17:43:53Z mik $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    static final String jdkURLPropertyName = "bluej.url.javaStdLib";

    private JTextField jdkURLField;
    private JCheckBox linkToLibBox;
    private JCheckBox optimiseBox;
    private JCheckBox showTestBox;

    private boolean optimiseMessageShown = false;
    
    /**
     * Registers the misc preference panel with the preferences
     * dialog
     */
    public static void register()
    {
        MiscPrefPanel p = new MiscPrefPanel();
        PrefMgrDialog.add(p, Config.getString("prefmgr.misc.prefpaneltitle"), p);
    }

    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    private MiscPrefPanel()
    {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

        add(Box.createVerticalGlue());

        JPanel docPanel = new JPanel();
        {
            docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
            String docTitle = Config.getString("prefmgr.misc.documentation.title");
            docPanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(docTitle),
                                        BlueJTheme.generalBorder));
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

            docPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

            linkToLibBox = new JCheckBox(Config.getString("prefmgr.misc.linkToLib"));
            linkToLibBox.setAlignmentX(LEFT_ALIGNMENT);
            docPanel.add(linkToLibBox);

            docPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

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

        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        JPanel testPanel = new JPanel(new GridLayout(1,2,0,0));
        {
            testPanel.setBorder(BorderFactory.createCompoundBorder(
                                          BorderFactory.createTitledBorder(
                                                 Config.getString("prefmgr.misc.testing.title")),
                                          BlueJTheme.generalBorder));
            testPanel.setAlignmentX(LEFT_ALIGNMENT);

            showTestBox = new JCheckBox(Config.getString("prefmgr.misc.showTesting"));
            testPanel.add(showTestBox);
        }
        add(testPanel);

        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        JPanel vmPanel = new JPanel(new GridLayout(1,2,0,0));
        {
            vmPanel.setBorder(BorderFactory.createCompoundBorder(
                                          BorderFactory.createTitledBorder(
                                                 Config.getString("prefmgr.misc.vm.title")),
                                          BlueJTheme.generalBorder));
            vmPanel.setAlignmentX(LEFT_ALIGNMENT);

            optimiseBox = new JCheckBox(Config.getString("prefmgr.misc.optimiseVM"));
            optimiseBox.addActionListener(new ActionListener() {
                                                public void actionPerformed(ActionEvent e) {
                                                    optimiseToggled();
                                                }
                                          });
            vmPanel.add(optimiseBox);
        }
        add(vmPanel);

        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        add(Box.createVerticalGlue());
    }

    public void beginEditing()
    {
        linkToLibBox.setSelected(PrefMgr.getFlag(PrefMgr.LINK_LIB));
        jdkURLField.setText(Config.getPropString(jdkURLPropertyName));
        showTestBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS));
        optimiseBox.setSelected(PrefMgr.getFlag(PrefMgr.OPTIMISE_VM));
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        PrefMgr.setFlag(PrefMgr.LINK_LIB, linkToLibBox.isSelected());
        PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, showTestBox.isSelected());
        PrefMgr.setFlag(PrefMgr.OPTIMISE_VM, optimiseBox.isSelected());

        PkgMgrFrame.updateTestingStatus();

        String jdkURL = jdkURLField.getText();

        if (Config.getDefaultPropString(jdkURLPropertyName, "") == jdkURL)
            Config.removeProperty(jdkURLPropertyName);
        else
            Config.putPropString(jdkURLPropertyName, jdkURL);
    }

    private void optimiseToggled() 
    {
        if(!optimiseMessageShown) {  // show only once per session
            DialogManager.showMessage(this, "pref-optimise-no-effect");
            optimiseMessageShown = true;
        }
    }

}
