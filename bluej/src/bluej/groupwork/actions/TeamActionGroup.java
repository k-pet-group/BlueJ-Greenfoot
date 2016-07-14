/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016  Michael Kolling and John Rosenberg 
 
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
import bluej.pkgmgr.PkgMgrFrame;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * A class to group team actions for a project, and manage enable/disable
 * of the actions.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Swing)
public class TeamActionGroup
{
    private final String commitLabel;
    private final boolean initialTeamMode;
    private final boolean initialIsDVCS;
    private StatusAction statusAction;
    private UpdateDialogAction updateAction;
    private TeamSettingsAction teamSettingsAction;
    private CommitCommentAction commitCommentAction;
    private ImportAction importAction;
    private ShowLogAction showLogAction;
    
    /**
     * Construct a new team action group, with various actions disabled
     * or enabled depending whether we are in team mode or non-team mode.
     * 
     * @param teamMode should teamMode be enabled
     */
    public TeamActionGroup(boolean teamMode)
    {
        this(teamMode, false);
    }
    /**
     * Construct a new team action group, with various actions disabled
     * or enabled depending whether we are in team mode or non-team mode.
     * 
     * @param teamMode should teamMode be enabled
     * @param isDVCS is this a distributed version control? (e.g. git).
     */
    public TeamActionGroup(boolean teamMode, boolean isDVCS)
    {
        String label = "team.commit";
        if (isDVCS){
            label = "team.commitPush";
        }
        this.commitLabel = label;
        this.initialTeamMode = teamMode;
        this.initialIsDVCS = isDVCS;
    }
    
    private void createAll(PkgMgrFrame pmf)
    {
        if (statusAction == null)
        {
            statusAction = new StatusAction(pmf);
            updateAction = new UpdateDialogAction(pmf);
            teamSettingsAction = new TeamSettingsAction(pmf);
            commitCommentAction = new CommitCommentAction(pmf, commitLabel);
            importAction = new ImportAction(pmf);
            showLogAction = new ShowLogAction(pmf);
            setTeamMode(pmf, initialTeamMode, initialIsDVCS);
        }
    }
    
    public StatusAction getStatusAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return statusAction;
    }
    
    public UpdateDialogAction getUpdateAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return updateAction;
    }
    
    public TeamSettingsAction getTeamSettingsAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return teamSettingsAction;
    }
    
    public CommitCommentAction getCommitCommentAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return commitCommentAction;
    }
    
    public ImportAction getImportAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return importAction;
    }
    
    public ShowLogAction getShowLogAction(PkgMgrFrame pmf)
    {
        createAll(pmf);
        return showLogAction;
    }
    
    public void setTeamMode(PkgMgrFrame pmf, boolean enabled, boolean isDCVS)
    {
        createAll(pmf);
        statusAction.setEnabled(enabled);
        updateAction.setEnabled(enabled);
        teamSettingsAction.setEnabled(enabled);
        showLogAction.setEnabled(enabled);
        
        String label = "team.commit";
        if (isDCVS){
            label = "team.commitPush";
        }
        commitCommentAction.setName(Config.getString(label));
        commitCommentAction.setEnabled(enabled);
        // import is allowed if we are not already shared
        importAction.setEnabled(!enabled);
    }
    
    public void setAllDisabled(PkgMgrFrame pmf)
    {
        createAll(pmf);
        statusAction.setEnabled(false);
        updateAction.setEnabled(false);
        teamSettingsAction.setEnabled(false);
        commitCommentAction.setEnabled(false);
        importAction.setEnabled(false);
        showLogAction.setEnabled(false);
    }

}
