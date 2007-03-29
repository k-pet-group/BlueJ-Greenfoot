package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "close project". Save & close the current project. If the command
 * was issued from a menu, always keep the last window open, otherwise close
 * the window regardless.
 * 
 * @author Davin McCall
 * @version $Id: CloseProjectAction.java 4905 2007-03-29 06:06:30Z davmac $
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
        pmf.doClose(true);
    }
}
