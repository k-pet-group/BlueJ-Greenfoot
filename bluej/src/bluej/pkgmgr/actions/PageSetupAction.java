package bluej.pkgmgr.actions;

import java.awt.Event;
import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Page setup, for printing. Specify page layout etc via a dialog box.
 * 
 * @author Davin McCall
 * @version $Id: PageSetupAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.package.pageSetup", KeyEvent.VK_P, SHORTCUT_MASK | Event.SHIFT_MASK);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPageSetup();
    }
}
