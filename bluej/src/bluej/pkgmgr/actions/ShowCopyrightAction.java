package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Show copyright info in a dialog box.
 * 
 * @author Davin McCall
 * @version $Id: ShowCopyrightAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class ShowCopyrightAction extends PkgMgrAction {
    
    static private ShowCopyrightAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowCopyrightAction getInstance()
    {
        if(instance == null)
            instance = new ShowCopyrightAction();
        return instance;
    }
    
    private ShowCopyrightAction()
    {
        super("menu.help.copyright");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showCopyright();
    }
}
