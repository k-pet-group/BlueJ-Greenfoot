/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012  Michael Kolling and John Rosenberg 
 
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.*;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;

/**
 * A frame to display the commit history, including dates, users, revisions
 * and commit comments.
 * 
 * @author Davin McCall
 */
public class HistoryFrame extends EscapeDialog
{
    Project project;
    ActivityIndicator activityBar;
    HistoryWorker worker;
    
    HistoryListModel listModel = new HistoryListModel();
    HistoryListRenderer renderer = new HistoryListRenderer(listModel);
    JList historyList;
    JScrollPane historyPane;
    List<HistoryInfo> historyInfoList;
    
    JComboBox fileFilterCombo;
    JComboBox userFilterCombo;
    ActionListener filterActionListener;
    JLabel filterSpacer;
    
    /**
     * Create a new HistoryFrame.
     */
    public HistoryFrame(PkgMgrFrame pmf)
    {
        super((Frame) null, Config.getString("team.history.title"));
        project = pmf.getProject();
        buildUI();
        pack();
    }
    
    /**
     * Construct the UI components.
     */
    private void buildUI()
    {
        // Content pane
        JPanel contentPane = new JPanel();
        DBoxLayout layout = new DBoxLayout(DBoxLayout.Y_AXIS, 0,
                BlueJTheme.generalSpacingWidth);
        contentPane.setLayout(layout); 
        contentPane.setBorder(BlueJTheme.dialogBorder);
        setContentPane(contentPane);

        // History list
        historyList = new JList(listModel) {
            public Dimension getPreferredScrollableViewportSize()
            {
                return getPreferredSize();
            }
        };
        historyList.setCellRenderer(renderer);
        historyPane = new JScrollPane(historyList);
        historyPane.setAlignmentX(0f);
        historyPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentPane.add(historyPane);
        
        // Find a suitable size for the history list
        List<HistoryInfo> tempList = new ArrayList<HistoryInfo>(5);
        HistoryInfo tempInfo = new HistoryInfo(new String[] {"somepath/abcdefg.java"}, "1.1", "2006/11/34 12:34:56", "abraham", "this is the expected comment length of comments");
        for (int i = 0; i < 8; i++) {
            tempList.add(tempInfo);
        }
        listModel.setListData(tempList);
        Dimension size = historyList.getPreferredSize();
        listModel.setListData(Collections.<HistoryInfo>emptyList());
        historyList.setPreferredSize(size);
        
        contentPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        
        // File and user filter boxes
        DBox filterBox = new DBox(DBox.X_AXIS, 0, BlueJTheme.componentSpacingLarge, 0.5f);
        filterBox.setAxisBounded(DBox.Y_AXIS, true);
        filterBox.add(new JLabel(Config.getString("team.history.filefilter") + " "));
        fileFilterCombo = new JComboBox();
        fileFilterCombo.setEnabled(false);
        filterBox.add(fileFilterCombo);
        // filterBox.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingLarge));
        filterBox.add(new JLabel(Config.getString("team.history.userfilter") + " "));
        userFilterCombo = new JComboBox();
        userFilterCombo.setEnabled(false);
        filterBox.add(userFilterCombo);
        // Add in a spacer, which helps ensure the initial size of the frame is ok.
        // When the filter combo boxes are filled, the spacer is removed.
        filterSpacer = new JLabel("                              ");
        userFilterCombo.addItem("         ");
        fileFilterCombo.addItem("               ");
        filterBox.add(filterSpacer);
        filterBox.setAlignmentX(0f);
        contentPane.add(filterBox);
        filterActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                refilter();
            };
        };
        
        contentPane.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical - BlueJTheme.generalSpacingWidth));
        
        // Activity indicator and close button
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        activityBar = new ActivityIndicator();
        buttonBox.add(activityBar);
        buttonBox.add(Box.createHorizontalGlue());
        JButton closeButton = new JButton(Config.getString("close"));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (worker != null) {
                    worker.cancel();
                }
                dispose();
            }
        });
        buttonBox.add(closeButton);
        buttonBox.setAlignmentX(0f);
        contentPane.add(buttonBox);
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(boolean vis)
    {
        super.setVisible(vis);
        
        if (vis) {
            Repository repository = project.getRepository();
            
            if (repository != null) {
                worker = new HistoryWorker(repository);
                worker.start();
                activityBar.setRunning(true);
            }
        }
        else {
            if (worker != null) {
                worker.cancel();
            }
        }
    }
    
    /**
     * Filter the history info list according to the selected file and user
     * filters. The filtered list is then displayed.
     */
    private void refilter()
    {
        String user = null;
        int userIndex = userFilterCombo.getSelectedIndex();
        if (userIndex != 0) {
            user = (String) userFilterCombo.getItemAt(userIndex);
        }
        
        String file = null;
        int fileIndex = fileFilterCombo.getSelectedIndex();
        if (fileIndex != 0) {
            file = (String) fileFilterCombo.getItemAt(fileIndex);
        }
        
        List<HistoryInfo> displayList;
        if (user == null && file == null) {
            displayList = historyInfoList;
        }
        else {
            displayList = new ArrayList<HistoryInfo>();
            for (Iterator<HistoryInfo> i = historyInfoList.iterator(); i.hasNext(); ) {
                HistoryInfo info = (HistoryInfo) i.next();
                if (user != null && ! info.getUser().equals(user)) {
                    continue;
                }
                if (file != null && ! hinfoHasFile(info, file)) {
                    continue;
                }
                
                displayList.add(info);
            }
        }
        
        listModel.setListData(displayList);
    }
    
    /**
     * Check whether a history item pertains at all to a particular file
     */
    private boolean hinfoHasFile(HistoryInfo info, String file)
    {
        String [] files = info.getFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(file)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Reset the filter boxes (user filter and file filter), adding a complete list
     * of all users and files.
     */
    private void resetFilterBoxes()
    {
        Set<String> users = new HashSet<String>();
        Set<String> files = new HashSet<String>();
        
        for (Iterator<HistoryInfo> i = historyInfoList.iterator(); i.hasNext(); ) {
            HistoryInfo info = i.next();
            String [] infoFiles = info.getFiles();
            for (int j = 0; j < infoFiles.length; j++) {
                files.add(infoFiles[j]);
            }
            users.add(info.getUser());
        }
        
        List<String> usersList = new ArrayList<String>(users);
        Collections.sort(usersList);
        List<String> filesList = new ArrayList<String>(files);
        Collections.sort(filesList);
        
        userFilterCombo.removeAllItems();
        userFilterCombo.addItem(Config.getString("team.history.allUsers"));
        Iterator<String> i = usersList.iterator();
        while (i.hasNext()) {
            userFilterCombo.addItem(i.next());
        }
        userFilterCombo.addActionListener(filterActionListener);
        userFilterCombo.setEnabled(true);
        
        fileFilterCombo.removeAllItems();
        fileFilterCombo.addItem(Config.getString("team.history.allFiles"));
        i = filesList.iterator();
        while (i.hasNext()) {
            fileFilterCombo.addItem(i.next());
        }
        fileFilterCombo.addActionListener(filterActionListener);
        fileFilterCombo.setEnabled(true);
        
        filterSpacer.setVisible(false);
    }
    
    /**
     * A worker class to fetch the required information from the repository
     * in the background.
     */
    private class HistoryWorker extends SwingWorker implements LogHistoryListener
    {
        private List<HistoryInfo> responseList;
        private Repository repository;
        private TeamworkCommand command;
        private TeamworkCommandResult response;
        
        public HistoryWorker(Repository repository)
        {
            this.responseList = new ArrayList<HistoryInfo>();
            
            command = repository.getLogHistory(this);
            this.repository = repository;
        }
        
        public Object construct()
        {
            response = command.getResult();
            return response;
        }
        
        public void logInfoAvailable(HistoryInfo hInfo)
        {
            responseList.add(hInfo);
        }
        
        public void finished()
        {
            if (command != null) {
                activityBar.setRunning(false);
                command = null; // marks the command as finished
                if (response.isError()) {
                    TeamUtils.handleServerResponse(response, HistoryFrame.this);
                    setVisible(false);
                }
                else {
                    
                    Collections.sort(responseList, new DateCompare());
                    
                    // Make the history list forget the preferred size that was forced
                    // upon it when we built the frame.
                    historyList.setPreferredSize(null);
                    
                    renderer.setWrapMode(historyPane);
                    listModel.setListData(responseList);
                    historyInfoList = responseList;
                    
                    resetFilterBoxes();
                    
                    DataCollector.teamHistoryProject(project, repository);
                }
            }
        }
        
        public void cancel()
        {
            if (command != null) {
                activityBar.setRunning(false);
                command.cancel();
                command = null;
            }
        }
    }
}

/**
 * A comparator to sort HistoryInfo objects by date.
 * 
 * @author Davin McCall
 */
class DateCompare implements Comparator<HistoryInfo>
{
    public int compare(HistoryInfo hi0, HistoryInfo hi1)
    {
        return hi1.getDate().compareTo(hi0.getDate());
    }
}
