package bluej.pkgmgr.target.role;

import javax.swing.JPopupMenu;

import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;

/**
 * A role object which a class target uses to delegate behaviour to.
 * StdClassRole is used to represent standard Java classes.
 *
 * @author Bruce Quig
 * @version $Id: StdClassRole.java 4746 2006-12-07 02:26:53Z davmac $
 */
public class StdClassRole extends ClassRole
{
    /**
     * Create the class role.
     */
    public StdClassRole()
    {
    }

    public String getRoleName()
    {
        return "ClassTarget";
    }
 
    /**
     * Generate a popup menu for this class role.
     *
     * @param   menu    the menu to add items to
     * @param   ct      the ClassTarget we are constructing the role for
     * @param   state   whether the target is COMPILED etc.
     * @return  true if we added any menu tiems, false otherwise
     */
    public boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, Class cl, int state)
    {
        return false;
    }


    /**
     * Adds role specific items at the bottom of the popup menu for this class target.
     *
     * @param menu the menu object to add to
     * @param ct ClassTarget object associated with this class role
     * @param state the state of the ClassTarget
     *
     * @return true if any menu items have been added
     */
    public boolean createRoleMenuEnd(JPopupMenu menu, ClassTarget ct, int state)
    {
        if(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS)) {
            if (ct.getAssociation() == null) {
                menu.addSeparator();
                addMenuItem(menu, ct.new CreateTestAction(), true);
            }
        }
        return true;
    }
}
