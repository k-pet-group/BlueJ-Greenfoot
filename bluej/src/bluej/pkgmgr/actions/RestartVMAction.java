package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Restart VM. Restarts the VM in which objects on the object bench live
 * (and in which programs running under BlueJ execute). This also removes
 * objects from the bench.
 * 
 * @author Davin McCall
 * @version $Id: RestartVMAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class RestartVMAction extends PkgMgrAction
{    
    static private RestartVMAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public RestartVMAction getInstance()
    {
        if(instance == null)
            instance = new RestartVMAction();
        return instance;
    }
    
    private RestartVMAction()
    {
        super("workIndicator.resetMachine");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.restartDebugger();
    }
}
