package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Run all tests" action (test panel). Runs all the unit tests which have
 * been created in this project. Displays the results.
 * 
 * @author Davin McCall
 * @version $Id: RunTestsAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class RunTestsAction extends PkgMgrAction
{
    public RunTestsAction()
    {
        super("menu.tools.run");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.test"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.doTest();
    }
}
