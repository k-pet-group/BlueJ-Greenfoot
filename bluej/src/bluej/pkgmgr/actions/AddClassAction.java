package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Add class from file" command. This allows the user to add into the current
 * project a class from another project or an external source.
 * 
 * @author Davin McCall
 * @version $Id: AddClassAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class AddClassAction extends PkgMgrAction {
    
    static private AddClassAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public AddClassAction getInstance()
    {
        if(instance == null)
            instance = new AddClassAction();
        return instance;
    }
    
    private AddClassAction()
    {
        super("menu.edit.addClass");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doAddFromFile();
    }
}
