package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * BlueJ tutorial website - attempt to show it in a web browser. The URL
 * is taken from bluej.defs configuration file.
 * 
 * @author Davin McCall
 * @version $Id: TutorialAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class TutorialAction extends PkgMgrAction {
    
    static private TutorialAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public TutorialAction getInstance()
    {
        if(instance == null)
            instance = new TutorialAction();
        return instance;
    }
    
    private TutorialAction()
    {
        super("menu.help.tutorial");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showWebPage(Config.getPropString("bluej.url.tutorial"));
    }
}
