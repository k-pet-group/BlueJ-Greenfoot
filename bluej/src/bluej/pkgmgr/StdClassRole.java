package bluej.pkgmgr;

import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;

/**
 * A role object which a class target uses to delegate behaviour to.
 * StdClassRole is used to represent standard Java classes.
 *
 * @author Bruce Quig
 * @version $Id: StdClassRole.java 1551 2002-12-02 05:58:45Z ajp $
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
