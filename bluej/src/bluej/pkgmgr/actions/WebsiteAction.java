package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Attempt to open the BlueJ website in a browser. The url is taken from the
 * bluej.defs configuration file.
 */

final public class WebsiteAction extends PkgMgrAction {
    
    static private WebsiteAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public WebsiteAction getInstance()
    {
        if(instance == null)
            instance = new WebsiteAction();
        return instance;
    }
    
    private WebsiteAction()
    {
        super("menu.help.website");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showWebPage(Config.getPropString("bluej.url.bluej"));
    }
}
