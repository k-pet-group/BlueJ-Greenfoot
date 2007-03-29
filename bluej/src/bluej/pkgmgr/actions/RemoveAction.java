package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Remove (class/relation/package)" command. Remove an item from the
 * graph. If it's a class/package, prompt before removal. For a dependency
 * relation, modify the source to reflect the change.
 * 
 * @author Davin McCall.
 * @version $Id: RemoveAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class RemoveAction extends PkgMgrAction
{
    public RemoveAction()
    {
        super("menu.edit.remove");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doRemove();
    }
}
