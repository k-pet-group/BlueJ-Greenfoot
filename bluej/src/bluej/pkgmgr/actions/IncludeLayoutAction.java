package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle whether the Graph Layout should be include in teamwork commits
 * 
 * @author Bruce Quig
 * @version $Id: IncludeLayoutAction.java 4708 2006-11-27 00:47:57Z bquig $
 */
final public class IncludeLayoutAction extends PkgMgrAction {
    
    //static private IncludeLayoutAction instance = null;
    
    public IncludeLayoutAction()
    {
        super("team.includeLayout");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        //pmf.updateIncludeLayout();
    }
}
