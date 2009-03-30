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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import bluej.BlueJTheme;
import bluej.groupwork.actions.UpdateDialogAction;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.Config;
import bluej.groupwork.actions.CommitCommentAction;

/**
 * This panel shows to buttons labeled "Update from Repository" and
 * "Commit to Repository" and a checkBox.
 *
 * @author fisker
 *
 */
public class TeamControlsPanel extends JPanel
{
    
    private JButton commitButton;
    private JButton updateButton;
    private PkgMgrFrame pmf;
    private JCheckBox includeGraphLayoutCheckBox;
    private JPanel helpPanel = null;
    
    private CommitCommentAction commitCommentAction;
    private UpdateDialogAction updateAction;
    
    /**
     * Create a TeamControlsPanel with a reference to the PkgMgrFrame holding the
     * project it is to work on. If the reference to Project is null, all graphical
     * elements are greyed out.
     * @param project
     */
    public TeamControlsPanel(PkgMgrFrame pmf)
    {
        this.pmf = pmf;
        updateAction = new UpdateDialogAction();
       
        commitCommentAction = new CommitCommentAction();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel codeSynchPanel = new JPanel();
        {
            codeSynchPanel.setLayout(new BoxLayout(codeSynchPanel, BoxLayout.Y_AXIS));
            
            codeSynchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Code Synchronization"),
                BlueJTheme.generalBorder));
            
            //teamControlsPanel.setBorder(BlueJTheme.dialogBorder);
            codeSynchPanel.setAlignmentX(CENTER_ALIGNMENT);
            // Commit button
            commitButton = new JButton(commitCommentAction);
            
            // Update button
            updateButton = new JButton(updateAction);
            
            
            // IncludeGraphLayoutCheckbox
            includeGraphLayoutCheckBox = new JCheckBox("Include graph layout");
            
            //allow the Add and Delete buttons to be resized to equal width
            commitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                commitButton.getPreferredSize().height));
            updateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                updateButton.getPreferredSize().height));
            
            codeSynchPanel.add(commitButton);
            codeSynchPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            codeSynchPanel.add(updateButton);
            codeSynchPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            codeSynchPanel.add(includeGraphLayoutCheckBox);
            
            add(codeSynchPanel);
            
        }
        doGreyOut(pmf.getProject() == null);
        configureHelp();
    }
    
    
    public void doGreyOut(boolean greyout)
    {
        updateAction.setEnabled(!greyout);
        commitCommentAction.setEnabled(!greyout);
        includeGraphLayoutCheckBox.setEnabled(!greyout);
    }
    
    public boolean includeGraphLayout()
    {
        return includeGraphLayoutCheckBox.isSelected();
    }
    
    /**
     *
     */
    public void configureHelp()
    {
        if (pmf.getProject() == null)
        {
            setHelp("To get a project from the repository, open the Team menu" +
                "and select Checkout Project...");
            doGreyOut(true);
            return;
        }
        
        //The project is not a team project. Show help and greyout
        if (!pmf.getProject().isTeamProject())
        {
            setHelp("This project is not a team project." + Config.nl +
                "To share this project, open the Team menu and select " +
                "Share Project..." + Config.nl +
                "To get a project from the repository, open the Team menu " +
                "and select Checkout Project...");
            doGreyOut(true);
        }
        
        //everything is good. Show no help and show all buttons
        if (pmf.getProject().isTeamProject())
        {
            if (helpPanel != null)
            {
                remove(helpPanel);
            }
            doGreyOut(false);
        }
        
    }
    
    private void setHelp(String helpStr)
    {
        JPanel p = makeHelpPanel(helpStr);
        setHelpPanel(p);
    }
    
    
    private JPanel makeHelpPanel(String input)
    {
        JPanel panel = new JPanel();
        {
            panel.setBorder(BlueJTheme.generalBorder);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setAlignmentX(CENTER_ALIGNMENT);
            Font helpFont = ((Font)UIManager.get("Label.font")).deriveFont(Font.ITALIC, 12.0f);
            JTextArea text = new JTextArea(input);
            {
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setBackground((Color)UIManager.get("Label.background"));
                text.setForeground((Color)UIManager.get("Label.foreground"));
                text.setFont(helpFont);
            }
            panel.add(text);
        }
        return panel;
    }
    
    /**
     * Set the helpPanel of the TeamControlsPanel
     * @param helpPanel
     */
    private void setHelpPanel(JPanel helpPanel)
    {
        if (this.helpPanel != null)
        {
            remove(this.helpPanel);
        }
        this.helpPanel = helpPanel;
        add(helpPanel, 0);
    }
}
