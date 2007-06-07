package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.HistoryFrame;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * An action to show the repository history.
 * 
 * @author Davin McCall
 * @version $Id: ShowLogAction.java 5090 2007-06-07 02:38:51Z bquig $
 */
public class ShowLogAction extends TeamAction
{
    public ShowLogAction()
    {
        super(Config.getString("team.history"), false);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        HistoryFrame hd = new HistoryFrame(pmf);
        hd.setLocationRelativeTo(pmf);
        hd.setVisible(true);
    }

}
