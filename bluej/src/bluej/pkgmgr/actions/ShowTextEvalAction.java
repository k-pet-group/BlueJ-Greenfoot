package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User wants to see the text evaluation component. Show it.
 */

final public class ShowTextEvalAction extends PkgMgrAction {
    
    static private ShowTextEvalAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowTextEvalAction getInstance()
    {
        if(instance == null)
            instance = new ShowTextEvalAction();
        return instance;
    }
    
    private ShowTextEvalAction()
    {
        super("menu.view.showTextEval");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.showTextEvaluator();
    }
}
