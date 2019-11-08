/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.StatusFrame;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import javafx.stage.Stage;
import javafx.stage.Window;


/**
 * Action to show status.
 * 
 * @author bquig
 */
public class StatusAction extends TeamAction
{
    /** Creates a new instance of StatusAction */
    public StatusAction()
    {
        super("team.status", false);
        shortDescription = Config.getString("tooltip.status");
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        // save all bluej.pkg files first
        Project project = pmf.getProject();
        project.saveAll();
        doStatus(pmf);
    }

    private void doStatus(PkgMgrFrame pmf)
    {
        if (pmf.getProject().getTeamSettingsController().initRepository()) {
            StatusFrame status = pmf.getProject().getStatusWindow();
            if (! status.isShowing())
            {
                status.setLocationRelativeTo(pmf.getWindow());
                status.show();
            }
            else
            {
                Window window = status.asWindow();
                if (window instanceof Stage)
                {
                    ((Stage) window).toFront();
                }
            }
            status.update();
        }
    }
}
