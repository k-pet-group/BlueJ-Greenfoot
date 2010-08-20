/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import bluej.BlueJTheme;
import bluej.Config;

import bluej.pkgmgr.Project;

import java.awt.FlowLayout;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * A dialog which presents conflicts after an update.
 * 
 * @author fisker
 */
public class ConflictsDialog extends JDialog
{
    private JLabel heading;
    private List<String> bluejConflicts;
    private List<String> nonBluejConflicts;
    private Project project;

    /**
     * @param project2
     * @param blueJconflicts
     * @param nonBlueJConflicts
     */
    public ConflictsDialog(Project project, List<String> bluejConflicts,
        List<String> nonBlueJConflicts)
    {
        super();
        this.project = project;
        this.bluejConflicts = bluejConflicts;
        this.nonBluejConflicts = nonBlueJConflicts;
        setTitle(Config.getString("team.conflicts.title"));
        makeWindow();
    }

    private void makeWindow()
    {
        JPanel mainPanel = new JPanel();
        JPanel bluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"),
                bluejConflicts);
        JPanel nonBluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"),
                nonBluejConflicts);
        JPanel buttonPanel = makeButtonPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);
        mainPanel.add(bluejConflictsPanel);

        if (nonBluejConflicts.size() > 0) {
            mainPanel.add(nonBluejConflictsPanel);
        }

        mainPanel.add(buttonPanel);
        getContentPane().add(mainPanel);

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.teamwork.conflicts", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.teamwork.conflicts"));
        pack();
    }

    private JPanel makeConflictsPanel(String headline, List<String> conflicts)
    {
        JPanel labelPanel = new JPanel();

        {
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
            labelPanel.setBorder(BlueJTheme.dialogBorder);

            /*labelPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Conflict"),
                    BlueJTheme.generalBorder));*/
            labelPanel.setAlignmentX(LEFT_ALIGNMENT);

            //heading
            heading = new JLabel(headline);

            Font smallFont = heading.getFont().deriveFont(Font.BOLD, 12.0f);
            heading.setFont(smallFont);
            labelPanel.add(heading);
            labelPanel.add(Box.createVerticalStrut(5));

            JPanel conflictsPanel = new JPanel();
            conflictsPanel.setLayout(new BoxLayout(conflictsPanel,
                    BoxLayout.Y_AXIS));
            conflictsPanel.setAlignmentX(LEFT_ALIGNMENT);

            //the conflicting files labels
            for (Iterator<String> i = conflicts.iterator(); i.hasNext();) {
                String conflict = i.next();
                conflictsPanel.add(new JLabel(conflict));
            }

            JScrollPane scrollPane = new JScrollPane(conflictsPanel);
            labelPanel.add(scrollPane);
        }

        return labelPanel;
    }

    /**
     * Create the button panel with a Resolve button and a close button
     * @return JPanel the buttonPanel
     */
    private JPanel makeButtonPanel()
    {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            //close button
            JButton closeButton = new JButton(Config.getString("close"));
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        setVisible(false);
                    }
                });

            //resolve button
            JButton resolveButton = new JButton(Config.getString("team.conflicts.show"));
            resolveButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        project.openEditorsForSelectedTargets();

                        // move to resolve button
                        dispose();
                    }
                });

            getRootPane().setDefaultButton(resolveButton);

            buttonPanel.add(resolveButton);
            buttonPanel.add(closeButton);
            resolveButton.setEnabled(bluejConflicts.size() > 0);
        }

        return buttonPanel;
    }
}
