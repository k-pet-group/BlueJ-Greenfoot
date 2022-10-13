/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013,2015,2018,2019  Poul Henriksen and Michael Kolling
 
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

import static greenfoot.export.Exporter.ExportFunction;

import bluej.Boot;
import bluej.Config;
import bluej.utility.Utility;
import greenfoot.export.mygame.ExportInfo;
import greenfoot.export.mygame.ScenarioInfo;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.SwingUtilities;

/**
 * Export dialog's tab for exporting to a standalone application.
 * 
 * @author Michael Kolling
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ExportAppTab extends ExportLocalTab
{
    /**
     * Creates a new instance of ExportAppTab
     *
     * @param parent            The window which will host this tab.
     * @param scenarioInfo      The scenario info needed for different export functions.
     * @param scenarioName      The name of the scenario to be shared.
     * @param defaultExportDir  The default directory to select from.
     */
    public ExportAppTab(Window parent, ScenarioInfo scenarioInfo, String scenarioName, File defaultExportDir)
    {
        super(parent, scenarioInfo, scenarioName, defaultExportDir, "app", ".jar");
    }

    @Override
    public ExportFunction getFunction()
    {
        return ExportFunction.APP;
    }

    @Override
    protected void buildContentPane(final File targetFile)
    {
        super.buildContentPane(targetFile);

        String sep = Config.isWinOS() ? ";" : ":";
        String javaAndClasspathBefore = "\"" + Config.getJDKExecutablePath(null, "java") + "\" -cp \"" +
            Utility.urlsToFiles(Boot.getInstance().getJavaFXClassPath()).stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(sep)) + sep;
        String javaAndClasspathAfter = "\" --module-path \"" + Boot.getInstance().getJavaFXLibDir() + "\" --add-modules=ALL-MODULE-PATH greenfoot.export.GreenfootScenarioApplication";

        Hyperlink moreInfo = new Hyperlink(Config.getString("export.app.more"));
        moreInfo.setOnAction(event -> SwingUtilities.invokeLater(() -> Utility.openWebBrowser("https://www.greenfoot.org/doc/run_standalone")));

        Label commandLineExplanation = new Label("Command to run scenario on this machine:");

        TextArea commandLineBox = new TextArea();
        commandLineBox.setWrapText(true);
        commandLineBox.setEditable(false);
        commandLineBox.textProperty().bind(Bindings.concat(javaAndClasspathBefore, targetDirField.textProperty(), javaAndClasspathAfter));

        Button copyButton = new Button(Config.getString("editor.copyLabel"));
        copyButton.setOnAction(e -> Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, commandLineBox.getSelectedText().isEmpty() ? commandLineBox.getText() : commandLineBox.getSelectedText())));

        BorderPane.setAlignment(copyButton, Pos.CENTER);
        BorderPane.setMargin(copyButton, new Insets(0, 0, 0, 10));
        BorderPane borderPane = new BorderPane(commandLineBox, commandLineExplanation, copyButton, moreInfo, null);
        
        ((Pane)getContent()).getChildren().addAll(borderPane, lockScenario, hideControls);
    }

    @Override
    protected ExportInfo getExportInfo()
    {
        ExportInfo info = super.getExportInfo();
        info.setLocked(isLockScenario());
        info.setHideControls(isHideControls());
        return info;
    }
    
    
}
