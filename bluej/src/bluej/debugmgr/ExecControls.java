/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
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
import bluej.utility.javafx.FXAbstractAction;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
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

/**
 * Window for controlling the debugger
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

    // the display for the list of active threads
    private ComboBox<DebuggerThreadDetails> threadList;

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
    private Map<String, Set<String>> restrictedClasses = Collections.emptyMap();

    private final SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty hideSystemThreads = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotStepOrContinue = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotHalt = new SimpleBooleanProperty(true);


    /**
     * Create a window to view and interact with a debug VM.
     * 
     * @param project  the project this window is associated with
     * @param debugger the debugger this window is debugging
     */
    public ExecControls(Project project, Debugger debugger, ObservableList<DebuggerThreadDetails> debuggerThreads)
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
        BorderPane lhsPane = new BorderPane(labelled(stackList, stackTitle), labelled(threadList, threadTitle), null, null, null);
        JavaFXUtil.addStyleClass(threadList, "debugger-thread-combo");
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
            //org.scenicview.ScenicView.show(scene);
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
                window.show();
            else if (!show && window.isShowing())
                window.hide();
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
     * Checks to make sure that a particular thread is
     * selected in the thread tree. Often when we get to this,
     * the thread in question should already be selected so
     * in that case we should not cause any more events, or
     * we'll end in a cycle.
     * 
     * If the thread is already selected, this method
     * will ensure that the status details are up to date.
     * 
     * @param  dt  the thread to hilight in the thread
     *             tree and whose status we want to display.
     */
    public void makeSureThreadIsSelected(final DebuggerThread dt)
    {
        DebuggerThreadDetails details = threadList.getItems().stream().filter(d -> d.isThread(dt)).findFirst().orElse(null);
        if (details != null)
            threadList.getSelectionModel().select(details);
    }

    /**
     * Set our internally selected thread and update the
     * UI to reflect its status.
     *
     * <p>This does not actually highlight the selected thread - use makeSureThreadIsSelected()
     * for that.
     * 
     * @see #makeSureThreadIsSelected(DebuggerThread)
     * 
     * @param dt  the thread to select or null if the thread
     *            selection has been cleared
     */
    public void setSelectedThread(DebuggerThreadDetails dt)
    {
        threadList.getSelectionModel().select(dt);

    }

    private void selectedThreadChanged(DebuggerThreadDetails dt)
    {
        if (dt == null)
        {
            cannotHalt.set(true);
            cannotStepOrContinue.set(true);
            stackList.getItems().clear();
        }
        else
        {
            boolean isSuspended = dt.getThread().isSuspended();
            cannotHalt.set(isSuspended);
            cannotStepOrContinue.set(!isSuspended);
            setThreadDetails(dt.getThread());
        }
    }

    /**
     * Display the details for the currently selected thread.
     * These details include showing the threads stack, and displaying 
     * the details for the top stack frame.
     */
    private void setThreadDetails(DebuggerThread selectedThread)
    {
        //Copy the list because we may alter it:
        List<SourceLocation> stack = new ArrayList<>(selectedThread.getStack());
        List<SourceLocation> filtered = Arrays.asList(getFilteredStack(stack));

        stackList.getItems().setAll(filtered);
        if (filtered.size() > 0)
        {
            // show details of top frame
            autoSelectionEvent = true;
            stackList.getSelectionModel().select(0);
            autoSelectionEvent = false;
        }
    }
    
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
    private void stackFrameSelectionChanged(DebuggerThread selectedThread, int index)
    {
        if (index >= 0) {
            setStackFrameDetails(selectedThread, index);
            selectedThread.setSelectedFrame(index);
                
            if (! autoSelectionEvent) {
                project.showSource(selectedThread,
                        selectedThread.getClass(index),
                        selectedThread.getClassSourceName(index),
                        selectedThread.getLineNumber(index));
            }
        }
    }

    /**
     * Display the detail information (current object fields and local var's)
     * for a specific stack frame.
     */
    private void setStackFrameDetails(DebuggerThread selectedThread, int frameNo)
    {
        DebuggerClass currentClass = selectedThread.getCurrentClass(frameNo);
        DebuggerObject currentObject = selectedThread.getCurrentObject(frameNo);
        if(currentClass != null) {
            List<DebuggerField> fields = currentClass.getStaticFields();
            List<VarDisplayInfo> listData = new ArrayList<>(fields.size());
            for (DebuggerField field : fields) {
                String declaringClass = field.getDeclaringClassName();
                Set<String> whiteList = restrictedClasses.get(declaringClass);
                if (whiteList == null || whiteList.contains(field.getName())) {
                    listData.add(new VarDisplayInfo(field));
                }
            }
            staticList.getItems().setAll(listData);
        }

        if(currentObject != null && !currentObject.isNullObject()) {
            List<DebuggerField> fields = currentObject.getFields();
            List<VarDisplayInfo> listData = new ArrayList<>(fields.size());
            for (DebuggerField field : fields) {
                if (! Modifier.isStatic(field.getModifiers())) {
                    String declaringClass = field.getDeclaringClassName();
                    Set<String> whiteList = restrictedClasses.get(declaringClass);
                    if (whiteList == null || whiteList.contains(field.getName())) {
                        listData.add(new VarDisplayInfo(field));
                    }
                }
            }
            instanceList.getItems().setAll(listData);
        }
        else {
            instanceList.getItems().clear();
        }
        
        localList.getItems().setAll(selectedThread.getLocalVariables(frameNo));
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
        JavaFXUtil.addChangeListenerPlatform(stackList.getSelectionModel().selectedIndexProperty(), index -> stackFrameSelectionChanged(getSelectedThread(), index.intValue()));
        Label placeholder = new Label(removeHTML(Config.getString("debugger.threadRunning")));
        placeholder.setTextAlignment(TextAlignment.CENTER);
        stackList.setPlaceholder(placeholder);


        FilteredList<DebuggerThreadDetails> filteredThreads = new FilteredList<>(debuggerThreads, this::showThread);
        threadList = new ComboBox<>(filteredThreads);
        // FilteredList doesn't know to recalculate after property changes, so
        // we have to manually trigger it:
        JavaFXUtil.addChangeListenerPlatform(hideSystemThreads, sys -> {
            // Need to make an actual change, so blank then set again:
            filteredThreads.setPredicate(null);
            filteredThreads.setPredicate(this::showThread);
        });
        JavaFXUtil.addChangeListenerPlatform(threadList.getSelectionModel().selectedItemProperty(), this::selectedThreadChanged);
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

    private DebuggerThread getSelectedThread()
    {
        DebuggerThreadDetails debuggerThreadDetails = getSelectedThreadDetails();
        if (debuggerThreadDetails == null)
            return null;
        return debuggerThreadDetails.getThread();
    }

    public DebuggerThreadDetails getSelectedThreadDetails()
    {
        return threadList.getSelectionModel().getSelectedItem();
    }

    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }

    public void threadStateChanged(DebuggerThreadDetails thread)
    {
        if (getSelectedThreadDetails().equals(thread))
        {
            setThreadDetails(thread.getThread());
        }
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
        
        public void actionPerformed()
        {
            DebuggerThread selectedThread = getSelectedThread();
            if (selectedThread == null)
                return;
            clearThreadDetails();
            if (!selectedThread.isSuspended()) {
                selectedThread.halt();
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
        
        public void actionPerformed()
        {
            DebuggerThread selectedThread = getSelectedThread();
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.step();
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
        
        public void actionPerformed()
        {
            DebuggerThread selectedThread = getSelectedThread();
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.stepInto();
            }
        }
    }

    private static Node makeStepIntoIcon()
    {
        SVGPath path = new SVGPath();
        // See http://jxnblk.com/paths/?d=M2%2016%20Q24%208%2038%2016%20L40%2010%20L48%2026%20L32%2034%20L34%2028%20Q22%2022%206%2028%20Z
        path.setContent("M2 16 Q24 8 38 16 L40 10 L48 26 L32 34 L34 28 Q22 22 6 28 Z");
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
        
        public void actionPerformed()
        {
            DebuggerThread selectedThread = getSelectedThread();
            selectedThread = getSelectedThread();
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.cont();
                DataCollector.debuggerContinue(project, selectedThread.getName());
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
        
        public void actionPerformed()
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
