package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Add class from file" command. This allows the user to add into the current
 * project a class from another project or an external source.
 * 
 * @author Davin McCall
 * @version $Id: AddClassAction.java 4905 2007-03-29 06:06:30Z davmac $
 */

final public class AddClassAction extends PkgMgrAction
{
    public AddClassAction()
    {
        super("menu.edit.addClass");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doAddFromFile();
    }
}
