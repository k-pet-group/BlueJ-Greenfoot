/*
 * Created on 19/04/2004
 */
package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "close project". Save & close the current project. If the command
 * was issued from a menu, always keep the last window open, otherwise close
 * the window regardless.
 * 
 * @author Davin McCall
 * @version $Id: CloseProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class CloseProjectAction extends PkgMgrAction {

    static private CloseProjectAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CloseProjectAction getInstance()
    {
        if(instance == null)
            instance = new CloseProjectAction();
        return instance;
    }
    
    private CloseProjectAction()
    {
        super("menu.package.close", KeyEvent.VK_W);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doClose(true);
    }
}
