package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New Class" command. Create a new class in the package, allow the user
 * to enter the name of the class and specify its type (abstract, interface,
 * applet, etc)
 * 
 * @author Davin McCall
 * @version $Id: NewClassAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class NewClassAction extends PkgMgrAction {
    
    static private NewClassAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public NewClassAction getInstance()
    {
        if(instance == null)
            instance = new NewClassAction();
        return instance;
    }
    
    private NewClassAction()
    {
        super("menu.edit.newClass", KeyEvent.VK_N);
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.newClass"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doCreateNewClass();
    }
}
