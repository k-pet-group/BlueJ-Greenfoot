package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New 'inherits' relationship" command. User can select two classes to
 * create an inheritance relationship between them. The relationship is also
 * inserted into the code ("class A extends B"...).
 * 
 * @author Davin McCall
 * @version $Id: NewInheritsAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class NewInheritsAction extends PkgMgrAction {
    
    static private NewInheritsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public NewInheritsAction getInstance()
    {
        if(instance == null)
            instance = new NewInheritsAction();
        return instance;
    }
    
    private NewInheritsAction()
    {
        super("menu.edit.newInherits");
        putValue(SMALL_ICON, Config.getImageAsIcon("image.build.extends"));
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.newExtends"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.clearStatus();
        pmf.doNewInherits();
    }
}
