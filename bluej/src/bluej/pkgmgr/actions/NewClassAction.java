package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New Class" command. Create a new class in the package, allow the user
 * to enter the name of the class and specify its type (abstract, interface,
 * applet, etc)
 * 
 * @author Davin McCall
 * @version $Id: NewClassAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class NewClassAction extends PkgMgrAction
{
    public NewClassAction()
    {
        super("menu.edit.newClass");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.newClass"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doCreateNewClass();
    }
}
