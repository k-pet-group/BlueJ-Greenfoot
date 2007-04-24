package greenfoot.gui.export;

import java.awt.BorderLayout;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

import bluej.BlueJTheme;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.export.Exporter;
import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ExportDialog extends EscapeDialog
        implements TabbedIconPaneListener
{
    // Internationalisation
    private static final String dialogTitle ="Greenfoot: Export";

    private Frame parent;
    private GProject project;
    private JPanel contentPane;
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel progressLabel = new JLabel();
    private JButton continueButton;
    private JButton closeButton;
    private HashMap<String, ExportPane> panes;
    private ExportPane selectedPane;
    private String selectedFunction;

    public ExportDialog(Frame parent)
    {
        super(parent, dialogTitle, false);
        this.parent = parent;
        
        project = GreenfootMain.getInstance().getProject();
        
        File projectDir = null;
        try {
            projectDir = project.getDir();
        }
        catch (ProjectNotOpenException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        createPanes(project.getName(), projectDir.getParentFile());
        makeDialog();
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public void display()
    {
        if(!project.isCompiled())  {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                return;         // Cancel export
            }
        }

        clearStatus();
        setVisible(true);  // returns after OK or Cancel, which set 'ok'
    }

    /**
     * Display or hide the progress bar and status text. If 'showProgress' is 
     * true, an indeterminite progress bar is shown, otherwise hidden. 
     * If 'text' is null, the text is hidden, otherwise shown.
     *
     * setProgress can be invoked from a worker thread.
     */
    public void setProgress(final boolean showProgress, final String text)
    {
        SwingUtilities.invokeLater(new Runnable() { public void run() { 
            progressBar.setVisible(showProgress);
            if(text == null) {
                progressLabel.setVisible(false);
            }
            else {
                progressLabel.setText(text);
                progressLabel.setVisible(true);
            }
        }});
    }

    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
        if(!project.isCompiled())  {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                return;         // Cancel export
            }
        }
        doExport();
    }

    /**
     * Close action when Cancel is pressed.
     */
    private void doClose()
    {
        setVisible(false);
    }

    /**
     * The export button was pressed. Do the exporting now.
     */
    private void doExport()
    {
        ExportThread expThread = new ExportThread();
        expThread.start();
    }

    /**
     * A sepatrate thread to execute the actual axporting.
     */
    class ExportThread extends Thread {
        public void run() 
        {
            enableButtons(false);
            
            String function = getSelectedFunction();
            ExportPane pane = getSelectedPane();

            Exporter exporter = Exporter.getInstance();

            if(function.equals(ExportPublishPane.FUNCTION)) {
                exporter.publishToWebServer(project, (ExportPublishPane)pane, ExportDialog.this);
            }
            if(function.equals(ExportWebPagePane.FUNCTION)) {
                exporter.makeWebPage(project, (ExportWebPagePane)pane, ExportDialog.this);
            }
            if(function.equals(ExportAppPane.FUNCTION)) {
                exporter.makeApplication(project, (ExportAppPane)pane, ExportDialog.this);
            }
            enableButtons(true);
        }
    }

    /**
     * Clear the status text, but only if we are not in the middle of a task.
     */
    private void clearStatus()
    {
        if(!progressBar.isVisible()) {
            progressLabel.setVisible(false);
        }
    }
    
    /**
     * Return the identifier for the specific export function selected.
     */
    private String getSelectedFunction()
    {
        return selectedFunction;
    }

    /**
     * Return the identifier for the specific export function selected.
     */
    private ExportPane getSelectedPane()
    {
        return selectedPane;
    }
    
    /**
     * Enable or disable the dialogue buttons.
     */
    private void enableButtons(boolean enable)
    {
        continueButton.setEnabled(enable);
        closeButton.setEnabled(enable);
    }
    
    // === TabbedIconPaneListener interface ===
    
    /** 
     * Called when the selection of the tabs changes.
     */
    public void tabSelected(String function)
    {
        showPane(function);
    }

    // === end of TabbedIconListener interface ===

    /** 
     * Called when the selection of the tabs changes.
     */
    public void showPane(String function)
    {
        ExportPane chosenPane = panes.get(function);
        if(chosenPane != selectedPane) {
            if(selectedPane != null)
                contentPane.remove(selectedPane);
            contentPane.add(chosenPane, BorderLayout.CENTER);
            selectedPane = chosenPane;
            selectedFunction = function;
            clearStatus();
            pack();
        }
    }
    
    /**
     * Create all the panes that should appear as part of this dialogue.
     */
    private void createPanes(String scenarioName, File defaultExportDir)
    {
        panes = new HashMap<String, ExportPane>();
        panes.put(ExportPublishPane.FUNCTION, new ExportPublishPane(scenarioName));
        panes.put(ExportWebPagePane.FUNCTION, new ExportWebPagePane(scenarioName, defaultExportDir));
        panes.put(ExportAppPane.FUNCTION, new ExportAppPane(scenarioName, defaultExportDir));
        
        fixSizes(panes);
    }

    /**
     * Create the dialog interface.
     * @param defaultExportDir The default place to export to.
     */
    private void makeDialog()
    {
        contentPane = (JPanel) getContentPane();
        
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(null);
        
        TabbedIconPane tabbedPane = new TabbedIconPane();
        tabbedPane.setListener(this);
        contentPane.add(tabbedPane, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout(12, 12));
        {
            bottomPanel.setBorder(BlueJTheme.dialogBorder);
            
            progressBar.setIndeterminate(true);
            progressBar.setVisible(false);
            Dimension size = progressBar.getPreferredSize();
            size.width = 100;
            progressBar.setPreferredSize(size);
            bottomPanel.add(progressBar, BorderLayout.WEST);
            
            progressLabel.setVisible(false);
            bottomPanel.add(progressLabel, BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                continueButton = new JButton("Export");
                continueButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) { doOK(); }                
                });

                closeButton = BlueJTheme.getCloseButton();
                closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) { doClose(); }                
                });

                buttonPanel.add(continueButton);
                buttonPanel.add(closeButton);

                getRootPane().setDefaultButton(continueButton);
            }
            bottomPanel.add(buttonPanel, BorderLayout.EAST);
        }

        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
        showPane(ExportPublishPane.FUNCTION);

        DialogManager.centreDialog(this);
    }

    /**
     * Set the preferred width for all tabs to the widest of the tabs.
     */
    private void fixSizes(HashMap<String, ExportPane> panes) 
    {
        int maxWidth = 0;
        
        for(ExportPane pane : panes.values()) {
            Dimension size = pane.getPreferredSize();
            maxWidth = Math.max(size.width, maxWidth);
        }
        
        for(ExportPane pane : panes.values()) {
            Dimension size = pane.getPreferredSize();
            size.width = maxWidth;
            pane.setPreferredSize(size);
        }
    }

    private boolean showCompileDialog(GProject project)
    {
        ExportCompileDialog dlg; 
        if(this.isVisible()) 
           dlg = new ExportCompileDialog(this, project);
        else
            dlg = new ExportCompileDialog(parent, project);
        
        GreenfootMain.getInstance().addCompileListener(dlg);
        boolean compiled = dlg.display();
        GreenfootMain.getInstance().removeCompileListener(dlg);
        
        return compiled;
    }
}
