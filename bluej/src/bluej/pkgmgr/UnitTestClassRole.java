package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.utility.BlueJFileReader;
import bluej.utility.FileUtility;
import bluej.prefmgr.PrefMgr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import junit.swingui.TestRunner;
import junit.framework.*;

/**
 * An JUnit test class role in a package, i.e. a target that is a JUnit class file
 * built from Java source code.
 *
 * @author  Andrew Patterson based on AppletClassRole
 * @version $Id: UnitTestClassRole.java 1125 2002-02-07 02:02:29Z ajp $
 */
public class UnitTestClassRole extends ClassRole
{
    static final String runAppletStr = Config.getString("pkgmgr.classmenu.runApplet");

    /**
     * Create the class role.
     */
    public UnitTestClassRole()
    {
    }

    /**
     * Save this TestClassRole details to file
     *
     * @param props the properties object that stores target information
     * @param prefix prefix for this target for identification
     */
    public void save(Properties props, int modifiers, String prefix)
    {
        super.save(props, modifiers, prefix);
        props.put(prefix + ".type", "UnitTestTarget");

//        props.put(prefix + ".appletWidth", String.valueOf(appletWidth));
    }

    /**
     * Load existing information about this test class role
     *
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify
     *               its properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix) throws NumberFormatException
    {
//        value = props.getProperty(prefix + ".appletHeight");
//        if(value != null)
//            appletHeight = Integer.parseInt(value);

    }


    /**
     * Generate a popup menu for this TestClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected void createMenu(JPopupMenu menu, ClassTarget ct, int state)
    {
        // add run applet option
        ct.addMenuItem(menu, runAppletStr, (state == Target.S_NORMAL));
        menu.addSeparator();
    }

    /**
     *  modified from ActionListener interface
     *
     */
    public void actionPerformed(ActionEvent e, ClassTarget ct)
    {
        String cmd = e.getActionCommand();

        if(runAppletStr.equals(cmd)) {
            ct.getPackage().getEditor().raiseRunTargetEvent(ct);

        }
    }

    public void draw(Graphics2D g, ClassTarget ct, int x, int y, int width,
                     int height)
    {
        if(!PrefMgr.isUML()) {
            g.setColor(ct.getTextColour());
            Utility.drawCentredText(g, "test",
                                Target.TEXT_BORDER,
                                (Target.TEXT_HEIGHT + Target.TEXT_BORDER),
                                width - (2 * Target.TEXT_BORDER),
                                height - (Target.TEXT_BORDER +Target.TEXT_HEIGHT));
        }
    }
}
