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
 * @version $Id: StdClassRole.java 1521 2002-11-27 13:22:48Z mik $
 */
public class StdClassRole extends ClassRole
{
    /**
     * Create the class role.
     */
    public StdClassRole()
    {
    }

    public void save(Properties props, int modifiers, String prefix)
    {
        super.save(props, modifiers, prefix);
        props.put(prefix + ".type", "ClassTarget");
    }

    /**
     * Load existing information about this class role.
     *
     * @param props  the properties object to read
     * @param prefix an internal name used for this target to identify
     *               its properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix) throws NumberFormatException
    {
        // no implementation needed as yet
    }

    /**
     * Generate a popup menu for this AppletClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected void createMenu(JPopupMenu menu, ClassTarget ct, int state)
    {
        // no implementation at present
    }


    // overloads method in Target super class
    public void draw(Graphics2D g, ClassTarget ct, int x, int y, int width, int height)
    {
        // no implementation as yet
    }
}
