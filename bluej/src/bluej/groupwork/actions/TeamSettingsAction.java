package bluej.groupwork.actions;

import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.actions.PkgMgrAction;


/**
 * Action to show teamwork settings dialog
 * 
 * @version $Id$
 */
public class TeamSettingsAction extends PkgMgrAction
{
    /** Creates a new instance of TeamSettingsAction */
    public TeamSettingsAction()
    {
        super("team.settings");
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        Project project = pmf.getProject();
        project.getTeamSettingsDialog().doTeamSettings();
    }
}
