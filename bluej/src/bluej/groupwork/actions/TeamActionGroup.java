/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016,2017,2019  Michael Kolling and John Rosenberg
 
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
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * A class to group team actions for a project, and manage enable/disable
 * of the actions.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class TeamActionGroup
{
    private final String commitLabel;
    private final boolean initialTeamMode;
    private StatusAction statusAction;
    private UpdateDialogAction updateAction;
    private TeamSettingsAction teamSettingsAction;
    private CommitCommentAction commitCommentAction;
    private ShareAction shareAction;
    private ShowLogAction showLogAction;
    
        /**
     * Construct a new team action group, with various actions disabled
     * or enabled depending whether we are in team mode or non-team mode.
     * 
     * @param teamMode should teamMode be enabled
     */
    public TeamActionGroup(boolean teamMode)
    {
        String label = "team.commit";
        label = "team.commitPush";
        this.commitLabel = label;
        this.initialTeamMode = teamMode;
    }
    
    private void createAll()
    {
        if (statusAction == null)
        {
            statusAction = new StatusAction();
            updateAction = new UpdateDialogAction();
            teamSettingsAction = new TeamSettingsAction();
            commitCommentAction = new CommitCommentAction(commitLabel);
            shareAction = new ShareAction();
            showLogAction = new ShowLogAction();
            setTeamMode(initialTeamMode);
        }
    }
    
    public StatusAction getStatusAction()
    {
        createAll();
        return statusAction;
    }
    
    public UpdateDialogAction getUpdateAction()
    {
        createAll();
        return updateAction;
    }
    
    public TeamSettingsAction getTeamSettingsAction()
    {
        createAll();
        return teamSettingsAction;
    }
    
    public CommitCommentAction getCommitCommentAction()
    {
        createAll();
        return commitCommentAction;
    }
    
    public ShareAction getShareAction()
    {
        createAll();
        return shareAction;
    }
    
    public ShowLogAction getShowLogAction()
    {
        createAll();
        return showLogAction;
    }
    
    public void setTeamMode(boolean enabled)
    {
        createAll();
        statusAction.setEnabled(enabled);
        updateAction.setEnabled(enabled);
        teamSettingsAction.setEnabled(enabled);
        showLogAction.setEnabled(enabled);
        
        String label = "team.commit";
        label = "team.commitPush";
        commitCommentAction.setName(Config.getString(label), true);
        commitCommentAction.setEnabled(enabled);
        // import is allowed if we are not already shared
        shareAction.setEnabled(!enabled);
    }
    
    public void setAllDisabled()
    {
        createAll();
        statusAction.setEnabled(false);
        updateAction.setEnabled(false);
        teamSettingsAction.setEnabled(false);
        commitCommentAction.setEnabled(false);
        shareAction.setEnabled(false);
        showLogAction.setEnabled(false);
    }
}
