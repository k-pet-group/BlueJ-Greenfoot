/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkProvider;
import bluej.utility.DBox;
import bluej.utility.EscapeDialog;
import bluej.utility.MultiWrapLabel;

/**
 * A dialog which displays an activity indicator while connection settings are
 * being verified
 * 
 * @author Davin McCall
 */
public class CheckConnectionDialog extends EscapeDialog
{
    private ActivityIndicator activityIndicator;
    private MultiWrapLabel connLabel;
    private JButton closeButton;
    
    private TeamSettings settings;
    private TeamworkProvider provider;
    
    public CheckConnectionDialog(Dialog owner, TeamworkProvider provider,
            TeamSettings settings)
    {
        super(owner, true);
        setTitle(Config.getString("team.settings.checkConnection"));
        
        this.provider = provider;
        this.settings = settings;
        
        buildUI();
        setLocationRelativeTo(owner);
    }
    
    private void buildUI()
    {
        DBox contentPane = new DBox(DBox.Y_AXIS, 0, BlueJTheme.componentSpacingLarge, 0.0f);
        contentPane.setBorder(BlueJTheme.dialogBorder);
        setContentPane(contentPane);
        
        connLabel = new MultiWrapLabel(Config.getString("team.checkconn.checking"));
        
        contentPane.add(connLabel);
        
        activityIndicator = new ActivityIndicator();
        activityIndicator.setRunning(true);
        contentPane.add(activityIndicator);
        
        closeButton = BlueJTheme.getCancelButton();
        closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);
                }
            });
        contentPane.add(closeButton);
        
        pack();
    }
    
    public void setVisible(boolean vis)
    {
        // Must start the thread before calling super.setVisible(), because
        // we are modal - super.setVisible() will block.
        if (vis) {
            new Thread() {
                public void run()
                {
                    final TeamworkCommandResult res = validateConnection();
                    EventQueue.invokeLater(new Runnable() {
                        public void run()
                        {
                            if (!res.isError()) {
                                connLabel.setText(Config.getString("team.checkconn.ok"));
                            }
                            else {
                                connLabel.setText(Config.getString("team.checkconn.bad")
                                        + System.getProperty("line.separator") + System.getProperty("line.separator")
                                        + res.getErrorMessage());
                            }
                            
                            activityIndicator.setRunning(false);
                            closeButton.setText(BlueJTheme.getCloseLabel());
                            pack();
                        }
                    });
                }
            }.start();
        }
        super.setVisible(vis);
    }
    
    private TeamworkCommandResult validateConnection()
    {
        return provider.checkConnection(settings);
    }   
}
