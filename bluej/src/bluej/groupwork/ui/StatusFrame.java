package bluej.groupwork.ui;

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

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;

/**
 * Main frame for CVS Status Dialog
 *
 * @author bquig
 * @version $Id: StatusFrame.java 4780 2006-12-22 04:14:21Z bquig $
 */
public class StatusFrame extends EscapeDialog
{
    private Project project;
    private JTable statusTable;
    private StatusTableModel statusModel;
    private JScrollPane statusScroller;
    private JButton refreshButton;
    private ActivityIndicator progressBar;
    
    private StatusWorker worker;
    
    private Repository repository;
    
    static final int MAX_ENTRIES = 20; 
    
    // store and map BlueJ PkgMgrFrames and associated cvs status dialog windows
   // private static Map statusFrames = new HashMap();
    
    /**
     * Factory method to get a status window.
     * Generally you should call update() on the window after retrieving it. 
     */
   // public static StatusFrame getStatusWindow(PkgMgrFrame pmf)
   // {
   //     StatusFrame window = (StatusFrame)statusFrames.get(pmf);
   //     if(window == null) {
   //         window = new StatusFrame(pmf);
   //         statusFrames.put(pmf, window);
   //     }
   //     return window;
   // }

    /** 
     * Creates a new instance of StatusFrame. Called via factory method
     * getStatusWindow. 
     */
    public StatusFrame(Project proj)
    {
        //super(proj);
        project = proj;
        makeWindow();
        //DialogManager.tileWindow(this, proj);
    }

    private void makeWindow()
    {              
        setTitle(Config.getString("team.status.status"));
        // try and set up a reasonable default amount of entries that avoids resizing
        // and scrolling once we get info back from repository
        statusModel = new StatusTableModel(estimateInitialEntries());
        statusTable = new JTable(statusModel);
        statusTable.getTableHeader().setReorderingAllowed(false);
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
        int initialEntries = project.getFilesInProject(project.getTeamSettingsController().includeLayout()).size() + 1;
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
                
            if(!project.getTeamSettingsController().includeLayout() && fileName.equals("bluej.pkg"))
                it.remove();        
        }
    }

    /**
     * Refresh the status window.
     */
    public void update()
    {
        repository = project.getRepository();
        if (repository != null) {
            statusTable.setModel(new StatusTableModel(statusModel.getRowCount()));
            //statusModel.clear();
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
    class StatusWorker extends SwingWorker
    {
          List resources;
          boolean aborted;
          
          public void abort()
          {
              aborted = true;
              interrupt();
          }
          
          public Object construct() 
          {
              resources = getStatusAsList();
              return resources;
          }
             
          public void finished() 
          {
              if (! aborted) {
                  if (resources != null) {
                      
                      Collections.sort(resources, new Comparator() {
                          public int compare(Object arg0, Object arg1)
                          {
                              TeamStatusInfo tsi0 = (TeamStatusInfo) arg0;
                              TeamStatusInfo tsi1 = (TeamStatusInfo) arg1;
                              
                              return tsi1.getStatus() - tsi0.getStatus();
                          }
                      });
                      
                      statusModel = new StatusTableModel(resources);
                      statusTable.setModel(statusModel);
                      //statusModel.setStatusData(resources);
                      refreshButton.setEnabled(true);
                  }
                  else {
                      setVisible(false);
                  }
              }
          }
          
          /**
           * Get the list of status information.
           * This is called on a non-GUI thread.
           */
          private List getStatusAsList()
          {
              List resourceStatus = null;

              try {
                  Set files = project.getTeamSettingsController().getProjectFiles(project.getTeamSettingsController().includeLayout());
                  
                  Set remoteDirs = new HashSet();
                  List remoteFiles = repository.getRemoteFiles(remoteDirs);
                  FilenameFilter filter = project.getTeamSettingsController().getFileFilter(true);
                  for (Iterator i = remoteFiles.iterator(); i.hasNext(); ) {
                      File remoteFile = (File) i.next();
                      File parentDir = remoteFile.getParentFile();
                      // We might not be interested in this file...
                      if (! filter.accept(parentDir, remoteFile.getName())) {
                          i.remove();
                      }
                  }
                  
                  files.addAll(remoteFiles);
                  try {
                      resourceStatus = repository.getStatus(files, remoteDirs);
                  }
                  finally {
                      progressBar.setRunning(false);
                  }
                  
              } catch (CommandAbortedException e) {
                  // e.printStackTrace();
                  // This is ok - the command might be aborted by us
              } catch (CommandException e) {
                  e.printStackTrace();
              } catch (AuthenticationException e) {
                  TeamUtils.handleAuthenticationException(StatusFrame.this);
              } catch (InvalidCvsRootException e) {
                  TeamUtils.handleInvalidCvsRootException(StatusFrame.this);
                  e.printStackTrace();
              }

              if (resourceStatus != null) {
                  filterStatusInformation(resourceStatus);
              }
              return resourceStatus;
          }
    }
    
}
