package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Page setup, for printing. Specify page layout etc via a dialog box.
 * 
 * @author Davin McCall
 * @version $Id: PageSetupAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class PageSetupAction extends PkgMgrAction {
    
    static private PageSetupAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public PageSetupAction getInstance()
    {
        if(instance == null)
            instance = new PageSetupAction();
        return instance;
    }
    
    private PageSetupAction()
    {
        super("menu.package.pageSetup");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPageSetup();
    }
}
