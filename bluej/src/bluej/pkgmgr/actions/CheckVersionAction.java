package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.VersionCheckDialog;

/**
 * help...check for new version. Displays a dialog box with a "check version"
 * button, when pressed queries the web server to see if a newer version of
 * BlueJ is available.
 * 
 * @author Davin McCall
 * @version $Id: CheckVersionAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class CheckVersionAction extends PkgMgrAction {
    
    static private CheckVersionAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CheckVersionAction getInstance()
    {
        if(instance == null)
            instance = new CheckVersionAction();
        return instance;
    }

    private CheckVersionAction()
    {
        super("menu.help.versionCheck");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        new VersionCheckDialog(pmf);
    }
}
