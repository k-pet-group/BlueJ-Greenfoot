/*
 * Created on 19/04/2004
 */
package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Print command. Allow user to print any of the class diagram, source code,
 * and project "README".
 * 
 * @author Davin McCall
 * @version $Id: PrintAction.java 2571 2004-06-03 13:35:37Z fisker $
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
        super("menu.package.print");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doPrint();
    }
}
