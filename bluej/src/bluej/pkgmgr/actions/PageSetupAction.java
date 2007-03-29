package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Page setup, for printing. Specify page layout etc via a dialog box.
 * 
 * @author Davin McCall
 * @version $Id: PageSetupAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class PageSetupAction extends PkgMgrAction
{
    public PageSetupAction()
    {
        super("menu.package.pageSetup");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPageSetup();
    }
}
