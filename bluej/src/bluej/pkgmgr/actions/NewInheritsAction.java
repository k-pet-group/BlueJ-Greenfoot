package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New 'inherits' relationship" command. User can select two classes to
 * create an inheritance relationship between them. The relationship is also
 * inserted into the code ("class A extends B"...).
 * 
 * @author Davin McCall
 * @version $Id: NewInheritsAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class NewInheritsAction extends PkgMgrAction
{
    public NewInheritsAction()
    {
        super("menu.edit.newInherits");
        putValue(SMALL_ICON, Config.getImageAsIcon("image.build.extends"));
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.newExtends"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.clearStatus();
        pmf.doNewInherits();
    }
}
