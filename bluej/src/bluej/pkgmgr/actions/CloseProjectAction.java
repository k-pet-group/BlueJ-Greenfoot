package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "close project". Save & close the current project. If the command
 * was issued from a menu, always keep the last window open, otherwise close
 * the window regardless.
 * 
 * @author Davin McCall
 * @version $Id: CloseProjectAction.java 5891 2008-09-22 11:31:58Z davmac $
 */
final public class CloseProjectAction extends PkgMgrAction
{
    public CloseProjectAction()
    {
        super("menu.package.close");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doClose(true, true);
    }
}
