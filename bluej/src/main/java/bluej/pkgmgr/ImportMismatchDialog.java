/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Dialog for showing the user a list of files which
 * had mismatched package lines on an open non-BlueJ.
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public class ImportMismatchDialog extends Dialog<Boolean>
{
    private static final String dialogTitle = Config.getString("pkgmgr.importmismatch.title");
    private final ButtonType CONTINUE;

    public ImportMismatchDialog(Window parent, List<File> files)
    {
        initOwner(parent);
        setTitle(dialogTitle);
        initModality(Modality.WINDOW_MODAL);
        Config.addDialogStylesheets(getDialogPane());
        setResizable(true);

        VBox mainPanel = new VBox();
        JavaFXUtil.addStyleClass(mainPanel, "import-mismatch-content");
        mainPanel.getChildren().add(new Label(Config.getStringList("pkgmgr.importmismatch.helpLine").stream().collect(Collectors.joining(" "))));
        mainPanel.getChildren().add(new ScrollPane(new VBox(Utility.mapList(files, f -> new Label(f.toString())).toArray(new Node[0]))));
        
        CONTINUE = new ButtonType(BlueJTheme.getContinueLabel(), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, CONTINUE);
        setResultConverter(bt -> bt == CONTINUE);
        
        getDialogPane().setContent(mainPanel);
        
        //setOnShown(e -> org.scenicview.ScenicView.show(mainPanel.getScene()));
    }
}
