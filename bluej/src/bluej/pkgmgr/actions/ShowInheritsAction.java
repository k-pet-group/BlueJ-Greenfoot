package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "extends" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowInheritsAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class ShowInheritsAction extends PkgMgrAction {
    
    static private ShowInheritsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowInheritsAction getInstance()
    {
        if(instance == null)
            instance = new ShowInheritsAction();
        return instance;
    }
    
    private ShowInheritsAction()
    {
        super("menu.view.showInherits");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowExtendsInPackage();
    }
}
