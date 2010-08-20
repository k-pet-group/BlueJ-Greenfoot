/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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


/**
 * A class to group team actions for a project, and manage enable/disable
 * of the actions.
 * 
 * @author Davin McCall
 */
public class TeamActionGroup
{
    private StatusAction statusAction = new StatusAction();
    private UpdateDialogAction updateAction = new UpdateDialogAction();
    private TeamSettingsAction teamSettingsAction = new TeamSettingsAction();
    private CommitCommentAction commitCommentAction = new CommitCommentAction();
    private ImportAction importAction = new ImportAction();
    private ShowLogAction showLogAction = new ShowLogAction();
    
    /**
     * Construct a new team action group, with various actions disabled
     * or enabled depending whether we are in team mode or non-team mode.
     */
    public TeamActionGroup(boolean teamMode)
    {
        setTeamMode(teamMode);
    }
    
    public StatusAction getStatusAction()
    {
        return statusAction;
    }
    
    public UpdateDialogAction getUpdateAction()
    {
        return updateAction;
    }
    
    public TeamSettingsAction getTeamSettingsAction()
    {
        return teamSettingsAction;
    }
    
    public CommitCommentAction getCommitCommentAction()
    {
        return commitCommentAction;
    }
    
    public ImportAction getImportAction()
    {
        return importAction;
    }
    
    public ShowLogAction getShowLogAction()
    {
        return showLogAction;
    }
    
    public void setTeamMode(boolean enabled)
    {
        statusAction.setEnabled(enabled);
        updateAction.setEnabled(enabled);
        teamSettingsAction.setEnabled(enabled);
        commitCommentAction.setEnabled(enabled);
        showLogAction.setEnabled(enabled);
        
        // import is allowed if we are not already shared
        importAction.setEnabled(!enabled);
    }
    
    public void setAllDisabled()
    {
        statusAction.setEnabled(false);
        updateAction.setEnabled(false);
        teamSettingsAction.setEnabled(false);
        commitCommentAction.setEnabled(false);
        importAction.setEnabled(false);
        showLogAction.setEnabled(false);
    }

}
