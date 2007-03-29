package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New Package" command. Allows the user to create a new sub-package with a
 * specified name.
 * 
 * @author Davin McCall
 * @version $Id: NewPackageAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class NewPackageAction extends PkgMgrAction
{
    public NewPackageAction()
    {
        super("menu.edit.newPackage");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doCreateNewPackage();
    }
}
