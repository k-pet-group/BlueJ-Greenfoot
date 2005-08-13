package bluej.debugmgr.inspector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;

/**
 * 
 * A window that displays the fields in an object or class. This class is
 * subclassed for objects, classes and method results separately
 * (ObjectInspector, ClassInspector, ResultInspector).
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Bruce Quig
 * @version $Id: Inspector.java 3519 2005-08-13 18:01:44Z polle $
 */
public abstract class Inspector extends JFrame
    implements ListSelectionListener
{
    // === static variables ===

    protected static HashMap inspectors = new HashMap();

    protected final static String showClassLabel = Config.getString("debugger.inspector.showClass");
    protected final static String inspectLabel = Config.getString("debugger.inspector.inspect");
    protected final static String getLabel = Config.getString("debugger.inspector.get");
    protected final static String close = Config.getString("close");

    // === instance variables ===

    protected FieldList fieldList = null;

    protected JButton inspectButton;
    protected JButton getButton;
    protected AssertPanel assertPanel;

    protected DebuggerObject selectedObject; // the object currently selected in
    // the list
    protected String selectedObjectName; // the name of the field of the
    // currently selected object
    protected InvokerRecord selectedInvokerRecord; // an InvokerRecord for the
    // selected
    // object (if possible, else null)

    protected Package pkg;
    protected Project project;
    protected InvokerRecord ir;

    //The maximum length of the description (modifiers + field-name)
    private final static int MAX_DESCRIPTION_LENGTH = 30;

    //The width of the list
    private static final int LIST_WIDTH = 200;

  
    /**
     * Update all open inspectors to show up-to-date values.
     */
    public static void updateInspectors()
    {
        for (Iterator it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.update();
        }
    }
  
    /**
     * Remove an Inspector from the pool of existing inspectors.
     */
    public static void removeInstance(Object key)
    {
        Inspector insp = (Inspector) inspectors.get(key);
        if (insp != null)
            insp.doClose();
    }
    
    /**
     * Remove all inspectors (for all projects).
     */
    public static void removeInspectors()
    {
        Iterator i = inspectors.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) i.next();
            Inspector inspector = (Inspector) mapEntry.getValue();
            inspector.doClose();
        }
    }

    /**
     * Constructor.
     * 
     * @param pkg
     *            the package this inspector belongs to (or null)
     * @param ir
     *            the InvokerRecord for this inspector (or null)
     */
    protected Inspector(Project proj, Package pkg, InvokerRecord ir)
    {
        super();

        setIconImage(BlueJTheme.getIconImage());

        if (pkg == null && ir != null) {
            // Get button cannot be enabled when pkg==null
            ir = null;
        }
        this.project = proj;
        this.pkg = pkg;
        this.ir = ir;

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                doClose();
            }
        });

        initFieldList();
    }

    /**
     * Initializes the list of fields. This creates the component that shows the
     * fields.
     */
    private void initFieldList()
    {
        fieldList = new FieldList(getMaxDescriptionLength());
        fieldList.setBackground(this.getBackground());
        fieldList.setOpaque(true);
        fieldList.setSelectionBackground(Config.getSelectionColour());
        fieldList.getSelectionModel().addListSelectionListener(this);
        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                // monitor for double clicks
                if (e.getClickCount() == 2) {
                    doInspect();
                }
            }
        };
        fieldList.addMouseListener(mouseListener);
        
        //to make it possible to close dialogs with the keyboard (ENTER or ESCAPE), we
        // grab the key event from the fieldlist. 
        fieldList.addKeyListener(new KeyListener() {            
            public void keyPressed(KeyEvent e)
            {                
            }

            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    doClose();
                }    
            }

            public void keyTyped(KeyEvent e)
            {
            }
        });        
    }

    protected boolean isGetEnabled()
    {
        return ir != null;
    }

    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible)
            fieldList.requestFocus(); // doesn't seem to work
        // requestFocus seems to work only of the
        // component is already visible
    }

    public void getEvent(InspectorEvent e)
    {}

    /**
     * De-iconify the window (if necessary) and bring it to the front.
     */
    public void bringToFront()
    {
        setState(Frame.NORMAL); // de-iconify
        toFront(); // window to front
    }

    // --- abstract interface to be implemented by subclasses ---

    /**
     * Returns the list of data.
     */
    abstract protected Object[] getListData();

    /**
     * An element in the field list was selected.
     */
    abstract protected void listElementSelected(int slot);

    /**
     * Show the inspector for the class of an object.
     */
    abstract protected void showClass();

    /**
     * We are about to inspect an object - prepare.
     */
    abstract protected void prepareInspection();

    /**
     * Remove this inspector.
     */
    abstract protected void remove();

    /**
     * Return the preferred number of rows that should be shown in the list
     * 
     * @return The number of rows
     */
    abstract protected int getPreferredRows();

    // --- end of abstract methods ---

    /**
     * Requests an update of the field values shown in this viewer to show current object
     * values.
     * 
     * Will be executed int the event dispatch thread. 
     */
    public void update()
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {                
                final Object[] listData = getListData();

                fieldList.setData(listData);
                fieldList.setTableHeader(null);

                // Ensures that an element is always seleceted (if there is any)
                if (fieldList.getSelectedRow() == -1 && listData.length > 0) {
                    fieldList.setRowSelectionInterval(0, 0);
                }

                double height = fieldList.getPreferredSize().getHeight();
                int rows = listData.length;
                if (rows > getPreferredRows()) {
                    height = fieldList.getRowHeight() * getPreferredRows();
                }

                int width = (int) fieldList.getPreferredSize().getWidth();
                if (width < LIST_WIDTH)
                    width = LIST_WIDTH;

                fieldList.setPreferredScrollableViewportSize(new Dimension(width, (int) height));
                pack();

                if (assertPanel != null) {
                    assertPanel.updateWithResultData((String) listData[0]);
                }
                
                int slot = fieldList.getSelectedRow();

                // occurs if valueChanged picked up a clearSelection event from
                // the list
                if (slot != -1) {
                    listElementSelected(slot);
                }

                repaint();
            }
        });
    }

    // ----- ListSelectionListener interface -----

    /**
     * The value of the list selection has changed. Update the selected object.
     * 
     * @param e
     *            The event object describing the event
     */
    public void valueChanged(ListSelectionEvent e)
    {
        // ignore mouse down, dragging, etc.
        if (e.getValueIsAdjusting()) {
            return;
        }

        int slot = fieldList.getSelectedRow();

        // occurs if valueChanged picked up a clearSelection event from
        // the list
        if (slot == -1) {
            return;
        }

        listElementSelected(slot);
    }

    // ----- end of ListSelectionListener interface -----

    /**
     * Store the object currently selected in the list.
     * 
     * @param object
     *            The new CurrentObj value
     * @param name
     *            The new CurrentObj value
     */
    protected void setCurrentObj(DebuggerObject object, String name)
    {
        selectedObject = object;
        selectedObjectName = name;
    }

    /**
     * Enable or disable the Inspect and Get buttons.
     * 
     * @param inspect
     *            The new ButtonsEnabled value
     * @param get
     *            The new ButtonsEnabled value
     */
    protected void setButtonsEnabled(boolean inspect, boolean get)
    {
        inspectButton.setEnabled(inspect);
        if (isGetEnabled()) {
            getButton.setEnabled(get);
        }
    }

    /**
     * The "Inspect" button was pressed. Inspect the selected object.
     */
    protected void doInspect()
    {
        prepareInspection();

        if (selectedObject != null) {
            boolean isPublic = getButton.isEnabled();

            InvokerRecord newIr = new ObjectInspectInvokerRecord("Math", selectedObjectName, ir);

            project.getInspectorInstance(selectedObject, selectedObjectName, pkg, isPublic ? newIr : null, this);
        }
    }

    /**
     * The "Get" button was pressed. Get the selected object on the object
     * bench.
     */
    private void doGet()
    {
        if (selectedObject != null) {
            pkg.getEditor().raisePutOnBenchEvent(this, selectedObject, ir);
        }
    }

    /**
     * Close this viewer. Don't forget to remove it from the list of open
     * inspectors.
     */
    public void doClose()
    {
        handleAssertions();

        setVisible(false);
        remove();
        dispose();
    }

    protected void handleAssertions()
    {
        if (assertPanel != null && assertPanel.isAssertEnabled()) {
            ir.addAssertion(assertPanel.getAssertStatement());
        }
    }

    public void setBorder(Border border)
    {
        ((JPanel) getContentPane()).setBorder(border);
    }

    public int getMaxDescriptionLength()
    {
        return MAX_DESCRIPTION_LENGTH;
    }

    public int getListWidth()
    {
        return LIST_WIDTH;
    }

    protected JButton createCloseButton()
    {
        JButton button = new JButton(close);
        {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    doClose();
                }
            });
        }

        return button;

    }

    /**
     * Creates a panel with an inspect button and a get button
     * 
     * @return A panel with two buttons
     */
    protected JPanel createInspectAndGetButtons()
    {
        // Create panel with "inspect" and "get" buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridLayout(0, 1));
        buttonPanel.setOpaque(false);
        inspectButton = new JButton(inspectLabel);
        inspectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doInspect();
            }
        });
        inspectButton.setEnabled(false);
        buttonPanel.add(inspectButton);

        getButton = new JButton(getLabel);
        getButton.setEnabled(false);
        getButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doGet();
            }
        });
        buttonPanel.add(getButton);

        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setOpaque(false);
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        return buttonFramePanel;
    }

    /**
     * Creates a ScrollPane for the fieldList
     */
    protected JScrollPane createFieldListScrollPane()
    {
        JScrollPane scrollPane = new JScrollPane(fieldList);
        scrollPane.setBorder(BlueJTheme.generalBorder);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        fieldList.setPreferredScrollableViewportSize(new Dimension(getListWidth(), 25));
        return scrollPane;
    }
}