package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of test results. This action provides a ButtonModel
 * which can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowTestResultsAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class ShowTestResultsAction extends PkgMgrAction
{
    
    static private ShowTestResultsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowTestResultsAction getInstance()
    {
        if(instance == null)
            instance = new ShowTestResultsAction();
        return instance;
    }
    
    private ShowTestResultsAction()
    {
        super("menu.view.showTestDisplay");
    }
    
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.testmgr.TestDisplayButtonModel();
    }
}
