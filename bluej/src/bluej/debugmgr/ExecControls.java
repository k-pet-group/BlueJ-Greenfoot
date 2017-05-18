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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import bluej.debugger.VarDisplayInfo;
import bluej.pkgmgr.Project.DebuggerThreadDetails;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.FXAbstractAction;
import bluej.utility.javafx.FXPlatformBiConsumer;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.SwingNodeFixed;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.pkgmgr.Project;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    @OnThread(Tag.Any)
    private SwingNode swingNode;
    @OnThread(Tag.FXPlatform)
    private BorderPane fxContent;

    // the display for the list of active threads
    private ComboBox<DebuggerThreadDetails> threadList;
    
    private JComponent mainPanel;
    private ListView<SourceLocation> stackList;
    private ListView<VarDisplayInfo> staticList, localList, instanceList;
    private Button stopButton, stepButton, stepIntoButton, continueButton, terminateButton;
    private CardLayout cardLayout;
    private JPanel flipPanel;
    private JCheckBoxMenuItem systemThreadItem;

    // the Project that owns this debugger
    private final Project project;

    // the debug machine this control is looking at
    private Debugger debugger = null;

    private DebuggerClass currentClass;     // the current class for the
                                            //  selected stack frame
    private DebuggerObject currentObject;   // the "this" object for the
                                            //  selected stack frame
    private int currentFrame = 0;           // currently selected frame
    
    // A flag to keep track of whether a stack frame selection was performed
    // explicitly via the gui or as a result of a debugger event
    private boolean autoSelectionEvent = false; 
    
    /**
     * Fields from these classes (key from map) are only shown if they are in the corresponding whitelist
     * of fields (corresponding value from map)
     */
    private Map<String, Set<String>> restrictedClasses = Collections.emptyMap();
    @OnThread(Tag.Any)
    private final AtomicBoolean visible = new AtomicBoolean(false);
    @OnThread(Tag.FXPlatform)
    private SimpleBooleanProperty readyToShow;
    private SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(false);


    /**
     * Create a window to view and interact with a debug VM.
     * 
     * @param project  the project this window is associated with
     * @param debugger the debugger this window is debugging
     */
    @OnThread(Tag.Swing)
    public ExecControls(Project project, Debugger debugger, ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        if (project == null || debugger == null) {
            throw new NullPointerException("project or debugger null in ExecControls");
        }

        this.project = project;
        this.debugger = debugger;

        this.readyToShow = new SimpleBooleanProperty(false);
        this.window = new Stage();
        window.setTitle(Config.getApplicationName() + ":  " + Config.getString("debugger.execControls.windowTitle"));
        BlueJTheme.setWindowIconFX(window);
        this.swingNode = new SwingNodeFixed();
        VBox.setVgrow(swingNode, Priority.ALWAYS);
        createWindowContent(debuggerThreads);
        HBox buttons = new HBox(stopButton, stepButton, stepIntoButton, continueButton, terminateButton);
        this.fxContent = new BorderPane();
        BorderPane vars = new BorderPane();
        vars.setTop(staticList);
        SplitPane varSplit = new SplitPane(instanceList, localList);
        varSplit.setOrientation(Orientation.VERTICAL);
        vars.setCenter(varSplit);
        fxContent.setCenter(new SplitPane(new VBox(threadList, stackList), vars));
        fxContent.setBottom(buttons);
        // Menu bar will be added later:
        Scene scene = new Scene(fxContent);
        Config.addDebuggerStylesheets(scene);
        window.setScene(scene);
        Config.rememberPositionAndSize(window, "bluej.debugger");
        window.setOnShown(e -> {
            SwingUtilities.invokeLater(() -> DataCollector.debuggerChangeVisible(project, true));
            visible.set(true);
            //org.scenicview.ScenicView.show(scene);
        });
        window.setOnHidden(e -> {
            SwingUtilities.invokeLater(() -> DataCollector.debuggerChangeVisible(project, false));
            visible.set(false);
        });

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
     * A list item was double clicked.
     * 
     * This will be in one of the variable lists. We try to
     * view the relevant object that was double clicked on.
     */
    private static <T> void listenForDoubleClick(ListView<T> listView, FXPlatformBiConsumer<Integer, T> onDoubleClick)
    {
        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
            {
                int selectedIndex = listView.getSelectionModel().getSelectedIndex();
                T selected = listView.getSelectionModel().getSelectedItem();
                if (selectedIndex >= 0)
                {
                    onDoubleClick.accept(selectedIndex, selected);
                }
            }
        });
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
        if (dt == null) {
            //MOEFX this should all be in bindings:
            //stopButton.setEnabled(false);
            //stepButton.setEnabled(false);
            //stepIntoButton.setEnabled(false);
            //continueButton.setEnabled(false);

            cardLayout.show(flipPanel, "blank");
            stackList.getItems().clear();
        }
        else {
            boolean isSuspended = dt.getThread().isSuspended();

            //MOEFX this should all be in bindings:
            //stopButton.setEnabled(!isSuspended);
            //stepButton.setEnabled(isSuspended);
            //stepIntoButton.setEnabled(isSuspended);
            //continueButton .setEnabled(isSuspended);

            cardLayout.show(flipPanel, isSuspended ? "split" : "blank");

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
            
            currentFrame = index;
        }
    }

    /**
     * Display the detail information (current object fields and local var's)
     * for a specific stack frame.
     */
    private void setStackFrameDetails(DebuggerThread selectedThread, int frameNo)
    {
        currentClass = selectedThread.getCurrentClass(frameNo);
        currentObject = selectedThread.getCurrentObject(frameNo);
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
     * Display an object inspector for an object in a static field.
     */
    private void viewStaticField(int index, VarDisplayInfo content)
    {
        DebuggerField field = currentClass.getStaticField(index);
        if(field.isReferenceType() && ! field.isNull()) {
            Platform.runLater(() -> project.getInspectorInstance(field.getValueObject(null), null, null, null, window, null));
        }
    }

    /**
     * Display an object inspector for an object in an instance field.
     */
    private void viewInstanceField(int index, VarDisplayInfo fieldInfo)
    {
        DebuggerField field = currentObject.getInstanceField(index);
        if(field.isReferenceType() && ! field.isNull()) {
            Platform.runLater(() -> project.getInspectorInstance(field.getValueObject(null), null, null, null, window, null));
        }
    }

    /**
     * Display an object inspector for an object in a local variable.
     */
    private void viewLocalVar(int index, VarDisplayInfo content)
    {
        DebuggerThread selectedThread = getSelectedThread();
        if (selectedThread != null && selectedThread.varIsObject(currentFrame, index)) {
            DebuggerObject obj = selectedThread.getStackObject(currentFrame, index);
            Platform.runLater(() -> project.getInspectorInstance(obj,
                           null, null, null, window, null));
        }
    }

    /**
     * Create and arrange the GUI components.
     * @param debuggerThreads
     */
    @OnThread(Tag.Swing)
    private void createWindowContent(ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        FXPlatformSupplier<MenuBar> fxMenuBar = JavaFXUtil.swingMenuBarToFX(makeMenuBar(), this);
        Platform.runLater(() -> {
            MenuBar bar = fxMenuBar.get();
            bar.setUseSystemMenuBar(true);
            fxContent.getChildren().add(0, bar);
        });

        JPanel contentPane = new JPanel(new BorderLayout(6,6));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Create the control button panel

        JPanel buttonBox = new JPanel();
        if (!Config.isRaspberryPi()) buttonBox.setOpaque(false);
        {
            buttonBox.setLayout(new GridLayout(1,0));

            stopButton = makeButton(new StopAction());
            stepButton = makeButton(new StepAction());
            stepIntoButton = makeButton(new StepIntoAction());
            continueButton = makeButton(new ContinueAction());

            // terminate is always on
            terminateButton = makeButton(new TerminateAction());
        }

        contentPane.add(buttonBox, BorderLayout.SOUTH);

        // create static variable panel
        staticList = makeVarListView();
        listenForDoubleClick(staticList, this::viewStaticField);
        //MOEFX
        //JLabel lbl = new JLabel(staticTitle);

        // create instance variable panel
        instanceList = makeVarListView();
        listenForDoubleClick(instanceList, this::viewInstanceField);

        //MOEFX
        //JLabel lbl = new JLabel(instanceTitle);

        // create local variable panel
        localList = makeVarListView();
        listenForDoubleClick(localList, this::viewLocalVar);
        //MOEFX
        //JLabel lbl = new JLabel(localTitle);

        // Create stack listing panel

        stackList = new ListView<>();
        stackList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        JavaFXUtil.addStyleClass(stackList, "debugger-stack");
        stackList.styleProperty().bind(PrefMgr.getEditorFontCSS(false));
        JavaFXUtil.addChangeListenerPlatform(stackList.getSelectionModel().selectedIndexProperty(), index -> stackFrameSelectionChanged(getSelectedThread(), index.intValue()));
        JScrollPane stackScrollPane = new JScrollPane();
        JLabel lbl = new JLabel(stackTitle);
        if (!Config.isRaspberryPi()) lbl.setOpaque(true);
        stackScrollPane.setColumnHeaderView(lbl);


        // Create thread panel
        JPanel threadPanel = new JPanel(new BorderLayout());
        if (!Config.isRaspberryPi()) threadPanel.setOpaque(false);

        threadList = new ComboBox<>(debuggerThreads);
        JavaFXUtil.addChangeListenerPlatform(threadList.getSelectionModel().selectedItemProperty(), this::selectedThreadChanged);

        JScrollPane threadScrollPane = new JScrollPane();
        lbl = new JLabel(threadTitle);
        if (!Config.isRaspberryPi()) lbl.setOpaque(true);
        threadScrollPane.setColumnHeaderView(lbl);
        threadPanel.add(threadScrollPane, BorderLayout.CENTER);
        //threadPanel.setMinimumSize(new Dimension(100,100));

        flipPanel = new JPanel();
        if (!Config.isRaspberryPi()) flipPanel.setOpaque(false);
        {
            flipPanel.setLayout(cardLayout = new CardLayout());

            JPanel tempPanel = new JPanel();
            JLabel infoLabel = new JLabel(Config.getString("debugger.threadRunning"));
            infoLabel.setForeground(Color.gray);
            tempPanel.add(infoLabel);
            flipPanel.add(tempPanel, "blank");
        }

        if (Config.isGreenfoot()) {
            mainPanel = flipPanel;
        } else {
        /* JSplitPane */ mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              threadPanel, flipPanel);
            ((JSplitPane)mainPanel).setDividerSize(6);
            if (!Config.isRaspberryPi()) mainPanel.setOpaque(false);
        }
        
        contentPane.add(mainPanel, BorderLayout.CENTER);
        swingNode.setContent(contentPane);
        contentPane.validate();
        Dimension preferredSize = contentPane.getPreferredSize();
        Platform.runLater(() -> {
            fxContent.setPrefWidth(preferredSize.getWidth());
            fxContent.setPrefHeight(preferredSize.getHeight());
            readyToShow.set(true);
        });
    }

    private ListView<VarDisplayInfo> makeVarListView()
    {
        ListView<VarDisplayInfo> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setCellFactory(lv -> {
            return new VarDisplayCell();
        });
        return listView;
    }

    /**
     * Create the debugger's menubar, all menus and items.
     */
    private JMenuBar makeMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(Config.getString("terminal.options"));

        
        if (!Config.isGreenfoot()) {
            systemThreadItem = new JCheckBoxMenuItem(new HideSystemThreadAction());
            systemThreadItem.setSelected(true);
            menu.add(systemThreadItem);
            menu.add(new JSeparator());
        }
        //MOEFX
        //debugger.hideSystemThreads(true);

        //MOEFX
        //menu.add(new CloseAction());

        menubar.add(menu);
        return menubar;
    }
    
    @OnThread(Tag.Any)
    public void show()
    {
        JavaFXUtil.runNowOrLater(() -> {
            if (readyToShow.get())
            {
                window.show();
                window.toFront();
            }
            else
                JavaFXUtil.addSelfRemovingListener(readyToShow, t -> show());
        });
    }

    @OnThread(Tag.Any)
    public void hide()
    {
        JavaFXUtil.runNowOrLater(() -> window.hide());
    }
    
    /**
     * Create a text & image button and add it to a panel.
     * 
     * @param action
     *            The assosciated Action (with text, icon, action, etc).
     * @param panel
     *            The panel to add the button to.
     */
    private Button makeButton(FXAbstractAction action)
    {
        Button button = action.makeButton();
        //MOEFX
        //button.setVerticalTextPosition(SwingConstants.BOTTOM);
        //button.setHorizontalTextPosition(SwingConstants.CENTER);
        //button.setEnabled(false);
        return button;
    }

    @OnThread(Tag.Any)
    public boolean isVisible()
    {
        return visible.get();
    }

    @OnThread(Tag.Any)
    public void toggleVisible()
    {
        Platform.runLater(() -> {
            if (window.isShowing())
                window.hide();
            else
                show();
        });
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
            super(haltButtonText, Config.getFixedImageAsFXImage("stop.gif"));
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
            super(stepButtonText, Config.getFixedImageAsFXImage("step.gif"));
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
            Platform.runLater(() -> project.updateInspectors());
        }
    }
    
    /**
     * Action to "step into" the code.
     */
    private class StepIntoAction extends FXAbstractAction
    {
        public StepIntoAction()
        {
            super(stepIntoButtonText, Config.getFixedImageAsFXImage("step_into.gif"));
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
    
    /**
     * Action to continue a halted thread. 
     */
    private class ContinueAction extends FXAbstractAction
    {
        public ContinueAction()
        {
            super(continueButtonText, Config.getFixedImageAsFXImage("continue.gif"));
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
            super(terminateButtonText, Config.getFixedImageAsFXImage("terminate.gif"));
        }
        
        public void actionPerformed()
        {
            try {
                // throws an illegal state exception
                // if we press this whilst we are already
                // restarting the remote VM
                project.restartVM();
                DataCollector.debuggerTerminate(project);
            }
            catch (IllegalStateException ise) { }
        }
    }
    
    /**
     * Action to enable/disable hiding of system threads. All this action
     * actually does is toggle an internal flag.
     */
    private class HideSystemThreadAction extends AbstractAction
    {
        public HideSystemThreadAction()
        {
            super(Config.getString("debugger.hideSystemThreads"));
        }

        public void actionPerformed(ActionEvent e) {
            //MOEFX
            //debugger.hideSystemThreads(systemThreadItem.isSelected());
        }
    }


    private class VarDisplayCell extends javafx.scene.control.ListCell<VarDisplayInfo>
    {
        private final Label access = new Label();
        private final Label type = new Label();
        private final Label name = new Label();
        private final Label value = new Label();
        private final BooleanProperty nonEmpty = new SimpleBooleanProperty();

        public VarDisplayCell()
        {
            HBox hBox = new HBox(access, type, name, new Label("="), this.value);
            hBox.visibleProperty().bind(nonEmpty);
            hBox.styleProperty().bind(PrefMgr.getEditorFontCSS(false));
            JavaFXUtil.addStyleClass(hBox, "debugger-var-cell");
            JavaFXUtil.addStyleClass(access, "debugger-var-access");
            JavaFXUtil.addStyleClass(type, "debugger-var-type");
            JavaFXUtil.addStyleClass(name, "debugger-var-name");
            JavaFXUtil.addStyleClass(value, "debugger-var-value");
            setGraphic(hBox);
        }

        @Override
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
            }
            else
            {
                access.setText(item.getAccess());
                type.setText(item.getType());
                name.setText(item.getName());
                value.setText(item.getValue());
            }
        }
    }
}
