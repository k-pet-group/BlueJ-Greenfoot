package bluej.debugmgr.inspector;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

/**
 * A window that displays the static fields in an class.
 *
 * @author     Michael Kolling
 * @author     Poul Henriksen
 * @version    $Id: ClassInspector.java 2486 2004-04-06 08:11:09Z mik $
 */
public class ClassInspector extends Inspector
{
    // === static variables ===

    protected final static String inspectTitle =
        Config.getString("debugger.inspector.class.title");
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
    public static ClassInspector getInstance(DebuggerClass clss, 
            Package pkg, JFrame parent)
    {
        ClassInspector inspector = (ClassInspector) inspectors.get(clss.getName());

        if (inspector == null) {
            // XXX
            inspector = new ClassInspector(clss, pkg, null, parent);
            inspectors.put(clss.getName(), inspector);
        }
        inspector.update();

        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }

    /**
     *  Constructor
     *  Note: private -- ClassInspectors can only be created with the static
     *  "getInstance" method. 'pkg' may be null if getEnabled is false.
     *
     *@param  inspect     Description of Parameter
     *@param  obj         Description of Parameter
     *@param  pkg         Description of Parameter
     *@param  id          Description of Parameter
     *@param  getEnabled  Description of Parameter
     *@param  parent      Description of Parameter
     */
    private ClassInspector(DebuggerClass clss, Package pkg,
                           InvokerRecord ir, JFrame parent)
    {
        super(pkg, ir);
        setTitle(inspectTitle);

        myClass = clss;
        setBorder(BlueJTheme.shadowBorder);
        
        String className = JavaNames.stripPrefix(clss.getName());
        JComponent header = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Dimension size = this.getSize();
                g.drawLine(0,size.height-1, size.width, size.height-1);
            }            
        };
        header.setOpaque(false);
        header.add(new JLabel(classNameLabel + " " + className));
        setHeader(header);
        makeFrame(false, false);
       	DialogManager.tileWindow(this, parent);
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
     * Show the inspector for the class of an object.
     */
    protected void showClass()
    {
        // nothing to do here - this is the class already
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
        inspectors.remove(myClass.getName());
    }  

    /**
     * Intialise additional inspector panels.
     */
    protected void initInspectors(JTabbedPane inspTabs)
    {
        // not supported for class inspectors.
    }

    protected int getPreferredRows() {        
        return 8;
    }
}
