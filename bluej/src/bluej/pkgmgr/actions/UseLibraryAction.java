package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Use library class" command. Allow the user to instantiate an object
 * from the standard library onto the object bench.
 * 
 * @author Davin McCall
 * @version $Id: UseLibraryAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.tools.callLibrary", KeyEvent.VK_L);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.callLibraryClass();
    }
}
