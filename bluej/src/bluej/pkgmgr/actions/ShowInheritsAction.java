package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "extends" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowInheritsAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.view.showInherits", KeyEvent.VK_I);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowExtendsInPackage();
    }
}
