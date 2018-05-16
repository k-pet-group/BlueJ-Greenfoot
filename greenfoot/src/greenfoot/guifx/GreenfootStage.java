/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2018  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Boot;
import bluej.Config;
import bluej.Main;
import bluej.collect.DataCollector;
import bluej.collect.GreenfootInterfaceEvent;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.compiler.FXCompileObserver;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.debugmgr.objectbench.ResultWatcherBase;
import bluej.editor.Editor;
import bluej.extensions.SourceType;
import bluej.pkgmgr.AboutDialogTemplate;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageUI;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.ProjectUtils;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaReflective;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UnfocusableScrollPane;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

import greenfoot.core.ProjectManager;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.export.ScenarioSaver;
import greenfoot.guifx.ControlPanel.ControlPanelListener;
import greenfoot.guifx.classes.GClassDiagram;
import greenfoot.guifx.classes.GClassDiagram.GClassType;
import greenfoot.guifx.classes.ImportClassDialog;
import greenfoot.guifx.classes.LocalGClassNode;
import greenfoot.guifx.export.ExportDialog;
import greenfoot.guifx.export.ExportException;
import greenfoot.guifx.images.NewImageClassFrame;
import greenfoot.guifx.images.SelectImageFrame;
import greenfoot.guifx.soundrecorder.SoundRecorderControls;
import greenfoot.record.GreenfootRecorder;
import greenfoot.util.GreenfootUtil;
import greenfoot.vmcomm.GreenfootDebugHandler;
import greenfoot.vmcomm.GreenfootDebugHandler.SimulationStateListener;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.*;

import static bluej.pkgmgr.target.ClassTarget.MENU_STYLE_INBUILT;
import static greenfoot.vmcomm.Command.*;

/**
 * Greenfoot's main window: a JavaFX replacement for GreenfootFrame which lives on the server VM.
 */
