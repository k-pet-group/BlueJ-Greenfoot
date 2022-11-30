/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.ui.CheckConnectionDialog;
import bluej.groupwork.ui.TeamSettingsPanel;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXPlatformSupplier;

import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Test the username, password, host, etc. settings to make sure they are valid
 * 
 * @author fisker
 */
@OnThread(Tag.FXPlatform)
public class ValidateConnectionAction extends TeamAction
{
    private TeamSettingsPanel teamSettingsPanel;
    private FXPlatformSupplier<Window> owner;
    
    public ValidateConnectionAction(TeamSettingsPanel teamSettingsPanel, FXPlatformSupplier<Window> owner)
    {
        super("team.settings.checkConnection", true);
        this.teamSettingsPanel = teamSettingsPanel;
        this.owner = owner;
    }
    
    @Override
    protected void actionPerformed(Project project)
    {
        TeamSettings settings = teamSettingsPanel.getSettings();
        new CheckConnectionDialog(owner.get(), settings).showAndCheck();
    }
}
