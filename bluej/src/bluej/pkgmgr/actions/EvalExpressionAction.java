package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Evaluate Expression" command. The user can type in a Java expression,
 * which is then evaluated and the result inspected.
 * 
 * @author Davin McCall
 * @version $Id: EvalExpressionAction.java 2571 2004-06-03 13:35:37Z fisker $
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
        super("menu.tools.callFreeForm");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.callFreeForm();
    }
}
