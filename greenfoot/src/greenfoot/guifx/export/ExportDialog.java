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
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.Utility;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

/**
 * A dialog allowing the user to export a scenario in a variety of ways.
 *
 * @author Amjad Altadmri
 */
public class ExportDialog extends FXCustomizedDialog<Void>
{
    private static final String dialogTitle = Config.getApplicationName() + ": " + Config.getString("export.dialog.title");

    private final Project project;
    private final ClassTarget currentWorld;
    private final Image snapshot;
    private String selectedFunction;
    private int uploadSize;

    private final TabPane tabbedPane = new TabPane();
    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private final HashMap<String, ExportPane> panes = new HashMap<>();
    private ExportPane selectedPane;
    private Button continueButton;
    private Button closeButton;

    public ExportDialog(Window parent, Project project, ClassTarget currentWorld, Image snapshot) throws ExportException
    {
        super(parent, dialogTitle, "");
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
        closeButton = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        continueButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        continueButton.setText(Config.getString("export.dialog.continue"));
        continueButton.setOnAction(event -> doExport());

        if (currentWorld == null)
        {
            throw new ExportException(Config.getString("export.noworld.dialog.msg"));
        }

        // Check that a zero-argument constructor is available
        Constructor<?>[] constructors = currentWorld.getClass().getConstructors();
        boolean noZeroArgConstructor = Stream.of(constructors).noneMatch(con -> con.getParameterCount() == 0);

        if (noZeroArgConstructor)
        {
            throw new ExportException(Config.getString("export.noconstructor.dialog.msg"));
        }

        createPanes();

        if (snapshot != null)
        {
            // TODO send a snapshot of the background
        }        
        clearStatus();
        
        if (selectedPane == null)
        {
            // TODO
        }
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
        if(selectedPane.prePublish())
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
        continueButton.setDisable(!enable);
        closeButton.setDisable(!enable);
    }
    
    /**
     * Called when the selection of the tabs changes.
     */
    private void showPane(String function, boolean saveAsDefault)
    {
        tabbedPane.getSelectionModel().select(panes.get(function));
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

        panes.put(ExportPublishPane.FUNCTION, new ExportPublishPane(project, this));
        panes.put(ExportAppPane.FUNCTION, new ExportAppPane(asWindow, projectName, defaultExportDir));
        panes.put(ExportProjectPane.FUNCTION, new ExportProjectPane(asWindow, projectName, defaultExportDir));

        tabbedPane.getTabs().setAll(panes.values());
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
        selectedPane.postPublish(success);
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
