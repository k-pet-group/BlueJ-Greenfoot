package bluej.debugger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

import bluej.Config;
import bluej.utility.JavaNames;
import bluej.pkgmgr.Package;

/**
 * A window that displays the static fields in an class.
 *
 * @author     Michael Kolling
 * @version    $Id: ClassInspector.java 1532 2002-11-29 10:28:51Z mik $
 */
public class ClassInspector extends Inspector
{
    // === static variables ===

    protected final static String inspectTitle =
        Config.getString("debugger.inspector.title");
    protected final static String staticListTitle =
        Config.getString("debugger.inspector.staticListTitle");
    protected final static String classNameLabel =
        Config.getString("debugger.inspector.classNameLabel");


    // === instance variables ===

    protected DebuggerClass myClass;

   /**
     *  Return a ClassInspector for a class. The inspector is visible.
     *  This is the only way to get access to viewers - they cannot be
     *  directly created.
     *
     * @param  clss        The class displayed by this viewer
     * @param  name        The name of this object or "null" if it is not on the
     *                     object bench
     * @param  pkg         The package all this belongs to
     * @param  getEnabled  if false, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     * @return             The Viewer value
     */
    public static ClassInspector getInstance(DebuggerClass clss, Package pkg,
            JFrame parent)
    {
        ClassInspector inspector = (ClassInspector) inspectors.get(clss);

        if (inspector == null) {
            String id = "#viewer" + count;  // # marks viewer for internal object
            count++;  //  which is not on bench

            inspector = new ClassInspector(clss, pkg, id, true, parent);
            inspectors.put(clss, inspector);
        }
        inspector.update();

        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }


    /**
     *  Constructor
     *  Note: private -- Objectviewers can only be created with the static
     *  "getViewer" method. 'pkg' may be null if getEnabled is false.
     *
     *@param  inspect     Description of Parameter
     *@param  obj         Description of Parameter
     *@param  pkg         Description of Parameter
     *@param  id          Description of Parameter
     *@param  getEnabled  Description of Parameter
     *@param  parent      Description of Parameter
     */
    private ClassInspector(DebuggerClass clss, Package pkg, String id, 
                           boolean getEnabled, JFrame parent)
    {
        super(pkg, id, getEnabled, parent,
              classNameLabel + " " + JavaNames.stripPrefix(clss.getName()), false);
        setTitle(inspectTitle);

        myClass = clss;
    }

    /**
     * Return the header title of the list in this inspector.
     */
    protected String getListTitle()
    {
        return staticListTitle;
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected boolean showingResult()
    {
        return false;
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected Object[] getListData()
    {
        return myClass.getStaticFields(true).toArray(new Object[0]);
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        if (myClass.staticFieldIsObject(slot)) {
            setCurrentObj(myClass.getStaticFieldObject(slot),
                          myClass.getStaticFieldName(slot));

            if (myClass.staticFieldIsPublic(slot)) {
                setButtonsEnabled(true, true);
            } else {
                setButtonsEnabled(true, false);
            }
        } 
        else {
            setCurrentObj(null, null);
            setButtonsEnabled(false, false);
        }
    }

    /**
     * We are about to inspect an object - prepare.
     */
    protected void prepareInspection()
    {
        // nothing to do here
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        inspectors.remove(myClass);
    }  

    /**
     * Intialise additional inspector panels.
     */
    protected void initInspectors(JTabbedPane inspTabs)
    {
        // not supported for class inspectors.
    }
}
