package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Cancel recording of a test method. Also removes from the bench objects which
 * were created since recording began.
 * 
 * @author Davin McCall
 * @version $Id: CancelTestRecordAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class CancelTestRecordAction extends PkgMgrAction
{
    static private CancelTestRecordAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CancelTestRecordAction getInstance()
    {
        if(instance == null)
            instance = new CancelTestRecordAction();
        return instance;
    }
    
    private CancelTestRecordAction()
    {
        super("cancel");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.test.cancel"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.doCancelTest();
    }
}
