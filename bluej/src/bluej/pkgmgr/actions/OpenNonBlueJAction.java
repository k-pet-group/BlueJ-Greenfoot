package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "open non-BlueJ". This allows them to choose a directory to
 * open as a project.
 */
final public class OpenNonBlueJAction extends PkgMgrAction {
    
    static private OpenNonBlueJAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public OpenNonBlueJAction getInstance()
    {
        if(instance == null)
            instance = new OpenNonBlueJAction();
        return instance;
    }
    
    private OpenNonBlueJAction()
    {
        super("menu.package.openNonBlueJ");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doOpenNonBlueJ();
    }
}
