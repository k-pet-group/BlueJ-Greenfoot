package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Remove (class/relation/package)" command. Remove an item from the
 * graph. If it's a class/package, prompt before removal. For a dependency
 * relation, modify the source to reflect the change.
 * 
 * @author Davin McCall.
 * @version $Id: RemoveAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class RemoveAction extends PkgMgrAction {
    
    static private RemoveAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public RemoveAction getInstance()
    {
        if(instance == null)
            instance = new RemoveAction();
        return instance;
    }
    
    private RemoveAction()
    {
        super("menu.edit.remove");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doRemove();
    }
}
