package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Standard API help. Attempt to show the API help in a web browser. The
 * URL is taken from bluej.defs configuration file.
 * 
 * @author Davin McCall
 * @version $Id: StandardAPIHelpAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class StandardAPIHelpAction extends PkgMgrAction {
    
    static private StandardAPIHelpAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public StandardAPIHelpAction getInstance()
    {
        if(instance == null)
            instance = new StandardAPIHelpAction();
        return instance;
    }
    
    private StandardAPIHelpAction()
    {
        super("menu.help.standardApi");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showWebPage(Config.getPropString("bluej.url.javaStdLib"));
    }
}
