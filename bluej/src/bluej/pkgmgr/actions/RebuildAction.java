package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Rebuild package" command. Re-compiles all classes regardless of whether
 * they need it or not (!).
 * 
 * @author Davin McCall
 * @version $Id: RebuildAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class RebuildAction extends PkgMgrAction {
    
    static private RebuildAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public RebuildAction getInstance()
    {
        if(instance == null)
            instance = new RebuildAction();
        return instance;
    }
    
    private RebuildAction()
    {
        super("menu.tools.rebuild");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getPackage().rebuild();
    }
}
