package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Evaluate Expression" command. The user can type in a Java expression,
 * which is then evaluated and the result inspected.
 * 
 * @author Davin McCall
 * @version $Id: EvalExpressionAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class EvalExpressionAction extends PkgMgrAction {
    
    static private EvalExpressionAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public EvalExpressionAction getInstance()
    {
        if(instance == null)
            instance = new EvalExpressionAction();
        return instance;
    }
    
    private EvalExpressionAction()
    {
        super("menu.tools.callFreeForm", KeyEvent.VK_E);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.callFreeForm();
    }
}
