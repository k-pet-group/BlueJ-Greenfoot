package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of "extends" relationships in the graph window.
 * 
 * @author Davin McCall
 * @version $Id: ShowInheritsAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class ShowInheritsAction extends PkgMgrAction
{
    public ShowInheritsAction()
    {
        super("menu.view.showInherits");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.updateShowExtendsInPackage();
    }
}
