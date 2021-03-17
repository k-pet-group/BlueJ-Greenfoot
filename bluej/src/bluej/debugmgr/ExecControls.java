/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.debugger.VarDisplayInfo;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Project.DebuggerThreadDetails;
import bluej.prefmgr.PrefMgr;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.utility.javafx.FXAbstractAction;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.sun.jdi.VMDisconnectedException;

/**
 * Window for controlling the debugger.
 * <p>
 * There are two modes; one which displays a list of all threads (and the user can select a thread
 * to control/inspect) and another where only a single thread is displayed.
 *
 * @author  Michael Kolling
 */
public class ExecControls
{
    private static final String stackTitle =
        Config.getString("debugger.execControls.stackTitle");
    private static final String staticTitle =
        Config.getString("debugger.execControls.staticTitle");
    private static final String instanceTitle =
        Config.getString("debugger.execControls.instanceTitle");
    private static final String localTitle =
        Config.getString("debugger.execControls.localTitle");
    private static final String threadTitle =
        Config.getString("debugger.execControls.threadTitle");

    private static final String haltButtonText =
        Config.getString("debugger.execControls.haltButtonText");
    private static final String stepButtonText =
        Config.getString("debugger.execControls.stepButtonText");
    private static final String stepIntoButtonText =
        Config.getString("debugger.execControls.stepIntoButtonText");
    private static final String continueButtonText =
        Config.getString("debugger.execControls.continueButtonText");
    private static final String terminateButtonText =
        Config.getString("debugger.execControls.terminateButtonText");

    // === instance ===

    @OnThread(Tag.FX)
    private Stage window;
    @OnThread(Tag.FXPlatform)
    private BorderPane fxContent;

    // the display for the list of active threads; may be null if there is no list (i.e. the
    // "single thread" mode is active)
    private ComboBox<DebuggerThreadDetails> threadList;

    @OnThread(Tag.FXPlatform)
    private ListView<SourceLocation> stackList;
    private ListView<VarDisplayInfo> staticList, localList, instanceList;
    private Button stopButton, stepButton, stepIntoButton, continueButton, terminateButton;

    // the Project that owns this debugger
    private final Project project;

    // A flag to keep track of whether a stack frame selection was performed
    // explicitly via the gui or as a result of a debugger event
    private boolean autoSelectionEvent = false; 
    
    /**
     * Fields from these classes (key from map) are only shown if they are in the corresponding whitelist
     * of fields (corresponding value from map)
     */
    @OnThread(Tag.Any) // Rarely modified
    private Map<String, Set<String>> restrictedClasses = Collections.emptyMap();

    private final SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty hideSystemThreads = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotStepOrContinue = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotHalt = new SimpleBooleanProperty(true);
    
    // The currently selected thread
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private DebuggerThreadDetails selectedThread;


