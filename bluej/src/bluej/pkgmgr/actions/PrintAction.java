/*
 * Created on 19/04/2004
 */
package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Print command. Allow user to print any of the class diagram, source code,
 * and project "README".
 * 
 * @author Davin McCall
 * @version $Id: PrintAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class PrintAction extends PkgMgrAction {
    
    static private PrintAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public PrintAction getInstance()
    {
        if(instance == null)
            instance = new PrintAction();
        return instance;
    }
    
    private PrintAction()
    {
        super("menu.package.print", KeyEvent.VK_P);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPrint();
    }
}
