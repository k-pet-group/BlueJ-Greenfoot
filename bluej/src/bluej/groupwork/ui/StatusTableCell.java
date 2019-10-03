/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2019  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.utility.Debug;

import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A TableCell which shows one cell in the second column's of Status table.
 * It is wrapped in a container to allow a border with padding to be applied.
 *
 * It also replaces StatusMessageCellRenderer class
 *
 * @author Amjad Altadmri
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class StatusTableCell extends TableCell<TeamStatusInfo, Object>
{
    private final int column;
    /**
     *
     */
    public StatusTableCell(int column)
    {
        this.column = column;
        setText("");
        setGraphic(null);
    }

    @Override
    protected void updateItem(Object v, boolean empty)
    {
        super.updateItem(v, empty);
        if (v != null)
        {
            if (v instanceof String)
            {
                setText(String.valueOf(v));
            }
            else if (v instanceof Status)
            {
                Status status = (Status) v;
                setText(getMessage(status));
                setTextFill(status.getStatusColour());
            }
            else
                {
                Debug.reportError("Status Table Cell should be either String or TeamStatusInfo.Status :" + v.toString());
            }
        }
    }

    // TODO NOT the best way but should work fine for now
    // Divide into three different Cell Factories: column1 and DVCS/nonDVCS for column 2
    private String getMessage(Status status)
    {
        switch (column) {
            case 1:
                return status.getDCVSStatusString(false);
            case 2:
                return status.getDCVSStatusString(true);
            default:
                break;
        }
        return null;
    }
}