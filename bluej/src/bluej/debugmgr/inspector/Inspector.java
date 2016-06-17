/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2014,2015,2016  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugmgr.inspector;

import javax.swing.SwingUtilities;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.GetInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.DialogManager;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * 
 * A window that displays the fields in an object or class. This class is
 * subclassed for objects, classes and method results separately
 * (ObjectInspector, ClassInspector, ResultInspector).
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Bruce Quig
 */
@OnThread(Tag.FXPlatform)
public abstract class Inspector extends Stage
{
    // === static variables ===

    protected final static String showClassLabel = Config.getString("debugger.inspector.showClass");
    protected final static String inspectLabel = Config.getString("debugger.inspector.inspect");
    protected final static String getLabel = Config.getString("debugger.inspector.get");
    protected final static String close = Config.getString("close");
 
    // === instance variables ===
    
    protected FieldList fieldList = null;

    protected Button inspectButton;
    protected Button getButton;
    protected AssertPanel assertPanel;

    protected DebuggerObject selectedField; // the object currently selected in
                                            // the list
    protected String selectedFieldName; // the name of the field of the
                                        // currently selected object
    protected String selectedFieldType;
    protected InvokerRecord selectedInvokerRecord; // an InvokerRecord for the
                                                   // selected object (if possible, else null)

    protected final Package pkg;
    protected final InspectorManager inspectorManager;
    protected final InvokerRecord ir;
    private double initialClickX;
    private double initialClickY;
    
    // Each inspector is uniquely numbered in a session, for the purposes
    // of data collection:
    private static AtomicInteger nextUniqueId = new AtomicInteger(1);
    private final int uniqueId;

    //The width of the list of fields
    private static final int MIN_LIST_WIDTH = 150;
    private static final int MAX_LIST_WIDTH = 400;

    /**
     * Convert a field to a string representation, used to display the field in the inspector value list.
     */
    @OnThread(Tag.Any)
    public static String fieldToString(DebuggerField field)
    {
        int mods = field.getModifiers();
        String result = "";
        if (Modifier.isPrivate(mods)) {
            result = "private ";
        }
        else if (Modifier.isPublic(mods)) {
            result = "public ";
        }
        else if (Modifier.isProtected(mods)) {
            result = "protected ";
        }
        
        if (field.isHidden()) {
            result += "(hidden) ";
        }
        
        result += field.getType().toString(true);
        result += " " + field.getName();
        return result;
    }
    
