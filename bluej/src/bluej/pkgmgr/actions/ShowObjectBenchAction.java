package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User wants to see the object bench. Show it.
 */

final public class ShowObjectBenchAction extends PkgMgrAction {
    
    static private ShowObjectBenchAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowObjectBenchAction getInstance()
    {
        if(instance == null)
            instance = new ShowObjectBenchAction();
        return instance;
    }
    
    private ShowObjectBenchAction()
    {
        super("menu.view.showObjectBench");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
//        pmf.menuCall();
//        pmf.doOpen();
    }
}
