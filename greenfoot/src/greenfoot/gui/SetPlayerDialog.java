/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.EscapeDialog;

public class SetPlayerDialog extends EscapeDialog implements DocumentListener
{
    JTextField playerNameTextField;
    private JLabel errorMsgLabel;
    private JButton okButton;

    public SetPlayerDialog(JFrame parent, String curPlayerName)
    {
        super(parent, Config.getString("playername.dialog.title"), true);

        JPanel mainPanel = new JPanel();
        setContentPane(mainPanel);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);

        // help labels
        JLabel helpLabel1 = GreenfootUtil.createHelpLabel();
        helpLabel1.setText(Config.getString("playername.dialog.help"));
        helpLabel1.setAlignmentX(0.0f);
        mainPanel.add(helpLabel1);
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, 2 * BlueJTheme.generalSpacingWidth));
        
        JLabel label = new JLabel(Config.getString("playername.dialog.playerName"));
        label.setAlignmentX(0.0f);
        mainPanel.add(label);
                
        mainPanel.add(Box.createVerticalStrut(4));

        playerNameTextField = new JTextField();
        playerNameTextField.setAlignmentX(0.0f);
        Dimension playerNameMax = playerNameTextField.getMaximumSize();
        playerNameMax.height = playerNameTextField.getPreferredSize().height;
        playerNameTextField.setMaximumSize(playerNameMax);
        mainPanel.add(playerNameTextField);
        
        errorMsgLabel = new JLabel();
        errorMsgLabel.setAlignmentX(0.0f);
        errorMsgLabel.setVisible(false);
        errorMsgLabel.setForeground(Color.RED);
        errorMsgLabel.setText("Player name must not be blank, and must not contain spaces"); //TODO move to config
        mainPanel.add(errorMsgLabel);
                
        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        // push buttons over to the right using a glue component
        buttonPanel.add(Box.createHorizontalGlue());

        okButton = BlueJTheme.getOkButton();
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                ok();
            }
        });
        okButton.setEnabled(false);
        
        buttonPanel.add(okButton);
        
        getRootPane().setDefaultButton(okButton);
        buttonPanel.setAlignmentX(0.0f);

        // Limit the growth of the button panel
        Dimension buttonPanelMax = buttonPanel.getMaximumSize();
        buttonPanelMax.height = buttonPanel.getPreferredSize().height;
        buttonPanel.setMaximumSize(buttonPanelMax);
        
        playerNameTextField.getDocument().addDocumentListener(this);
        //playerNameTextField.setInputVerifier(this);
        playerNameTextField.setVerifyInputWhenFocusTarget(true);
        
        playerNameTextField.setText(curPlayerName);
        
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS,  2 * BlueJTheme.generalSpacingWidth));
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);
        pack();

        this.setLocationRelativeTo(parent);
    }

    private void ok()
    {
        dispose();
    }
    public String getPlayerName()
    {
        return playerNameTextField.getText();
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        checkValid();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        checkValid();        
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
    }
    
    private void checkValid()
    {
        String name = playerNameTextField.getText();
        boolean invalid = name == null || name.equals("") || name.matches(".*\\s.*");
        errorMsgLabel.setVisible(invalid);
        okButton.setEnabled(!invalid);
    }
}
