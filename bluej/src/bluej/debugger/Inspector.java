package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.utility.DialogManager;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.Border;

/**
 * A window that displays the fields in an object or classThis class is subclassed
 * for objects and classes separately (ObjectInspector, ClassInspector).
 *
 * @author     Michael Kolling
 * @version    $Id: Inspector.java 1528 2002-11-28 15:38:40Z mik $
 */
public abstract class Inspector extends JFrame
    implements ListSelectionListener
{
    // === static variables ===

    protected static int count = 0;
    protected static HashMap inspectors = new HashMap();

    protected final static Color bgColor = new Color(208, 212, 208);

    protected final static String inspectorDirectoryName = "+inspector";

    protected final static String inspectLabel =
        Config.getString("debugger.inspector.inspect");
    protected final static String getLabel =
        Config.getString("debugger.inspector.get");
    protected final static String close =
        Config.getString("close");


    // === instance variables ===

    protected JList fieldList = null;

    protected JButton inspectButton;
    protected JButton getButton;
    protected DebuggerObject selectedObject;    // the object currently selected in the list
    protected String selectedObjectName;
    protected Package pkg;
    protected String pkgScopeId;
    protected boolean getEnabled;
    protected boolean isInScope;

    // either a tabbed pane or null if there is only the standard inspector
    protected JTabbedPane inspectorTabs = null;

    protected String viewerId;    // a unique ID used to enter the
                                  // viewer's object into the package scope


    // === static methods ===


    /**
     *  Update all open inspectors to show up-to-date values.
     */
    public static void updateInspectors()
    {
        for (Iterator it = inspectors.values().iterator(); it.hasNext(); ) {
            Inspector inspector = (Inspector) it.next();
            inspector.update();
        }
    }


    // === instance methods ===

    /**
     *  Constructor.
     *
     *@param  inspect     Description of Parameter
     *@param  obj         Description of Parameter
     *@param  pkg         Description of Parameter
     *@param  id          Description of Parameter
     *@param  getEnabled  Description of Parameter
     *@param  parent      Description of Parameter
     */
    protected Inspector(Package pkg, String id, boolean getEnabled,
                       JFrame parent, String nameLabel, boolean isResult)
    {
        super();

        setIconImage(Config.frameImage);

        this.pkg = pkg;
        viewerId = id;
        this.getEnabled = getEnabled;
        isInScope = false;
        if (pkg == null) {
            if (getEnabled) {
                Debug.reportError("cannot enable 'get' with null package");
            }
            pkgScopeId = "";
        } else {
            pkgScopeId = pkg.getId();
        }

        makeFrame(parent, isResult, nameLabel);
    }

    /**
     *  Return this viewer's ID.
     *
     *@return    The Id value
     */
    public String getId()
    {
        return viewerId;
    }

    public void getEvent(InspectorEvent e)
    {
    }

    /**
     *  De-iconify the window (if necessary) and bring it to the front.
     */
    public void bringToFront()
    {
        setState(Frame.NORMAL);  // de-iconify
        toFront();  // window to front
    }

    // --- abstract interface to be implemented by subclasses ---
    
    /**
     * Return the header title of the list in this inspector.
     */
    abstract protected String getListTitle();
    
    /**
     * True if this inspector is used to display a method call result.
     */
    abstract protected boolean showingResult();
    
    /**
     * True if this inspector is used to display a method call result.
     */
    abstract protected Object[] getListData();

    /**
     * An element in the field list was selected.
     */
    abstract protected void listElementSelected(int slot);

    /**
     * We are about to inspect an object - prepare.
     */
    abstract protected void prepareInspection();

    /**
     * Remove this inspector.
     */
    abstract protected void remove();

    /**
     * Intialise additional inspector panels.
     */
    abstract protected void initInspectors(JTabbedPane inspTabs);

    // --- end of abstract methods ---


    /**
     *  Update the field values shown in this viewer to show current
     *  object values.
     */
    public void update()
    {
        int maxRows = 7;

        Object[] listData = getListData();
        fieldList.setListData(listData);

        int rows = listData.length + 2;
        if (showingResult()) {
            rows = 2;
        }
        else if (rows > maxRows) {
            rows = maxRows;
        }
        fieldList.setVisibleRowCount(rows);

        if (fieldList != null) {
            fieldList.revalidate();
        }

        // Ensure a minimum width for the lists: if list is narrower
        // than 200 pixels, set it to 200.

        double width = fieldList.getPreferredScrollableViewportSize().getWidth();
        if(width <= 200)
            fieldList.setFixedCellWidth(200);
        else
            fieldList.setFixedCellWidth(-1);

        pack();

        if (inspectorTabs != null) {
            for (int i = 1; i < inspectorTabs.getTabCount(); i++) {
                ((InspectorPanel) inspectorTabs.getComponentAt(i)).refresh();
            }
        }

        repaint();
    }


    // ----- ListSelectionListener interface -----

    /**
     *  The value of the list selection has changed. Update the selected
     *  object.
     *
     *@param  e  The event object describing the event
     */
    public void valueChanged(ListSelectionEvent e)
    {
        // ignore mouse down, dragging, etc.
        if (e.getValueIsAdjusting()) {
            return;
        }
        
        int slot = fieldList.getSelectedIndex();

        // occurs if valueChanged picked up a clearSelection event from
        // the list
        if (slot == -1) {
            return;
        }

        listElementSelected(slot);
    }

    // ----- end of ListSelectionListener interface -----


    /**
     *  Store the object currently selected in the list.
     *
     *@param  object  The new CurrentObj value
     *@param  name    The new CurrentObj value
     */
    protected void setCurrentObj(DebuggerObject object, String name)
    {
        selectedObject = object;
        selectedObjectName = name;
    }


    /**
     * Enable or disable the Inspect and Get buttons.
     *
     * @param  inspect  The new ButtonsEnabled value
     * @param  get      The new ButtonsEnabled value
     */
    protected void setButtonsEnabled(boolean inspect, boolean get)
    {
        inspectButton.setEnabled(inspect);
        if (getEnabled) {
            getButton.setEnabled(get);
        }
    }

    /**
     *  The "Inspect" button was pressed. Inspect the
     *  selected object.
     */
    private void doInspect()
    {
        prepareInspection();

        if (selectedObject != null) {
            boolean isPublic = getButton.isEnabled();
            ObjectInspector viewer = ObjectInspector.getInstance(false, selectedObject, 
                    null, pkg, isPublic, this);

            // If the newly opened object is public, enter it into the
            // package scope, so that we can perform "Get" operations on it.
            if (isPublic) {
                viewer.addToScope(viewerId, selectedObjectName);
            }
        }
    }

    /**
     *  The "Get" button was pressed. Get the selected object on the
     *  object bench.
     */
    private void doGet()
    {
        if(selectedObject != null) {
            pkg.getEditor().raisePutOnBenchEvent(selectedObject, viewerId,
                selectedObjectName);
        }
    }

    /**
     *@param  parentViewerId  The feature to be added to the ToScope attribute
     *@param  objectName      The feature to be added to the ToScope attribute
     */
    protected void addToScope(String parentViewerId, String objectName)
    {
        Debugger.debugger.addObjectToScope(pkgScopeId, parentViewerId,
                objectName, viewerId);
        isInScope = true;
    }


    /**
     *  Close this viewer. Don't forget to remove it from the list of open
     *  inspectors.
     */
    private void doClose()
    {
        setVisible(false);
        remove();
        dispose();

        // if the object shown here is not on the object bench, also
        // remove it from the package scope

        if (isInScope && (viewerId.charAt(0) == '#')) {
            Debugger.debugger.removeObjectFromScope(pkgScopeId, viewerId);
        }
    }

    /**
     *  Build the GUI interface.
     *
     *@param  parent        Description of Parameter
     *@param  isResult      Indicates if this is a result window or an inspector window
     *@param  obj           The debugger object we want to look at
     */
    private void makeFrame(JFrame parent, boolean isResult,
                            String nameLabel)
    {
        //	setFont(font);
        setBackground(bgColor);

        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent E)
                {
                    doClose();
                }
            });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(Config.generalBorder);

        // if we are doing an inspection, we add a label at the top
        if (!isResult) {
            JPanel titlePanel = new JPanel();
            titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            JLabel classNameLabel = new JLabel(nameLabel);
            titlePanel.add(classNameLabel, BorderLayout.CENTER);
            mainPanel.add(titlePanel, BorderLayout.NORTH);
        }

        // the field list is either the fields of an object or class, the elements
        // of an array, or if we are viewing a result, the result of a method call
        fieldList = new JList(new DefaultListModel());
        fieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fieldList.addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(fieldList);

        // if we are inspecting, we need a header
        if (!isResult) {
            scrollPane.setColumnHeaderView(new JLabel(getListTitle()));
        }

        mainPanel.add(scrollPane);

        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        MouseListener mouseListener =
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    // monitor for double clicks
                    if (e.getClickCount() == 2) {
                        doInspect();
                    }
                }
            };
        fieldList.addMouseListener(mouseListener);

        // Create panel with "inspect" and "get" buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1));

        inspectButton = new JButton(inspectLabel);
        inspectButton.addActionListener(new ActionListener() {
                         public void actionPerformed(ActionEvent e) { doInspect(); }
                      });
        inspectButton.setEnabled(false);
        buttonPanel.add(inspectButton);

        getButton = new JButton(getLabel);
        getButton.setEnabled(false);
        getButton.addActionListener(new ActionListener() {
                         public void actionPerformed(ActionEvent e) { doGet(); }
                      });
        buttonPanel.add(getButton);

        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(buttonFramePanel, BorderLayout.EAST);

        inspectorTabs = new JTabbedPane();
        initInspectors(inspectorTabs);

        // if we have any non-standard inspectors then we add a tabbed pane to
        // hold them, otherwise we just add the one panel
        if (inspectorTabs.getTabCount() > 0) {
            inspectorTabs.insertTab("Standard", null, mainPanel, "Standard", 0);
            ((JPanel) getContentPane()).add(inspectorTabs, BorderLayout.CENTER);
        } else {
            inspectorTabs = null;
            ((JPanel) getContentPane()).add(mainPanel, BorderLayout.CENTER);
        }

        // create bottom button pane with "Close" button

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton button = new JButton(close);
        buttonPanel.add(button);
        button.addActionListener(new ActionListener() {
                         public void actionPerformed(ActionEvent e) { doClose(); }
                      });
        getRootPane().setDefaultButton(button);
        ((JPanel) getContentPane()).add(buttonPanel, BorderLayout.SOUTH);

        if (isResult) {
            DialogManager.centreWindow(this, parent);
        } else {
            DialogManager.tileWindow(this, parent);
        }

        button.requestFocus();
    }
}
