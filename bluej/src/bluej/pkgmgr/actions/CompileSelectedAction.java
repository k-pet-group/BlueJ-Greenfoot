package bluej.pkgmgr.actions;

import java.awt.Event;
import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Compile selected" command. Compiles the selected classes.
 * 
 * @author Davin McCall
 * @version $Id: CompileSelectedAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class CompileSelectedAction extends PkgMgrAction {
    
    static private CompileSelectedAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CompileSelectedAction getInstance()
    {
        if(instance == null)
            instance = new CompileSelectedAction();
        return instance;
    }
    
    private CompileSelectedAction()
    {
        super("menu.tools.compileSelected", KeyEvent.VK_K, SHORTCUT_MASK | Event.SHIFT_MASK);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.compileSelected();
    }
}
