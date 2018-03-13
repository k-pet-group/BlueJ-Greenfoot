/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013,2015,2018  Poul Henriksen and Michael Kolling
 
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
import bluej.utility.DialogManager;

import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Export dialog pane for exporting to a standalone application.
 * 
 * @author Michael Kolling
 * @author Amjad Altadmri
 */
public class ExportAppPane extends ExportPane
{
    public static final String FUNCTION = "APP";
    
    private final Window parent;
    private TextField targetDirField;
    
    /**
     * Creates a new instance of ExportAppPane
     *
     * @param parent            The parent window of the contained dialog.
     * @param scenarioName      The name of the scenario to be shared.
     * @param defaultExportDir  The default directory to select from.
     */
    public ExportAppPane(Window parent, String scenarioName, File defaultExportDir)
    {
        super();
        this.parent = parent;
        File targetFile = new File(defaultExportDir, scenarioName + ".jar");
        makePane(targetFile);
    }
    
    /**
     * Return the directory where the scenario should be exported.
     */
    public String getExportName()
    {
        return targetDirField.getText();
    }
    
    /**
     * Build the component.
     *
     * @param targetFile  The initial target file that will be export to.
     */
    private void makePane(final File targetFile)
    {
        Label exportLocationLabel = new Label(Config.getString("export.app.location"));

        targetDirField = new TextField(targetFile.toString());
        targetDirField.setPrefColumnCount(26);
        targetDirField.setEditable(false);

        Button browse = new Button(Config.getString("export.app.browse"));
        browse.setOnAction(event -> targetDirField.setText(askForFileName(targetFile)));

        HBox exportLocationPane = new HBox(exportLocationLabel, targetDirField, browse);
        // exportLocationPane.setBackground(backgroundColor);
        exportLocationPane.setAlignment(Pos.BASELINE_LEFT);

        VBox inputPane = new VBox(exportLocationPane, lockScenario, hideControls);
        inputPane.setAlignment(Pos.BASELINE_LEFT);
        // inputPane.setBackground(backgroundColor);

        Label helpLabel = new Label(Config.getString("export.app.help"));
        VBox mainPane = new VBox(helpLabel, inputPane);
        setContent(mainPane);
    }
    
    /**
     * Get a user-chosen file name via a file system browser.
     * Set the pane's text field to the selected file.
     *
     * @param targetFile  The initial target file that will be export to.
     */
    private String askForFileName(File targetFile)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Config.getString("export.app.choose"));
        fileChooser.setInitialDirectory(targetFile.getParentFile());
        File file = fileChooser.showSaveDialog(parent);
        if (file == null)
        {
            // The user canceled the file chooser dialog.
            return "";
        }

        String newName = file.getPath();
        if (!newName.endsWith(".jar"))
        {
            if (newName.toLowerCase().endsWith(".jar"))
            {
                // This means there is .jar but has capital letters, so get rid of
                // it to be replaced with the proper small letters extension.
                newName = newName.substring(0, newName.length()-".jar".length());
            }
            newName += ".jar";
        }
        if (file.exists())
        {
            boolean overwrite = DialogManager.askQuestionFX(parent, "file-exists-overwrite",
                    new String[] {newName}) == 0;
            if (!overwrite)
            {
                // The user didn't accept to overwrite the file,
                // so re-ask them to choose a file.
                return askForFileName(targetFile);
            }
        }
        return newName;
    }

    @Override
    public void activated()
    {
        // Nothing special to do here
    }    
    
    @Override
    public boolean prePublish()
    {
        // Nothing special to do here   
        return true;
    }
    
    @Override
    public void postPublish(boolean success)
    {
        // Nothing special to do here       
    }
}
