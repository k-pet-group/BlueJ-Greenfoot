package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Rebuild package" command. Re-compiles all classes regardless of whether
 * they need it or not (!).
 * 
 * @author Davin McCall
 * @version $Id: RebuildAction.java 4905 2007-03-29 06:06:30Z davmac $
 */

final public class RebuildAction extends PkgMgrAction
{
    public RebuildAction()
    {
        super("menu.tools.rebuild");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getPackage().rebuild();
    }
}
