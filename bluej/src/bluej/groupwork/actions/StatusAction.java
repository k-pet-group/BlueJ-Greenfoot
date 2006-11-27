package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.StatusFrame;
import bluej.pkgmgr.PkgMgrFrame;


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
        super("team.status");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.status"));
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        // save all bluej.pkg files first
        pmf.getProject().saveAllGraphLayout();
        doStatus(pmf);
    }

    private void doStatus(final PkgMgrFrame pmf)
    {
        StatusFrame status = StatusFrame.getStatusWindow(pmf);
        status.setVisible(true);
        status.update();
    }
}
