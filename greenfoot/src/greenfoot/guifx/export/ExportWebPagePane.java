/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013,2018  Poul Henriksen and Michael Kolling
 
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
 * A Pane to Export a scenario to a Web Page.
 *
 * @author Michael Kolling
 * @author Amjad Altadmri
 */
public class ExportWebPagePane extends ExportPane
{
    public static final String FUNCTION = "WEB";
    
    private static final String helpLine1 = Config.getString("export.web.help");
    private static final String exportLocationLabelText = Config.getString("export.web.exportLocation");
    private Window parent;
    private TextField targetDirField;

    /** 
     * Create a an export pane for export to web pages.
     *
     * @param parent            The parent window of the contained dialog
     * @param scenarioName      The name of the scenario to be shared.
     * @param defaultExportDir  The default directory to select from.
     */
    public ExportWebPagePane(Window parent, String scenarioName, File defaultExportDir)
    {
        super();
        this.parent = parent;
        File exportDir = new File(defaultExportDir, scenarioName + "-export");

        if (exportDir.exists())
        {
            exportDir.delete();
        }
        
        makePane(exportDir);
    }
    
    /**
     * Return the directory where the scenario should be exported.
     */
    public String getExportLocation()
    {
        return targetDirField.getText();
    }

    /**
     * Build the component.
     *
     * @param defaultDir The default directory to select from.
     */
    private void makePane(final File defaultDir)
    {
        HBox exportLocationPanel = new HBox();
        exportLocationPanel.setAlignment(Pos.BASELINE_LEFT);
        // exportLocationPanel.setBackground(backgroundColor);

        Label exportLocationLabel = new Label(exportLocationLabelText);

        targetDirField = new TextField(defaultDir.toString());
        targetDirField.setPrefColumnCount(24);
        targetDirField.setEditable(false);

        Button browse = new Button(Config.getString("export.web.browse"));
        browse.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Config.getString("export.web.choose"));
            fileChooser.setInitialDirectory(defaultDir);
            File file = fileChooser.showOpenDialog(parent);
            if(file != null)
            {
                targetDirField.setText(file.getPath());
            }
        });

        exportLocationPanel.getChildren().addAll(exportLocationLabel, targetDirField, browse);

        VBox inputPanel = new VBox();
        inputPanel.setAlignment(Pos.BASELINE_LEFT);
        // TODO see if the next line is needed or delete it
        // inputPanel.setBackground(backgroundColor); // if needed, convert it to inputPanel.getStyleClass().add(super.getStyle());
        inputPanel.getChildren().addAll(exportLocationPanel, lockScenario);

        VBox mainPane = new VBox();
        setContent(mainPane);
        Label helpText1 = new Label(helpLine1);
        mainPane.getChildren().addAll(helpText1, inputPanel);
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