@OnThread(Tag.FXPlatform)
public class GreenfootStage extends Stage implements BlueJEventListener, FXCompileObserver,
        SimulationStateListener, PackageUI, ControlPanelListener, ScenarioSaver
{
    private static int numberOfOpenProjects = 0;
    private static List<GreenfootStage> stages = new ArrayList<>();

    private Project project;
    private BooleanProperty hasNoProject = new SimpleBooleanProperty(true);
    // The glass pane used to show a new actor while it is being placed:
    private final Pane glassPane;
    // Details of the new actor while it is being placed (null otherwise):
    private final ObjectProperty<NewActor> newActorProperty = new SimpleObjectProperty<>(null);
    private final WorldDisplay worldDisplay;
    private final GClassDiagram classDiagram;
    // The currently-showing context menu, or null if none
    private ContextMenu contextMenu;
    // Last mouse position, in scene coordinates:
    private Point2D lastMousePosInScene = new Point2D(0, 0);

    // The message shown behind the world (blank content if none): 
    private final Label backgroundMessage;
    // A property tracking whether the world is visible (if false, there should be 
    // a background message set in backgroundMessage)
    private final BooleanProperty worldVisible = new SimpleBooleanProperty(false);
    private boolean worldInstantiationError = false;

    // The last speed value set by the user altering it in interface (rather than programmatically):
    private int lastUserSetSpeed;
    // Used to stop an infinite loop if we set the speed slider in response to a programmatic change: 
    private boolean settingSpeedFromSimulation = false;
    
    private final ExecutionTwirler executionTwirler;
    // When did the user code last start executing?
    private long lastExecStartTime;
    private final ControlPanel controlPanel;
    
    private DebuggerObject draggedActor;

    public static enum State
    {
        RUNNING, RUNNING_REQUESTED_PAUSE, PAUSED, PAUSED_REQUESTED_ACT_OR_RUN, UNCOMPILED, NO_PROJECT;
    }
    
    private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<>(State.NO_PROJECT);
    private boolean atBreakpoint = false;

    // Details for pick requests that we have sent to the debug VM:
    private static enum PickType
    {
        LEFT_CLICK, CONTEXT_MENU, DRAG;
    }

    // The next free pick ID that we will use
    private int nextPickId = 1;
    // The most recent pick ID that we are waiting on from the debug VM.
    private int curPickRequest;
    // The point at which the most recent pick happened.
    private Point2D curPickPoint;
    // If true, most recent pick was for right-click menu.  If false, was for a left-click drag.
    private PickType curPickType;
    // The current drag request ID, or -1 if not currently dragging:
    private int curDragRequest;

    private GreenfootRecorder saveTheWorldRecorder;
    private final SoundRecorderControls soundRecorder;
    private GreenfootDebugHandler debugHandler;
    private final Menu recentProjectsMenu = new Menu(Config.getString("menu.openRecent"));
    private final SimpleBooleanProperty showingDebugger = new SimpleBooleanProperty(false);

    // The current active world. This will be set either by properties when opening
    // a scenario, or by calling a world constructor through the context menu.
    // This will NOT change if the world changes by user's code.
    private ClassTarget currentWorld;

    // World image
    private WritableImage worldImg;

    // The scenario information that usually shipped with it when uploading
    // to the gallery. We should maintain a reference to it and make sure
    // that its properties are written to the project properties file always.
    // This change is needed as we now wipe the properties file each time
    // before saving to avoid stacking unneeded properties hanging forever.
    private ScenarioInfo scenarioInfo;

    /**
     * Details for a new actor being added to the world, after you have made it
     * but before it is ready to be placed.
     */
    private static class NewActor
    {
        // The actual image node (will be a child of glassPane)
        private final Region previewNode;
        // Property tracking whether current location is valid or not
        private final BooleanProperty cannotDrop = new SimpleBooleanProperty(true);
        // The execution event that created the actor, if the actor has already been created:
        private final ExecutionEvent creationEvent;
        // The type name if the actor has not been constructed:
        private final String typeName;

        private Region makePreviewNode(ImageView imageView)
        {
            ImageView cannotDropIcon = new ImageView(this.getClass().getClassLoader().getResource("noParking.png").toExternalForm());
            cannotDropIcon.visibleProperty().bind(cannotDrop);
            StackPane.setAlignment(cannotDropIcon, Pos.TOP_RIGHT);
            StackPane stackPane = new StackPane(imageView, cannotDropIcon);
            stackPane.setEffect(new DropShadow(10.0, 3.0, 3.0, Color.BLACK));
            return stackPane;
        }

        public NewActor(ImageView imageView, ExecutionEvent creationEvent)
        {
            this.previewNode = makePreviewNode(imageView);
            this.creationEvent = creationEvent;
            this.typeName = null;
        }

        public NewActor(ImageView imageView, String typeName)
        {
            this.previewNode = makePreviewNode(imageView);
            this.creationEvent = null;
            this.typeName = typeName;
        }
    }

    
    /**
     * Creates a GreenfootStage for the given project with given debug interface handler.
     * @param project   the project to show (may be null for none)
     * @param greenfootDebugHandler   the debug handler interface
     */
    private GreenfootStage(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        stages.add(this);
        
        BlueJEvent.addListener(this);
        soundRecorder = new SoundRecorderControls(project);

        executionTwirler = new ExecutionTwirler(project, greenfootDebugHandler);
        controlPanel = new ControlPanel(this, executionTwirler);

        backgroundMessage = new Label();
        backgroundMessage.getStyleClass().add("background-message");
        
        worldDisplay = new WorldDisplay();
        
        classDiagram = new GClassDiagram(this);
        ScrollPane classDiagramScroll = new UnfocusableScrollPane(classDiagram);
        JavaFXUtil.expandScrollPaneContent(classDiagramScroll);
        classDiagramScroll.getStyleClass().add("gclass-diagram-scroll");
        classDiagramScroll.setMinViewportWidth(150.0);
        classDiagramScroll.setMinViewportHeight(200.0);

        ScrollPane worldViewScroll = new UnfocusableScrollPane(worldDisplay);
        worldViewScroll.getStyleClass().add("world-display-scroll");
        JavaFXUtil.expandScrollPaneContent(worldViewScroll);
        worldViewScroll.visibleProperty().bind(worldVisible);
        StackPane worldPane = new StackPane(backgroundMessage, worldViewScroll);
        GreenfootStageContentPane contentPane = new GreenfootStageContentPane(worldPane, classDiagramScroll, controlPanel);
        BorderPane root = new BorderPane(contentPane, makeMenu(), null, null, null);
        glassPane = new Pane();
        glassPane.setMouseTransparent(true);
        StackPane stackPane = new StackPane(root, glassPane);
        setupMouseForPlacingNewActor(stackPane);
        Scene scene = new Scene(stackPane);
        Config.addGreenfootStylesheets(scene);
        Config.addPMFStylesheets(scene);
        setScene(scene);
        
        if (project != null)
        {
            showProject(project, greenfootDebugHandler);
        }
        // Do this whether we have a project or not:
        updateBackgroundMessage();
                
        setOnCloseRequest((e) -> {
            doClose(false);
        });
        
        JavaFXUtil.addChangeListenerPlatform(stateProperty, this::updateGUIState);
        JavaFXUtil.addChangeListenerPlatform(focusedProperty(), focused -> {
            if (focused && project != null)
            {
                DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WINDOW_ACTIVATED);
                for (ClassTarget classTarget : project.getUnnamedPackage().getClassTargets())
                {
                    Editor editor = classTarget.getEditorIfOpen();
                    if (editor != null)
                    {
                        editor.cancelFreshState();
                    }
                }
                if (worldDisplay.isGreyedOut() && stateProperty.get() != State.UNCOMPILED)
                {
                    doReset();
                }
            }
        });
        
        /* Uncomment this to use ScenicView temporarily during development (use reflection to avoid needing to mess with Ant classpath)
        try
        {
            getClass().getClassLoader().loadClass("org.scenicview.ScenicView").getMethod("show", Scene.class).invoke(null, scene);
        }
        catch (Exception e)
        {
            Debug.reportError(e);
        }*/
    }
    
    /**
     * Show a particular project in this window.
     * @param project   The project to display
     * @param greenfootDebugHandler   The debug handler for this project
     */
    private void showProject(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        setTitle("Greenfoot: " + project.getProjectName());
        
        this.project = project;
        this.saveTheWorldRecorder = greenfootDebugHandler.getRecorder();
        project.getPackage("").setUI(this);
        this.debugHandler = greenfootDebugHandler;
        hasNoProject.set(false);
        numberOfOpenProjects++;

        project.getUnnamedPackage().addCompileObserver(this);
        greenfootDebugHandler.setPickListener(this::pickResults);
        greenfootDebugHandler.setSimulationListener(this);
        project.setClassIconFetcherDelegate(classDiagram);
        showingDebugger.bindBidirectional(project.debuggerShowing());
        
        classDiagram.setProject(project);
        soundRecorder.setProject(project);
        executionTwirler.setProject(project, greenfootDebugHandler);

        setupWorldDrawingAndEvents();
        loadAndMirrorProperties();
        Properties lastSavedProperties = project.getUnnamedPackage().getLastSavedProperties();
        String lastInstantiatedWorldName = lastSavedProperties.getProperty("world.lastInstantiated");
        // We send a reset to make a new world after the project properties have been sent across:
        if (lastInstantiatedWorldName != null)
        {
            debugHandler.getVmComms().instantiateWorld(lastInstantiatedWorldName);
        }
        currentWorld = lastInstantiatedWorldName != null
                ? (ClassTarget) project.getTarget(lastInstantiatedWorldName)
                : null;

        JavaFXUtil.addChangeListenerPlatform(worldVisible, b -> updateBackgroundMessage());

        scenarioInfo = new ScenarioInfo(lastSavedProperties);
        String xPosition = lastSavedProperties.getProperty("xPosition");
        String yPosition = lastSavedProperties.getProperty("yPosition");
        if (xPosition != null)
        {
            getStage().setX(Double.valueOf(xPosition));
        }
        if (yPosition != null)
        {
            getStage().setY(Double.valueOf(yPosition));
        }

        String width = lastSavedProperties.getProperty("width");
        String height = lastSavedProperties.getProperty("height");
        if (width != null)
        {
            getStage().setWidth(Double.valueOf(width));
        }
        if (height != null)
        {
            getStage().setHeight(Double.valueOf(height));
        }
    }

    /**
     * Updates the message that is shown in place of the world when there is not a world
     * showing, e.g. no project open, no world classes, or problem initialising the world.
     */
    private void updateBackgroundMessage()
    {
        final String message;
        if (stateProperty.get() == State.UNCOMPILED && !classDiagram.hasUserWorld())
        {
            // May be totally blank project (in which case state remains as UNCOMPILED),
            // hint to the user to create a world:
            message = Config.getString("centrePanel.message.createWorldClass");
        }
        else if (worldVisible.get())
        {
            message = "";
        }
        else
        {
            if (stateProperty.get() == State.NO_PROJECT)
            {
                message = Config.getString("centrePanel.message.openScenario");
            }
            else if (stateProperty.get() == State.PAUSED)
            {
                // If we are paused, but no world is visible, the user either
                // needs to instantiate a world (if they have one) or create a world class
                if (worldInstantiationError)
                {
                    message = Config.getString("centrePanel.message.error1") + " " + Config.getString("centrePanel.message.error2");
                }
                else if (classDiagram.hasInstantiatableWorld())
                {
                    message = Config.getString("centrePanel.message.createWorldObject");
                }
                else if (classDiagram.hasUserWorld())
                {
                    message = Config.getString("centrePanel.message.missingWorldConstructor1")
                        + " " + Config.getString("centrePanel.message.missingWorldConstructor2");
                }
                else
                {
                    message = Config.getString("centrePanel.message.createWorldClass");
                }
            }
            else
            {
                message = "";
            }
        }
        backgroundMessage.setText(message);
    }

    /**
     * Make a stage suitable for displaying a project.
     * 
     * @param project   The project to display
     * @param greenfootDebugHandler   The debug handler for this project
     * @return  the stage (a new stage, or a previously empty stage with the project now displayed)
     */
    public static GreenfootStage makeStage(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        if (stages.size() == 1 && stages.get(0).project == null)
        {
            stages.get(0).showProject(project, greenfootDebugHandler);
            return stages.get(0);
        }
        else
        {
            return new GreenfootStage(project, greenfootDebugHandler);
        }
    }

    /**
     * Perform a reset.  This is done by sending a discard-world command, and
     * setting a flag noting that we want to send an instantiate-world command
     * once the discard-world command has taken effect. It also makes sure that
     * the simulation thread is resumed.
     */
    @OnThread(Tag.FXPlatform)
    public void doReset()
    {
        controlPanel.disableControlPanelButtons(true);
        DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WORLD_RESET);
        debugHandler.getVmComms().discardWorld();
        if (currentWorld != null)
        {
            debugHandler.getVmComms().instantiateWorld(currentWorld.getQualifiedName());
        }
        stateProperty.set(State.PAUSED);
        debugHandler.simulationThreadResumeOnResetClick();
    }

    /**
     * Send package properties to the other VM. (This allows the actor/world classes to determine their
     * image).
     */
    private void loadAndMirrorProperties()
    {
        Properties props = project.getUnnamedPackage().getLastSavedProperties();
        for (String key : props.stringPropertyNames()) 
        {
            String value = props.getProperty(key);
            debugHandler.getVmComms().sendProperty(key, value);
        }
        // Load the speed into our slider and inform debug VM:
        int speed = 50;
        try
        {
            String speedString = project.getUnnamedPackage().getLastSavedProperties().getProperty("simulation.speed");
            if (speedString != null)
            {
                speed = Integer.valueOf(speedString);
            }
        }
        catch (NumberFormatException e)
        {
            // Just leave it as the default 50 if there is a problem
        }
        controlPanel.setSpeed(speed);
        debugHandler.getVmComms().setSimulationSpeed(speed);
    }

    /**
     * Allow the user to choose a scenario to open, and open it.
     */
    private void doOpenScenario()
    {
        File choice = FileUtility.getOpenProjectFX(this);
        if (choice != null)
        {
            doOpenScenario(choice);
        }
    }

    /**
     * Allow the user to choose a gfar file and open its scenario
     * after extracting the gfar file into a new folder.
     */
    public void doOpenGfarScenario()
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Greenfoot Scenarios (*.gfar)", "*.gfar"));
        chooser.setInitialDirectory(PrefMgr.getProjectDirectory());

        File chosen = chooser.showOpenDialog(this.getStage());
        if (chosen != null)
        {
            openArchive(chosen, getStage());
        }
    }

    /**
     * Open a gfar archive file as a Greenfoot project.
     * The file contents are extracted, the containing directory
     * is then converted into a Greenfoot project, and opened.
     * Opening already extracted gfar will show an error message.
     * @param archive The chosen archived file.
     * @param window  The parent javafx window to show error message if needed. It could be null.
     * @return A true value if the archived is open successfully or false otherwise.
     */
    public static boolean openArchive(File archive, Window window)
    {
        // Determine the output path.
        File oPath = Utility.maybeExtractArchive(archive, () -> window);

        if (oPath != null && Project.isProject(oPath.getPath()))
        {
            ProjectManager.instance().launchProject(Project.openProject(oPath.toString()));
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Open the specified scenario. Display an error dialog on failure.
     * 
     * @param projPath  path of the scenario to open.
     */
    private void doOpenScenario(File projPath)
    {
        Project p = Project.openProject(projPath.getAbsolutePath());
        if (p != null)
        {
            GreenfootStage stage = findStageForProject(p);
            if (stage != null)
            {
                // If already open, bring the window to the foreground:
                stage.toFront();
            }
            else
            {
                ProjectManager.instance().launchProject(p);
            }
        }
        else
        {
            // display error dialog
            DialogManager.showErrorFX(this, "could-not-open-project");
        }
    }

    /**
     * Update the recent projects menu with all recently-opened projects.
     */
    private void updateRecentProjects()
    {
        recentProjectsMenu.getItems().clear();

        List<String> projects = PrefMgr.getRecentProjects();
        for (String projectToOpen : projects)
        {
            MenuItem item = new MenuItem(projectToOpen);
            recentProjectsMenu.getItems().add(item);
            item.setOnAction(e -> {
                doOpenScenario(new File(projectToOpen));
            });
        }
        
        if (projects.isEmpty())
        {
            MenuItem noRecentProjects = new MenuItem(Config.getString("menu.noRecentProjects"));
            noRecentProjects.setDisable(true);
            recentProjectsMenu.getItems().add(noRecentProjects);
        }
    }
    
    /**
     * Close the scenario that this stage is showing.
     * @param keepLast  if true, don't close the last stage; leave it open without a scenario. If
     *                  false, quit BlueJ when the last stage is closed.
     */
    private void doClose(boolean keepLast)
    {
        if (numberOfOpenProjects <= 1 && ! keepLast)
        {
            // We quit with the current scenario still open, so that it will be saved to the
            // projects-for-re-opening list and re-opened when Greenfoot is next started:
            close();
            Main.doQuit();
            return;
        }

        // Remove inspectors, terminal, etc:
        if (project != null)
        {
            doSave();
            Project.cleanUp(project);
            project.getPackage("").closeAllEditors();
            numberOfOpenProjects--;
        }

        if (numberOfOpenProjects == 0)
        {
            // Keep this stage open, but show it as empty
            removeScenarioDetails();
        }
        else
        {
            stages.remove(this);
            BlueJEvent.removeListener(this);
            close();
        }
    }
    
    /**
     * Remove scenario details, making the stage empty.
     */
    private void removeScenarioDetails()
    {
        if (project != null)
        {
            showingDebugger.unbindBidirectional(project.debuggerShowing());
            project = null;
        }
        hasNoProject.set(true);
        worldDisplay.setImage(null);
        worldVisible.set(false);
        classDiagram.setProject(null);
        // Setting the state will update background message:
        stateProperty.set(State.NO_PROJECT);
    }

    /**
     * Save the project (all editors and all project information).
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doSave()
    {
        try
        {
            // Collect the various properties to be written out:
            Properties p = project.getProjectPropertiesCopy();
            p.setProperty("simulation.speed", Integer.toString(lastUserSetSpeed));
            p.put("width", Integer.toString((int) this.getWidth()));
            p.put("height", Integer.toString((int) this.getHeight()));
            p.put("xPosition", Integer.toString((int) this.getX()));
            p.put("yPosition", Integer.toString((int) this.getY()));
            p.put("version", Boot.GREENFOOT_API_VERSION);
            if (currentWorld != null)
            {
                p.put("world.lastInstantiated", currentWorld.getQualifiedName());
            }
            project.saveEditorLocations(p);
            classDiagram.save(p);
            scenarioInfo.store(p);

            // Actually write out the properties to disk:
            project.getUnnamedPackage().save(p);
            
            // Save editor contents, etc:
            project.getImportScanner().saveCachedImports();
            project.saveAllEditors();
        }
        catch (IOException ioe)
        {
            // The exception is logged earlier, so we won't bother logging again.
            // However, alert the user:
            DialogManager.showMessageFX(this, "error-saving-project");
        }
    }

    /**
     * Prompt for a location, save the scenario to the chosen location, and re-open the scenario
     * from its new location.
     */
    public void doSaveAs()
    {
        File choice = FileUtility.getSaveProjectFX(this, "project.saveAs.title");
        if (choice == null)
        {
            return;
        }
        
        if (! ProjectUtils.saveProjectCopy(project, choice, this))
        {
            return;
        }
        
        doClose(true);
        
        Project p = Project.openProject(choice.getAbsolutePath());
        if (p == null) {
            // This shouldn't happen, but log an error just in case:
            Debug.reportError("Project save-as succeeded, but new project could not be opened");
            return;
        }
        
        ProjectManager.instance().launchProject(p);
    }

    /**
     * Show a dialog to Export a project. It can be exported to many formats,
     * which the user will pick from. This is only possible in case the project
     * is compiled, otherwise, show an error dialog.
     */
    private void doShare()
    {
        // TODO These two conditions are good enough currently for all cases, however
        // more testing is needed and maybe changing to direct test for the project
        // compilation status.
        if (worldDisplay.isGreyedOut() || stateProperty.get() == State.UNCOMPILED)
        {
            DialogManager.showErrorFX(this, "export-compile-not-compiled");
        }
        else
        {
            try
            {
                new ExportDialog(this, project, this, scenarioInfo, currentWorld,
                        worldDisplay.snapshot(null, null)).showAndWait();
            }
            catch (ExportException e)
            {
                DialogManager.showErrorTextFX(this, e.getMessage());
            }
        }
    }

    /**
     * Perform a single act step, if paused, by adding to the list of pending commands.
     */
    public void act()
    {
        if (stateProperty.get() == State.PAUSED)
        {
            DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WORLD_ACT);
            debugHandler.getVmComms().act();
            stateProperty.set(State.PAUSED_REQUESTED_ACT_OR_RUN);
        }
    }
    
    /**
     * Run or pause the simulation (depending on current state).
     */
    public void doRunPause()
    {
        if (stateProperty.get() == State.PAUSED)
        {
            DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WORLD_RUN);
            debugHandler.getVmComms().runSimulation();
            stateProperty.set(State.PAUSED_REQUESTED_ACT_OR_RUN);
        }
        else if (stateProperty.get() == State.RUNNING)
        {
            DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WORLD_PAUSE);
            debugHandler.getVmComms().pauseSimulation();
            stateProperty.set(State.RUNNING_REQUESTED_PAUSE);
        }
    }

    /**
     * Opens the given page of the Greenfoot API documentation in a web browser.
     * 
     * @param page   name of the page relative to the root of the API doc.
     */
    public void showApiDoc(String page)
    {
        try {
            String customUrl = Utility.getGreenfootApiDocURL(page);
            if (customUrl != null)
            {
                openWebBrowser(customUrl);
            }
        }
        catch (IOException ioe) {
            DialogManager.showErrorWithTextFX(this, "cannot-read-apidoc", ioe.getLocalizedMessage());
        }
    }
    
    /**
     * Display a URL in a web browser.
     */
    private static void openWebBrowser(String url)
    {
        EventQueue.invokeLater(() -> {
            boolean success = Utility.openWebBrowser(url);
            if (! success)
            {
                Platform.runLater(() -> DialogManager.showErrorFX(null, "cannot-open-browser"));
            }
        });
    }
    
    /**
     * Show a dialog with copyright information.
     */
    private void showCopyright()
    {
        String text = Config.getString("menu.help.copyright.line1") + "\n" +
                Config.getString("menu.help.copyright.line2") + "\n" +
                Config.getString("menu.help.copyright.line3") + "\n" +
                Config.getString("menu.help.copyright.line4");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setTitle(Config.getString("menu.help.copyright.title"));
        alert.initOwner(this);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setHeaderText(Config.getString("menu.help.copyright.line0"));
        alert.showAndWait();
    }

    /**
     * Make the menu bar for the whole window.
     */
    private MenuBar makeMenu()
    {
        recentProjectsMenu.setOnShowing(e -> updateRecentProjects());
        updateRecentProjects();
        
        Menu scenarioMenu = new Menu(Config.getString("menu.scenario"), null,
                JavaFXUtil.makeMenuItem("stride.new.project",
                        new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                        () -> {doNewProject(SourceType.Stride);}, null
                    ),
                    JavaFXUtil.makeMenuItem("java.new.project",
                        new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN),
                        () -> {doNewProject(SourceType.Java);}, null
                    ),
                    JavaFXUtil.makeMenuItem("open.project",
                        new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                        this::doOpenScenario, null
                    ),
                    recentProjectsMenu,
                    JavaFXUtil.makeMenuItem("open.gfar.project",
                        null, this::doOpenGfarScenario, null
                    ),
                    JavaFXUtil.makeMenuItem("project.close",
                        new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
                        () -> { doClose(true); }, hasNoProject
                    ),
                    JavaFXUtil.makeMenuItem("project.save",
                        new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                        this::doSave, hasNoProject
                    ),
                    JavaFXUtil.makeMenuItem("project.saveAs",
                        null,
                        this::doSaveAs, hasNoProject
                    ),
                    new SeparatorMenuItem(),
                    JavaFXUtil.makeMenuItem("show.readme",
                        null,
                        this::openReadme, hasNoProject
                    ),
                    JavaFXUtil.makeMenuItem("export.project",
                        new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
                        this::doShare, hasNoProject
                    )
                );

        if (! Config.isMacOS()) {
            scenarioMenu.getItems().add(new SeparatorMenuItem());
            scenarioMenu.getItems().add(JavaFXUtil.makeMenuItem("greenfoot.quit",
                    new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN),
                    () -> Main.wantToQuit(), null)
                );
        }

        Menu toolsMenu = new Menu(Config.getString("menu.tools"), null);
        toolsMenu.getItems().addAll(
                JavaFXUtil.makeMenuItem("menu.tools.generateDoc",new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                        this::generateDocumentation, hasNoProject),
                JavaFXUtil.makeCheckMenuItem(Config.getString("menu.soundRecorder"),
                        soundRecorder.getShowingProperty(),
                        new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN),
                        this::toggleSoundRecorder),
                JavaFXUtil.makeCheckMenuItem(Config.getString("menu.debugger"),
                        showingDebugger,
                        new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN)),
                JavaFXUtil.makeMenuItem("set.player",
                        new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                        this::setPlayer, hasNoProject)
        );

        if (! Config.isMacOS())
        {
            toolsMenu.getItems().add(JavaFXUtil.makeMenuItem("greenfoot.preferences",
                    new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN),
                    () -> showPreferences(), null));
        }

        Menu helpMenu = new Menu(Config.getString("menu.help"), null);
        if (! Config.isMacOS())
        {
            helpMenu.getItems().add(JavaFXUtil.makeMenuItem("menu.help.about", null, () -> aboutGreenfoot(this), null));
        }
        helpMenu.getItems().addAll(
                JavaFXUtil.makeMenuItem("greenfoot.copyright", null, this::showCopyright, null),
                new SeparatorMenuItem(),
                JavaFXUtil.makeMenuItem("menu.help.classDoc", null, () -> showApiDoc("index.html"), null),
                JavaFXUtil.makeMenuItem("menu.help.javadoc", null,
                        () -> openWebBrowser(Config.getPropString("greenfoot.url.javaStdLib")), null),
                new SeparatorMenuItem(),
                JavaFXUtil.makeMenuItem("menu.help.tutorial", null,
                        () -> openWebBrowser(Config.getPropString("greenfoot.url.tutorial")), null),
                JavaFXUtil.makeMenuItem("menu.help.website", null,
                        () -> openWebBrowser(Config.getPropString("greenfoot.url.greenfoot")), null),
                JavaFXUtil.makeMenuItem("menu.help.moreScenarios", null,
                        () -> openWebBrowser(Config.getPropString("greenfoot.url.scenarios")), null),
                new SeparatorMenuItem(),
                JavaFXUtil.makeMenuItem(Config.getPropString("greenfoot.gameserver.name"),
                        () -> openWebBrowser(Config.getPropString("greenfoot.gameserver.address")), null),
                JavaFXUtil.makeMenuItem("menu.help.discuss", null,
                        () -> openWebBrowser(Config.getPropString("greenfoot.url.discuss")), null)
        );
        
        return new MenuBar(
            scenarioMenu,
            new Menu(Config.getString("menu.edit"), null,
                JavaFXUtil.makeMenuItem("new.other.class", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                            () -> newNonImageClass(project.getUnnamedPackage(), null), hasNoProject),
                JavaFXUtil.makeMenuItem("import.action",
                        new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), () -> doImportClass(),
                        hasNoProject)
            ),
            new Menu(Config.getString("menu.controls"), null, controlPanel.makeMenuItems().toArray(new MenuItem[0])
            ),
            toolsMenu,
            helpMenu
        );
    }

    /**
     * Show/hide the sound soundRecorder.
     *
     * @param showing if true show the soundRecorder, hide for false.
     */
    private void toggleSoundRecorder(Boolean showing)
    {
        if (showing)
        {
            soundRecorder.show();
        }
        else
        {
            soundRecorder.close();
        }
    }

    /**
     * Generates the Documentation for the current scenario
     */
    private void generateDocumentation()
    {
        String message = project.generateDocumentation();
        if (message.length() != 0) {
            DialogManager.showTextFX(this, message);
        }
    }

    /**
     * Opens a set player dialog so the user can enter a player name
     * to be stored in the project's properties.
     */
    private void setPlayer()
    {
        SetPlayerDialog dlg = new SetPlayerDialog(this, Config.getPropString("greenfoot.player.name", "Player1"));
        dlg.showAndWait().ifPresent(name -> Config.putPropString("greenfoot.player.name", name));
    }

    /**
     * Shows preferences Dialog
     */
    public static void showPreferences()
    {
        PrefMgrDialog.showDialog(null);
    }

    /**
     * Update scenario controls (act, run/pause, reset etc) according to scenario state.
     */
    private void updateGUIState(State newState)
    {
        controlPanel.updateState(newState, atBreakpoint);
        updateBackgroundMessage();
    }

    /**
     * Setup mouse listeners for showing a new actor underneath the mouse cursor
     */
    private void setupMouseForPlacingNewActor(StackPane stackPane)
    {
        stackPane.setOnMouseMoved(e -> {
            lastMousePosInScene = new Point2D(e.getSceneX(), e.getSceneY());
            if (newActorProperty.get() != null)
            {
                // We use e.getX/getY here, which is already local to StackPane:
                // TranslateX/Y seems to have a bit less lag than LayoutX/Y:
                newActorProperty.get().previewNode.setTranslateX(e.getX() - newActorProperty.get().previewNode.getWidth() / 2.0);
                newActorProperty.get().previewNode.setTranslateY(e.getY() - newActorProperty.get().previewNode.getHeight() / 2.0);

                newActorProperty.get().cannotDrop.set(!worldDisplay.worldContains(worldDisplay.sceneToWorld(lastMousePosInScene)));
            }
        });
        stackPane.setOnMouseClicked(e -> {
            lastMousePosInScene = new Point2D(e.getSceneX(), e.getSceneY());
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && newActorProperty.get() != null)
            {
                Point2D dest = worldDisplay.sceneToWorld(lastMousePosInScene);
                if (worldDisplay.worldContains(dest))
                {
                    // Bit hacky to pass positions as strings, but mirroring the values as integers
                    // would have taken a lot of code changes to route through to VMReference:
                    DebuggerObject xObject = project.getDebugger().getMirror("" + (int) dest.getX());
                    DebuggerObject yObject = project.getDebugger().getMirror("" + (int) dest.getY());
                    DebuggerObject actor = null;
                    ExecutionEvent executionEvent = newActorProperty.get().creationEvent;
                    if (executionEvent != null)
                    {
                        // Place the already-constructed actor:
                        actor = executionEvent.getResultObject();
                        // Can't place same instance twice:
                        newActorProperty.set(null);
                        saveTheWorldRecorder.createActor(executionEvent.getResultObject(), executionEvent.getParameters(), executionEvent.getSignature());
                    }
                    else
                    {
                        // Must be shift-clicking; will need to make a new instance:
                        DebuggerResult result = project.getDebugger().instantiateClass(newActorProperty.get().typeName).get();
                        if (result.getResultObject() != null)
                        {
                            actor = result.getResultObject();
                            saveTheWorldRecorder.createActor(actor, new String[0], new JavaType[0]);
                        }
                    }
                    if (actor != null)
                    {
                        Point2D cell = pixelToCellCoordinates(dest);
                        saveTheWorldRecorder.addActorToWorld(actor, (int)cell.getX(), (int)cell.getY());

                        project.getDebugger().instantiateClass(
                                "greenfoot.core.AddToWorldHelper",
                                new String[]{"java.lang.Object", "java.lang.String", "java.lang.String"},
                                new DebuggerObject[]{actor, xObject, yObject});
                    }

                }
            }
        });
        newActorProperty.addListener(new ChangeListener<NewActor>()
        {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends NewActor> prop, NewActor oldVal, NewActor newVal)
            {
                if (oldVal != null)
                {
                    glassPane.getChildren().remove(oldVal.previewNode);
                }

                if (newVal != null)
                {
                    glassPane.getChildren().add(newVal.previewNode);
                    // Need to do a layout to get the correct width and height:
                    glassPane.requestLayout();
                    glassPane.layout();

                    newVal.previewNode.setTranslateX(lastMousePosInScene.getX() - newVal.previewNode.getWidth() / 2.0);
                    newVal.previewNode.setTranslateY(lastMousePosInScene.getY() - newVal.previewNode.getHeight() / 2.0);
                    newVal.cannotDrop.set(!worldDisplay.worldContains(worldDisplay.sceneToWorld(lastMousePosInScene)));
                }
            }
        });
    }

    /**
     * Update our latest mouse position to the given *SCREEN* (not Scene!) position.
     * Used by GClassDiagram to track position even while a context menu is showing.
     */
    public void setLatestMousePosOnScreen(double screenX, double screenY)
    {
        // There is no screenToScene call, so we must go via this convoluted path:
        Point2D rootPoint = getScene().getRoot().screenToLocal(screenX, screenY);
        lastMousePosInScene = getScene().getRoot().localToScene(rootPoint);
    }

    /**
     * Check whether a type has a no-argument constructor.
     */
    private static boolean hasNoArgConstructor(Reflective type)
    {
        return type.getDeclaredConstructors().stream().anyMatch(c -> c.getParamTypes().isEmpty()
                && !Modifier.isPrivate(c.getModifiers()));
    }
    
    /**
     * Convert world pixel coordinates to cell coordinates.
     */
    private Point2D pixelToCellCoordinates(Point2D worldPixels)
    {
        int cellSize = debugHandler.getVmComms().getWorldCellSize();
        if (cellSize == 0)
        {
            return worldPixels;
        }
        
        int xpos = (int)worldPixels.getX() / cellSize;
        int ypos = (int)worldPixels.getY() / cellSize;
        return new Point2D(xpos, ypos);
    }
    
    /**
     * Sets up the drawing of the world from the shared memory buffer, and the writing
     * of keyboard and mouse events back to the buffer.
     */
    private void setupWorldDrawingAndEvents()
    {
        getScene().addEventFilter(KeyEvent.ANY, e -> {
            // Ignore keypresses if we are currently waiting for an ask-answer:
            if (worldDisplay.isAsking())
            {
                return;
            }

            int eventType;
            if (e.getEventType() == KeyEvent.KEY_PRESSED)
            {
                if (e.getCode() == KeyCode.ESCAPE && newActorProperty.get() != null)
                {
                    newActorProperty.set(null);
                    return;
                }

                // We only want fully paused; if they've requested a run, don't allow a shift-click:
                boolean paused = stateProperty.get() == State.PAUSED;
                ClassTarget selectedClassTarget = classDiagram.getSelectedClassTarget();
                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() == null && selectedClassTarget != null && paused)
                {
                    // Holding shift, so show actor preview if it is an actor with no-arg constructor:
                    Reflective type = selectedClassTarget.getTypeReflective();
                    if (type != null
                            && getActorReflective().isAssignableFrom(type)
                            && hasNoArgConstructor(type))
                    {
                        newActorProperty.set(new NewActor(getImageViewForClass(type), selectedClassTarget.getBaseName()));
                    }
                }

                eventType = KEY_DOWN;
            }
            else if (e.getEventType() == KeyEvent.KEY_RELEASED)
            {
                eventType = KEY_UP;

                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() != null && newActorProperty.get().creationEvent == null)
                {
                    newActorProperty.set(null);
                }
            }
            else if (e.getEventType() == KeyEvent.KEY_TYPED)
            {
                eventType = KEY_TYPED;
            }
            else
            {
                return;
            }

            debugHandler.getVmComms().sendKeyEvent(eventType, e.getCode(), e.getText());
            e.consume();
        });

        worldDisplay.setOnContextMenuRequested(e -> {
            boolean paused = stateProperty.get() == State.PAUSED;
            if (paused)
            { 
                Point2D worldPos = worldDisplay.sceneToWorld(new Point2D(e.getSceneX(), e.getSceneY()));
                pickRequest(worldPos.getX(), worldPos.getY(), PickType.CONTEXT_MENU);
            }
        });
        worldDisplay.addEventFilter(MouseEvent.ANY, e -> {
            boolean paused = stateProperty.get() == State.PAUSED;
            Point2D worldPos = worldDisplay.sceneToWorld(new Point2D(e.getSceneX(), e.getSceneY()));
            int eventType;
            if (e.getEventType() == MouseEvent.MOUSE_CLICKED)
            {
                if (e.getButton() == MouseButton.PRIMARY)
                {
                    hideContextMenu();
                    if (paused)
                    {
                        pickRequest(worldPos.getX(), worldPos.getY(), PickType.LEFT_CLICK);
                    }
                }
                eventType = MOUSE_CLICKED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_PRESSED)
            {
                eventType = MOUSE_PRESSED;
                if (paused && e.isPrimaryButtonDown() && !e.isControlDown())
                {
                    // Begin a drag. We do this on MOUSE_PRESSED, because MOUSE_DRAG_DETECTED requires
                    // several pixels of movement, which might take us off the actor if it is small.
                    pickRequest(worldPos.getX(), worldPos.getY(), PickType.DRAG);
                }
            }
            else if (e.getEventType() == MouseEvent.MOUSE_RELEASED)
            {
                eventType = MOUSE_RELEASED;
                // Finish any current drag:
                if (curDragRequest != -1)
                {
                    debugHandler.getVmComms().endDrag(curDragRequest);
                    curDragRequest = -1;
                    Point2D cellPos = pixelToCellCoordinates(worldPos);
                    saveTheWorldRecorder.moveActor(draggedActor, (int)cellPos.getX(), (int)cellPos.getY());
                }
            }
            else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
            {
                // Continue the drag if one is going:
                if (e.getButton() == MouseButton.PRIMARY && paused && curDragRequest != -1)
                {
                    debugHandler.getVmComms().continueDrag(curDragRequest, (int)worldPos.getX(), (int)worldPos.getY());
                }

                eventType = MOUSE_DRAGGED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_MOVED)
            {
                eventType = MOUSE_MOVED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_EXITED)
            {
                eventType = MOUSE_EXITED;
            }
            else
            {
                return;
            }
            MouseButton button = e.getButton();
            if (Config.isMacOS() && button == MouseButton.PRIMARY && e.isControlDown())
            {
                button = MouseButton.SECONDARY;
            }
            
            debugHandler.getVmComms().sendMouseEvent(
                eventType, (int)worldPos.getX(), (int)worldPos.getY(),
                button.ordinal(), e.getClickCount());
        });

        new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                debugHandler.getVmComms().checkIO(GreenfootStage.this);
            }
        }.start();
    }

    /**
     * A world image has been received from the remote VM.
     * 
     * @param width   The image width
     * @param height  The image height
     * @param buffer  The buffer containing the pixel data
     */
    public void receivedWorldImage(int width, int height, IntBuffer buffer)
    {
        // If we are closing a project but receive an image late on, ignore it:
        if (project == null)
        {
            return;
        }
        
        if (worldImg == null || worldImg.getWidth() != width || worldImg.getHeight() != height)
        {
            worldImg = new WritableImage(width == 0 ? 1 : width, height == 0 ? 1 : height);
            // We don't call sizeToScene() directly while holding the file lock because it can cause us to re-enter
            // the animation timer (see commit comment).  So we set this flag to true as a way of queueing up the request:
            JavaFXUtil.runAfterCurrent(() -> sizeToScene());
        }
        worldImg.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(),
                buffer, width);
        worldDisplay.setImage(worldImg);
        worldInstantiationError = false;
        worldVisible.set(true);
        controlPanel.disableControlPanelButtons(false);
    }
    
    /**
     * A "world discarded" notification has been received from the remote VM.
     */
    public void worldDiscarded()
    {
        worldDisplay.greyOutWorld();
    }
    
    /**
     * An "ask" request has been received from the remote VM.
     * 
     * @param promptCodepoints   the codepoints making up the prompt string.
     */
    public void receivedAsk(int[] promptCodepoints)
    {
        // Tell worldDisplay to ask:
        worldDisplay.ensureAsking(new String(promptCodepoints, 0, promptCodepoints.length), (String s) -> {
            debugHandler.getVmComms().sendAnswer(s);
        });
    }
    
    /**
     * Performs a pick request on the debug VM at given coordinates.
     */
    private void pickRequest(double x, double y, PickType pickType)
    {
        curPickType = pickType;
        Debugger debugger = project.getDebugger();
        // Bit hacky to pass positions as strings, but mirroring the values as integers
        // would have taken a lot of code changes to route through to VMReference:
        DebuggerObject xObject = debugger.getMirror("" + (int) x);
        DebuggerObject yObject = debugger.getMirror("" + (int) y);
        int thisPickId = nextPickId++;
        DebuggerObject pickIdObject = debugger.getMirror("" + thisPickId);
        String requestTypeString = pickType == PickType.DRAG ? "drag" : "";
        DebuggerObject requestTypeObject = debugger.getMirror(requestTypeString);
        // One pick at a time only:
        curPickRequest = thisPickId;
        curPickPoint = new Point2D(x, y);
        

        // Need to find out which actors are at the point.  Do this in background thread to
        // avoid blocking the GUI thread:
        Utility.runBackground(() -> 
            debugger.instantiateClass("greenfoot.core.PickActorHelper",
                new String[] {"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"},
                new DebuggerObject[] {xObject, yObject, pickIdObject, requestTypeObject})
        );
        // Once that completes, pickResults(..) will be called.
    }

    /**
     * Callback when a pick has completed (i.e. a request to find actors at given position)
     * @param pickId The ID of the pick has requested
     * @param actors The list of actors found.  May be any size.
     * @param world The world -- only relevant if actors list is empty.
     */
    @OnThread(Tag.Any)
    public void pickResults(int pickId, List<DebuggerObject> actors, DebuggerObject world)
    {
        Platform.runLater(() -> {
            if (curPickRequest != pickId)
            {
                return; // Pick has been cancelled by a more recent pick, so ignore
            }

            if (curPickType == PickType.CONTEXT_MENU)
            {
                // If single actor, show simple context menu:
                if (!actors.isEmpty())
                {
                    // This is a list of menus; if there's only one we'll display
                    // directly in context menu.  If there's more than one, we'll
                    // have a higher level menu to pick between them.
                    List<Menu> actorMenus = new ArrayList<>();
                    for (DebuggerObject actor : actors)
                    {
                        Target target = project.getTarget(actor.getClassName());
                        // Should always be ClassTarget, but check in case:
                        if (target instanceof ClassTarget)
                        {
                            Menu menu = new Menu(actor.getClassName());
                            ObjectWrapper.createMethodMenuItems(menu.getItems(), project.loadClass(actor.getClassName()), new RecordInvoke(actor), "", true);
                            menu.getItems().add(makeInspectMenuItem(actor));
                            //add a listener to the action event on the items in the sub-menu to hide the context menus
                            for (MenuItem menuItem : menu.getItems())
                            {
                                menuItem.addEventHandler(ActionEvent.ACTION, e -> hideContextMenu());
                            }

                            MenuItem removeItem = new MenuItem(Config.getString("world.handlerDelegate.remove"));
                            JavaFXUtil.addStyleClass(removeItem, MENU_STYLE_INBUILT);
                            removeItem.setOnAction(e -> {
                                project.getDebugger().instantiateClass(
                                        "greenfoot.core.RemoveFromWorldHelper",
                                        new String[]{"java.lang.Object"},
                                        new DebuggerObject[]{actor});
                                saveTheWorldRecorder.removeActor(actor);
                            });
                            menu.getItems().add(removeItem);
                            actorMenus.add(menu);
                        }
                    }
                    hideContextMenu();
                    contextMenu = new ContextMenu();
                    contextMenu.setOnHidden(e -> {
                        contextMenu = null;
                    });
                    if (actorMenus.size() == 1)
                    {
                        // No point showing higher-level menu with one item, collapse:
                        contextMenu.getItems().addAll(actorMenus.get(0).getItems());
                    }
                    else
                    {
                        contextMenu.getItems().addAll(actorMenus);
                    }
                    Point2D screenLocation = worldDisplay.worldToScreen(curPickPoint);
                    contextMenu.show(worldDisplay, screenLocation.getX(), screenLocation.getY());
                }
                else
                {
                    Target target = project.getTarget(world.getClassName());
                    // Should always be ClassTarget, but check in case:
                    if (target instanceof ClassTarget)
                    {
                        hideContextMenu();
                        contextMenu = new ContextMenu();
                        contextMenu.setOnHidden(e -> {
                            contextMenu = null;
                        });
                        ObjectWrapper.createMethodMenuItems(contextMenu.getItems(), project.loadClass(world.getClassName()), new RecordInvoke(world), "", true);
                        contextMenu.getItems().add(makeInspectMenuItem(world));

                        MenuItem saveTheWorld = new MenuItem(Config.getString("save.world"));
                        // Temporary while developing - print out saved world to Terminal window:
                        saveTheWorld.setOnAction(e -> {
                            saveTheWorldRecorder.writeCode(className -> ((ClassTarget) project.getUnnamedPackage().getTarget(className)).getEditor());
                        });
                        contextMenu.getItems().add(saveTheWorld);

                        Point2D screenLocation = worldDisplay.worldToScreen(curPickPoint);
                        contextMenu.show(worldDisplay, screenLocation.getX(), screenLocation.getY());
                    }
                }
            }
            else if (curPickType == PickType.DRAG && !actors.isEmpty())
            {
                // Left-click drag, and there is an actor there, so begin drag:
                curDragRequest = pickId;
                draggedActor = actors.get(0);
            }
            else if (curPickType == PickType.LEFT_CLICK && !actors.isEmpty())
            {
                DebuggerObject target = actors.get(0);
                debugHandler.addSelectedObject(target, target.getGenType(),
                        target.getClassName().toLowerCase());
            }
        });
    }

    /**
     * Hide the context menu if one is currently showing on the world.
     */
    private void hideContextMenu()
    {
        if (contextMenu != null)
        {
            // The onHidden handler sets the contextMenu field back to null:
            contextMenu.hide();
        }
    }

    /**
     * Makes a MenuItem with an Inspect command for the given debugger object
     */
    private MenuItem makeInspectMenuItem(DebuggerObject debuggerObject)
    {
        MenuItem inspectItem = new MenuItem(Config.getString("debugger.objectwrapper.inspect"));
        JavaFXUtil.addStyleClass(inspectItem, MENU_STYLE_INBUILT);
        inspectItem.setOnAction(e -> {
            InvokerRecord ir = new ObjectInspectInvokerRecord(debuggerObject.getClassName());
            project.getInspectorInstance(debuggerObject, debuggerObject.getClassName(), project.getUnnamedPackage(), ir, this, null);  // shows the inspector
        });
        return inspectItem;
    }

    /**
     * Overrides BlueJEventListener method
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        // Look for results of actor/world/other-object construction:
        if (eventId == BlueJEvent.EXECUTION_RESULT)
        {
            ExecutionEvent executionEvent = (ExecutionEvent)arg;
            // Was it a constructor of a class in the default package?
            if (executionEvent.getMethodName() == null && executionEvent.getPackage().getQualifiedName().equals(""))
            {
                String className = executionEvent.getClassName();
                Target t = project.getTarget(className);
                // Should always be a ClassTarget but just in case:
                if (t instanceof ClassTarget)
                {
                    ClassTarget ct = (ClassTarget) t;
                    Reflective typeReflective = ct.getTypeReflective();
                    if (typeReflective != null && getActorReflective().isAssignableFrom(typeReflective))
                    {
                        // It's an actor!
                        ImageView imageView = getImageViewForClass(typeReflective);
                        if (imageView != null)
                        {
                            newActorProperty.set(new NewActor(imageView, executionEvent));
                        }
                    }
                    else if (typeReflective != null && getWorldReflective().isAssignableFrom(typeReflective))
                    {
                        // It's a world
                        currentWorld = ct;
                        // Not a good idea to call back debugger from a listener, so runLater:
                        JavaFXUtil.runAfterCurrent(() -> project.getDebugger()
                                .instantiateClass("greenfoot.core.SetWorldHelper",
                                        new String[]{"java.lang.Object"},
                                        new DebuggerObject[]{executionEvent.getResultObject()}));
                    }
                    else
                    {
                        // If neither actor nor world, we just inspect the constructed object:
                        project.getInspectorInstance(executionEvent.getResultObject(), "<object>", executionEvent.getPackage(), null, this, null);
                    }
                }
            }
        }
    }

    /**
     * Get an ImageView with the appropriate preview image for this class type.
     * If no suitable image found, the Greenfoot icon will be used.
     */
    private ImageView getImageViewForClass(Reflective typeReflective)
    {
        File file = getImageFilename(typeReflective);
        // If no image, use the default:
        if (file == null)
        {
            file = new File(getGreenfootLogoPath());
        }

        ImageView imageView = null;
        try
        {
            imageView = new ImageView(file.toURI().toURL().toExternalForm());
        }
        catch (MalformedURLException e)
        {
            Debug.reportError(e);
        }
        return imageView;
    }

    private static String getGreenfootLogoPath()
    {
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";
    }

    /**
     * Returns a file name for the image of the first class
     * in the given class' class hierarchy that has an image set.
     */
    private File getImageFilename(Reflective type)
    {
        String imageFileName = classDiagram.getImageForActorClass(type);
        if (imageFileName == null)
        {
            return null;
        }
        
        File imageDir = new File(project.getProjectDir(), "images");
        return new File(imageDir, imageFileName);
    }

    @Override
    public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
    {
        // Grey out the world display until compilation finishes:
        worldDisplay.greyOutWorld();
        stateProperty.set(State.UNCOMPILED);
        updateBackgroundMessage();
    }

    @Override
    public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
    {
        return false;
    }

    @Override
    public void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type, int compilationSequence)
    {
        stateProperty.set(State.PAUSED);
        // We only create the world if the window is focused, otherwise
        // we let it remain greyed out:
        if (isFocused())
        {
            doReset();
        }
        updateBackgroundMessage();
    }

    @Override
    @OnThread(Tag.Any)
    public void simulationStartedRunning()
    {
        Platform.runLater(() -> stateProperty.set(State.RUNNING));
    }

    @Override
    @OnThread(Tag.Any)
    public void simulationPaused()
    {
        Platform.runLater(() -> {
            // We can see this message when closing a project, in which case we want to ignore it:
            if (project != null)
            {
                stateProperty.set(State.PAUSED);
            }
        });
    }

    @Override
    public @OnThread(Tag.Any) void simulationDebugHalted()
    {
        Platform.runLater(() -> {
            atBreakpoint = true;
            updateGUIState(stateProperty.get());
        });
    }

    @Override
    public @OnThread(Tag.Any) void simulationDebugResumed()
    {
        Platform.runLater(() -> {
            atBreakpoint = false;
            updateGUIState(stateProperty.get());
        });
    }

    @Override
    @OnThread(Tag.Any)
    public void worldInstantiationError()
    {
        Platform.runLater(() -> {
            worldInstantiationError = true;
            // This will update the background message:
            worldVisible.set(false);
        });
    }

    @Override
    @OnThread(Tag.Any)
    public void simulationVMTerminated()
    {
        Platform.runLater(() -> {
            // We must reset the debug VM related state ready for the new debug VM:
            worldDisplay.setImage(null);
            worldInstantiationError = false;
            settingSpeedFromSimulation = false;
            lastExecStartTime = 0L;
            atBreakpoint = false;
            nextPickId = 1;
            curPickRequest = 0;
            curDragRequest = -1;
            currentWorld = null;
            worldVisible.set(false);
            stateProperty.set(State.UNCOMPILED);
            // This will set up pendingCommands, ready for when
            // the new debug VM can process data:
            loadAndMirrorProperties();
        });
    }

    /**
     * Show a dialog to set the image for the given class target.  Will only be called
     * for classes which have Actor or World as an ancestor.
     *
     * @param classNode   The class node of the class to be assigned an image.
     */
    public void setImageFor(LocalGClassNode classNode)
    {
        // initialise our image library frame
        SelectImageFrame selectImageFrame = new SelectImageFrame(this, project, classNode);
        // if the frame is not canceled after showing, set the image of the class to the selected file
        selectImageFrame.showAndWait().ifPresent(selectedFile -> setImageToClassNode(classNode, selectedFile));
    }

    /**
     * Copies an image file to the local images folder, if it is not already there,
     * and then set it to a class node.
     *
     * @param classNode          The class node to be assigned the image. Can't be null
     * @param originalImageFile  The image's file. Can't be null
     */
    private void setImageToClassNode(LocalGClassNode classNode, File originalImageFile)
    {
        File localImageFile;
        File imagesDir = new File(project.getProjectDir(), "images");
        if (originalImageFile.getParentFile().equals(imagesDir))
        {
            // The file is already in the project's images dir
            localImageFile = originalImageFile;
        }
        else
        {
            // Copy the image file to the project's images dir
            localImageFile = new File(imagesDir, originalImageFile.getName());
            GreenfootUtil.copyFile(originalImageFile, localImageFile);
        }
        classNode.setImageFilename(localImageFile.getName());
        debugHandler.getVmComms().sendProperty(
                "class." + classNode.getQualifiedName() + ".image",
                localImageFile.getName());
    }

    /**
     * Show a dialog for a new name, and then duplicate the class target with that new name.
     */
    public void duplicateClass(LocalGClassNode originalNode, ClassTarget originalClassTarget)
    {
        String originalClassName = originalClassTarget.getDisplayName();
        SourceType sourceType = originalClassTarget.getSourceType();
        NewClassDialog dialog = new NewClassDialog(this, sourceType);
        dialog.setSuggestedClassName("CopyOf" + originalClassName);
        dialog.disableLanguageBox(true);
        dialog.showAndWait().ifPresent(newClassInfo -> {
            String newClassName = newClassInfo.className;
            try
            {
                File dir = originalClassTarget.getPackage().getProject().getProjectDir();
                final String extension = sourceType.getExtension();
                File newFile = new File(dir, newClassName + "." + extension);
                File originalFile = new File(dir, originalClassName + "." + extension);
                ProjectUtils.duplicate(originalClassName, newClassName, originalFile, newFile, sourceType);
                ClassTarget newClass = originalClassTarget.getPackage().addClass(newClassName);
                LocalGClassNode newNode = classDiagram.addClass(newClass);
                
                String originalImage = originalNode.getImageFilename();
                if (originalImage != null)
                {
                    File imagesDir = new File(project.getProjectDir(), "images");
                    File srcImage = new File(imagesDir, originalImage);
                    setImageToClassNode(newNode, srcImage);
                }
            }
            catch (IOException ioe)
            {
                Debug.reportError(ioe);
            }
        });
    }

    /**
     * Import a class using the import class dialog
     */
    public void doImportClass()
    {
        File srcFile = new ImportClassDialog(this).showAndWait().orElse(null);

        if (srcFile != null)
        {
            boolean librariesImportedFlag = false;
            String className = GreenfootUtil.removeExtension(srcFile.getName());

            // Check if a class of the same name already exists in the project.
            // Renaming would be too tricky, so just issue error and stop in that case:
            for (ClassTarget preexist : project.getUnnamedPackage().getClassTargets())
            {
                if (preexist.getQualifiedName().equals(className))
                {
                    DialogManager.showMessageFX(this, "import-class-exists", className);
                    return;
                }
            }
            File srcImage = ImportClassDialog.findImage(srcFile);
            File destImage = null;
            if (srcImage != null)
            {
                destImage = new File(new File(project.getProjectDir(), "images"), srcImage.getName());
                if (destImage.exists())
                {
                    DialogManager.showMessageFX(this, "import-image-exists", srcImage.getName());
                    destImage = null;
                }
            }

            // Copy the java/class file cross:
            File destFile = new File(project.getProjectDir(), srcFile.getName());
            GreenfootUtil.copyFile(srcFile, destFile);

            // Copy the lib files cross:
            File libFolder = new File(srcFile.getParentFile(), className + "/lib");
            if ( (libFolder.exists()) && (libFolder.listFiles().length > 0) )
            {
                for (File srcLibFile : libFolder.listFiles())
                {
                    File destLibFile = new File(project.getProjectDir(), "+libs/" + srcLibFile.getName());
                    GreenfootUtil.copyFile(srcLibFile, destLibFile);
                }
                librariesImportedFlag = true;
            }

            // We must reload the package to be able to access the GClass object:
            project.getUnnamedPackage().reload();
            ClassTarget gclass = (ClassTarget)project.getUnnamedPackage().getTarget(className);

            if (gclass == null)
            {
                return;
            }

            // Finally, update the class browser:
            LocalGClassNode gclassNode = classDiagram.addClass(gclass);

            // Copy the image across and set it as the class image:
            if (srcImage != null && destImage != null && !destImage.exists())
            {
                GreenfootUtil.copyFile(srcImage, destImage);
                setImageToClassNode(gclassNode, destImage);
            }
            
            if (librariesImportedFlag)
            {
                // Must restart debug VM to load the imported library:
                project.restartVM();
            }
        }
    }

    /**
     * Prompt the user by the NewClassDialog to create a new non-image class.
     *
     * @param pkg            The package that should contain the new class.
     * @param superClassName The super class's full qualified name.
     */
    public void newNonImageClass(Package pkg, String superClassName)
    {
        NewClassDialog dlg = new NewClassDialog(this, project.getUnnamedPackage().getDefaultSourceType());
        dlg.showAndWait().ifPresent(result -> {
            createNewClass(pkg, superClassName, result.className, result.sourceType,
                    getNormalTemplateFileName(result.sourceType));
        });
    }

    /**
     * Create a new non-image class in the specified package.
     *
     * @param pkg            The package that should contain the new class.
     * @param superClassName The full qualified name of the super class.
     * @param className      The class's name, which will be created.
     * @param language       The source type of the class, e.g. Java or Stride.
     * @param templateFileName  The name of the template file to use
     *
     * @return A class info reference for the class created.
     */
    private LocalGClassNode createNewClass(Package pkg, String superClassName, String className, SourceType language,
            String templateFileName)
    {
        try
        {
            File dir = project.getProjectDir();
            final String extension = language.getExtension();
            File newFile = new File(dir, className + "." + extension);
            ProjectUtils.createSkeleton(className, superClassName, newFile,
                    templateFileName, project.getProjectCharset().toString());
            ClassTarget newClass = pkg.addClass(className);
            return classDiagram.addClass(newClass);
        }
        catch (IOException ioe)
        {
            Debug.reportError(ioe);
            return null;
        }
    }

    private static String getNormalTemplateFileName(SourceType language)
    {
        return "std" + language + ".tmpl";
    }

    public static String getActorTemplateFileName(SourceType language)
    {
        return "actor" + language + ".tmpl";
    }

    public static String getWorldTemplateFileName(boolean makeDirectSubclassOfWorld, SourceType language)
    {
        if (!makeDirectSubclassOfWorld)
        {
            return "subworld" + language + ".tmpl";
        }
        else
        {
            return "world" + language + ".tmpl";
        }
    }

    /**
     * Show a dialog to ask for details, then make a new subclass of the given class
     * using those details.
     * 
     * @param parentName  the fully-qualified name of the parent class
     * @param classType  the type of the parent class
     */
    public void newSubClassOf(String parentName, GClassType classType)
    {
        if (classType == GClassType.WORLD || classType == GClassType.ACTOR)
        {
            NewImageClassFrame frame = new NewImageClassFrame(this, project);
            // if the frame is not canceled after showing, create the new class
            // and set its image to the selected file
            frame.showAndWait().ifPresent(classInfo ->
            {
                SourceType sourceType = classInfo.sourceType;
                String extendsName = parentName;
                if (extendsName.startsWith("greenfoot."))
                {
                    extendsName = extendsName.substring("greenfoot.".length());
                }
                LocalGClassNode newClass = createNewClass(project.getUnnamedPackage(), extendsName, classInfo.className,
                        sourceType, getTemplateFileName(classType, parentName, sourceType));

                // set the image of the class to the selected file, if there is one selected.
                File imageFile = classInfo.imageFile;
                if (imageFile != null)
                {
                    setImageToClassNode(newClass, imageFile);
                }
            });
        }
        else
        {
            ClassTarget classTarget = classDiagram.getSelectedClassTarget();
            Package pkg = classTarget.getPackage();
            newNonImageClass(pkg, parentName);
        }
    }

    /**
     * Return the template file name which is built based on
     * the class type (i.e. world or actor), the parent and the source type.
     * Other class type is not allowed and will throw an IllegalArgumentException.
     *
     * @param classType   World or Actor. If Other is passed, an IllegalArgumentException will be fired.
     * @param parentName  The direct parent's name.
     * @param sourceType  Java or Stride.
     * @return The suitable template file name.
     * @throws IllegalStateException Only if the class type is not World or Actor.
     */
    private String getTemplateFileName(GClassType classType, String parentName, SourceType sourceType)
    {
        if (classType == GClassType.WORLD)
        {
            return getWorldTemplateFileName("greenfoot.World".equals(parentName), sourceType);
        }
        else if (classType == GClassType.ACTOR)
        {
            return getActorTemplateFileName(sourceType);
        }
        throw new IllegalArgumentException("This method should be called only on World or Actor classes.");
    }

    /**
     * Gets a Reflective for the Actor class.
     */
    private Reflective getActorReflective()
    {
        return new JavaReflective(project.loadClass("greenfoot.Actor"));
    }

    /**
     * Gets a Reflective for the World class.
     */
    private Reflective getWorldReflective()
    {
        return new JavaReflective(project.loadClass("greenfoot.World"));
    }

    /**
     * An InvokeListener which also records the invocation for save-the-world purposes
     */
    private class RecordInvoke implements InvokeListener
    {
        private final DebuggerObject target;

        public RecordInvoke(DebuggerObject target)
        {
            this.target = target;
        }

        @Override
        public void executeMethod(MethodView mv)
        {
            // We must put the object on the bench so that it has a name on the debug VM
            // side.  Without a name, you can't call a method on it using the BlueJ workers.
            // Also, the object bench gets cleared on compile, so that takes care of clean-up:
            String objInstanceName = debugHandler.addObject(target, target.getGenType(), target.getClassName().toLowerCase());
            project.getDebugger().addObject(project.getPackage("").getId(), objInstanceName, target);

            ResultWatcher watcher = new ResultWatcherBase(target, objInstanceName,
                    project.getUnnamedPackage(), GreenfootStage.this, mv) {
                @Override
                protected void addInteraction(InvokerRecord ir)
                {
                    saveTheWorldRecorder.callActorOrWorldMethod(target, mv.getMethod(),
                            ir.getArgumentValues(), mv.getParamTypes(false));
                }
            };
            
            if (ProjectUtils.checkDebuggerState(project, GreenfootStage.this)) {
                // Invoker invoker = new Invoker(pmf, mv, objInstanceName, target, watcher);
                Package unpkg = project.getPackage("");
                Invoker invoker = new Invoker(GreenfootStage.this, unpkg, mv, watcher, unpkg.getCallHistory(), debugHandler, debugHandler,
                        project.getDebugger(), objInstanceName);
                invoker.invokeInteractive();
            }
        }

        @Override
        public void callConstructor(ConstructorView cv)
        {
            //We are not used for constructors, so this won't get called.
        }
    }

    /**
     * Opens a browser tab in the editor showing the given URL
     */
    public void openBrowser(String url)
    {
        project.getDefaultFXTabbedEditor().openWebViewTab(url);
    }

    /**
     * Called when a class has been modified, to notify us that the .class
     * files are now out of date.
     */
    public void classModified()
    {
        debugHandler.getVmComms().discardWorld();
        if (isFocused() && currentWorld != null)
        {
            debugHandler.getVmComms().instantiateWorld(currentWorld.getQualifiedName());
        }
        stateProperty.set(State.UNCOMPILED);
    }

    /**
     * Shows About-Greenfoot dialog including information about the development team and translators.
     * 
     * @param parentWindow   the parent window; may be null.
     */
    public static void aboutGreenfoot(Window parentWindow)
    {
        // Finds the image file that is supposed to be exist in the "resources" directory
        URL resource = GreenfootStage.class.getClassLoader().getResource("greenfoot-about.png");
        Image image = new javafx.scene.image.Image(resource.toString());

        String[] translatorNames = {"Wombat Yuan", "Zdenék Chalupský", "Erik van Veen & Renske Smetsers-Weeda",
                "Guillaume Baudoin", "Matthias Taulien", "Stefan Mueller", "Mantzas Ioannis",
                "Stefano Federici", "John Kim", "Przemysław Adam Śmiejek", "Paulo Abadie & Fabio Hedayioglu",
                "Sergy Zemlyannikov", "Esteban Iglesias Manríquez"};
        TitledPane translators = new TitledPane("Translators",
                new Label(String.join("\n", Arrays.asList(translatorNames))));
        translators.setExpanded(false);
        translators.setCollapsible(true);
        Dialog<Void> aboutDlg = new AboutDialogTemplate(parentWindow, Boot.GREENFOOT_VERSION,
                "Greenfoot", "https://greenfoot.org/", image, translators);
        aboutDlg.showAndWait();
    }

    /**
     * Close all Greenfoot windows (before exit).
     */
    public static void closeAll()
    {
        Collection<GreenfootStage> stages_copy = new ArrayList<>(stages);

        // Save the list of open projects, to be re-opened next time:
        int i = 0;
        for (GreenfootStage stage : stages_copy)
        {
            if (stage.project != null)
            {
                i++;
                Config.putPropString(Config.BLUEJ_OPENPACKAGE + i,
                        stage.project.getProjectDir().getPath());
            }
        }

        // Remove any extra open projects from the list:
        String exists;
        do {
            i++;
            exists = Config.removeProperty(Config.BLUEJ_OPENPACKAGE + i);
        } while (exists != null);

        // Close all stages:
        for (GreenfootStage stage : stages_copy)
        {
            // pass keepLast = true to avoid closing the final stage causing infinite recursion:
            stage.doClose(true);
        }
    }
    
    /**
     * Find the stage currently showing the specified project (if any).
     */
    public static GreenfootStage findStageForProject(Project project)
    {
        for (GreenfootStage stage : stages)
        {
            if (stage.project == project)
            {
                return stage;
            }
        }
        
        return null;
    }

    /**
     * Shows the terminal for this project, and brings it to the front.
     */
    public void bringTerminalToFront()
    {
        project.getTerminal().showHide(true);
        project.getTerminal().getWindow().toFront();
    }
    
    /*
     * PackageUI getStage() implementation.
     * @see bluej.pkgmgr.PackageUI#getStage()
     */
    @OnThread(Tag.FXPlatform)
    @Override
    public Stage getStage()
    {
        return this;
    }
    
    @OnThread(Tag.FXPlatform)
    @Override
    public void callStaticMethodOrConstructor(CallableView cv)
    {
        ResultWatcher watcher = null;
        Package pkg = project.getPackage("");

        if (cv instanceof ConstructorView) {
            // if we are constructing an object, create a watcher that waits for
            // completion of the call and then places the object on the object
            // bench
            watcher = new ResultWatcherBase(pkg, this, cv) {
                @Override
                public void beginCompile()
                {
                    super.beginCompile();
                }
                
                @Override
                protected void nonNullResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    if ((name == null) || (name.length() == 0))
                        name = "result";

                    debugHandler.addObject(result, result.getGenType(), name);
                    project.getDebugger().addObject(project.getPackage("").getId(), name, result);
                    
                    // TODO save interaction in the saveTheWorldRecorder?
                }

                @Override
                protected void addInteraction(InvokerRecord ir)
                {
                    // Nothing we can do here.
                }
            };
        }
        else if (cv instanceof MethodView) {
            // create a watcher
            // that waits for completion of the call and then displays the
            // result (or does nothing if void)
            watcher = new ResultWatcherBase(pkg, this, cv) {
                @Override
                protected void addInteraction(InvokerRecord ir)
                {
                    // Nothing we can do
                }
            };
        }

        // create an Invoker to handle the actual invocation
        if (ProjectUtils.checkDebuggerState(project, this)) {
            new Invoker(this, pkg, cv, watcher, pkg.getCallHistory(), debugHandler, debugHandler,
                    project.getDebugger(), null).invokeInteractive();
        }
    }

    /**
     * Allow the user to select a directory into which we create a project.
     */
    public void doNewProject(SourceType sourceType)
    {
        String title = Config.getString("greenfoot.utilDelegate.newScenario");
        File newnameFile = FileUtility.getSaveProjectFX(this.getStage(), title);
        if (newnameFile == null)
        {
            return;
        }
        if (! newProject(newnameFile.getAbsolutePath(),sourceType))
        {
            DialogManager.showErrorWithTextFX(null, "cannot-create-directory", newnameFile.getPath());
        }
    }

    /**
     * Create new Greenfoot project with a sub class of World.
     *
     * @param dirName The directory to create the project in.
     * @param sourceType The source type of the project which is either Stride or Java.
     * @return     true if successful, false otherwise
     */
    public boolean newProject(String dirName, SourceType sourceType)
    {
        if (Project.createNewProject(dirName))
        {
            Project proj = Project.openProject(dirName);

            if (proj != null)
            {
                Package unNamedPkg = proj.getUnnamedPackage();
                Properties props = new Properties(unNamedPkg.getLastSavedProperties());
                props.put("version", Boot.GREENFOOT_API_VERSION);
                unNamedPkg.save(props);
                ProjectManager.instance().launchProject(proj);
                GreenfootStage stage = findStageForProject(proj);
                stage.createNewClass(unNamedPkg, "greenfoot.World",
                        "MyWorld", sourceType, getWorldTemplateFileName(true, sourceType));
                stage.toFront();
                return true;
            }
            else
            {
                // display error dialog
                DialogManager.showErrorFX(this, "could-not-open-project");
                return false;
            }
        }
        
        DialogManager.showErrorFX(this, "cannot-create-project");
        return false;
    }

    /**
     * Record the last time (from System.currentTimeMillis) that the user code started executing.
     * If enough time has passed then show the execution twirler.
     * @param lastExecStartTime The last time the user code started executing, or zero if it has now finished executing.
     * @param delayLoop The true or false value to indicate whether there is a delay loop or not
     */
    public void setLastUserExecutionStartTime(long lastExecStartTime, boolean delayLoop)
    {
        this.lastExecStartTime = lastExecStartTime;
        if (lastExecStartTime == 0L)
        {
            executionTwirler.stopTwirling();
        }
        else
        {
            long duration = System.currentTimeMillis() - lastExecStartTime;
            if (duration < 4000L)
            {
                executionTwirler.stopTwirling();
                JavaFXUtil.runAfter(Duration.millis(4000L - duration), () -> {
                    if (this.lastExecStartTime == lastExecStartTime && !delayLoop)
                    {
                        executionTwirler.startTwirling();
                    }
                });
            }
            else if (!delayLoop)
            {
                executionTwirler.startTwirling();
            }
        }
    }

    /**
     * Called with the latest simulation speed
     * @param simSpeed The simulation speed we received from the debug VM:
     */
    public void notifySimulationSpeed(int simSpeed)
    {
        // We want to update the speed slider, but we don't want to alter
        // the speed in lastUserSetSpeed which will get saved, and we don't want to
        // tell the simulation about a speed change that they instigated.
        // So we set a boolean flag to block the slider listener:
        settingSpeedFromSimulation = true;
        controlPanel.setSpeed(simSpeed);
        settingSpeedFromSimulation = false;
    }

    /**
     * Show the readme file for this project in an editor window.
     */
    public void openReadme()
    {
        ReadmeTarget target = project.getUnnamedPackage().getReadmeTarget();
        if (target.getEditor() == null)
        {
            DialogManager.showErrorFX(this, "error-open-readme");
        }
        else
        {
            target.getEditor().setEditorVisible(true);
        }
    }

    /**
     * When the speed slider is moved, this method is called
     * to communicate the new value to the debug VM.
     * @param newSpeed The new speed, from the slider.
     */
    public void setSpeedFromSlider(int newSpeed)
    {
        if (!settingSpeedFromSimulation)
        {
            lastUserSetSpeed = newSpeed;
            debugHandler.getVmComms().setSimulationSpeed(newSpeed);
        }
    }

    /**
     * Checks if the class to be deleted is the current world class, then it sets
     * currentWorld field to null. This avoids generating exception when Greenfoot
     * later tries to use the currentWorld field in GreenfootStage.
     * @param classTarget The class to be deleted.
     */
    public void fireWorldRemovedCheck(ClassTarget classTarget)
    {
        if (classTarget.equals(currentWorld))
        {
            currentWorld = null;
            worldVisible.set(false);
            doReset();
        }
        else
        {
            // In case this was last world class, update background message:
            updateBackgroundMessage();
        }
    }
}
