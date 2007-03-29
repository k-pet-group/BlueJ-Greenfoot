package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Print command. Allow user to print any of the class diagram, source code,
 * and project "README".
 * 
 * @author Davin McCall
 * @version $Id: PrintAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class PrintAction extends PkgMgrAction
{
    public PrintAction()
    {
        super("menu.package.print");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPrint();
    }
}
