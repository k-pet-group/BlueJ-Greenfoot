package bluej.debugmgr.inspector;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;

import bluej.*;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.*;
import bluej.utility.DialogManager;

/**
 * 
 * A window that displays the fields in an object or class. This class is subclassed
 * for objects and classes separately (ObjectInspector, ClassInspector).
 *
 * @author     Michael Kolling
 * @author     Poul Henriksen
 * @version    $Id: Inspector.java 2322 2003-11-11 11:14:16Z polle $
 */
public abstract class Inspector extends JFrame
    implements ListSelectionListener
{
    // === static variables ===

    protected static HashMap inspectors = new HashMap();

   
    protected final static String showClassLabel =
        Config.getString("debugger.inspector.showClass");
    protected final static String inspectLabel =
        Config.getString("debugger.inspector.inspect");
    protected final static String getLabel =
        Config.getString("debugger.inspector.get");
    protected final static String close =
        Config.getString("close");

    private static final Color selectionColor = Config.getItemColour("colour.inspector.list.selection");
    
    // === instance variables ===

    protected JList fieldList = null;

    protected JButton inspectButton;
    protected JButton getButton;
    protected AssertPanel assertPanel;

    protected DebuggerObject selectedObject;    // the object currently selected in the list
    protected String selectedObjectName;        // the name of the field of the
                                                // currently selected object
    protected InvokerRecord selectedInvokerRecord;  // an InvokerRecord for the selected
                                                    // object (if possible, else null)

    protected Package pkg;
    protected InvokerRecord ir;

  
    // The top component of the UI
	private JPanel header = new JPanel();

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

    /**
     *  Remove an Inspector from the pool of existing inspectors.
     */
    public static void removeInstance(Object key)
    {
        Inspector insp = (Inspector)inspectors.get(key);
        if(insp != null)
            insp.doClose();
    }



    // === instance methods ===

    /**
     *  Constructor.
     *
     * @param   pkg         the package this inspector belongs to (or null)
     * @param   ir          the InvokerRecord for this inspector (or null)
     */
    protected Inspector(Package pkg, InvokerRecord ir)
    {
        super();

        setIconImage(BlueJTheme.getIconImage());

        this.pkg = pkg;
        this.ir = ir;

        if (pkg == null && ir != null) {
            throw new IllegalArgumentException("Get button cannot be enabled when pkg==null");
        }
    }

    protected boolean isGetEnabled()
    {
        return ir != null;    
    }
    
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if(visible)
            fieldList.requestFocus();   // doesn't seem to work
                                        // requestFocus seems to work only of the
                                        // component is already visible
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
     * @return The number of rows
     */
    abstract protected int getPreferredRows();
    // --- end of abstract methods ---



    /**
     *  Update the field values shown in this viewer to show current
     *  object values.
     */
    public void update()
    {
        

        Object[] listData = getListData();
        fieldList.setListData(listData);

        int rows = listData.length + 2;
        
        if(rows > getPreferredRows()) {
            rows = getPreferredRows();
        }
        
        fieldList.setVisibleRowCount(rows);

        if (fieldList != null) {
            fieldList.revalidate();
        }

        //Ensures that an element is always seleceted (if there is any)       
        if(fieldList.isSelectionEmpty()) {
            try {
                fieldList.setSelectedIndex(0);
            } catch (IndexOutOfBoundsException e) {
                //the list is empty
            }            
        }
        
        // Ensure a minimum width for the lists: if list is narrower
        // than 200 pixels, set it to 200.

        double width = fieldList.getPreferredScrollableViewportSize().getWidth();
        if(width <= 200)
            fieldList.setFixedCellWidth(200);
        else
            fieldList.setFixedCellWidth(-1);

        pack();       

        repaint();

        if (assertPanel != null) {
            assertPanel.updateWithResultData((String) listData[0]);
        }
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
     * @param  object  The new CurrentObj value
     * @param  name    The new CurrentObj value
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
        if (isGetEnabled()) {
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

            InvokerRecord newIr =
                new ObjectInspectInvokerRecord("Math", selectedObjectName, ir);
                
            ObjectInspector.getInstance(selectedObject, selectedObjectName, 
                                        pkg, isPublic ? newIr:null, this);
        }
    }

    /**
     *  The "Get" button was pressed. Get the selected object on the
     *  object bench.
     */
    private void doGet()
    {
        if(selectedObject != null) {
            pkg.getEditor().raisePutOnBenchEvent(this, selectedObject, ir);
        }
    }

    /**
     * Close this viewer. Don't forget to remove it from the list of open
     * inspectors.
     */
    private void doClose()
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
	
	/**
	 * Sets the component that will be displayed in the top left.
	 * @param icon The icon.
	 */
	public void setHeader(JComponent newComponent) {       
        header.setLayout(new GridLayout(1,1));
		this.header.add(newComponent);	
	}
	
	public void setBorder(Border border) {
		((JPanel) getContentPane()).setBorder(border);
	}
	

    /**
     * Build the GUI interface.
     *
     * @param  parent        The parent frame
     * @param  isResult      Indicates if this is a result window or an inspector window
     * @param  isObject      Indicates if this is a object inspector window
     * @param  showAssert    Indicates if assertions should be shown.
     */
    protected void makeFrame(JFrame parent, boolean isObject, boolean showAssert)
    {
       
        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent E)
                {
                    doClose();
                }
            });
        
        
        ((JComponent) this.getContentPane()).setBackground(getBackground());
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        
        // the field list is either the fields of an object or class, the elements
        // of an array, or if we are viewing a result, the result of a method call
        fieldList = new JList(new DefaultListModel());
        fieldList.setCellRenderer(new FieldCellRenderer(100));
        fieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fieldList.addListSelectionListener(this);
        //fieldList.setBackground(new Color(230,230,230));
        fieldList.setBackground(this.getBackground());
        fieldList.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(fieldList);
        scrollPane.setBorder(null);
        fieldList.requestDefaultFocus();
        fieldList.setFixedCellHeight(25);
        // if we are inspecting, we need a header
        fieldList.setSelectionBackground(selectionColor);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);

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
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridLayout(0, 1));
        buttonPanel.setOpaque(false);
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
        buttonFramePanel.setOpaque(false);
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(buttonFramePanel, BorderLayout.EAST);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        // create bottom button pane with "Close" button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (showAssert && (this instanceof  ResultInspector) && pkg.getProject().inTestMode()) {          
            assertPanel = new AssertPanel();
            {
				assertPanel.setAlignmentX(LEFT_ALIGNMENT);
                bottomPanel.add(assertPanel);
            }
        }
        
        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);

        // if we are doing an inspection, we add a label at the bottom, left of the close-button
        if (!(this instanceof  ResultInspector)) {
            mainPanel.add(header, BorderLayout.NORTH);
            if(isObject) {                
                JButton classButton = new JButton(showClassLabel);
                classButton.addActionListener(new ActionListener() {
                         public void actionPerformed(ActionEvent e) { showClass(); }
                      });
                buttonPanel.add(classButton, BorderLayout.WEST);                
            }            
        }

        JButton button = new JButton(close);
        {
            buttonPanel.add(button,BorderLayout.EAST);
            button.addActionListener(new ActionListener() {
                 public void actionPerformed(ActionEvent e) { doClose(); }
              });
        }
        bottomPanel.add(buttonPanel);

        //because the class inspector header needs to draw a line
        //from left to right border we can't have an empty border
        //Instead we put these borders on the sub-components        
        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        buttonFramePanel.setBorder(new EmptyBorder(0, 0, 0, insets.right));
        fieldList.setBorder(BorderFactory.createEmptyBorder(0,insets.left,0,0));        
        
        getRootPane().setDefaultButton(button);
        ((JPanel) getContentPane()).add(bottomPanel, BorderLayout.SOUTH);

        if ((this instanceof  ResultInspector)) {
            DialogManager.centreWindow(this, parent);
        } else {
            DialogManager.tileWindow(this, parent);
        }
    }
    
    /**
     * Renderer to display a field. The field is split into two parts:<br>
     *  The first contains the type and modifiers of the field.<br> 
     *  The second contains the value of the field.
     *
     * @author Poul Henriksen
     */
    private static class FieldCellRenderer
    extends JComponent
    implements ListCellRenderer {
        final static private ImageIcon objectrefIcon = Config.getImageAsIcon("image.inspector.objectref");
        
        final private JLabel descriptionLabel = new JLabel();
        final private JLabel valueLabel;
        
        final private JComponent valueContainer = new JPanel();
        
        /**
         * Creates new rendere to display fields of an object
         * @param valueFieldWidth The width of value field
         */
        public FieldCellRenderer(final int valueFieldWidth) {
            this.setLayout(new BorderLayout());
            
            valueLabel = new JLabel() {
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    size.width = valueFieldWidth;
                    return size;
                }  
            };
            
            descriptionLabel.setOpaque(true);
            valueContainer.setOpaque(true);
            valueLabel.setOpaque(true);
            setOpaque(true);
            
            valueLabel.setBackground(Color.WHITE);
            valueLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            valueLabel.setHorizontalAlignment(JLabel.CENTER);
            
            valueContainer.add(valueLabel);
            this.add(descriptionLabel,BorderLayout.CENTER);
            this.add(valueContainer, BorderLayout.EAST);
        }
        
       public Component getListCellRendererComponent(
                JList list,
				Object value,				
				int index, 
				boolean isSelected,
				boolean cellHasFocus)
        {
            String s = value.toString();
            
            //split on "="
            int delimiterIndex = s.indexOf('=');
            if (delimiterIndex >= 0) {
                String descriptionString = s.substring(0, delimiterIndex);
                String valueString = s.substring(delimiterIndex + 1);
                descriptionLabel.setText(descriptionString);
                if(valueString.equals(" <object reference>")) {
                    valueLabel.setText("");
                    valueLabel.setIcon(objectrefIcon);
                    this.setToolTipText(null);
                }else {              
                    valueLabel.setIcon(null);
                    valueLabel.setText(valueString);   
                    this.setToolTipText(valueString);                                            
                }
                valueLabel.setVisible(true);
            } else {
                //It was not a "normal" object. We just show the string.
                //It could be an array compression [...]
                descriptionLabel.setText(s);
                valueLabel.setVisible(false);
                this.setToolTipText(null);
            }
            
            if (isSelected) {
                valueContainer.setBackground(list.getSelectionBackground());
                descriptionLabel.setBackground(list.getSelectionBackground());              
            } else {
                valueContainer.setBackground(list.getBackground());
                descriptionLabel.setBackground(list.getBackground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            
            return this;
        }
    }
    
}
