package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.HistoryFrame;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * An action to show the repository history.
 * 
 * @author Davin McCall
 * @version $Id: ShowLogAction.java 5046 2007-05-22 05:00:26Z bquig $
 */
public class ShowLogAction extends TeamAction
{
    public ShowLogAction()
    {
        super(Config.getString("team.history"), true);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        HistoryFrame hd = new HistoryFrame(pmf);
        hd.setLocationRelativeTo(pmf);
        hd.setVisible(true);
    }

}
