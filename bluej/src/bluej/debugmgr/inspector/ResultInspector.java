package bluej.debugmgr.inspector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

/**
 * A window that displays a method return value.
 *
 * @author Poul Henriksen
 */
public class ResultInspector extends Inspector implements InspectorListener {

    // === static variables ===

    protected final static String resultTitle =
        Config.getString("debugger.inspector.result.title");
   

   /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * This is the only way to get access to viewers - they cannot be
     * directly created.
     *
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if the name is unobtainable
     * @param  pkg         The package all this belongs to
     * @param  ir          the InvokerRecord explaining how we got this result/object
     *                     if null, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     * @return             The Viewer value
     */
    public static ResultInspector getInstance(DebuggerObject obj,
                                                String name, Package pkg,
                                                InvokerRecord ir, JFrame parent)
    {
        ResultInspector inspector = (ResultInspector) inspectors.get(obj);

        if (inspector == null) {
            inspector = new ResultInspector(obj, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }
        inspector.update();

        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }


    // === instance variables ===

    protected DebuggerObject obj;
    protected String objName;           // a String for display that contains this objects
                                        // name on the object bench
                                            
    /**
     *  Constructor
     *  Note: private -- Objectviewers can only be created with the static
     *  "getViewer" method. 'pkg' may be null if 'ir' is null.
     *
     * @param  isResult    false is this is an inspection, true for result
     *                     displays
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if the name is unobtainable
     * @param  pkg         The package all this belongs to
     * @param  ir          the InvokerRecord explaining how we created this result/object
     *                     if null, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     */
    private ResultInspector(DebuggerObject obj, String name,
                            Package pkg, InvokerRecord ir, JFrame parent)
    {
        super(pkg, ir);
        String className = JavaNames.stripPrefix(obj.getClassName());
        
        setTitle(resultTitle);        
        setBorder(BlueJTheme.generalBorderWithStatusBar);
        
        this.obj = obj;
        this.objName = name;        
        
        makeFrame(true, true);
      	DialogManager.centreWindow(this, parent);
    }
    

    /**
     * True if this inspector is used to display a method call result.
     */
    protected Object[] getListData()
    {
        DebuggerObject realValue = obj.getFieldObject(0);
                
        String fieldString = JavaNames.stripPrefix(realValue.getClassName())
        + " " + obj.getInstanceFieldName(0)
        + " = " + DebuggerObject.OBJECT_REFERENCE;
        
        return new Object[] {fieldString};        
    }

    
    /**
     * Build the GUI
     *
     * @param  parent        The parent frame
     * @param  isResult      Indicates if this is a result window or an inspector window
     * @param  isObject      Indicates if this is a object inspector window
     * @param  showAssert    Indicates if assertions should be shown.
     */
    protected void makeFrame(boolean isObject, boolean showAssert)
    {   
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

     
        JScrollPane scrollPane = createFieldListScrollPane();  
        mainPanel.add(scrollPane, BorderLayout.CENTER);  

        JPanel inspectAndGetButtons = createInspectAndGetButtons();
        mainPanel.add(inspectAndGetButtons, BorderLayout.EAST);

        
        
        // create bottom button pane with "Close" button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
               
        JPanel buttonPanel;        
        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);       
        JButton button = createCloseButton();
        buttonPanel.add(button,BorderLayout.EAST);
        
        bottomPanel.add(buttonPanel);                    

        if (showAssert && pkg.getProject().inTestMode()) {          
            assertPanel = new AssertPanel();
            {
                assertPanel.setAlignmentX(LEFT_ALIGNMENT);
                bottomPanel.add(assertPanel);
            }
        }   
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        
      

        //TODO move to superclass
        //because the class inspector header needs to draw a line
        //from left to right border we can't have an empty border
        //Instead we put these borders on the sub-components        
        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        inspectAndGetButtons.setBorder(new EmptyBorder(0, 0, 0, insets.right));
        fieldList.setBorder(BorderFactory.createEmptyBorder(0,insets.left,0,0));        
        
        getRootPane().setDefaultButton(button);
        pack();
    }
    
    
    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
       
        if (obj.instanceFieldIsObject(slot)) {
            String newInspectedName;
            
            if (objName != null ) {
                newInspectedName = objName + "." + obj.getInstanceFieldName(slot);
            }            
            else {
                newInspectedName = obj.getInstanceFieldName(slot);
            }
            
            setCurrentObj(obj.getInstanceFieldObject(slot),
                          newInspectedName);

            if (obj.instanceFieldIsPublic(slot)) {
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
        ClassInspector insp =
           ClassInspector.getInstance(obj.getClassRef(), pkg, this);
    }

    /**
     * We are about to inspect an object - prepare.
     */
    protected void prepareInspection()
    {
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        inspectors.remove(obj);
    }  

   
    /**
     *  return a String with the result.
     *
     * @return    The Result value
     */
    public String getResult()  {       
            return (String) obj.getInstanceFields(false).get(0);      
    }
 
    public void inspectEvent(InspectorEvent e)
    {
        getInstance(e.getDebuggerObject(), null, pkg, null, this);
    }
    
    protected int getPreferredRows() {
        return 2;
    }
    
    protected void initInspectors(JTabbedPane inspTabs) {         
    }
}
