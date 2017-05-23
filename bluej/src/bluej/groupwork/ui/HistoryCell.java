/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.ui;

import bluej.groupwork.HistoryInfo;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Renderer for cells in the log/history list.
 * 
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class HistoryCell extends ListCell<HistoryInfo>
{
    @Override
    public void updateItem(HistoryInfo info, boolean empty)
    {
        super.updateItem(info, empty);
        if (empty || info == null)
        {
            setText(null);
            setGraphic(null);
        }
        else
        {
            setText(info.getDate() + "  "  + info.getRevision() + "  " + info.getUser());
            setGraphic(getGraphics(info));
        }
    }

    private Node getGraphics(HistoryInfo info)
    {
        String [] files = info.getFiles();
        String filesText = files[0];
        for (int i = 1; i < files.length; i++) {
            filesText += "\n" + files[i];
        }
        HBox filesBox = new HBox();
        filesBox.getChildren().addAll(new Label("    "), new TextArea(filesText));

        Label commentArea = new Label();
        commentArea.setWrapText(true);
        commentArea.setText(info.getComment());
        HBox commentBox = new HBox();
        commentBox.getChildren().addAll(new Label("        "), commentArea);

        VBox mainBox = new VBox();
        mainBox.getChildren().addAll(/*topLabel,*/ filesBox, commentBox);
        return mainBox;
    }
}
