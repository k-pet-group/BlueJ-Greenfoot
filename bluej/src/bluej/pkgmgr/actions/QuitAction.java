package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Quit command. Save all projects, close all windows.
 * 
 * @author Davin McCall
 * @version $Id: QuitAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class QuitAction extends PkgMgrAction {
    
    static private QuitAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public QuitAction getInstance()
    {
        if(instance == null)
            instance = new QuitAction();
        return instance;
    }
    
    private QuitAction()
    {
        super("menu.package.quit");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall(); 
        pmf.wantToQuit();
    }
}