    /**
     * Create a window to view and interact with a debug VM. The window optionally shows a thread pane
     * from which the user can select a thread to control; otherwise the thread selection is controlled
     * programmatically.
     * 
     * @param project  the project this window is associated with
     * @param debugger the debugger this window is debugging
     * @param debuggerThreads  an observable list of all threads that should be displayed by the debugger,
     *                         or null if the thread pane should not be displayed.
     */
    public ExecControls(Project project, Debugger debugger,
            ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        if (project == null || debugger == null) {
            throw new NullPointerException("project or debugger null in ExecControls");
        }

        this.project = project;
        this.window = new Stage();
        window.setTitle(Config.getApplicationName() + ":  " + Config.getString("debugger.execControls.windowTitle"));
        BlueJTheme.setWindowIconFX(window);
        createWindowContent(debuggerThreads);
        TilePane buttons = new TilePane(Orientation.HORIZONTAL, stopButton, stepButton, stepIntoButton, continueButton, terminateButton);
        buttons.setPrefColumns(buttons.getChildren().size());
        JavaFXUtil.addStyleClass(buttons, "debugger-buttons");
        this.fxContent = new BorderPane();
        BorderPane vars = new BorderPane();
        vars.setTop(labelled(staticList, staticTitle));
        SplitPane varSplit = new SplitPane(labelled(instanceList, instanceTitle), labelled(localList, localTitle));
        varSplit.setOrientation(Orientation.VERTICAL);
        vars.setCenter(varSplit);
        
        // There are two possible pane layouts: with thread list and without.
        BorderPane lhsPane;
        if (debuggerThreads != null)
        {
            lhsPane = new BorderPane(labelled(stackList, stackTitle), labelled(threadList, threadTitle), null, null, null);
            JavaFXUtil.addStyleClass(threadList, "debugger-thread-combo");
        }
        else
        {
            lhsPane = new BorderPane(labelled(stackList, stackTitle), null, null, null, null);
        }
        JavaFXUtil.addStyleClass(lhsPane, "debugger-thread-and-stack");
        
        fxContent.setTop(makeMenuBar());
        fxContent.setCenter(new SplitPane(lhsPane, vars));
        fxContent.setBottom(buttons);
        JavaFXUtil.addStyleClass(fxContent, "debugger");
        // Menu bar will be added later:
        Scene scene = new Scene(fxContent);
        Config.addDebuggerStylesheets(scene);
        window.setScene(scene);
        Config.loadAndTrackPositionAndSize(window, "bluej.debugger");
        window.setOnShown(e -> {
            DataCollector.debuggerChangeVisible(project, true);
            showingProperty.set(true);
        });
        window.setOnHidden(e -> {
            DataCollector.debuggerChangeVisible(project, false);
            showingProperty.set(false);
        });
        // showingProperty should mirror the window state.  Note that it
        // can be set either externally as a request to show the window,
        // or internally as an update of the state, so we must be careful
        // not to end up in an infinite loop:
        JavaFXUtil.addChangeListenerPlatform(showingProperty, show -> {
            if (show && !window.isShowing())
            {
                window.show();
            }
            else if (!show && window.isShowing())
            {
                window.hide();
            }
        });

    }

    private static Node labelled(Node content, String title)
    {
        Label titleLabel = new Label(title);
        JavaFXUtil.addStyleClass(titleLabel, "debugger-section-title");
        BorderPane borderPane = new BorderPane(content, titleLabel, null, null, null);
        JavaFXUtil.addStyleClass(borderPane, "debugger-section");
        return borderPane;
    }

    /**
     * Sets the restricted classes - classes for which only some fields should be displayed.
     * 
     * @param restrictedClasses a map of class name to a set of white-listed fields.
     */
    public void setRestrictedClasses(Map<String, Set<String>> restrictedClasses)
    {
        this.restrictedClasses = restrictedClasses;
    }
    
