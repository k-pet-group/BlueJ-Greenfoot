package bluej.pkgmgr.actions;

import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Check installed extensions. Pop up a dialog box displaying summary info
 * about each installed extension, allowing user to get a brief description
 * of each one.
 * 
 * @author Davin McCall
 * @version $Id: CheckExtensionsAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
public final class CheckExtensionsAction extends PkgMgrAction {
    
    static private CheckExtensionsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CheckExtensionsAction getInstance()
    {
        if(instance == null)
            instance = new CheckExtensionsAction();
        return instance;
    }
    
    private CheckExtensionsAction()
    {
        super("menu.help.extensions");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        ExtensionsManager.get().showHelp(pmf);
    }
}
