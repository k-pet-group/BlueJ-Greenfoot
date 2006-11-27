package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.HistoryFrame;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * An action to show the repository history.
 * 
 * @author Davin McCall
 * @version $Id: ShowLogAction.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class ShowLogAction extends TeamAction
{
    public ShowLogAction()
    {
        super(Config.getString("team.history"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        HistoryFrame hd = new HistoryFrame(pmf);
        hd.setLocationRelativeTo(pmf);
        hd.setVisible(true);
    }

}