    public Map<String, Set<String>> getRestrictedClasses()
    {
        HashMap<String, Set<String>> copy = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> e : restrictedClasses.entrySet())
        {
            copy.put(e.getKey(), new HashSet<String>(e.getValue()));
        }
        return copy;
    }

    /**
     * Make sure that a particular thread is displayed and the details are up-to-date.
     * Note that if the controls window is invisible this will not show it. 
     * 
     * @param  dt  the thread to highlight in the thread
     *             tree and whose status we want to display.
     */
    public void selectThread(final DebuggerThread dt)
    {
        if (threadList != null)
        {
            if (dt.isKnownSystemThread())
            {
                hideSystemThreads.set(false);
            }
            
            DebuggerThreadDetails details = threadList.getItems().stream()
                    .filter(d -> d.isThread(dt))
                    .findFirst().orElse(null);
            if (details != null)
            {
                threadList.getSelectionModel().select(details);
            }
        }
        else if (getSelectedThreadDetails() == null || ! dt.sameThread(getSelectedThreadDetails().getThread()))
        {
            project.getDebugger().runOnEventHandler(() -> {
                DebuggerThreadDetails threadDetails = new DebuggerThreadDetails(dt);
                Platform.runLater(() -> selectedThreadChanged(threadDetails));
            });
        }
    }

    /**
     * Update the details displayed for the given thread (if they are currently displayed).
     */
    @OnThread(Tag.VMEventHandler)
    public void updateThreadDetails(DebuggerThread dt)
    {
        DebuggerThreadDetails sel = getSelectedThreadDetails();
        if (sel != null && sel.isThread(dt))
        {
            if (isSingleThreadMode())
            {
                sel.update();
            }
            setThreadDetails(sel);
        }
    }

    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    private boolean isSingleThreadMode()
    {
        return threadList == null;
    }

    @OnThread(Tag.FXPlatform)
    private void selectedThreadChanged(DebuggerThreadDetails dt)
    {
        if (dt == null)
        {
            synchronized (this)
            {
                selectedThread = null;
            }
            cannotHalt.set(true);
            cannotStepOrContinue.set(true);
            stackList.getItems().clear();
        }
        else
        {
            synchronized (this)
            {
                selectedThread = dt;
            }
            project.getDebugger().runOnEventHandler(() -> setThreadDetails(dt));
        }
    }

    /**
     * Display the details for the currently selected thread.
     * These details include showing the threads stack, and displaying 
     * the details for the top stack frame.
     */
    @OnThread(Tag.VMEventHandler)
    private void setThreadDetails(DebuggerThreadDetails dt)
    {
        //Copy the list because we may alter it:
        List<SourceLocation> stack = new ArrayList<>(dt.getThread().getStack());
        List<SourceLocation> filtered = Arrays.asList(getFilteredStack(stack));

        boolean isSuspended = dt.isSuspended();
        Platform.runLater(() -> {
            cannotHalt.set(isSuspended);
            cannotStepOrContinue.set(!isSuspended);

            stackList.getItems().setAll(filtered);
            if (filtered.size() > 0)
            {
                // show details of top frame
                autoSelectionEvent = true;
                stackList.getSelectionModel().select(0);
                autoSelectionEvent = false;
            }
        });
    }
    
    @OnThread(Tag.Any)
    public static SourceLocation [] getFilteredStack(List<SourceLocation> stack)
    {
        int first = -1;
        int i;
        for (i = 0; i < stack.size(); i++) {
            SourceLocation loc = stack.get(i);
            String className = loc.getClassName();

            // ensure that the bluej.runtime.ExecServer frames are not shown
            if (className.startsWith("bluej.runtime.") && !className.equals(bluej.runtime.BJInputStream.class.getCanonicalName())) {
                break;
            }

            // must getBase on classname so that we find __SHELL
            // classes in other packages ie a.b.__SHELL
            // if it is a __SHELL class, stop processing the stack
            if (JavaNames.getBase(className).startsWith("__SHELL")) {
                break;
            }
            
            if (Config.isGreenfoot() && className.startsWith("greenfoot.core.Simulation")) {
                break;
            }
            
            // Topmost stack location shown will have source available!
            if (first == -1 && loc.getFileName() != null) {
                first = i;
            }
        }
        
        if (first == -1 || i == 0) {
            return new SourceLocation[0];
        }
        
        SourceLocation[] filtered = new SourceLocation[i - first];
        for (int j = first; j < i; j++) {
            filtered[j - first] = stack.get(j);
        }
        
        return filtered;
    }
    
    /**
     * Clear the display of thread details (stack and variables).
     */
    private void clearThreadDetails()
    {
        stackList.getItems().clear();
        staticList.getItems().clear();
        instanceList.getItems().clear();
        localList.getItems().clear();
    }

    /**
     * Make a stack frame in the stack display the selected stack frame.
     * This will cause this frame's details (local variables, etc.) to be
     * displayed, as well as the current source position being marked.
     */
    @OnThread(Tag.VMEventHandler)
    private void stackFrameSelectionChanged(DebuggerThread thread, int index, boolean showSource)
    {
        if (index >= 0) {
            setStackFrameDetails(thread, index);
            thread.setSelectedFrame(index);
                
            if (showSource) {
                String aClass = thread.getClass(index);
                String classSourceName = thread.getClassSourceName(index);
                int lineNumber = thread.getLineNumber(index);
                DebuggerObject currentObject = thread.getCurrentObject(index);
                Platform.runLater(() -> project.showSource(thread,
                        aClass,
                        classSourceName,
                        lineNumber,
                        currentObject));
            }
        }
    }

    /**
     * Display the detail information (current object fields and local var's)
     * for a specific stack frame.
     */
    @OnThread(Tag.VMEventHandler)
    private void setStackFrameDetails(DebuggerThread thread, int frameNo)
    {
        try {
            DebuggerClass currentClass = thread.getCurrentClass(frameNo);
            DebuggerObject currentObject = thread.getCurrentObject(frameNo);
            List<FXPlatformSupplier<VarDisplayInfo>> staticVars = new ArrayList<>();
            if(currentClass != null) {
                List<DebuggerField> fields = currentClass.getStaticFields();
                
                for (DebuggerField field : fields) {
                    String declaringClass = field.getDeclaringClassName();
                    Set<String> whiteList = restrictedClasses.get(declaringClass);
                    if (whiteList == null || whiteList.contains(field.getName())) {
                        staticVars.add(() -> new VarDisplayInfo(field));
                    }
                }
                
            }

            List<FXPlatformSupplier<VarDisplayInfo>> instanceVars = new ArrayList<>();
    
            if(currentObject != null && !currentObject.isNullObject()) {
                List<DebuggerField> fields = currentObject.getFields();
                
                for (DebuggerField field : fields) {
                    if (! Modifier.isStatic(field.getModifiers())) {
                        String declaringClass = field.getDeclaringClassName();
                        Set<String> whiteList = restrictedClasses.get(declaringClass);
                        if (whiteList == null || whiteList.contains(field.getName())) {
                            instanceVars.add(() -> new VarDisplayInfo(field));
                        }
                    }
                }
                
            }
            
            List<FXPlatformSupplier<VarDisplayInfo>> localVariables = thread.getLocalVariables(frameNo);
            
            Platform.runLater(() -> {
                staticList.getItems().setAll(Utility.mapList(staticVars, v -> v.get()));
                instanceList.getItems().setAll(Utility.mapList(instanceVars, v -> v.get()));
                localList.getItems().setAll(Utility.mapList(localVariables, v -> v.get()));
            });
            
            
        }
        catch (VMDisconnectedException vmde)
        {
            // Do nothing.
        }
    }

    /**
     * Create and arrange the GUI components.
     * @param debuggerThreads
     */
    private void createWindowContent(ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        stopButton = new StopAction().makeButton();
        stepButton = new StepAction().makeButton();
        stepIntoButton = new StepIntoAction().makeButton();
        continueButton = new ContinueAction().makeButton();

        // terminate is always on
        terminateButton = new TerminateAction().makeButton();

        stepButton.disableProperty().bind(cannotStepOrContinue);
        stepIntoButton.disableProperty().bind(cannotStepOrContinue);
        continueButton.disableProperty().bind(cannotStepOrContinue);
        stopButton.disableProperty().bind(cannotHalt);


        // create static variable panel
        staticList = makeVarListView();
        JavaFXUtil.addStyleClass(staticList, "debugger-static-var-list");

        // create instance variable panel
        instanceList = makeVarListView();

        // create local variable panel
        localList = makeVarListView();

        // Create stack listing panel

        stackList = new ListView<>();
        stackList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        JavaFXUtil.addStyleClass(stackList, "debugger-stack");
        stackList.styleProperty().bind(PrefMgr.getEditorFontCSS(false));
        JavaFXUtil.addChangeListenerPlatform(stackList.getSelectionModel().selectedIndexProperty(), index -> {
            DebuggerThread thread = getSelectedThreadDetails() == null ? null : getSelectedThreadDetails().getThread();
            boolean showSource = !autoSelectionEvent;
            project.getDebugger().runOnEventHandler(() -> {
                stackFrameSelectionChanged(thread, index.intValue(), showSource);
            });
        });
        Label placeholder = new Label(removeHTML(Config.getString("debugger.threadRunning")));
        placeholder.setTextAlignment(TextAlignment.CENTER);
        stackList.setPlaceholder(placeholder);

        if (debuggerThreads != null)
        {
            FilteredList<DebuggerThreadDetails> filteredThreads = new FilteredList<>(debuggerThreads, this::showThread);
            threadList = new ComboBox<>(filteredThreads);
            // FilteredList doesn't know to recalculate after property changes, so
            // we have to manually trigger it:
            JavaFXUtil.addChangeListenerPlatform(hideSystemThreads, sys -> {
                // Need to make an actual change, so blank then set again:
                filteredThreads.setPredicate(null);
                filteredThreads.setPredicate(this::showThread);
            });
            JavaFXUtil.addChangeListenerPlatform(threadList.getSelectionModel().selectedItemProperty(), t -> selectedThreadChanged(t));
        }
    }

    // The label is <html><center>...<br>...</html> (silly, really)
    // so we remove the tags here:
    private static String removeHTML(String label)
    {
        return label.replace("<html>", "").replace("<center>", "").replace("<br>", "\n").replace("</html>", "");
    }

    private boolean showThread(DebuggerThreadDetails thread)
    {
        if (hideSystemThreads.get())
            return !thread.getThread().isKnownSystemThread();
        else
            return true;
    }

    private ListView<VarDisplayInfo> makeVarListView()
    {
        ListView<VarDisplayInfo> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setCellFactory(lv -> {
            return new VarDisplayCell(project, window);
        });
        return listView;
    }

    /**
     * Create the debugger's menubar, all menus and items.
     */
    private MenuBar makeMenuBar()
    {
        MenuBar menubar = new MenuBar();
        menubar.setUseSystemMenuBar(true);
        Menu menu = new Menu(Config.getString("terminal.options"));

        
        if (!Config.isGreenfoot()) {
            MenuItem systemThreadItem = JavaFXUtil.makeCheckMenuItem(Config.getString("debugger.hideSystemThreads"), hideSystemThreads, null);
            menu.getItems().add(systemThreadItem);
            menu.getItems().add(new SeparatorMenuItem());
        }
        menu.getItems().add(JavaFXUtil.makeMenuItem(Config.getString("close"), this::hide, new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN)));

        menubar.getMenus().add(menu);
        return menubar;
    }

    public void show()
    {
        window.show();
        window.toFront();
    }

    public void hide()
    {
        window.hide();
    }

    @OnThread(Tag.Any)
    public synchronized DebuggerThreadDetails getSelectedThreadDetails()
    {
        return selectedThread;
    }

    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }

    /**
     * Action to halt the selected thread.
     */
    private class StopAction extends FXAbstractAction
    {
        public StopAction()
        {
            super(haltButtonText, Config.makeStopIcon(true));
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            if (!details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().halt());
            }
        }
    }
        
    /**
     * Action to step through the code.
     */
    private class StepAction extends FXAbstractAction
    {
        public StepAction()
        {
            super(stepButtonText, makeStepIcon());
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().step());
            }
            project.updateInspectors();
        }
    }

    private static Node makeStepIcon()
    {
        Polygon arrowShape = makeScaledUpArrow(false);
        JavaFXUtil.addStyleClass(arrowShape, "step-icon-arrow");
        Rectangle bar = new Rectangle(28, 6);
        JavaFXUtil.addStyleClass(bar, "step-icon-bar");
        VBox vBox = new VBox(arrowShape, bar);
        JavaFXUtil.addStyleClass(vBox, "step-icon");
        return vBox;
    }

    private static Polygon makeScaledUpArrow(boolean shortTail)
    {
        Polygon arrowShape = Config.makeArrowShape(shortTail);
        JavaFXUtil.scalePolygonPoints(arrowShape, 1.5, true);
        return arrowShape;
    }

    private static Node makeContinueIcon()
    {
        Polygon arrowShape1 = makeScaledUpArrow(true);
        Polygon arrowShape2 = makeScaledUpArrow(true);
        Polygon arrowShape3 = makeScaledUpArrow(true);
        JavaFXUtil.addStyleClass(arrowShape1, "continue-icon-arrow");
        JavaFXUtil.addStyleClass(arrowShape2, "continue-icon-arrow");
        JavaFXUtil.addStyleClass(arrowShape3, "continue-icon-arrow");
        arrowShape1.setOpacity(0.2);
        arrowShape2.setOpacity(0.5);
        Pane pane = new Pane(arrowShape1, arrowShape2, arrowShape3);
        arrowShape2.setLayoutX(2.0);
        arrowShape2.setLayoutY(6.0);
        arrowShape3.setLayoutX(4.0);
        arrowShape3.setLayoutY(12.0);
        return pane;
    }

    /**
     * Action to "step into" the code.
     */
    private class StepIntoAction extends FXAbstractAction
    {
        public StepIntoAction()
        {
            super(stepIntoButtonText, makeStepIntoIcon());
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().stepInto());
            }
        }
    }

    private static Node makeStepIntoIcon()
    {
        SVGPath path = new SVGPath();
        // See https://jxnblk.github.io/paths/?d=M14%2022%20L14%2040%20L28%2040%20L28%2048%20L18%2048%20L33%2058%20L48%2048%20L38%2048%20L38%2032%20L24%2032%20L24%2022%20Z
        path.setContent("M14 22 L14 40 L28 40 L28 48 L18 48 L33 58 L48 48 L38 48 L38 32 L24 32 L24 22 Z"
            // + "M18 60 L48 60 L48 68 L18 68 Z"
            );
        path.setScaleX(0.75);
        path.setScaleY(0.85);
        JavaFXUtil.addStyleClass(path, "step-into-icon");
        return new Group(path);
    }

    /**
     * Action to continue a halted thread. 
     */
    private class ContinueAction extends FXAbstractAction
    {
        public ContinueAction()
        {
            super(continueButtonText, makeContinueIcon());
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().cont());
                DataCollector.debuggerContinue(project, details.getThread().getName());
            }
        }
    }

    /**
     * Action to terminate the program, restart the VM.
     */
    private class TerminateAction extends FXAbstractAction
    {
        public TerminateAction()
        {
            super(terminateButtonText, makeTerminateIcon());
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            try {
                clearThreadDetails();
                
                // throws an illegal state exception
                // if we press this whilst we are already
                // restarting the remote VM
                project.restartVM();
                DataCollector.debuggerTerminate(project);
            }
            catch (IllegalStateException ise) { }
        }
    }

    private static Node makeTerminateIcon()
    {
        Polygon s = new Polygon(
            5, 0,
            15, 10,
            25, 0,
            30, 5,
            20, 15,
            30, 25,
            25, 30,
            15, 20,
            5, 30,
            0, 25,
            10, 15,
            0, 5
        );
        JavaFXUtil.addStyleClass(s, "terminate-icon");
        return s;
    }


    /**
     * A cell in a list view which has a variable's type, name and value.  (And optionally, access modifier)
     */
    private static class VarDisplayCell extends javafx.scene.control.ListCell<VarDisplayInfo>
    {
        private final Label access = new Label();
        private final Label type = new Label();
        private final Label name = new Label();
        private final Label value = new Label();
        private final BooleanProperty nonEmpty = new SimpleBooleanProperty();
        private static final Image objectImage =
                Config.getImageAsFXImage("image.eval.object");
        // A property so that we can listen for it changing from null to/from non-null:
        private final SimpleObjectProperty<FXPlatformSupplier<DebuggerObject>> fetchObject = new SimpleObjectProperty<>(null);

        public VarDisplayCell(Project project, Window window)
        {
            // Only visible when there is a relevant object reference which can be inspected:
            ImageView objectImageView = new ImageView(objectImage);
            JavaFXUtil.addStyleClass(objectImageView, "debugger-var-object-ref");
            objectImageView.visibleProperty().bind(fetchObject.isNotNull());
            objectImageView.managedProperty().bind(objectImageView.visibleProperty());
            objectImageView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1)
                    inspect(project, window, objectImageView);
            });

            // The spacing is added via CSS, not by space characters:
            HBox hBox = new HBox(access, type, name, new Label("="), objectImageView, this.value);
            hBox.visibleProperty().bind(nonEmpty);
            hBox.styleProperty().bind(PrefMgr.getEditorFontCSS(false));
            JavaFXUtil.addStyleClass(hBox, "debugger-var-cell");
            JavaFXUtil.addStyleClass(access, "debugger-var-access");
            JavaFXUtil.addStyleClass(type, "debugger-var-type");
            JavaFXUtil.addStyleClass(name, "debugger-var-name");
            JavaFXUtil.addStyleClass(value, "debugger-var-value");
            setGraphic(hBox);

            // Double click anywhere on the row does an object inspection, as it used to:
            hBox.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                {
                    inspect(project, window, objectImageView);
                }
            });
        }

        @OnThread(Tag.FXPlatform)
        private void inspect(Project project, Window window, Node sourceNode)
        {
            if (fetchObject.get() != null)
            {
                project.getInspectorInstance(fetchObject.get().get(), null, null, null, window, sourceNode);
            }
        }

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        protected void updateItem(VarDisplayInfo item, boolean empty)
        {
            super.updateItem(item, empty);
            nonEmpty.set(!empty);
            if (empty)
            {
                access.setText("");
                type.setText("");
                name.setText("");
                value.setText("");
                fetchObject.set(null);
            }
            else
            {
                access.setText(item.getAccess());
                type.setText(item.getType());
                name.setText(item.getName());
                value.setText(item.getValue());
                fetchObject.set(item.getFetchObject());
            }
        }
    }
}
