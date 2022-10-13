/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013,2016,2018,2021  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx.export;

import bluej.Config;
import bluej.debugger.gentype.ConstructorReflective;
import bluej.debugger.gentype.Reflective;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.Utility;

import greenfoot.export.Exporter;
import static greenfoot.export.Exporter.ExportFunction;

import greenfoot.export.mygame.ExportInfo;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.export.ScenarioSaver;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;

/**
 * A dialog allowing the user to export a scenario in a variety of ways.
 *
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ExportDialog extends FXCustomizedDialog<Void>
{
    private static final String dialogTitle = Config.getApplicationName() + ": "
            + Config.getString("export.dialog.title");

    private final Project project;
    private final ScenarioSaver scenarioSaver;
    private final ScenarioInfo scenarioInfo;
    private final ClassTarget currentWorld;
    private final Image snapshot;
    private int uploadSize;
    private final BooleanProperty exportingProperty = new SimpleBooleanProperty(false);
    private BooleanBinding tabInvalidity;

    private final TabPane tabbedPane = new TabPane();
    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private final Map<ExportFunction, ExportTab> exportTabs = new LinkedHashMap<>();
    private final Button exportButton = new Button(Config.getString("export.dialog.export"));

    /**
     * Creates a new instance of the export dialog.
     *
     * @param parent         The parent window.
     * @param project        The project that will be shared.
     * @param scenarioSaver  The listener that will enable us to save the scenario when exporting.
     * @param scenarioInfo   The previously stored scenario info in the properties file.
     * @param currentWorld   The class target of the current active world.
     * @param snapshot       A snapshot of the world.
     * @throws ExportException if the current world is null or cannot be instantianted.
     */
    public ExportDialog(Window parent, Project project, ScenarioSaver scenarioSaver,
                        ScenarioInfo scenarioInfo, ClassTarget currentWorld, Image snapshot)
            throws ExportException
    {
        super(parent, dialogTitle, "export-dialog");
        this.project = project;
        this.scenarioSaver = scenarioSaver;
        this.scenarioInfo = scenarioInfo;
        this.currentWorld = currentWorld;
        this.snapshot = snapshot;
        setModal(true);
        makeDialog();
    }

    /**
     * Create the dialog interface.
     *
     * @throws ExportException if the world hasn't been instantiated, or cannot be instantiated
     *                         because there is no suitable constructor.
     */
    private void makeDialog() throws ExportException
    {
        BorderPane contentPane = new BorderPane();
        setContentPane(contentPane);
        contentPane.setTop(tabbedPane);

        progressBar.setProgress(-1);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(100);
        progressLabel.setVisible(false);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeButton = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setOnAction(event ->
                Config.putPropString("greenfoot.lastExportPane", getSelectedFunction().name()));
        // Close button is only disabled when the exporting is in progress.
        closeButton.disableProperty().bind(exportingProperty);

        exportButton.setOnAction(event -> doExport());

        HBox bottomBox = new HBox(exportButton, progressLabel, progressBar);
        bottomBox.getStyleClass().add("bottom-box");
        contentPane.setBottom(bottomBox);

        if (currentWorld == null)
        {
            throw new ExportException(Config.getString("export.noworld.dialog.msg"));
        }

        // Check that a zero-argument constructor is available

        boolean hasZeroArgConstructor = false; // set false initially
        
        Reflective currentWorldType = currentWorld.getTypeReflective();
        if (currentWorldType != null)
        {
            List<ConstructorReflective> constructors = currentWorldType.getDeclaredConstructors();
            hasZeroArgConstructor = constructors.stream().anyMatch(con -> con.getParamTypes().isEmpty());
        }
        
        if (! hasZeroArgConstructor)
        {
            throw new ExportException(Config.getString("export.noconstructor.dialog.msg"));
        }

        createTabs();

        if (snapshot != null)
        {
            ExportPublishTab publishPane = (ExportPublishTab) exportTabs.get(ExportFunction.PUBLISH);
            publishPane.setImage(snapshot);
        }

        JavaFXUtil.addChangeListenerPlatform(tabbedPane.selectionModelProperty().get().selectedItemProperty(),
                tab -> updateControls((ExportTab) tab));

        selectTab(Config.getPropString("greenfoot.lastExportPane"));
    }

    /**
     * Display or hide the progress bar and status text. If 'showProgress' is 
     * true, an indeterminate progress bar is shown, otherwise hidden. 
     * If 'text' is null, the text is hidden, otherwise shown.
     * This method can be invoked from a worker thread.
     *
     * @param showProgress  True to show an indeterminate progress bar, otherwise false.
     * @param text          The message to be shown next to progress bar. Could be null,
     *                      which means do not show any message.
     */
    @OnThread(Tag.Any)
    public void setProgress(final boolean showProgress, final String text)
    {
        Platform.runLater(() -> {
            progressBar.setVisible(showProgress);
            if (!showProgress)
            {
                progressBar.setProgress(-1);
            }

            if (text == null)
            {
                progressLabel.setVisible(false);
            }
            else
            {
                progressLabel.setText(text);
                progressLabel.setVisible(true);
            }
        });
    }

    /**
     * Set the text for the export/share button.
     */
    public void setExportButtonText(String s)
    {
        exportButton.setText(s);
    }
    
    /**
     * The export button was pressed. Do the exporting now.
     */
    private void doExport()
    {
        if (getSelectedTab().prePublish())
        {
            ExportInfo info = getSelectedTab().getExportInfo();
            exportingProperty.set(true);
            scenarioSaver.doSave();
            new ExportThread(info).start();
        }
    }

    /**
     * A separate thread to execute the actual exporting.
     */
    class ExportThread extends Thread
    {
        private final double snapshotWidth;
        private final double snapshotHeight;
        private final String displayName;
        private final ExportFunction function;
        private final ExportInfo info;
        
        /**
         * Construct an export thread, which will use the given ExportInfo information.
         * 
         * @param info  the information for export. The new thread assumes ownership of
         *              this object.
         */
        @OnThread(Tag.FXPlatform)
        public ExportThread(ExportInfo info)
        {
            super("Export scenario");
            snapshotWidth = snapshot.getWidth();
            snapshotHeight = snapshot.getHeight();
            displayName = currentWorld.getDisplayName();
            function = getSelectedFunction();
            this.info = info;
        }
        
        @Override
        @OnThread(value = Tag.Worker, ignoreParent = true)
        public void run()
        {
            try
            {
                Exporter exporter = Exporter.getInstance();
                exporter.doExport(project, ExportDialog.this, scenarioSaver, info, function,
                        displayName, snapshotWidth, snapshotHeight);
            }
            finally
            {
                Platform.runLater(() -> exportingProperty.set(false));
            }
        }
    }

    /**
     * Clear the status text, but only if we are not in the middle of a task.
     */
    private void clearStatus()
    {
        if (!progressBar.isVisible())
        {
            progressLabel.setVisible(false);
        }
    }
    
    /**
     * Return the identifier for the specific export function selected.
     */
    private ExportFunction getSelectedFunction()
    {
        return getSelectedTab().getFunction();
    }

    /**
     * Return the identifier for the specific export function selected.
     */
    private ExportTab getSelectedTab()
    {
        return (ExportTab) tabbedPane.getSelectionModel().getSelectedItem();
    }

    /**
     * Select a tab based on a function name.
     *
     * @param name The name of the function which match the tab to be selected.
     */
    private void selectTab(String name)
    {
        ExportTab tab = exportTabs.get(ExportFunction.getFunction(name));
        tabbedPane.getSelectionModel().select(tab);
        updateControls(tab);
    }

    /**
     * Call when the selection of the exportTabs changes to update related controls.
     *
     * @param tab The selected export tab.
     */
    private void updateControls(ExportTab tab)
    {
        exportButton.disableProperty().unbind();
        // Export button is disabled either:
        //  - when the exporting is in progress, or
        //  - if the selected tab is has missing essential info.
        tabInvalidity = tab.validProperty.not();
        exportButton.disableProperty().bind(tabInvalidity.or(exportingProperty));
        exportButton.setText(Config.getString("export.dialog.export"));
        clearStatus();
    }

    /**
     * Create all the export tabs that should appear as part of this dialogue.
     */
    private void createTabs()
    {
        Window asWindow = this.asWindow();
        String projectName = project.getProjectName();
        // The default directory to export to when export locally.
        File defaultExportDir = project.getProjectDir().getParentFile();

        addTab(new ExportPublishTab(project, this, scenarioSaver, scenarioInfo));
        addTab(new ExportAppTab(asWindow, scenarioInfo, projectName, defaultExportDir));
        addTab(new ExportProjectTab(asWindow, scenarioInfo, projectName, defaultExportDir));

        tabbedPane.getTabs().setAll(exportTabs.values());
        // This is to change the width of the tabs headers to fill the available space of the tabbed
        // pane. 30 is subtracted to forbid showing the autoscroll of the tab header. Currently,
        // there is no way of disabling it. See: https://bugs.openjdk.java.net/browse/JDK-8091334
        tabbedPane.tabMinWidthProperty()
                .bind(widthProperty().divide(tabbedPane.getTabs().size()).subtract(30));
    }

    /**
     * Adds a tab to the exportTabs map, by placing the function of the tab
     * as the key to the tab stored in the value.
     *
     * @param exportTab An Export tab to be added to the exportTabs map.
     */
    private void addTab(ExportTab exportTab)
    {
        exportTabs.put(exportTab.getFunction(), exportTab);
    }

    /**
     * Tell this dialog that the publish (to the Gallery) has finished and whether it was successful.
     *
     * @param success  True if the publish to the Gallery was successful, otherwise false.
     * @param msg      The message to be shown next to progress bar. Could be null, which
     *                 means do not show any message.
     */
    public void publishFinished(boolean success, String msg)
    {
        getSelectedTab().postPublish(success);
        setProgress(false, msg);
        if (success)
        {
            SwingUtilities.invokeLater(() -> Utility.openWebBrowser(Config.getPropString("greenfoot.gameserver.address") + "/home"));
        }
    }
    
    /**
     * We now know the upload size.
     * @param bytes  The maximum number of bytes will be transmitted
     */
    public void setUploadSize(int bytes)
    {
        uploadSize = bytes;
        progressBar.setProgress(0);
    }
    
    /**
     * The upload is progressing, a certain number of bytes were just transmitted.
     * @param bytes  The number of bytes just transmitted
     */
    public void progressMade(int bytes)
    {
        progressBar.setProgress(progressBar.getProgress() + ((double)bytes / uploadSize));
    }
}