    /**
     * Constructor.
     * 
     * @param pkg
     *            the package this inspector belongs to (or null)
     * @param ir
     *            the InvokerRecord for this inspector (or null)
     */
    protected Inspector(InspectorManager inspectorManager, Package pkg, InvokerRecord ir)
    {
        if(inspectorManager == null) {
            throw new NullPointerException("An inspector must have an InspectorManager.");
        }

        if (pkg == null && ir != null) {
            // Get button cannot be enabled when pkg==null
            ir = null;
        }
        this.inspectorManager = inspectorManager;
        this.pkg = pkg;
        this.ir = ir;
        this.uniqueId = nextUniqueId.incrementAndGet();

        // We want to be able to veto a close
        setOnCloseRequest(e -> { e.consume(); doClose(true); });
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                doClose(true);
        });

        initFieldList();
    }
    
    /**
     * Initializes the list of fields. This creates the component that shows the
     * fields.
     * @param valueFieldColor 
     */
    private void initFieldList()
    {
        fieldList = new FieldList(MAX_LIST_WIDTH);
        JavaFXUtil.addChangeListener(fieldList.getSelectionModel().selectedIndexProperty(), index -> listElementSelected(index.intValue()));
        
        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        fieldList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY)
            {
                doInspect();
            }
        });
        
        /*TODO
        //to make it possible to close dialogs with the keyboard (ENTER or ESCAPE), we
        // grab the key event from the fieldlist. 
        fieldList.addKeyListener(new KeyListener() {            
            public void keyPressed(KeyEvent e)
            {
            }

            public void keyReleased(KeyEvent e)
            {
            }

            public void keyTyped(KeyEvent e)
            {
                // Enter or escape?
                if (e.getKeyChar() == '\n' || e.getKeyChar() == 27) {
                    // On MacOS, we'll never see escape here. We have set up an
                    // action on the root pane which will handle it instead.
                    doClose(true);
                    e.consume();
                }    
            }
        });   
             */
    }

    protected boolean isGetEnabled()
    {
        return ir != null;
    }

    /**
     * De-iconify the window (if necessary) and bring it to the front.
     */
    public void bringToFront()
    {
        setIconified(false); // de-iconify
        toFront(); // window to front
    }

    // --- abstract interface to be implemented by subclasses ---

    /**
     * Returns the list of data.
     */
    abstract protected List<FieldInfo> getListData();

    /**
     * An element in the field list was selected.
     */
    abstract protected void listElementSelected(int slot);

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
     */
    public void update()
    {
        final List<FieldInfo> listData = getListData();
        
        fieldList.setData(listData);
        //fieldList.setTableHeader(null);
        
        // Ensures that an element (if any exist) is always selected
        if (fieldList.getSelectionModel().getSelectedIndex() == -1 && listData.size() > 0) {
            fieldList.getSelectionModel().select(0);
        }
                
        // if (assertPanel != null) {
        //    assertPanel.updateWithResultData((String) listData[0]);
        // }
        
        int slot = fieldList.getSelectionModel().getSelectedIndex();
        
        // occurs if valueChanged picked up a clearSelection event from
        // the list
        if (slot != -1) {
            listElementSelected(slot);
        }
    }

    /**
     * Store the object currently selected in the list.
     * 
     * @param object
     *            The new CurrentObj value
     * @param name
     *            The name of the selected field
     * @param type
     *            The type of the selected field
     */
    protected void setCurrentObj(DebuggerObject object, String name, String type)
    {
        selectedField = object;
        selectedFieldName = name;
        selectedFieldType = type;
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
        inspectButton.setDisable(!inspect);
        getButton.setDisable(!(get && isGetEnabled()));
    }

    /**
     * The "Inspect" button was pressed. Inspect the selected object.
     */
    protected void doInspect()
    {
        if (selectedField != null) {
            boolean isPublic = !getButton.isDisable();
            
            InvokerRecord newIr = new ObjectInspectInvokerRecord(selectedFieldName, ir);
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? newIr : null, this);
        }
    }

    /**
     * The "Get" button was pressed. Get the selected object on the object
     * bench.
     */
    protected void doGet()
    {
        if (selectedField != null) {
            GetInvokerRecord getIr = new GetInvokerRecord(selectedFieldType, selectedFieldName, ir);
            DebuggerObject selField = this.selectedField;
            SwingUtilities.invokeLater(() -> {
                PackageEditor pkgEd = pkg.getEditor();
                pkgEd.recordInteraction(getIr);
                pkgEd.raisePutOnBenchEvent(this, selField, selField.getGenType(), getIr);
            });
        }
    }

    /**
     * Close this inspector. The caller should remove it from the list of open
     * inspectors.
     * 
     * @param handleAssertions   Whether assertions should be attached to the
     *                           invoker record. If true, the user may be prompted
     *                           to fill in assertion data. 
     */
    public void doClose(boolean handleAssertions)
    {
        boolean closeOk = true;

        if (handleAssertions) {
            // handleAssertions may veto the close
            closeOk = handleAssertions();
        }

        if (closeOk) {
            hide();
            remove();
        }
    }

    protected boolean handleAssertions()
    {
        if (assertPanel != null && assertPanel.isAssertEnabled()) {
            
            if (! assertPanel.isAssertComplete()) {
                int choice = DialogManager.askQuestionFX(this, "empty-assertion-text");
                
                if (choice == 0) {
                    return false;
                }
            }
            
            ir.addAssertion(assertPanel.getAssertStatement());

            assertPanel.recordAssertion(pkg, () -> Optional.ofNullable(PkgMgrFrame.findFrame(pkg)).map(PkgMgrFrame::getTestIdentifier), ir.getUniqueIdentifier());
        }
        return true;
    }

    protected Button createCloseButton()
    {
        Button button = new Button(close);
        button.setOnAction(e -> doClose(true));
        return button;
    }

    /**
     * Creates a panel with an inspect button and a get button
     * 
     * @return A panel with two buttons
     */
    protected Node createInspectAndGetButtons()
    {
        // Create panel with "inspect" and "get" buttons
        Pane buttonPanel = new VBox();
        inspectButton = new Button(inspectLabel);
        inspectButton.setOnAction(e -> doInspect());
        inspectButton.setDisable(true);
        buttonPanel.getChildren().add(inspectButton);

        getButton = new Button(getLabel);
        getButton.setDisable(true);
        getButton.setOnAction(e -> doGet());
        buttonPanel.getChildren().add(getButton);

        return buttonPanel;
    }
    
    // Allow movement of the window by dragging
    // Adapted from: http://www.stupidjavatricks.com/?p=4
    // (with improvements).
    protected void installListenersForMoveDrag()
    {
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { initialClickX = e.getScreenX(); initialClickY = e.getScreenY(); });
        //addEventHandler(MouseEvent.MOUSE_RELEASED,e -> { initialClick = null;});
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            setX(e.getScreenX() + initialClickX);
            setY(e.getScreenY() + initialClickY);
        });
    }
    
    @OnThread(Tag.Any)
    public int getUniqueId()
    {
        return uniqueId;
    }
}
