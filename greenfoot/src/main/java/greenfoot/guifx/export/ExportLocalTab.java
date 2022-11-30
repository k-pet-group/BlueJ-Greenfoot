/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2019  Poul Henriksen and Michael Kolling
 
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
import greenfoot.export.mygame.ExportInfo;
import greenfoot.export.mygame.ScenarioInfo;

import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Export dialog's tab for exporting to a local file (project or standalone application).
 * 
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public abstract class ExportLocalTab extends ExportTab
{
    private final String type;
    private final String extension;

    private final Window parent;
    protected TextField targetDirField;

    /**
     * Creates a new instance of an Export Local Tab.
     *
     * @param parent            The window which will host this tab.
     * @param scenarioInfo      The scenario info needed for different export functions.
     * @param scenarioName      The name of the scenario to be shared.
     * @param defaultExportDir  The default directory to select from.
     * @param type              The type of the export. e.g. "app" or "project".
     * @param extension         The extension of the exported file. e.g. ".jar" or ".gfar".
     */
    public ExportLocalTab(Window parent, ScenarioInfo scenarioInfo, String scenarioName,
                          File defaultExportDir, String type, String extension)
    {
        super(scenarioInfo, "export-" + type + ".png");
        this.parent = parent;
        this.type = type;
        this.extension = extension;

        buildContentPane(new File(defaultExportDir, scenarioName + extension));
        applySharedStyle();
        getContent().getStyleClass().add("export-local-tab");
    }

    /**
     * Return the directory where the scenario should be exported.
     */
    public String getExportFileName()
    {
        return targetDirField.getText();
    }

    /**
     * Build the component.
     *
     * @param targetFile  The initial target file that will be export to.
     */
    protected void buildContentPane(final File targetFile)
    {
        Label exportLocationLabel = new Label(Config.getString("export." + type + ".location"));

        targetDirField = new TextField(targetFile.toString());
        targetDirField.setPrefColumnCount(30);
        targetDirField.setEditable(false);

        validProperty.bind(targetDirField.textProperty().isNotEmpty());

        Button browse = new Button(Config.getString("export." + type + ".browse"));
        browse.setOnAction(event -> targetDirField.setText(askForFileName(targetFile)));

        HBox exportLocationPane = new HBox(exportLocationLabel, targetDirField, browse);
        exportLocationPane.setAlignment(Pos.BASELINE_LEFT);
        exportLocationPane.getStyleClass().add("location-pane");

        Label helpLabel = new Label(Config.getString("export." + type + ".help"));
        helpLabel.setWrapText(true);
        VBox vBox = new VBox(helpLabel, exportLocationPane);
        vBox.setPrefWidth(400.0);
        setContent(vBox);
    }

    /**
     * Get a user-chosen file name via a file system browser.
     * Set the tab's text field to the selected file.
     *
     * @param targetFile  The initial target file that will be export to.
     * @return The file name chosen by the user. If the user canceled the dialog,
     *         an empty string will be returned.
     */
    private String askForFileName(File targetFile)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Config.getString("export." + type + ".choose"));
        fileChooser.setInitialDirectory(targetFile.getParentFile());
        File file = fileChooser.showSaveDialog(parent);
        if (file == null)
        {
            // The user canceled the file chooser dialog.
            return "";
        }

        String newName = file.getPath();
        if (!newName.endsWith(extension))
        {
            if (newName.toLowerCase().endsWith(extension))
            {
                // This means that the extension exists but it has at least a capital letter,
                // so get rid of it to be replaced with the proper small letters extension.
                newName = newName.substring(0, newName.length() - extension.length());
            }
            newName += extension;
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
    protected void updateInfoFromFields()
    {
        // Nothing to do.
    }
    
    @Override
    protected ExportInfo getExportInfo()
    {
        ExportInfo info = new ExportInfo(scenarioInfo);
        info.setExportFileName(getExportFileName());
        return info;
    }
}
