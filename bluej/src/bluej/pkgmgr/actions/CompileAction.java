/*
 * Created on 19/04/2004
 */
package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Compile" command. Compiles all class files in the project which need to
 * be compiled.
 * 
 * @author Davin McCall
 * @version $Id: CompileAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class CompileAction extends PkgMgrAction {
    
    static private CompileAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CompileAction getInstance()
    {
        if(instance == null)
            instance = new CompileAction();
        return instance;
    }
    
    private CompileAction()
    {
        super("menu.tools.compile", KeyEvent.VK_K);
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.compile"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getPackage().compile();
    }
}
