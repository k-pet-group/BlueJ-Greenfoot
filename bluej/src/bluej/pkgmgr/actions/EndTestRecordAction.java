package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * End a recording of a test method. Creates a new test case class and
 * compiles it. Removes objects from the bench which were created since
 * recording began.
 * 
 * @author Davin McCall
 * @version $Id: EndTestRecordAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class EndTestRecordAction extends PkgMgrAction
{
    static private EndTestRecordAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public EndTestRecordAction getInstance()
    {
        if(instance == null)
            instance = new EndTestRecordAction();
        return instance;
    }
    
    private EndTestRecordAction()
    {
        super("pkgmgr.test.end");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.test.end"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.doEndTest();
    }
}
