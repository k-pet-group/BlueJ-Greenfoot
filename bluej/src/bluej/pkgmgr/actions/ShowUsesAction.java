package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "uses" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowUsesAction.java 2571 2004-06-03 13:35:37Z fisker $
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
        super("menu.view.showUses");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowUsesInPackage();
    }
}
