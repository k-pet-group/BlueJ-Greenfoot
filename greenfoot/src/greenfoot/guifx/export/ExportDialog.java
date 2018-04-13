/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013,2016,2018  Poul Henriksen and Michael Kolling
 
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
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.Utility;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
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

import static greenfoot.guifx.export.ExportPane.ExportFunction;

/**
 * A dialog allowing the user to export a scenario in a variety of ways.
 *
 * @author Amjad Altadmri
 */
public class ExportDialog extends FXCustomizedDialog<Void>
{
    private static final String dialogTitle = Config.getApplicationName() + ": "
            + Config.getString("export.dialog.title");

    private final Project project;
    private final ClassTarget currentWorld;
    private final Image snapshot;
    private int uploadSize;
    private final BooleanProperty exportingProperty = new SimpleBooleanProperty(false);

    private final TabPane tabbedPane = new TabPane();
    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private final Map<ExportFunction, ExportPane> panes = new LinkedHashMap<>();
    private Button continueButton;

    public ExportDialog(Window parent, Project project, ClassTarget currentWorld, Image snapshot)
            throws ExportException
    {
        super(parent, dialogTitle, "export-dialog");
        this.project = project;
        this.currentWorld = currentWorld;
        this.snapshot = snapshot;
        setModal(true);
        makeDialog();
    }

    /**
     * Create the dialog interface.
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
        contentPane.setBottom(new HBox(progressBar, progressLabel));

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, ButtonType.OK);
        Button closeButton = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setOnAction(event ->
                Config.putPropString("greenfoot.lastExportPane", getSelectedFunction().name()));
        closeButton.disableProperty().bind(exportingProperty);

        continueButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        continueButton.setText(Config.getString("export.dialog.export"));
        continueButton.setOnAction(event -> doExport());
        continueButton.disableProperty().bind(exportingProperty);

        if (currentWorld == null)
        {
            throw new ExportException(Config.getString("export.noworld.dialog.msg"));
        }

        // Check that a zero-argument constructor is available
        List<ConstructorReflective> constructors = currentWorld.getTypeReflective().getDeclaredConstructors();
        boolean noZeroArgConstructor = constructors.stream().noneMatch(con -> con.getParamTypes().isEmpty());
        if (noZeroArgConstructor)
        {
            throw new ExportException(Config.getString("export.noconstructor.dialog.msg"));
        }

        createPanes();

        if (snapshot != null)
        {
            ExportPublishPane publishPane = (ExportPublishPane) panes.get(ExportFunction.Publish);
            publishPane.setImage(snapshot);
        }
        selectPane(Config.getPropString("greenfoot.lastExportPane"));
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
        continueButton.setText(s);
    }
    
    /**
     * The export button was pressed. Do the exporting now.
     */
    private void doExport()
    {
        if (getSelectedPane().prePublish())
        {
            // TODO expThread
        }
    }

    /**
     * Clear the status text, but only if we are not in the middle of a task.
     */
    private void clearStatus()
    {
        if(!progressBar.isVisible())
        {
            progressLabel.setVisible(false);
        }
    }
    
    /**
     * Return the identifier for the specific export function selected.
     */
    private ExportFunction getSelectedFunction()
    {
        return getSelectedPane().getFunction();
    }

    /**
     * Return the identifier for the specific export function selected.
     */
    private ExportPane getSelectedPane()
    {
        return (ExportPane) tabbedPane.getSelectionModel().getSelectedItem();
    }

    /**
     * Called when the selection of the tabs changes.
     */
    private void selectPane(String name)
    {
        tabbedPane.getSelectionModel().select(panes.get(ExportFunction.getFunction(name)));
        continueButton.setText(Config.getString("export.dialog.export"));
        clearStatus();
    }
    
    /**
     * Create all the panes that should appear as part of this dialogue.
     */
    private void createPanes()
    {
        Window asWindow = this.asWindow();
        String projectName = project.getProjectName();
        // The default directory to export to when export locally.
        File defaultExportDir = project.getProjectDir().getParentFile();

        addPane(new ExportPublishPane(project, this));
        addPane(new ExportAppPane(asWindow, projectName, defaultExportDir));
        addPane(new ExportProjectPane(asWindow, projectName, defaultExportDir));

        tabbedPane.getTabs().setAll(panes.values());
        // This is to change the width of the tabs headers to fill the available space of the tabbed
        // pane. 30 is subtracted to forbid showing the autoscroll of the tab header. Currently,
        // there is no way of disabling it. See: https://bugs.openjdk.java.net/browse/JDK-8091334
        tabbedPane.tabMinWidthProperty()
                .bind(widthProperty().divide(tabbedPane.getTabs().size()).subtract(30));
    }

    /**
     * Adds a pane to the panes map, by placing the function of the pane
     * as the key to the pane stored in the value.
     *
     * @param exportPane An Export pane to be added to the panes map.
     */
    private void addPane(ExportPane exportPane)
    {
        panes.put(exportPane.getFunction(), exportPane);
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
        getSelectedPane().postPublish(success);
        setProgress(false, msg);
        if (success)
        {
            Utility.openWebBrowser(Config.getPropString("greenfoot.gameserver.address") + "/home");
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
