package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Use library class" command. Allow the user to instantiate an object
 * from the standard library onto the object bench.
 * 
 * @author Davin McCall
 * @version $Id: UseLibraryAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
public class UseLibraryAction extends PkgMgrAction {
    
    static private UseLibraryAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public UseLibraryAction getInstance()
    {
        if(instance == null)
            instance = new UseLibraryAction();
        return instance;
    }
    
    private UseLibraryAction()
    {
        super("menu.tools.callLibrary");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.callLibraryClass();
    }
}
