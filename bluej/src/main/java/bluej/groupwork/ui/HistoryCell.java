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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Renderer for cells in the log/history list.
 * 
 * @author Amjad Altadmri
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
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
            setText(info.getDate() + "  "  + info.getRevision() + "\n" + info.getUser() + "\n" + info.getComment());
            setGraphic(getGraphics(info));
        }
    }

    private Node getGraphics(HistoryInfo info)
    {
        ObservableList<String> files = FXCollections.observableArrayList(info.getFiles());
        ListView<String> filesListView = new ListView<>(files);
        // Generally cells are around 24px
        final int ROW_HEIGHT = 24;
        // This sets the initial height of the ListView:
        filesListView.setPrefHeight(files.size() * ROW_HEIGHT + 2);
        return filesListView;
    }
}
