/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.prefmgr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 *
 * @author  Andrew Patterson
 */
public class MiscPrefPanel extends JPanel 
                           implements PrefPanelListener
{
    private static final String bluejJdkURL = "bluej.url.javaStdLib";
    private static final String greenfootJdkURL = "greenfoot.url.javaStdLib";
   
    private JTextField jdkURLField;
    private JCheckBox linkToLibBox;
    private JCheckBox showUncheckedBox; // show "unchecked" compiler warning
    private String jdkURLPropertyName;
    private JTextField playerNameField;
     
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
        
        if (Config.isGreenfoot()) {
            JPanel playerNamePanel = new JPanel();
                      
            playerNamePanel.setLayout(new BoxLayout(playerNamePanel, BoxLayout.Y_AXIS));
            String playerNameTitle = Config.getString("prefmgr.misc.playername.title");
            playerNamePanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(playerNameTitle),
                                        BlueJTheme.generalBorder));
            playerNamePanel.setAlignmentX(LEFT_ALIGNMENT);
            
            // get Accelerator text
            String shortcutText = " ";
            KeyStroke accelerator = Config.GREENFOOT_SET_PLAYER_NAME_SHORTCUT;
            if (accelerator != null) {
                int modifiers = accelerator.getModifiers();
                if (modifiers > 0) {
                    shortcutText += KeyEvent.getKeyModifiersText(modifiers);
                    shortcutText += Config.isMacOS() ? "" : "+";
                }

                int keyCode = accelerator.getKeyCode();
                if (keyCode != 0) {
                    shortcutText += KeyEvent.getKeyText(keyCode);
                } else {
                    shortcutText += accelerator.getKeyChar();
                }
            }
            
            playerNamePanel.add(new JLabel(Config.getString("playername.dialog.help")));
            
            playerNameField = new JTextField(Config.getPropString("extensions.rmiextension.RMIExtension.settings.greenfoot.player.name", "Player"), 20);
            playerNameField.setMaximumSize(playerNameField.getPreferredSize());
            JPanel namePanel = new JPanel();
            namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
            namePanel.add(playerNameField);
            namePanel.add(Box.createHorizontalGlue());
            playerNamePanel.add(namePanel);
            
            
            JLabel playerNameNote = new JLabel(
                    Config.getString("prefmgr.misc.playerNameNote") + shortcutText);
            Font smallFont = playerNameNote.getFont().deriveFont(10);
            playerNameNote.setFont(smallFont);
            playerNamePanel.add(playerNameNote);
                        
            box.add(playerNamePanel);
        }
        else {
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
            
            {
                JPanel dataCollectionPanel = new JPanel();
                dataCollectionPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Data Collection"),
                         BlueJTheme.generalBorder));
                dataCollectionPanel.setAlignmentX(LEFT_ALIGNMENT);
                
                {
                    final JLabel statusLabel = new JLabel(DataCollector.getOptInOutStatus());
                    JButton optButton = new JButton("Change");
                    optButton.addActionListener(new ActionListener() {
                        
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            DataCollector.changeOptInOut();
                            statusLabel.setText(DataCollector.getOptInOutStatus());
                        }
                    });
                    JPanel statusPanel = new JPanel();
                    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
                    statusPanel.add(statusLabel);
                    statusPanel.add(Box.createRigidArea(new Dimension(15, 0)));
                    statusPanel.add(optButton);
                    dataCollectionPanel.add(statusPanel);
                }
                box.add(dataCollectionPanel);
            }
        }
        box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
    }

    public void beginEditing()
    {
        linkToLibBox.setSelected(PrefMgr.getFlag(PrefMgr.LINK_LIB));
        jdkURLField.setText(Config.getPropString(jdkURLPropertyName));
        if(!Config.isGreenfoot()) {
            showUncheckedBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_UNCHECKED));
        }
        else
        {
            playerNameField.setText(Config.getPropString("extensions.rmiextension.RMIExtension.settings.greenfoot.player.name", "Player"));
        }
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        PrefMgr.setFlag(PrefMgr.LINK_LIB, linkToLibBox.isSelected());
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_UNCHECKED, showUncheckedBox.isSelected());

            PkgMgrFrame.updateTestingStatus();
            PkgMgrFrame.updateTeamStatus();
            PkgMgrFrame.updateJavaMEstatus(); 
        }
        
        String jdkURL = jdkURLField.getText();
        Config.putPropString(jdkURLPropertyName, jdkURL);

        if (Config.isGreenfoot()) {
            Config.putPropString("extensions.rmiextension.RMIExtension.settings.greenfoot.player.name", playerNameField.getText());
        }
        
    }
}
