/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamViewFilter;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.Project;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;

/**
 * Main frame for CVS Status Dialog
 *
 * @author bquig
 */
public class StatusFrame extends EscapeDialog
{
    private Project project;
    private JTable statusTable;
    private StatusTableModel statusModel;
    private JScrollPane statusScroller;
    private JButton refreshButton;
    private ActivityIndicator progressBar;
    private StatusMessageCellRenderer statusRenderer;
    
    private StatusWorker worker;
    
    private Repository repository;
    
    private static final int MAX_ENTRIES = 20; 
    
    /** 
     * Creates a new instance of StatusFrame. Called via factory method
     * getStatusWindow. 
     */
    public StatusFrame(Project proj)
    {
        project = proj;
        makeWindow();
        //DialogManager.tileWindow(this, proj);
    }

    private void makeWindow()
    {              
        setTitle(Config.getString("team.status.status"));
        // try and set up a reasonable default amount of entries that avoids resizing
        // and scrolling once we get info back from repository
        statusModel = new StatusTableModel(project, estimateInitialEntries());
        statusTable = new JTable(statusModel);
        statusTable.getTableHeader().setReorderingAllowed(false);
        
        // set relative column widths
        statusTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        statusTable.getColumnModel().getColumn(1).setPreferredWidth(30);
        statusTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        
        //set up custom renderer to colour code status message field
        statusRenderer = new StatusMessageCellRenderer(project);
        statusTable.setDefaultRenderer(java.lang.Object.class, statusRenderer);
        
        statusScroller = new JScrollPane(statusTable);               
        statusScroller.setBorder(BlueJTheme.generalBorderWithStatusBar);
        Dimension prefSize = statusTable.getMaximumSize();
        Dimension scrollPrefSize =  statusTable.getPreferredScrollableViewportSize();
        
        Dimension best = new Dimension(scrollPrefSize.width + 50, prefSize.height + 30);
        statusScroller.setPreferredSize(best);
        getContentPane().add(statusScroller, BorderLayout.CENTER);
        getContentPane().add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
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
            buttonPanel.setBorder(BlueJTheme.generalBorder);
            
            // progress bar
            progressBar = new ActivityIndicator();
            progressBar.setRunning(false);
            buttonPanel.add(progressBar);
            
            //close button
            JButton closeButton = BlueJTheme.getCloseButton();
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        if (worker != null) {
                            worker.abort();
                        }
                        setVisible(false);
                    }
                });

            //refresh button
            refreshButton = new JButton(Config.getString("team.status.refresh"));
            refreshButton.setEnabled(false);
            refreshButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        update();
                    }
                });

            getRootPane().setDefaultButton(refreshButton);

            buttonPanel.add(refreshButton);
            buttonPanel.add(closeButton);
        }

        return buttonPanel;
    }
    
    /**
     * try and estimate the number of entries in status table to avoid resizing
     * once repository has responded.
     */
    private int estimateInitialEntries()
    {
        // Use number of targets + README.TXT
        int initialEntries = project.getFilesInProject(true, false).size() + 1;
        // may need to include diagram layout
        //if(project.includeLayout())
        //    initialEntries++;
        // Limit to a reasonable maximum
        if(initialEntries > MAX_ENTRIES) {
            initialEntries = MAX_ENTRIES;
        }
        return initialEntries;
    }

    /**
     * Refresh the status window.
     */
    public void update()
    {
        repository = project.getRepository();
        if (repository != null) {
            progressBar.setRunning(true);
            refreshButton.setEnabled(false);
            worker = new StatusWorker();
            worker.start();
        }
        else {
            setVisible(false);
        }
    }
    
    /**
     * Inner class to do the actual cvs status call to ensure that the UI is not 
     * blocked during remote call
     */
    class StatusWorker extends SwingWorker implements StatusListener
    {
        List<TeamStatusInfo> resources;
        TeamworkCommand command;
        TeamworkCommandResult result;
        boolean aborted;
        FileFilter filter = project.getTeamSettingsController().getFileFilter(true);

        public StatusWorker()
        {
            super();
            resources = new ArrayList<TeamStatusInfo>();
            //Set files = project.getTeamSettingsController().getProjectFiles(true);
            command = repository.getStatus(this, filter, true);
        }

        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        public Object construct() 
        {
            result = command.getResult();
            return resources;
        }

        public void gotStatus(TeamStatusInfo info)
        {
            resources.add(info);
        }

        public void statusComplete(StatusHandle commitHandle)
        {
            // Nothing to be done here.
        }
        
        public void finished() 
        {
            progressBar.setRunning(false);
            if (! aborted) {
                if (result.isError()) {
                    TeamUtils.handleServerResponse(result, StatusFrame.this);
                    setVisible(false);
                }
                else {
                    Collections.sort(resources, new Comparator<TeamStatusInfo>() {
                        public int compare(TeamStatusInfo arg0, TeamStatusInfo arg1)
                        {
                            TeamStatusInfo tsi0 = (TeamStatusInfo) arg0;
                            TeamStatusInfo tsi1 = (TeamStatusInfo) arg1;

                            return tsi1.getStatus() - tsi0.getStatus();
                        }
                    });

                    TeamViewFilter filter = new TeamViewFilter();
                    // Remove old package files from display
                    for (Iterator<TeamStatusInfo> iterator = resources.iterator(); iterator.hasNext();) {
                        TeamStatusInfo info = iterator.next();
                        if(! filter.accept(info)) {
                            iterator.remove();
                        }                        
                    }
                    statusModel.setStatusData(resources);
                    
                    Map<File, String> statusMap = new HashMap<File, String>();
                    
                    for (TeamStatusInfo s : resources)
                    {
                        statusMap.put(s.getFile(), TeamStatusInfo.getStatusString(s.getStatus()));
                    }
                    
                    DataCollector.teamStatusProject(project, repository, statusMap);
                }
                refreshButton.setEnabled(true);
            }
        }
    }
}
