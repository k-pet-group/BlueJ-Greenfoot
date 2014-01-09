/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.WorldHandler;
import greenfoot.export.Exporter;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.Utility;

/**
 * A dialog allowing the user to export a scenario in a variety of ways.
 */
public class ExportDialog extends EscapeDialog
        implements TabbedIconPaneListener
{
    // Internationalisation
    private static final String dialogTitle = Config.getApplicationName() + ": "
        + Config.getString("export.dialog.title");

    private static final String noWorldDialogTitle = Config.getString("export.noworld.dialog.title");
    private static final String noWorldDialogMsg = Config.getString("export.noworld.dialog.msg");
    private static final String noZeroArgConsTitle = Config.getString("export.noconstructor.dialog.title");
    private static final String noZeroArgConsMsg = Config.getString("export.noconstructor.dialog.msg");
    
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
    private int progress;
    /** Has the dialog been made visible previously? */
    private boolean haveBeenVisible;

    private TabbedIconPane tabbedPane;

    public ExportDialog(GreenfootFrame parent)
    {
        super(parent, dialogTitle, false);
        this.parent = parent;
        
        project = parent.getProject();
        
        File projectDir = project.getDir();
        
        createPanes(project, projectDir.getParentFile());
        makeDialog();
    }

    /**
     * Show this dialog.
     */
    public void display()
    {
        if (!project.isCompiled())  {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                return;         // Cancel export
            }
        }
        String lastWorldClassName = project.getLastWorldClassName();
        GClass lastWorldClass = lastWorldClassName == null ? null
                : project.getDefaultPackage().getClass(lastWorldClassName);
        if (lastWorldClass == null) {
            JButton[] buttons = new JButton[]{new JButton(Config.getString("greenfoot.continue"))};
            MessageDialog errorDialog = new MessageDialog(parent, noWorldDialogMsg, noWorldDialogTitle, 50 , buttons);
            errorDialog.display();
            return;
        }

        // Check that a zero-argument constructor is available
        boolean haveNoArgConstructor = false;
        try {
            Class<?> realClass = lastWorldClass.getJavaClass();
            Constructor<?> [] cons = realClass.getConstructors();
            for (Constructor<?> con : cons) {
                if (con.getParameterTypes().length == 0) {
                    haveNoArgConstructor = true;
                    break;
                }
            }
        }
        catch (LinkageError le) {}
        
        if (! haveNoArgConstructor) {
            JButton[] buttons = new JButton[]{new JButton(Config.getString("greenfoot.continue"))};
            MessageDialog errorDialog = new MessageDialog(parent, noZeroArgConsMsg, noZeroArgConsTitle, 50 , buttons);
            errorDialog.display();
            return;
        }
        
        final ExportPublishPane publishPane = (ExportPublishPane) panes.get(ExportPublishPane.FUNCTION);
        
        BufferedImage snapShot = WorldHandler.getInstance().getSnapShot();
        if(snapShot != null) {
            publishPane.setImage(snapShot);
        }        
        clearStatus();
        
        if (selectedPane == null) {
            String preferredPane = Config.getPropString("greenfoot.lastExportPane", ExportPublishPane.FUNCTION);
            showPane(preferredPane, false);
        }
        
        if (! haveBeenVisible) {
            pack();
            DialogManager.centreDialog(this);
            haveBeenVisible = true;
        }

        setVisible(true);  // returns after OK or Cancel, which set 'ok'
    }

    /**
     * Display or hide the progress bar and status text. If 'showProgress' is 
     * true, an indeterminate progress bar is shown, otherwise hidden. 
     * If 'text' is null, the text is hidden, otherwise shown.
     *
     * setProgress can be invoked from a worker thread.
     */
    public void setProgress(final boolean showProgress, final String text)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { 
                progressBar.setVisible(showProgress);
                if (! showProgress) {
                    progressBar.setIndeterminate(true);
                }
                if(text == null) {
                    progressLabel.setVisible(false);
                }
                else {
                    progressLabel.setText(text);
                    progressLabel.setVisible(true);
                }
            }
        });
    }

    /**
     * Set the text for the export/share button.
     */
    public void setExportButtonText(String s)
    {
        continueButton.setText(s);
    }
    
    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
        if(!project.isCompiled()) {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                return;  // Cancel export
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
        if(selectedPane.prePublish()) {
            ExportThread expThread = new ExportThread();
            enableButtons(false);
            expThread.start();
        }
    }
    /**
     * A separate thread to execute the actual exporting.
     */
    class ExportThread extends Thread {
        @Override
        public void run() 
        {
            try {
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
                if(function.equals(ExportProjectPane.FUNCTION)) {
                    exporter.makeProject(project, (ExportProjectPane)pane, ExportDialog.this);
                }
            }
            finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        enableButtons(true);
                    }
                });
            }
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
    @Override
    public void tabSelected(String function)
    {
        showPane(function, true);
    }

    // === end of TabbedIconListener interface ===

    /** 
     * Called when the selection of the tabs changes.
     */
    private void showPane(String function, boolean saveAsDefault)
    {
        ExportPane chosenPane = panes.get(function);
        if(chosenPane != selectedPane) {
            if(selectedPane != null) {
                contentPane.remove(selectedPane);
            }
            continueButton.setText(Config.getString("export.dialog.export"));
            chosenPane.activated();
            contentPane.add(chosenPane, BorderLayout.CENTER);
            selectedPane = chosenPane;
            selectedFunction = function;
            clearStatus();
            pack();
            if (saveAsDefault) {
                Config.putPropString("greenfoot.lastExportPane", function);
            }
        }
    }
    
    /**
     * Create all the panes that should appear as part of this dialogue.
     */
    private void createPanes(GProject project, File defaultExportDir)
    {
        panes = new HashMap<String, ExportPane>();
        panes.put(ExportPublishPane.FUNCTION, new ExportPublishPane(project, this));
        panes.put(ExportWebPagePane.FUNCTION, new ExportWebPagePane(project.getName(), defaultExportDir));
        panes.put(ExportAppPane.FUNCTION, new ExportAppPane(project.getName(), defaultExportDir));
        panes.put(ExportProjectPane.FUNCTION, new ExportProjectPane(project.getName(), defaultExportDir));
        
        fixSizes(panes);
    }

    /**
     * Create the dialog interface.
     * @param defaultExportDir The default place to export to.
     */
    private void makeDialog()
    {
        String preferredPane = Config.getPropString("greenfoot.lastExportPane", ExportPublishPane.FUNCTION);

        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(null);
        contentPane.setBackground(new Color(220, 220, 220));
        
        tabbedPane = new TabbedIconPane(preferredPane);
        tabbedPane.setListener(this);
        contentPane.add(tabbedPane, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout(12, 12));
        {
            bottomPanel.setBorder(BlueJTheme.dialogBorder);
            //bottomPanel.setBackground(backgroundColor);

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
                //buttonPanel.setBackground(backgroundColor);
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                continueButton = new JButton(Config.getString("export.dialog.continue"));
                continueButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt)
                    {
                        doOK();
                    }                
                });

                closeButton = BlueJTheme.getCloseButton();
                closeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt)
                    {
                        doClose();
                    }                
                });

                if (Config.isMacOS()) {
                    buttonPanel.add(closeButton);
                    buttonPanel.add(continueButton);
                }
                else {
                    buttonPanel.add(continueButton);
                    buttonPanel.add(closeButton);
                }

                getRootPane().setDefaultButton(continueButton);
            }
            bottomPanel.add(buttonPanel, BorderLayout.EAST);
        }

        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
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

    /**
     * Inform the user that some classes aren't compiled, and give the option to compile them.
     */
    private boolean showCompileDialog(GProject project)
    {
        ExportCompileDialog dlg; 
        if(this.isVisible()) {
           dlg = new ExportCompileDialog(this, project);
        }
        else {
            dlg = new ExportCompileDialog(parent, project);
        }
        
        project.addCompileListener(dlg);
        boolean compiled = dlg.display();
        project.removeCompileListener(dlg);
        
        return compiled;
    }

    /**
     * Tell this dialog that the publish (to the Gallery) has finished and whether it was successful.
     */
    public void publishFinished(boolean success, String msg)
    {
        selectedPane.postPublish(success);
        setProgress(false, msg);
        if (success) {
            Utility.openWebBrowser(Config.getPropString("greenfoot.gameserver.address") + "/home");
        }
    }
    
    /**
     * We now know the upload size.
     */
    public void gotUploadSize(int bytes)
    {
        progressBar.setMinimum(0);
        progressBar.setMaximum(bytes);
        progressBar.setValue(0);
        progress = 0;
        progressBar.setIndeterminate(false);
    }
    
    /**
     * The upload is progressing, a certain number of bytes were just transmitted.
     * @param bytes  The number of bytes just transmitted
     */
    public void progressMade(int bytes)
    {
        progress += bytes;
        progressBar.setValue(progress);
    }

    /**
     * Selects the pane with the gallery export
     */
    public void selectGalleryPane()
    {
        tabbedPane.select(ExportPublishPane.FUNCTION);
        showPane(ExportPublishPane.FUNCTION, false);
    }
}
