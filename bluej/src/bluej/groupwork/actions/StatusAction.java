package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.StatusFrame;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;


/**
 * Action to show status.
 * 
 * @author bquig
 * @version $Id$
 */
public class StatusAction extends TeamAction
{
    /** Creates a new instance of StatusAction */
    public StatusAction()
    {
        super("team.status", true);
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.status"));
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        // save all bluej.pkg files first
        Project project = pmf.getProject();
        project.saveAllGraphLayout();
        doStatus(pmf);
    }

    private void doStatus(PkgMgrFrame pmf)
    {
        StatusFrame status = pmf.getProject().getStatusWindow(pmf);
        status.setVisible(true);
        status.update();
    }
}
