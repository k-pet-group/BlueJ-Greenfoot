/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2014,2015,2016,2017,2018,2019,2021  Michael Kolling and John Rosenberg
 
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

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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
import bluej.utility.javafx.JavaFXUtil;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
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
    private final ResizeListener resizeListener;

    /**
     * Convert a field to a string representation, used to display the field in the inspector value list.
     */
    @OnThread(Tag.FXPlatform)
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
    protected Inspector(InspectorManager inspectorManager, Package pkg, InvokerRecord ir, StageStyle stageStyle)
    {
        if(inspectorManager == null) {
            throw new NullPointerException("An inspector must have an InspectorManager.");
        }

        if (pkg == null && ir != null) {
            // Get button cannot be enabled when pkg==null
            ir = null;
        }
        JavaFXUtil.addSelfRemovingListener(sceneProperty(), Config::addInspectorStylesheets);
        initStyle(stageStyle);
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

        resizeListener = new ResizeListener(this);

        //setOnShown(e -> org.scenicview.ScenicView.show(getScene()));

        // If appropriate (object/class inspector in Greenfoot), update the
        // inspector content every second while the window is showing:
        final Timeline autoUpdate = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            update();
        }));
        autoUpdate.setCycleCount(Timeline.INDEFINITE);
        addEventHandler(WindowEvent.ANY, e -> {
            boolean shown = e.getEventType() == WindowEvent.WINDOW_SHOWN;
            boolean hidden = e.getEventType() == WindowEvent.WINDOW_HIDDEN;
            
            if (hidden)
            {
                autoUpdate.stop();
            }
            else if (shown && shouldAutoUpdate())
            {
                // Start updating:
                autoUpdate.playFromStart();
            }
        });
        
        initFieldList();
        
        
    }

    /**
     * Should we auto-update the inspector window every second while it is showing?
     * Currently true for class and object inspectors in Greenfoot only.
     */
    protected abstract boolean shouldAutoUpdate();

    /**
     * Initializes the list of fields. This creates the component that shows the
     * fields.
     */
    private void initFieldList()
    {
        fieldList = new FieldList();
        JavaFXUtil.addChangeListenerPlatform(fieldList.selectedIndexProperty(), index -> listElementSelected(index.intValue()));
        
        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        fieldList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY)
            {
                doInspect();
            }
        });
        
        // To make it possible to close dialogs with the keyboard (ENTER), we
        // grab the key event from the fieldlist which otherwise consumes it
        // as part of the edit action (even though it's not editable)
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // Enter or escape?
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER)
            {
                doClose(true);
                e.consume();
            }
            else if (e.getCode() == KeyCode.SPACE && fieldList.isFocused())
            {
                doInspect();
                e.consume();    
            }
            else if (e.getCode() == KeyCode.UP)
            {
                fieldList.requestFocus();
                fieldList.up();
                e.consume();
            }
            else if (e.getCode() == KeyCode.DOWN)
            {
                fieldList.requestFocus();
                fieldList.down();
                e.consume();
            }
        });
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

        int prevSelection = fieldList.selectedIndexProperty().get();
        
        fieldList.setData(listData);
        //fieldList.setTableHeader(null);

        // Ensures that an element (if any exist) is always selected, preferably previously selected item:
        if (!listData.isEmpty())
            fieldList.select(prevSelection == -1 || prevSelection >= listData.size() ? 0 : prevSelection);
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
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? newIr : null, this, null);
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
            PackageEditor pkgEd = pkg.getEditor();
            pkgEd.recordInteraction(getIr);
            pkgEd.raisePutOnBenchEvent(this, selField, selField.getGenType(), getIr, true, Optional.empty());
        }
    }

    /**
     * Close this inspector. The caller should remove it from the list of open
     * inspectors.
     */
    public void doClose(boolean handleAssertions)
    {
        hide();
        remove();
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
        // getButton is only used in BlueJ
        getButton.setVisible(!Config.isGreenfoot());
        getButton.setDisable(true);
        getButton.setOnAction(e -> doGet());
        buttonPanel.getChildren().add(getButton);

        JavaFXUtil.addStyleClass(buttonPanel, "inspector-side-buttons");
        return buttonPanel;
    }

    // Allow movement of the window by dragging
    // Adapted from: http://www.stupidjavatricks.com/?p=4
    // (with improvements).
    protected void installListenersForMoveDrag(double curvedCornersMargin)
    {
        resizeListener.setCurvedCorners(curvedCornersMargin);
        addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener);
        addEventHandler(MouseEvent.MOUSE_PRESSED, resizeListener);
        // Must be filter to allows us to consume event before next handler for window-move:
        addEventFilter(MouseEvent.MOUSE_DRAGGED, resizeListener);
        addEventHandler(MouseEvent.MOUSE_EXITED, resizeListener);
        addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, resizeListener);

        addEventHandler(MouseEvent.MOUSE_PRESSED, e ->
        {
            initialClickX = e.getScreenX() - getX();
            initialClickY = e.getScreenY() - getY();
        });
        //addEventHandler(MouseEvent.MOUSE_RELEASED,e -> { initialClick = null;});
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e ->
        {
            setX(e.getScreenX() - initialClickX);
            setY(e.getScreenY() - initialClickY);
        });


    }
    
    @OnThread(Tag.Any)
    public int getUniqueId()
    {
        return uniqueId;
    }

    // Adapted from HeavyweightDialog.positionStage
    public void centerOnOwner()
    {
        // Firstly we need to force CSS and layout to happen, as the window
        // may not have been shown yet (so it has no dimensions)
        getScene().getRoot().applyCss();
        getScene().getRoot().layout();

        final Window owner = getOwner();
        if (owner == null)
        {
            centerOnScreen();
            return;
        }
        final Scene ownerScene = owner.getScene();

        // scene.getY() seems to represent the y-offset from the top of the titlebar to the
        // start point of the scene, so it is the titlebar height
        final double titleBarHeight = ownerScene.getY();

        // because Stage does not seem to centre itself over its owner, we
        // do it here.

        // then we can get the dimensions and position the dialog appropriately.
        final double dialogWidth = getScene().getRoot().prefWidth(-1);
        final double dialogHeight = getScene().getRoot().prefHeight(dialogWidth);

//        stage.sizeToScene();

        double x = owner.getX() + (ownerScene.getWidth() / 2.0) - (dialogWidth / 2.0);
        double y = owner.getY() + titleBarHeight / 2.0 + (ownerScene.getHeight() / 2.0) - (dialogHeight / 2.0);

        setX(Math.max(0, x));
        setY(Math.max(0, y));
    }

    // Gets the content for the purposes of animation
    public abstract Region getContent();

    /**
     * Adapted from:
     * http://stackoverflow.com/questions/19455059/allow-user-to-resize-an-undecorated-stage
     */
    static class ResizeListener implements EventHandler<MouseEvent>
    {
        private Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
        private int border = 4;
        private double startX = 0;
        private double startY = 0;
        private double curvedCornersMargin;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            EventType<? extends MouseEvent> mouseEventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            double mouseEventX = mouseEvent.getSceneX(),
                mouseEventY = mouseEvent.getSceneY(),
                sceneWidth = scene.getWidth(),
                sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(mouseEventType) == true) {
                double cornerBorder = Math.max(curvedCornersMargin, border);
                if (mouseEventX < cornerBorder && mouseEventY < cornerBorder) {
                    cursorEvent = Cursor.NW_RESIZE;
                } else if (mouseEventX < cornerBorder && mouseEventY > sceneHeight - cornerBorder) {
                    cursorEvent = Cursor.SW_RESIZE;
                } else if (mouseEventX > sceneWidth - cornerBorder && mouseEventY < cornerBorder) {
                    cursorEvent = Cursor.NE_RESIZE;
                } else if (mouseEventX > sceneWidth - cornerBorder && mouseEventY > sceneHeight - cornerBorder) {
                    cursorEvent = Cursor.SE_RESIZE;
                } else if (mouseEventX < border) {
                    cursorEvent = Cursor.W_RESIZE;
                } else if (mouseEventX > sceneWidth - border) {
                    cursorEvent = Cursor.E_RESIZE;
                } else if (mouseEventY < border) {
                    cursorEvent = Cursor.N_RESIZE;
                } else if (mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.S_RESIZE;
                } else {
                    cursorEvent = Cursor.DEFAULT;
                }
                scene.setCursor(cursorEvent);
            } else if(MouseEvent.MOUSE_EXITED.equals(mouseEventType) || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)){
                scene.setCursor(Cursor.DEFAULT);
            } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType) == true) {
                startX = stage.getWidth() - mouseEventX;
                startY = stage.getHeight() - mouseEventY;
            } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType) == true) {
                if (Cursor.DEFAULT.equals(cursorEvent) == false) {
                    if (Cursor.W_RESIZE.equals(cursorEvent) == false && Cursor.E_RESIZE.equals(cursorEvent) == false) {
                        double minHeight = stage.getMinHeight() > (border*2) ? stage.getMinHeight() : (border*2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) == true || Cursor.N_RESIZE.equals(cursorEvent) == true || Cursor.NE_RESIZE.equals(cursorEvent) == true) {
                            if (stage.getHeight() > minHeight || mouseEventY < 0) {
                                stage.setHeight(stage.getY() - mouseEvent.getScreenY() + stage.getHeight());
                                stage.setY(mouseEvent.getScreenY());
                            }
                        } else {
                            if (stage.getHeight() > minHeight || mouseEventY + startY - stage.getHeight() > 0) {
                                stage.setHeight(mouseEventY + startY);
                            }
                        }
                    }

                    if (Cursor.N_RESIZE.equals(cursorEvent) == false && Cursor.S_RESIZE.equals(cursorEvent) == false) {
                        double minWidth = stage.getMinWidth() > (border*2) ? stage.getMinWidth() : (border*2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) == true || Cursor.W_RESIZE.equals(cursorEvent) == true || Cursor.SW_RESIZE.equals(cursorEvent) == true) {
                            if (stage.getWidth() > minWidth || mouseEventX < 0) {
                                stage.setWidth(stage.getX() - mouseEvent.getScreenX() + stage.getWidth());
                                stage.setX(mouseEvent.getScreenX());
                            }
                        } else {
                            if (stage.getWidth() > minWidth || mouseEventX + startX - stage.getWidth() > 0) {
                                stage.setWidth(mouseEventX + startX);
                            }
                        }
                    }

                    mouseEvent.consume();
                }

            }
        }

        public void setCurvedCorners(double curvedCornersMargin)
        {
            this.curvedCornersMargin = curvedCornersMargin;
        }
    }
}
