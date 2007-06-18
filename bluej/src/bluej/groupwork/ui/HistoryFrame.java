package bluej.groupwork.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
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
 * @version $Id: HistoryFrame.java 5104 2007-06-18 04:37:37Z davmac $
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
    List historyInfoList;
    
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
        List tempList = new ArrayList(5);
        HistoryInfo tempInfo = new HistoryInfo("somepath/abcdefg.java", "1.1", "2006/11/34 12:34:56", "abraham", "this is the expected comment length of comments");
        for (int i = 0; i < 8; i++) {
            tempList.add(tempInfo);
        }
        listModel.setListData(tempList);
        Dimension size = historyList.getPreferredSize();
        listModel.setListData(Collections.EMPTY_LIST);
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
        
        List displayList;
        if (user == null && file == null) {
            displayList = historyInfoList;
        }
        else {
            displayList = new ArrayList();
            for (Iterator i = historyInfoList.iterator(); i.hasNext(); ) {
                HistoryInfo info = (HistoryInfo) i.next();
                if (user != null && ! info.getUser().equals(user)) {
                    continue;
                }
                if (file != null && ! info.getFile().equals(file)) {
                    continue;
                }
                
                displayList.add(info);
            }
        }
        
        listModel.setListData(displayList);
    }
    
    /**
     * Reset the filter boxes (user filter and file filter), adding a complete list
     * of all users and files.
     */
    private void resetFilterBoxes()
    {
        Set users = new HashSet();
        Set files = new HashSet();
        
        for (Iterator i = historyInfoList.iterator(); i.hasNext(); ) {
            HistoryInfo info = (HistoryInfo) i.next();
            files.add(info.getFile());
            users.add(info.getUser());
        }
        
        List usersList = new ArrayList(users);
        Collections.sort(usersList);
        List filesList = new ArrayList(files);
        Collections.sort(filesList);
        
        userFilterCombo.removeAllItems();
        userFilterCombo.addItem("All users");
        Iterator i = usersList.iterator();
        while (i.hasNext()) {
            userFilterCombo.addItem(i.next());
        }
        userFilterCombo.addActionListener(filterActionListener);
        userFilterCombo.setEnabled(true);
        
        fileFilterCombo.removeAllItems();
        fileFilterCombo.addItem("All files");
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
        private Repository repository;
        private List responseList;
        private TeamworkCommand command;
        private TeamworkCommandResult response;
        
        public HistoryWorker(Repository repository)
        {
            this.repository = repository;
            this.responseList = new ArrayList();
            
            command = repository.getLogHistory(this);
        }
        
        public Object construct()
        {
            response = command.getResult();
            return response;
        }
        
        public void logInfoAvailable(LogInformation logInfo)
        {
            responseList.add(logInfo);
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
                    List modelList = new ArrayList();
                    
                    // Convert the list of LogInformation into a list of
                    // HistoryInfo
                    for (Iterator i = responseList.iterator(); i.hasNext(); ) {
                        LogInformation logInfo = (LogInformation) i.next();
                        String file = logInfo.getFile().getPath();
                        String projectPath = project.getProjectDir().getPath();
                        if (file.startsWith(projectPath)) {
                            file = file.substring(projectPath.length() + 1);
                        }
                        Iterator j = logInfo.getRevisionList().iterator();
                        while (j.hasNext()) {
                            Revision revision = (Revision) j.next();
                            String user = revision.getAuthor();
                            String rev = revision.getNumber();
                            String date = revision.getDateString();
                            String comment = revision.getMessage();
                            HistoryInfo info = new HistoryInfo(file, rev, date, user, comment);
                            modelList.add(info);
                        }
                    }
                    
                    Collections.sort(modelList, new DateCompare());
                    
                    // Make the history list forget the preferred size that was forced
                    // upon it when we built the frame.
                    historyList.setPreferredSize(null);
                    
                    renderer.setWrapMode(historyPane);
                    listModel.setListData(modelList);
                    historyInfoList = modelList;
                    
                    resetFilterBoxes();
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
class DateCompare implements Comparator
{
    public int compare(Object arg0, Object arg1)
    {
        HistoryInfo hi0 = (HistoryInfo) arg0;
        HistoryInfo hi1 = (HistoryInfo) arg1;
        
        return hi1.getDate().compareTo(hi0.getDate());
    }
}
