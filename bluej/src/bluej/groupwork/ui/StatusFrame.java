package bluej.groupwork.ui;

import bluej.utility.Debug;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.*;
import bluej.pkgmgr.Project;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Main frame for CVS Status Dialog
 *
 * @author bquig
 * @version $Id: StatusFrame.java 5082 2007-06-04 04:29:25Z bquig $
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
        
        //set up custom renderer to colour code status message field
        statusRenderer = new StatusMessageCellRenderer(project);
        statusTable.setDefaultRenderer(java.lang.Object.class, statusRenderer);
        
        statusScroller = new JScrollPane(statusTable);               
        statusScroller.setBorder(BlueJTheme.generalBorderWithStatusBar);
        Dimension prefSize = statusTable.getMaximumSize();
        Dimension scrollPrefSize =  statusTable.getPreferredScrollableViewportSize();
        
        Dimension best = new Dimension(scrollPrefSize.width, prefSize.height + 30);
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
        int initialEntries = project.getFilesInProject(true).size() + 1;
        // may need to include diagram layout
        //if(project.includeLayout())
        //    initialEntries++;
        // Limit to a reasonable maximum
        if(initialEntries > MAX_ENTRIES)
            initialEntries = MAX_ENTRIES;
        return initialEntries;
    }
    
    /**
     * Simple attempt at information filtering, should later be expanded
     */
    private void filterStatusInformation(List info)
    {
        for(Iterator it = info.iterator(); it.hasNext(); ) {
            TeamStatusInfo statusInfo = (TeamStatusInfo) it.next();
            
            File file = statusInfo.getFile();
            String fileName = file.getName();
            //File dir = file.getParentFile();
                
            //if(!project.getTeamSettingsController().includeLayout() && fileName.equals("bluej.pkg"))
            //    it.remove();        
        }
    }

    /**
     * Refresh the status window.
     */
    public void update()
    {
        repository = project.getRepository();
        if (repository != null) {
            //statusTable.setModel(new StatusTableModel(statusModel.getRowCount()));
            //statusModel.setStatusData()
            
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
        List resources;
        TeamworkCommand command;
        TeamworkCommandResult result;
        boolean aborted;
        FilenameFilter filter = project.getTeamSettingsController().getFileFilter(true);

        public StatusWorker()
        {
            super();
            resources = new ArrayList();
            Set files = project.getTeamSettingsController().getProjectFiles(true);
            command = repository.getStatus(this, files, true);
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
            File infoFile = info.getFile();
            if (filter.accept(infoFile.getParentFile(), infoFile.getName())) {
                resources.add(info);
            }
        }

        public void finished() 
        {
            progressBar.setRunning(false);
            if (! aborted) {
                if (result.isError()) {
                    TeamUtils.handleServerResponse(result, StatusFrame.this);
                }
                else {
                    Collections.sort(resources, new Comparator() {
                        public int compare(Object arg0, Object arg1)
                        {
                            TeamStatusInfo tsi0 = (TeamStatusInfo) arg0;
                            TeamStatusInfo tsi1 = (TeamStatusInfo) arg1;

                            return tsi1.getStatus() - tsi0.getStatus();
                        }
                    });

                    statusModel.setStatusData(resources);
                    //statusModel = new StatusTableModel(resources);
                    //statusTable.setModel(statusModel);
                    //statusModel.setStatusData(resources);
                }
                refreshButton.setEnabled(true);
            }
        }
    }
}
