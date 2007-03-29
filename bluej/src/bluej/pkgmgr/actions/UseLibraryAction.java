package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Use library class" command. Allow the user to instantiate an object
 * from the standard library onto the object bench.
 * 
 * @author Davin McCall
 * @version $Id: UseLibraryAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
public class UseLibraryAction extends PkgMgrAction
{
    public UseLibraryAction()
    {
        super("menu.tools.callLibrary");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.callLibraryClass();
    }
}
