package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "uses" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowUsesAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class ShowUsesAction extends PkgMgrAction {
    
    static private ShowUsesAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowUsesAction getInstance()
    {
        if(instance == null)
            instance = new ShowUsesAction();
        return instance;
    }
    
    private ShowUsesAction()
    {
        super("menu.view.showUses", KeyEvent.VK_U);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowUsesInPackage();
    }
}
