package bluej.pkgmgr;

import javax.swing.*;

/**
 * A role object which a class target uses to delegate behaviour to.
 * StdClassRole is used to represent standard Java classes.
 *
 * @author Bruce Quig
 * @version $Id: StdClassRole.java 1819 2003-04-10 13:47:50Z fisker $
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
    protected boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, int state)
    {
        return false;
    }
}
