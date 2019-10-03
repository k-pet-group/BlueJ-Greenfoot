/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2019  Michael Kolling and John Rosenberg
 
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
import bluej.pkgmgr.Project;

import javafx.scene.control.ListCell;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class to display files to be committed in a list for the CommitCommentsFrame
 * 
 * @author Amjad Altadmri
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class TeamStatusInfoCell extends ListCell<TeamStatusInfo>
{
    private Project project;

    public TeamStatusInfoCell(Project proj)
    {
        super();
        project = proj;
    }

    @Override
    public void updateItem(TeamStatusInfo info, boolean empty)
    {
        super.updateItem(info, empty);
        if (empty || info == null) {
            setText(null);
        } else {
            String topText;
            topText = ResourceDescriptor.getDCVSResource(project, info, true, false);
            setText(topText);
        }
        setGraphic(null);
    }
}