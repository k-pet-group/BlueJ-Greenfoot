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
package greenfoot.gui.export;

import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DBox;
import bluej.utility.DialogManager;
import bluej.utility.MiksGridLayout;

/**
 * Display a "proxy authentication required" dialog, prompting for username and password.
 * 
 * @author Davin McCall
 */
public class ProxyAuthDialog extends JDialog
{
    private JTextField usernameField;
    private JTextField passwordField;
    
    private int result;
    public static final int OK = 0;
    public static final int CANCEL = 1;
    
    /**
     * Construct a new proxy authentication dialog
     */
    public ProxyAuthDialog(Window parentWindow)
    {
        super(parentWindow, ModalityType.APPLICATION_MODAL);
        setTitle(Config.getString("export.publish.proxyAuth"));
        buildUI();
        DialogManager.centreDialog(this);
    }
    
    /**
     * Get the result - either OK or CANCEL.
     */
    public int getResult()
    {
        return result;
    }

    /**
     * Get the username entered by the user.
     */
    public String getUsername()
    {
        return usernameField.getText();
    }
    
    /**
     * Get the password entered by the user.
     */
    public String getPassword()
    {
        return passwordField.getText();
    }
    
    /**
     * Build the user interface
     */
    private void buildUI()
    {
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        
        contentPane.setBorder(BlueJTheme.generalBorder);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        
        JLabel msgLabel = new JLabel(Config.getString("export.publish.needProxyAuth"));
        msgLabel.setAlignmentX(0.0f);
        contentPane.add(msgLabel);
        
        contentPane.add(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge));
        
        LayoutManager lm = new MiksGridLayout(2, 2, BlueJTheme.componentSpacingSmall, BlueJTheme.componentSpacingSmall);
        JPanel authPanel = new JPanel(lm);
        
        authPanel.add(new JLabel(Config.getString("export.publish.username")));
        usernameField = new JTextField(20);
        authPanel.add(usernameField);
        
        authPanel.add(new JLabel(Config.getString("export.publish.password")));
        passwordField = new JTextField(20);
        authPanel.add(passwordField);
        
        JButton okButton = new JButton(Config.getString("okay"));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                result = OK;
                dispose();
            }
        });
        
        JButton cancelButton = new JButton(Config.getString("cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                result = CANCEL;
                dispose();
            }
        });
        
        DBox buttonBox = new DBox(DBox.X_AXIS, BlueJTheme.commandButtonSpacing, BlueJTheme.commandButtonSpacing, 0.5f);
        
        buttonBox.add(Box.createHorizontalGlue());
        DialogManager.addOKCancelButtons(buttonBox, okButton, cancelButton);
        
        contentPane.add(authPanel);
        contentPane.add(buttonBox);
        getRootPane().setDefaultButton(okButton);
        
        pack();
    }    
}
