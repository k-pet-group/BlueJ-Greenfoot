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
 * @version $Id: StdClassRole.java 1538 2002-11-29 13:43:32Z ajp $
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
     * Generate a popup menu for this AppletClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, int state)
    {
        return false;
    }
}
