package bluej.groupwork.actions;

import bluej.pkgmgr.actions.IncludeLayoutAction;
import bluej.pkgmgr.actions.PkgMgrAction;

/**
 * A class to group team actions for a project, and manage enable/disable
 * of the actions.
 * 
 * @author Davin McCall
 * @version $Id: TeamActionGroup.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class TeamActionGroup
{
    private StatusAction statusAction = new StatusAction();
    private UpdateAction updateAction = new UpdateAction();
    private TeamSettingsAction teamSettingsAction = new TeamSettingsAction();
    private CommitCommentAction commitCommentAction = new CommitCommentAction();
    private ImportAction importAction = new ImportAction();
    private IncludeLayoutAction includeLayoutAction = new IncludeLayoutAction();
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
    
    public UpdateAction getUpdateAction()
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
        includeLayoutAction.setEnabled(enabled);
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
        includeLayoutAction.setEnabled(false);
        showLogAction.setEnabled(false);
    }

    public PkgMgrAction getIncludeLayoutAction()
    {
        return includeLayoutAction;
    }
}
