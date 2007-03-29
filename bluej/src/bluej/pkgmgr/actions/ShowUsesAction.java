package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "uses" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowUsesAction.java 4905 2007-03-29 06:06:30Z davmac $
 */

final public class ShowUsesAction extends PkgMgrAction
{
    public ShowUsesAction()
    {
        super("menu.view.showUses");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowUsesInPackage();
    }
}
