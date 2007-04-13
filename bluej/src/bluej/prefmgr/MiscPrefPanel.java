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
 * @version $Id: MiscPrefPanel.java 4929 2007-04-13 09:49:20Z mik $
 */
public class MiscPrefPanel extends JPanel implements PrefPanelListener
{
    private static final String bluejJdkURL = "bluej.url.javaStdLib";
    private static final String greenfootJdkURL = "greenfoot.url.javaStdLib";

    private JTextField jdkURLField;
    private JCheckBox linkToLibBox;
    private JCheckBox showUncheckedBox; // show "unchecked" compiler warning
    private JCheckBox showTestBox;
    private JCheckBox showTeamBox;
    private String jdkURLPropertyName;
     
    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public MiscPrefPanel()
    {
        if(Config.isGreenfoot()) {
            jdkURLPropertyName = greenfootJdkURL;
        }
        else {
            jdkURLPropertyName = bluejJdkURL;
        }
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        add(box);
        
        setBorder(BlueJTheme.generalBorder);

        box.add(Box.createVerticalGlue());

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
        box.add(docPanel);

        if(!Config.isGreenfoot()) {
            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

            JPanel testPanel = new JPanel(new GridLayout(0,1,0,0));
            {
                testPanel.setBorder(BorderFactory.createCompoundBorder(
                                              BorderFactory.createTitledBorder(
                                                     Config.getString("prefmgr.misc.tools.title")),
                                              BlueJTheme.generalBorder));
                testPanel.setAlignmentX(LEFT_ALIGNMENT);

                showTestBox = new JCheckBox(Config.getString("prefmgr.misc.showTesting"));
                testPanel.add(showTestBox);

                showTeamBox = new JCheckBox(Config.getString("prefmgr.misc.showTeam"));
                testPanel.add(showTeamBox);
            }
            box.add(testPanel);

            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

            JPanel vmPanel = new JPanel(new GridLayout(0,1,0,0));
            {
                vmPanel.setBorder(BorderFactory.createCompoundBorder(
                                              BorderFactory.createTitledBorder(
                                                     Config.getString("prefmgr.misc.vm.title")),
                                              BlueJTheme.generalBorder));
                vmPanel.setAlignmentX(LEFT_ALIGNMENT);

                showUncheckedBox = new JCheckBox(Config.getString("prefmgr.misc.showUnchecked"));
                if (Config.isJava15()) {
                    // "unchecked" warnings only occur in Java 5.
                    vmPanel.add(showUncheckedBox);
                }
            }
            box.add(vmPanel);
        }

        box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
    }

    public void beginEditing()
    {
        linkToLibBox.setSelected(PrefMgr.getFlag(PrefMgr.LINK_LIB));
        jdkURLField.setText(Config.getPropString(jdkURLPropertyName));
        if(!Config.isGreenfoot()) {
            showTestBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS));
            showTeamBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEAM_TOOLS));
            showUncheckedBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_UNCHECKED));
        }
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        PrefMgr.setFlag(PrefMgr.LINK_LIB, linkToLibBox.isSelected());
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, showTestBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_TEAM_TOOLS, showTeamBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_UNCHECKED, showUncheckedBox.isSelected());

            PkgMgrFrame.updateTestingStatus();
            PkgMgrFrame.updateTeamStatus();
        }
        
        String jdkURL = jdkURLField.getText();
        Config.putPropString(jdkURLPropertyName, jdkURL);
    }
}
