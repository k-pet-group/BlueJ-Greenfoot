package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * help...about. Display a brief info dialog on BlueJ.
 * 
 * @author Davin McCall
 * @version $Id: HelpAboutAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class HelpAboutAction extends PkgMgrAction {
    
    static private HelpAboutAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public HelpAboutAction getInstance()
    {
        if(instance == null)
            instance = new HelpAboutAction();
        return instance;
    }
    
    private HelpAboutAction()
    {
        super("menu.help.about");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.aboutBlueJ();
    }
}
