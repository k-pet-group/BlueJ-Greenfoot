/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2012,2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.editor.stride;

import bluej.Config;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.DebuggerThread;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.stride.ErrorOverviewBar.ErrorInfo;
import bluej.editor.stride.ErrorOverviewBar.ErrorState;
import bluej.editor.stride.FXTabbedEditor.CodeCompletionState;
import bluej.editor.stride.FrameCatalogue.Hint;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.ConstructorCompletion;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageOrClass;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.role.Kind;
import bluej.prefmgr.PrefMgr;
import bluej.stride.framedjava.ast.ASTUtility;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.links.PossibleKnownMethodLink;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleMethodUseLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.ast.links.PossibleVarLink;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.LocatableElement.LocationMap;
import bluej.stride.framedjava.elements.MethodWithBodyElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay;
import bluej.stride.framedjava.frames.*;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.*;
import bluej.stride.generic.Frame.ShowReason;
import bluej.stride.generic.Frame.View;
import bluej.stride.operations.UndoRedoManager;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.LinkedIdentifier;
import bluej.editor.fixes.SuggestionList.SuggestionListParent;
import bluej.utility.BackgroundConsumer;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.CircleCountdown;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.ViewportHeightBinding;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The big central editor class for the frame editor.  The frames analogue of MoeEditor.
 *
 * This class contains all the coordinating functionality for each frame editor.  It is exposed to sub-elements
 * (frames, slots, etc) via the InteractionManager interface, so that class is a good place
 * to understand the public interface of this class.
 */
@OnThread(Tag.FX)
public class FrameEditorTab extends FXTab implements InteractionManager, SuggestionListParent
{
    // We keep track ourselves of which item is focused.  Only focusable things in the editor
    // should be frame cursors and slots:
    private final SimpleObjectProperty<CursorOrSlot> focusedItem = new SimpleObjectProperty<>(null);
    private final TopLevelCodeElement initialSource;
    // Name of the top-level class/interface
    private final StringProperty nameProperty = new SimpleStringProperty();
    private final FrameSelection selection = new FrameSelection(this);
    private final FrameEditor editor;
    private final UndoRedoManager undoRedoManager;
    private final ObjectProperty<View> viewProperty = new SimpleObjectProperty<>(View.NORMAL);
    private final EntityResolver projectResolver;
    private final FrameMenuManager menuManager;
    // The overlays (see individual class documentation)
    private WindowOverlayPane windowOverlayPane;
    private CodeOverlayPane codeOverlayPane;
    // Second window overlay, in front, for showing editor banners
    private VBox bannerPane;
    private boolean undoBannerShowing = false;
    // A property to observe for when the scroll value changes on scroll pane:
    private Observable observableScroll;
    private ViewportHeightBinding viewportHeight;
    // Keeps track of whether the user has scrolled since there was last a focus change
    // (ignoring focus becoming null)
    private boolean manualScrolledSinceLastFocusChange = false;
    private CursorOrSlot focusOwnerDuringLastManualScroll = null;
    // Frame won't change after initialiseFX, so it's effectively final,
    // and protected by an FX tab, so it's ok for the field to be read from any thread:
    @OnThread(Tag.FX)
    private final Property<TopLevelFrame<? extends TopLevelCodeElement>> topLevelFrameProperty = new SimpleObjectProperty<>();
    private ContextMenu menu;
    // The debugger controls (currently unused):
    private HBox controlPanel;
    private FrameCursor dragTarget;
    private BorderPaneWithHighlightColor contentRoot;
    private StackPane scrollAndOverlays;
    private StackPane scrollContent;
    private final ObjectProperty<FXTabbedEditor> parent = new SimpleObjectProperty<>();
    private final Project project;
    private ScrollPane scroll;
    private boolean selectingByDrag;
    private BirdseyeManager birdseyeManager;
    private Rectangle birdseyeSelection;
    private Pane birdseyeSelectionPane;
    // If escape is pressed, focus returns to where it was beforehand,
    // which is saved in these variables:
    private Node birdseyeDefaultFocusAfter;
    private FXRunnable birdseyeDefaultRequestFocusAfter;
    private Iterator<CodeError> errors;
    private SimpleBooleanProperty initialised = new SimpleBooleanProperty(false);
    private boolean startedInitialising;
    private Frame stackHighlight;
    private EditableSlot showingUnderlinesFor = null;
    private ErrorOverviewBar errorOverviewBar;
    @OnThread(Tag.FX)
    private boolean loading = false;
    // True when we are part way through an animation to set the scroll value:
    private boolean animatingScroll = false;
    private boolean anyButtonsPressed = false;
    private SharedTransition viewChange;
    private ErrorAndFixDisplay cursorErrorDisplay;
    private boolean inScrollTo = false;
    private Canvas execHistoryCanvas;
    private final Set<Node> execNodesListenedTo = new HashSet<>();
    private final SimpleBooleanProperty debugVarVisibleProperty = new SimpleBooleanProperty(false);
    private List<HighlightedBreakpoint> latestExecHistory;
    private StringBinding strideFontSizeAsString;
    private StringExpression strideFontCSS;
    private final SimpleObjectProperty<Image> imageProperty = new SimpleObjectProperty<>(null);

    @OnThread(Tag.FXPlatform)
    public FrameEditorTab(Project project, EntityResolver resolver, FrameEditor editor, TopLevelCodeElement initialSource)
    {
        super(true);
        this.project = project;
        this.projectResolver = resolver;
        this.editor = editor;
        this.initialSource = initialSource;
        this.undoRedoManager = new UndoRedoManager(new FrameState(initialSource));
        this.menuManager = new FrameMenuManager(this);
    }

    public static String blockSkipModifierLabel()
    {
        return Config.isMacOS() ? "\u2325" : "^";
    }

    private static boolean hasBlockSkipModifierPressed(KeyEvent event)
    {
        if (Config.isMacOS()) {
            return event.isAltDown();
        }
        else {
            return event.isControlDown();
        }
    }

    // Must be run on FX thread
    // @Override
    @OnThread(Tag.FXPlatform)
    public void initialiseFX()
    {
        if (startedInitialising)
            return;
        startedInitialising = true;

        // We put all the info in the graphic, so that we can use the graphic as a drag target:
        setText("");
        Label titleLabel = new Label(initialSource.getName());
        titleLabel.textProperty().bind(nameProperty);
        HBox tabHeader = new HBox(titleLabel, makeClassGraphicIcon(imageProperty, 16, false));
        tabHeader.setAlignment(Pos.CENTER);
        tabHeader.setSpacing(3.0);
        tabHeader.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.MIDDLE)
            {
                setWindowVisible(false, false);
            }
        });
        setGraphic(tabHeader);
        JavaFXUtil.addStyleClass(this, "frame-editor-tab", initialSource.getStylePrefix() + "frame-editor-tab");

        JavaFXUtil.addChangeListener(focusedItem, focused -> {
            JavaFXUtil.runNowOrLater(() -> menuManager.setMenuItems(focused != null ? focused.getMenuItems(false) : selection != null ? selection.getEditMenuItems(false) : Collections.emptyMap()));

            // Reset whether we've scrolled since last focus change, if this is a new owner:
            if (focused != null && !focused.equals(focusOwnerDuringLastManualScroll))
                manualScrolledSinceLastFocusChange = false;
        });

        selection.addChangeListener(() -> menuManager.setMenuItems(focusedItem.get() != null ? focusedItem.get().getMenuItems(false) : Collections.emptyMap()));

        contentRoot = new BorderPaneWithHighlightColor();
        JavaFXUtil.addStyleClass(contentRoot, "frame-editor-tab-content", initialSource.getStylePrefix() + "frame-editor-tab-content");
        scrollAndOverlays = new StackPane();
        windowOverlayPane = new WindowOverlayPane();
        bannerPane = new VBox();
        bannerPane.setPickOnBounds(false);
        scroll = new ScrollPane() {
            @Override
            public void requestFocus() {
                // Do nothing
            }
        };


        scroll.getStyleClass().add("frame-editor-scroll-pane");
        scroll.setFitToWidth(true);
        observableScroll = scroll.vvalueProperty();
        viewportHeight = new ViewportHeightBinding(scroll);

        scrollAndOverlays.getChildren().addAll(scroll, windowOverlayPane.getNode(), bannerPane);

        // Make class block fill window width:
        scroll.setFitToWidth(true);

        JavaFXUtil.addChangeListener(scroll.vvalueProperty(), v -> {
            if (!animatingScroll)
            {
                manualScrolledSinceLastFocusChange = true;
                // Focus owner may be null, if user is scrolling window on mouse over
                // without focusing.  In this case, we want to keep track of previous
                // focus owner
                if (focusedItem.get() != null)
                    focusOwnerDuringLastManualScroll = focusedItem.get();
            }
        });

        scrollAndOverlays.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            final FrameCursor focusedCursor = getFocusedCursor();
            boolean blockCursorFocused = focusedCursor != null;

                switch (event.getCode()) {
                case UP:
                    if (blockCursorFocused) {
                        FrameCursor c;
                        if (event.isShiftDown() && viewProperty.get() != View.JAVA_PREVIEW) {
                            c = focusedCursor.getPrevSkip();
                            selection.toggleSelectUp(focusedCursor.getFrameBefore());
                        }
                        else if (hasBlockSkipModifierPressed(event) || (viewProperty.get() == View.JAVA_PREVIEW && focusedCursor.getFrameBefore() != null && !focusedCursor.getFrameBefore().isFrameEnabled())) {
                            selection.clear();
                            c = focusedCursor.getPrevSkip();
                        }
                        else {
                            selection.clear();
                            c = focusedCursor.getParentCanvas().getPrevCursor(focusedCursor, true);
                        }

                        if (c != null) {
                            c.requestFocus();
                        }
                        event.consume();
                    }
                    break;
                case DOWN:
                    if (blockCursorFocused) {
                        FrameCursor c;
                        if (event.isShiftDown() && viewProperty.get() != View.JAVA_PREVIEW) {
                            c = focusedCursor.getNextSkip();
                            selection.toggleSelectDown(focusedCursor.getFrameAfter());
                        }
                        else if (hasBlockSkipModifierPressed(event) || (viewProperty.get() == View.JAVA_PREVIEW && focusedCursor.getFrameAfter() != null && !focusedCursor.getFrameAfter().isFrameEnabled())) {
                            selection.clear();
                            c = focusedCursor.getNextSkip();
                        }
                        else {
                            selection.clear();
                            c = focusedCursor.getParentCanvas().getNextCursor(focusedCursor, true);
                        }

                        if (c != null) {
                            c.requestFocus();
                        }
                        event.consume();
                    }
                    break;
                case HOME:
                    if (blockCursorFocused) {
                        getTopLevelFrame().focusOnBody(TopLevelFrame.BodyFocus.TOP);
                        selection.clear();
                        event.consume();
                    }
                    break;
                case END:
                    if (blockCursorFocused) {
                        getTopLevelFrame().focusOnBody(TopLevelFrame.BodyFocus.BOTTOM);
                        selection.clear();
                        event.consume();
                    }
                    break;
                case LEFT:
                    if (blockCursorFocused) {
                        // Is there a frame above the cursor within the same canvas?
                        Frame frameBefore = focusedCursor.getFrameBefore();
                        if (frameBefore != null) {
                            // Yes; see if it can be focused
                            if ( !frameBefore.focusFrameEnd(false) ) {
                                // If it can't (e.g. blank frame), go for the cursor above it:
                                focusedCursor.getUp().requestFocus();
                            }
                        }
                        else {
                            // We must be top cursor in the canvas; so go to end of item before
                            // us within our enclosing frame:
                            Frame enclosingFrame = focusedCursor.getEnclosingFrame();
                            if (enclosingFrame != null) {
                                enclosingFrame.focusLeft(focusedCursor.getParentCanvas());
                            }
                            else
                            {
                                // Nowhere to go; don't think this should even be possible?
                                Debug.message("No enclosing frame on cursor");
                            }
                        }
                        selection.clear();
                        event.consume();
                    }
                    break;
                    // Block right key when focused on BlockCursor:
                case RIGHT:
                    if (blockCursorFocused) {
                        // Is there a frame below the cursor?
                        Frame frame = focusedCursor.getFrameAfter();
                        if (frame != null) {
                            if ( !frame.focusFrameStart() ) {
                                // If nothing to focus on in the frame (e.g. blank),
                                // focus the cursor afterwards:
                                focusedCursor.getParentCanvas().getNextCursor(focusedCursor, true).requestFocus();
                            }
                        }
                        else {
                            // No frame beneath the cursor, we must be bottom cursor in canvas
                            Frame enclosingFrame = focusedCursor.getEnclosingFrame();
                            if (enclosingFrame != null) {
                                enclosingFrame.focusRight(focusedCursor.getParentCanvas());
                            }
                            else
                            {
                                // Nowhere to go; don't think this should even be possible?
                                Debug.message("No enclosing frame on cursor");
                            }
                        }
                        selection.clear();
                        event.consume();
                    }
                    break;


                //This is a workaround for a JDK bug on Mac.
                //'='/'-' don't work as menu accelerators.
                //TODO Remove when the JDK bug is fixed
                case EQUALS:
                    if (Config.isMacOS() && event.isMetaDown()) {
                        increaseFontSize();
                    }
                    break;
                case MINUS:
                    if (Config.isMacOS() && event.isMetaDown()) {
                        decreaseFontSize();
                    }
                    break;


                default:
                    if (event.getCode() == Config.getKeyCodeForYesNo(ShortcutKey.YES_ANYWHERE))
                    {
                        SuggestedFollowUpDisplay.shortcutTyped(this, ShortcutKey.YES_ANYWHERE);
                    }
                    else if (event.getCode() == Config.getKeyCodeForYesNo(ShortcutKey.NO_ANYWHERE))
                    {
                        SuggestedFollowUpDisplay.shortcutTyped(this, ShortcutKey.NO_ANYWHERE);
                    }
                    break;
                }
        });

        scrollAndOverlays.addEventFilter(MouseEvent.ANY, e -> {
            anyButtonsPressed = e.isPrimaryButtonDown() || e.isSecondaryButtonDown() || e.isMiddleButtonDown();
        });

        controlPanel = new HBox();

        Button stepButton = new Button("Step");
        //stepButton.setOnAction(e -> SwingUtilities.invokeLater(() -> editor.step()));

        Button continueButton = new Button("Continue");
        //continueButton.setOnAction(e -> SwingUtilities.invokeLater(() -> editor.cont()));

        controlPanel.getChildren().addAll(stepButton, continueButton);
        controlPanel.setSpacing(10.0);


        // Add all to scene
        // menuBar.setUseSystemMenuBar(true);
        // Remove menu for demo:
        // p.setTop(menuBar);
        // menuHeight = menuBar.heightProperty();
        contentRoot.setCenter(scrollAndOverlays);
        //p.setTop(controlPanel);

        // We must create code overlay before we create the ClassFrame:
        codeOverlayPane = new CodeOverlayPane();

        // Need to create scrollContent before createTopLevelFrame as they will
        // call keepNodeVisibleWhenFocused:
        scrollContent = new StackPane();


        errorOverviewBar = new ErrorOverviewBar(this, scrollContent, this::nextError);
        JavaFXUtil.addChangeListener(errorOverviewBar.showingCount(), count -> {
            // We can't use pseudoclasses because Tab doesn't allow them to be changed,
            // so we must use full classes:
            if (count.intValue() > 0)
                JavaFXUtil.addStyleClass(this, "bj-tab-error");
            else
                getStyleClass().removeAll("bj-tab-error");
        });
        contentRoot.setRight(errorOverviewBar);

        loading = true;
        FrameEditor frameEditor = getFrameEditor();
        new Thread() {
            @OnThread(value = Tag.FX, ignoreParent = true)
            public void run()
            {
                TopLevelFrame<? extends TopLevelCodeElement> frame = initialSource.createTopLevelFrame(FrameEditorTab.this);
                frame.regenerateCode();
                TopLevelCodeElement el = frame.getCode();
                // Finish on the platform thread:
                JavaFXUtil.runPlatformLater(() ->
                {
                    el.updateSourcePositions();
                    FrameEditorTab.this.topLevelFrameProperty.setValue(frame);
                    nameProperty.bind(getTopLevelFrame().nameProperty());
                    // Whenever name changes, trigger recompile even without leaving slot:
                    JavaFXUtil.addChangeListener(getTopLevelFrame().nameProperty(), n ->
                    {
                        JavaFXUtil.runNowOrLater(() -> {
                            editor.codeModified();
                            try
                            {
                                editor.save();
                            }
                            catch (IOException e)
                            {
                                Debug.reportError("Problem saving after name change", e);
                            }
                        });
                    });
                    // Make class at least as high as scroll view:
                    getTopLevelFrame().bindMinHeight(viewportHeight);
                    scrollContent.getChildren().add(0, getTopLevelFrame().getNode());

                    updateFontSize();

                    // When imports change, we provide a new future to calculate the types:
                    List<Future<List<AssistContentThreadSafe>>> importsToUpdate = FrameEditorTab.this.getFrameEditor().getEditorFixesManager().getImportedTypesFutureList();
                    JavaFXUtil.bindMap(importsToUpdate, getTopLevelFrame().getImports(), FrameEditorTab.this.getFrameEditor().getEditorFixesManager()::scanImports, change ->
                    {
                        frameEditor.getEditorFixesManager().getImportedTypesLock().writeLock().lock();
                        change.run();
                        frameEditor.getEditorFixesManager().getImportedTypesLock().writeLock().unlock();
                    });

                    if (getTopLevelFrame() != null)
                    {
                        saved();
                        // Force generation of early errors on load:
                        // No relevant compile sequence to use, so we use -1 meaning N/A:
                        editor.earlyErrorCheck(getTopLevelFrame().getCode().findEarlyErrors(), -1);
                        Platform.runLater(FrameEditorTab.this::updateDisplays);
                    }

                    initialised.set(true);

                    loading = false;
                    //Debug.time("Finished loading");
                });
            }
        }.start();

        JavaFXUtil.addChangeListener(viewProperty, menuManager::notifyView);
        birdseyeSelection = new Rectangle();
        JavaFXUtil.addStyleClass(birdseyeSelection, "birdseye-selection");
        birdseyeSelectionPane = new Pane(birdseyeSelection);
        birdseyeSelectionPane.setVisible(false);
        birdseyeSelectionPane.setMouseTransparent(false);
        birdseyeSelectionPane.setOnMouseClicked(e -> {
            FrameCursor clickTarget = birdseyeManager.getClickedTarget(e.getSceneX(), e.getSceneY());
            // Clicked somewhere outside a selection or has expanded; either way, disable the view
            if (clickTarget == null)
                disableBirdseyeView(Frame.ViewChangeReason.MOUSE_CLICKED);
            else
                disableBirdseyeView(clickTarget.getNode(), Frame.ViewChangeReason.MOUSE_CLICKED, clickTarget::requestFocus);
            e.consume();
        });
        birdseyeSelectionPane.setOnMouseMoved(e -> {
            birdseyeSelectionPane.setCursor(birdseyeManager.canClick(e.getSceneX(), e.getSceneY()) ? Cursor.HAND : Cursor.DEFAULT);
        });

        scrollContent.getChildren().addAll(/*topLevelFrame.getNode(), */codeOverlayPane.getNode(), birdseyeSelectionPane );
        scroll.setContent(scrollContent);

        setContent(contentRoot);
        // Consume mouse pressed events at the root, to stop them falling to the tab pane,
        // which requests focus on its tab header (not something we ever want):
        contentRoot.addEventHandler(MouseEvent.MOUSE_PRESSED, Event::consume);




        // TEMP scaling for taking hi-res screenshots
        // p.getTransforms().add(new Scale(2.0, 2.0));

        contentRoot.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode())
            {
                // Undo/Redo don't need to be handled here on mac as cmd+Z/cmd+shift+Z will fire the accelerator by
                // default. This is different from Linux/Windows where not all cases cause the accelerator to be fired,
                // so we have to handle the event on the key press on the later systems.
                case Y:
                    if (!Config.isMacOS() && event.isShortcutDown() && !event.isShiftDown())
                    {
                        redo();
                        event.consume();
                    }
                    break;
                case Z:
                    if (!Config.isMacOS() && event.isShortcutDown() && !event.isShiftDown())
                    {
                        undo();
                        event.consume();
                    }
                    break;
                case UP:
                    if (viewProperty.get().isBirdseye())
                    {
                        birdseyeManager.up();
                        calculateBirdseyeRectangle();
                        event.consume();
                    }
                    break;
                case DOWN:
                    if (viewProperty.get().isBirdseye())
                    {
                        birdseyeManager.down();
                        calculateBirdseyeRectangle();
                        event.consume();
                    }
                    break;
                case ENTER:
                    if (viewProperty.get().isBirdseye())
                    {
                        FrameCursor target = birdseyeManager.getCursorForCurrent();
                        disableBirdseyeView(target.getNode(), Frame.ViewChangeReason.KEY_PRESSED_ENTER, target::requestFocus);
                        event.consume();
                    }
                    break;
                case ESCAPE:
                    if (viewProperty.get() == View.JAVA_PREVIEW)
                    {
                        disableJavaPreview(Frame.ViewChangeReason.KEY_PRESSED_ESCAPE);
                        event.consume();
                    }
                    else if (viewProperty.get().isBirdseye())
                    {
                        disableBirdseyeView(Frame.ViewChangeReason.KEY_PRESSED_ESCAPE);
                        event.consume();
                    }
                    break;
            }
        });

        contentRoot.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown())
            {
                if (e.getDeltaY() > 0)
                    increaseFontSize();
                else
                    decreaseFontSize();

                e.consume();
            }
        });

        addWeakFontSizeUpdater(this);

        // This will call updateDisplays:
        regenerateAndReparse();


    }

    public void cleanup()
    {
        FrameCursor.editorClosing(this);
    }

    private TopLevelFrame<? extends TopLevelCodeElement> getTopLevelFrame()
    {
        return topLevelFrameProperty.getValue();
    }

    @OnThread(Tag.FXPlatform)
    public void withTopLevelFrame(FXPlatformConsumer<TopLevelFrame<? extends TopLevelCodeElement>> action)
    {
        JavaFXUtil.onceNotNull(topLevelFrameProperty, action);
    }

    @OnThread(Tag.FXPlatform)
    public void redrawExecHistory(List<HighlightedBreakpoint> execHistory)
    {
        this.latestExecHistory = execHistory;
        CodeOverlayPane overlay = getCodeOverlayPane();
        if (execHistoryCanvas != null)
            overlay.removeOverlay(execHistoryCanvas);

        execHistoryCanvas = overlay.addFullSizeCanvas();
        GraphicsContext g = execHistoryCanvas.getGraphicsContext2D();

        double prevTargetX = 0, prevTargetY = 0;
        HighlightedBreakpoint prevPoint = null;
        List<HighlightedBreakpoint> drawnFrom = new ArrayList<>();
        for (int i = 0; i < execHistory.size(); i++)
        {
            HighlightedBreakpoint b = execHistory.get(i);
            if (b.getNode() != null)
            {
                Bounds bounds = b.getNode().localToScene(b.getNode().getBoundsInLocal());
                if (!execNodesListenedTo.contains(b.getNode()))
                {
                    JavaFXUtil.addChangeListenerPlatform(b.getNode().localToSceneTransformProperty(), t -> {
                        redrawExecHistory(latestExecHistory);
                    });
                    // TODO remove the listener when done
                    // TODO only redraw once for each scroll
                    execNodesListenedTo.add(b.getNode());
                }
                
                
                double targetX = execHistoryCanvas.getWidth()*0.75; //bounds.getMinX() + 100;
                double targetY = overlay.sceneYToCodeOverlayY(bounds.getMinY()) + b.getYOffset();
                if (b.showExec(i))
                {
                    if (i == 0)
                    {
                        prevTargetX = targetX;
                        prevTargetY = targetY - 10;
                    }
                    g.setStroke(Color.WHITE);
                    g.setLineWidth(4.0);
                    //Debug.message("Drawing from " + prevTargetY + " to " + targetY + ": " + b.getNode());
                    // Draw twice; first white, then smaller blue line over the top:
                    for (int k = 0; k < 2; k++)
                    {
                        if (Math.abs(prevTargetY - targetY) < 15.0f)
                        {
                            g.strokeLine(prevTargetX, prevTargetY, targetX, targetY);
                            g.strokeLine(targetX - 10, targetY - 10, targetX, targetY);
                            g.strokeLine(targetX + 10, targetY - 10, targetX, targetY);
                        }
                        else
                        {
                            double bulge, angle;
                            if (prevPoint != null && drawnFrom.contains(prevPoint))
                            {
                                // Drawn an arrow from the source before; need to bulge more:
                                bulge = 35;
                                angle = 0.15;
                            }
                            else if (Math.abs(prevTargetY - targetY) < 60.0f)
                            {
                                bulge = 5;
                                angle = 0.4;
                            }
                            else
                            {
                                bulge = 10;
                                angle = 0.15;
                            }
                            //g.strokeArc((prevTargetX + targetX) / 2.0, (prevTargetY + targetY) / 2.0, 10, Math.abs(targetY - prevTargetY), -30, -60, ArcType.OPEN);
                            if (prevTargetY < targetY)
                            {
                                g.beginPath();
                                g.moveTo(prevTargetX, prevTargetY);
                                g.bezierCurveTo(prevTargetX + bulge, prevTargetY, prevTargetX + bulge, targetY, prevTargetX, targetY);
                                g.stroke();
                                g.strokeLine(targetX - 14.4 * Math.sin(angle), targetY - 14.4 * Math.cos(angle), targetX, targetY);
                                g.strokeLine(targetX + 14.4 * Math.cos(angle), targetY - 14.4 * Math.sin(angle), targetX, targetY);
                            }
                            else
                            {
                                // Draw line down to turn-back, then back up again:
                                double turnBack = overlay.sceneYToCodeOverlayY(bounds.getMinY()) + b.getYOffsetOfTurnBack();
                                g.beginPath();
                                g.moveTo(prevTargetX, prevTargetY);
                                g.bezierCurveTo(prevTargetX + bulge, prevTargetY, prevTargetX + bulge, turnBack, prevTargetX, turnBack);
                                g.bezierCurveTo(targetX - 2 * bulge, turnBack, targetX - 2 * bulge, targetY, targetX, targetY);
                                g.stroke();

                                g.strokeLine(targetX + 14.4 * Math.sin(angle), targetY + 14.4 * Math.cos(angle), targetX, targetY);
                                g.strokeLine(targetX - 14.4 * Math.cos(angle), targetY + 14.4 * Math.sin(angle), targetX, targetY);
                            }
                        }

                        g.setStroke(Color.BLUE);
                        g.setLineWidth(2.0);
                    }
                    if (prevPoint != null)
                        drawnFrom.add(prevPoint);
                }
                prevTargetX = targetX;
                prevTargetY = targetY + 5;
                prevPoint = b;
            }
        }
    }

    // package-visible
    ObservableBooleanValue debugVarVisibleProperty()
    {
        return debugVarVisibleProperty;
    }

    @OnThread(Tag.FXPlatform)
    public void addExtends(String className)
    {
        withTopLevelFrame(f -> {
            f.addExtendsClassOrInterface(className);
            saveAfterAutomatedEdit();
        });
    }

    @OnThread(Tag.FXPlatform)
    public void removeExtendsClass()
    {
        withTopLevelFrame(f -> {
            f.removeExtendsClass();
            saveAfterAutomatedEdit();
        });
    }

    @OnThread(Tag.FXPlatform)
    public void addImplements(String className)
    {
        withTopLevelFrame(f -> {
            f.addImplements(className);
            saveAfterAutomatedEdit();
        });
    }

    @OnThread(Tag.FXPlatform)
    public void removeExtendsOrImplementsInterface(String interfaceName)
    {
        withTopLevelFrame(f -> {
            f.removeExtendsOrImplementsInterface(interfaceName);
            saveAfterAutomatedEdit();
        });
    }

    @OnThread(Tag.FXPlatform)
    private void saveAfterAutomatedEdit()
    {
        modifiedFrame(null, true);
        editor.getWatcher().scheduleCompilation(false, CompileReason.MODIFIED, CompileType.INDIRECT_USER_COMPILE);
    }

    @OnThread(Tag.Any)
    private static enum ShowVars
    {
        NONE, FIELDS;

        @Override
        public String toString()
        {
            if (this == NONE)
                return "None";
            else
                return "Fields";
        }
    }

    //package-visible
    void showDebuggerControls(DebuggerThread thread)
    {
        if (contentRoot.getBottom() != null)
            return; // Already added
        
        HBox buttons = new HBox();
        JavaFXUtil.addStyleClass(buttons, "debugger-buttons");
        ImageView stepIcon = new ImageView(Config.getFixedImageAsFXImage("step.gif"));
        stepIcon.setRotate(90);
        Button stepButton = new Button("Step", stepIcon);
        stepButton.setOnAction(e -> project.getDebugger().runOnEventHandler(() -> thread.step()));
        ImageView continueIcon = new ImageView(Config.getFixedImageAsFXImage("continue.gif"));
        continueIcon.setRotate(90);
        Button continueButton = new Button("Continue", continueIcon);
        continueButton.setOnAction(e -> {project.getDebugger().runOnEventHandler(() -> thread.cont()); hideDebuggerControls(); });
        Button haltButton = new Button("Halt", Config.makeStopIcon(true));
        // Halt does nothing at the moment
        Label showVarLabel = new Label("Show variables: ");
        ComboBox<ShowVars> showVars = new ComboBox<>(FXCollections.observableArrayList(ShowVars.values()));
        showVars.getSelectionModel().select(0);
        debugVarVisibleProperty.bind(showVars.getSelectionModel().selectedItemProperty().isEqualTo(ShowVars.FIELDS));

        buttons.getChildren().addAll(stepButton, continueButton, haltButton, showVarLabel, showVars);
        contentRoot.setBottom(buttons);
    }
    
    private void hideDebuggerControls()
    {
        contentRoot.setBottom(null);
    }

    /**
     * Note: very important that this is a static inner class, so that a reference
     * is not retained to the outer FrameEditorTab class.
     */
    private static class WeakFontSizeUpdater implements ChangeListener<Number>
    {
        private final WeakReference<FrameEditorTab> editorRef;
        public WeakFontSizeUpdater(FrameEditorTab ed)
        {
            this.editorRef = new WeakReference<FrameEditorTab>(ed);
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            FrameEditorTab ed = editorRef.get();
            if (ed == null)
            {
                // Editor has been GC-ed; remove us as listener
                observable.removeListener(this);
            }
            else
            {
                JavaFXUtil.runPlatformLater(() -> ed.updateFontSize());
            }
        }
    }

    private static void addWeakFontSizeUpdater(FrameEditorTab ed)
    {
        // Original code was:
        //JavaFXUtil.addChangeListener(PrefMgr.strideFontSizeProperty(), s -> ed.updateFontSize());
        // However, this creates a strong reference to the FrameEditorTab, which
        // prevents it ever being GC-ed.  So this method was added to prevent a strong
        // reference being held to the FrameEditorTab:
        PrefMgr.strideFontSizeProperty().addListener(new WeakFontSizeUpdater(ed));
    }

    // package visible.
    // Sets font size back to default
    void resetFontSize()
    {
        PrefMgr.strideFontSizeProperty().set(PrefMgr.DEFAULT_STRIDE_FONT_SIZE);
    }

    @OnThread(Tag.FXPlatform)
    public void searchLink(PossibleLink link, FXPlatformConsumer<Optional<LinkedIdentifier>> paramCallback)
    {

        // I know instanceof is nasty, but it makes more sense to have the logic here than
        // in the Possible*Link classes.  Doing this in lieu of algebraic data types:

        Consumer<Optional<LinkedIdentifier>> callback = new Consumer<Optional<LinkedIdentifier>>()
        {
            @Override
            @OnThread(Tag.Any)
            public void accept(Optional<LinkedIdentifier> ol)
            {
                Platform.runLater(() -> paramCallback.accept(ol));
            }
        };

        TopLevelFrame<? extends TopLevelCodeElement> topLevelFrame = getTopLevelFrame();
        if (topLevelFrame == null)
        {
            // Still loading:
            callback.accept(Optional.empty());
            return;
        }


        if (link instanceof PossibleTypeLink)
        {
            String name = ((PossibleTypeLink)link).getTypeName();

            bluej.pkgmgr.Package pkg = project.getPackage("");
            if (pkg.getAllClassnamesWithSource().contains(name))
            {
                Target t = pkg.getTarget(name);
                if (t instanceof ClassTarget)
                {
                    callback.accept(Optional.of(new LinkedIdentifier(name, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> {
                        link.getSlot().removeAllUnderlines();
                        ((ClassTarget) t).open();
                    })));
                    return;
                }
            } else
            {
                TopLevelCodeElement code = topLevelFrame.getCode();
                if (code != null)
                {
                    PackageOrClass resolved = code.getResolver().resolvePackageOrClass(name, null);
                    // Slightly hacky way of deciding if it's in the standard API:
                    if (resolved.getName().startsWith("java.") || resolved.getName().startsWith("javax."))
                    {
                        callback.accept(Optional.of(new LinkedIdentifier(name, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> getParent().openJavaCoreDocTab(resolved.getName()))));
                        return;
                    } else if (resolved.getName().startsWith("greenfoot."))
                    {
                        callback.accept(Optional.of(new LinkedIdentifier(name, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> getParent().openGreenfootDocTab(resolved.getName()))));
                        return;
                    }
                }
            }
            callback.accept(Optional.empty());
        }
        else if (link instanceof PossibleKnownMethodLink)
        {
            PossibleKnownMethodLink pml = (PossibleKnownMethodLink) link;
            final String qualClassName = pml.getQualClassName();
            final String urlSuffix = pml.getURLMethodSuffix();
            searchMethodLink(topLevelFrame.getCode(), link, qualClassName, pml.getDisplayName(), pml.getDisplayName(), urlSuffix, callback);
        }
        else if (link instanceof PossibleMethodUseLink)
        {
            PossibleMethodUseLink pmul = (PossibleMethodUseLink) link;
            // Need to find which method it is:
            List<AssistContent> candidates = editor.getAvailableMembers(topLevelFrame.getCode(), pmul.getSourcePositionSupplier().get(), Collections.singleton(CompletionKind.METHOD), true)
                .stream()
                .filter(ac -> ac.getName().equals(pmul.getMethodName()))
                .collect(Collectors.toList());
            if (candidates.size() > 1)
            {
                // Try to narrow down the list to those with the right number of parameters.
                // but only if at least one match (otherwise leave them all in):
                if (candidates.stream().anyMatch(ac -> ac.getParams().size() == pmul.getNumParams()))
                {
                    candidates.removeIf(ac -> ac.getParams().size() != pmul.getNumParams());
                }
            }

            // At this point, just pick the first in the list if any are available:
            if (candidates.size() >= 1)
            {
                AssistContent ac = candidates.get(0);
                String displayName = ac.getName() + "(" + ac.getParams().stream().map(ParamInfo::getUnqualifiedType).collect(Collectors.joining(", ")) + ")";
                searchMethodLink(topLevelFrame.getCode(), link, ac.getDeclaringClass(), ac.getName(), displayName, PossibleKnownMethodLink.encodeSuffix(ac.getName(), Utility.mapList(ac.getParams(), ParamInfo::getQualifiedType)), callback);
            }
            else
            {
                // Otherwise, can't find it anywhere:
                callback.accept(Optional.empty());
            }
        }
        else if (link instanceof PossibleVarLink)
        {
            final String name = ((PossibleVarLink)link).getVarName();
            final CodeElement el = ((PossibleVarLink) link).getUsePoint();
            FrameEditorTab ed = (FrameEditorTab)ASTUtility.getTopLevelElement(el).getEditor();
            if (ed == FrameEditorTab.this) {
                callback.accept(Optional.of(new LinkedIdentifier(name, link.getStartPosition(), link.getEndPosition(), link.getSlot(),  () -> el.show(ShowReason.LINK_TARGET))));
            }
            else {
                callback.accept(Optional.of(new LinkedIdentifier(name, link.getStartPosition(), link.getEndPosition(), link.getSlot(),  () -> {
                    getParent().setWindowVisible(true, ed);
                    // TODO gets tricky here; what if editor hasn't been loaded yet?
                    el.show(ShowReason.LINK_TARGET);
                })));
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private void searchMethodLink(TopLevelCodeElement code, PossibleLink link, String qualClassName, String methodName, String methodDisplayName, String urlSuffix, Consumer<Optional<LinkedIdentifier>> callback)
    {
        bluej.pkgmgr.Package pkg = project.getPackage("");
        if (pkg.getAllClassnamesWithSource().contains(qualClassName))
        {
            Target t = pkg.getTarget(qualClassName);
            if (t instanceof ClassTarget)
            {
                ClassTarget classTarget = (ClassTarget) t;
                callback.accept(Optional.of(new LinkedIdentifier(methodDisplayName, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> {
                    link.getSlot().removeAllUnderlines();
                    classTarget.open();
                    classTarget.getEditor().focusMethod(methodName, null);
                })));
                return;
            }
        }
        else
        {
            if (code != null)
            {
                PackageOrClass resolved = code.getResolver().resolvePackageOrClass(qualClassName, null);
                // Slightly hacky way of deciding if it's in the standard API:
                if (resolved.getName().startsWith("java.") || resolved.getName().startsWith("javax."))
                {
                    callback.accept(Optional.of(new LinkedIdentifier(methodDisplayName, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> getParent().openJavaCoreDocTab(resolved.getName(), urlSuffix))));
                    return;
                }
                else if (resolved.getName().startsWith("greenfoot."))
                {
                    callback.accept(Optional.of(new LinkedIdentifier(methodDisplayName, link.getStartPosition(), link.getEndPosition(), link.getSlot(), () -> getParent().openGreenfootDocTab(resolved.getName(), urlSuffix))));
                    return;
                }
            }
        }
        callback.accept(Optional.empty());
    }

    @OnThread(Tag.FXPlatform)
    void enableCycleBirdseyeView()
    {
        // Clear the selection when entering bird's eye view:
        selection.clear();
        
        if (viewProperty.get() == View.NORMAL && getTopLevelFrame().canDoBirdseye())
        {
            if (viewChange != null)
                viewChange.stop();
            viewChange = new SharedTransition();
            viewChange.addOnStopped(() -> JavaFXUtil.runAfter(Duration.millis(50), () -> {
                    birdseyeSelectionPane.setVisible(true);
                    calculateBirdseyeRectangle();
            }));

            birdseyeDefaultFocusAfter = scroll.getScene().getFocusOwner();
            birdseyeDefaultRequestFocusAfter = () -> { if (birdseyeDefaultFocusAfter != null) birdseyeDefaultFocusAfter.requestFocus(); };
            birdseyeManager = getTopLevelFrame().prepareBirdsEyeView(viewChange);

            changeViewMode(View.BIRDSEYE_NODOC, Frame.ViewChangeReason.MENU_OR_SHORTCUT);
            setupAnimateViewTo(View.NORMAL, View.BIRDSEYE_NODOC, viewChange);

            viewChange.animateOver(Duration.millis(500));
        }
        else if (viewProperty.get() == View.JAVA_PREVIEW)
        {
            disableJavaPreview(Frame.ViewChangeReason.MENU_OR_SHORTCUT);
            enableCycleBirdseyeView();
        }
        else if (viewProperty.get().isBirdseye())
        {
            if (viewChange != null)
                viewChange.stop();
            viewChange = new SharedTransition();

            viewChange.addOnStopped(() -> JavaFXUtil.runAfter(Duration.millis(50), () -> {
                calculateBirdseyeRectangle();
            }));
            JavaFXUtil.addChangeListener(viewChange.getProgress(), t -> calculateBirdseyeRectangle());
            
            View oldView = viewProperty.get();
            View newView = viewProperty.get() == View.BIRDSEYE_DOC ? View.BIRDSEYE_NODOC : View.BIRDSEYE_DOC;
            changeViewMode(newView, Frame.ViewChangeReason.MENU_OR_SHORTCUT);
            setupAnimateViewTo(oldView, newView, viewChange);

            viewChange.animateOver(Duration.millis(500));
        }
    }

    @OnThread(Tag.FXPlatform)
    void disableBirdseyeView(Node viewTarget, Frame.ViewChangeReason reason, FXRunnable requestFocus)
    {
        if (viewProperty.get().isBirdseye())
        {
            if (viewChange != null)
                viewChange.stop();
            viewChange = new SharedTransition();

            birdseyeSelectionPane.setVisible(false);
            birdseyeManager = null;
            FXRunnable remove = JavaFXUtil.addChangeListener(viewTarget.localToSceneTransformProperty(), t -> {
                if (!inScrollTo)
                    scrollTo(viewTarget, -200.0);
            });
            viewChange.addOnStopped(() -> {
                remove.run();
                if (requestFocus != null)
                {
                    requestFocus.run();
                }
            });
            View oldView = viewProperty.get();
            changeViewMode(View.NORMAL, reason);
            setupAnimateViewTo(oldView, View.NORMAL, viewChange);
            viewChange.animateOver(Duration.millis(500));
        }
    }

    @OnThread(Tag.FXPlatform)
    void disableBirdseyeView(Frame.ViewChangeReason reason)
    {
        disableBirdseyeView(birdseyeDefaultFocusAfter, reason, birdseyeDefaultRequestFocusAfter);
    }

    @OnThread(Tag.FXPlatform)
    void enableJavaPreview(Frame.ViewChangeReason reason)
    {
        if (viewProperty.get() == View.NORMAL)
        {
            selection.clear();
            if (viewChange != null)
                viewChange.stop();
            viewChange = new SharedTransition();
            changeViewMode(View.JAVA_PREVIEW, reason);
            setupAnimateViewTo(View.NORMAL, View.JAVA_PREVIEW, viewChange);
            viewChange.animateOver(Duration.millis(3000));
        }
        else if (viewProperty.get().isBirdseye())
        {
            disableBirdseyeView(reason);
            enableJavaPreview(reason);
        }
    }

    @OnThread(Tag.FXPlatform)
    void disableJavaPreview(Frame.ViewChangeReason reason)
    {
        if (viewProperty.get() == View.JAVA_PREVIEW)
        {
            if (viewChange != null)
                viewChange.stop();
            viewChange = new SharedTransition();
            changeViewMode(View.NORMAL, reason);
            setupAnimateViewTo(View.JAVA_PREVIEW, View.NORMAL, viewChange);
            viewChange.animateOver(Duration.millis(3000));
        }
    }

    /**
     * Records the change of the View mode and sets the view property to the new mode.
     *
     * @param newView              The new view mode that been switch to.
     * @param reason               The user interaction which triggered the change.
     */
    @OnThread(Tag.FXPlatform)
    private void changeViewMode(View newView, Frame.ViewChangeReason reason)
    {
        recordViewChange(viewProperty.get(), newView, reason);
        viewProperty.set(newView);
    }

    @OnThread(Tag.FXPlatform)
    private void setupAnimateViewTo(View oldView, View newView, SharedTransition animateProgress)
    {
        FrameCursor fixpoint = getFocusedCursor();
        double y = fixpoint == null ? 0 : (fixpoint.getSceneBounds().getMinY() - scroll.localToScene(scroll.getBoundsInLocal()).getMinY());

        getTopLevelFrame().getAllFrames().forEach(f -> f.setView(oldView, newView, animateProgress));

        if (fixpoint != null)
        {
            final FrameCursor finalFixpoint = fixpoint;
            FXRunnable remove = JavaFXUtil.addChangeListener(getFocusedCursor().getNode().localToSceneTransformProperty(), ignore -> {
                scrollTo(finalFixpoint.getNode(), -y);
            });
            // runLater, otherwise we have a step change at the end where we don't keep the cursor in the same spot:
            animateProgress.addOnStopped(() -> JavaFXUtil.runAfterCurrent(remove));
        }

        getParent().scheduleUpdateCatalogue(this, newView == View.NORMAL ? getFocusedCursor() : null, CodeCompletionState.NOT_POSSIBLE, false, newView, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public BooleanProperty cheatSheetShowingProperty()
    {
        return getParent().catalogueShowingProperty();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void focusWhenShown()
    {
        withTopLevelFrame(f -> f.focusOnBody(TopLevelFrame.BodyFocus.BEST_PICK));
    }

    @OnThread(Tag.FXPlatform)
    public void cancelFreshState()
    {
        withTopLevelFrame(f -> f.getAllFrames().forEach(Frame::markNonFresh));
    }

    /**
     * Called by FXTabbedEditor when drag ends on us, at the position of the last
     * draggedToTab call.
     */
    // package-visible
    @OnThread(Tag.FXPlatform)
    void dragEndTab(List<Frame> dragSourceFrames, boolean fromShelf, boolean copying)
    {
        // First, move the blocks:
        if (dragSourceFrames != null && !dragSourceFrames.isEmpty())
        {  
            if (dragTarget != null)
            {
                // Check all of them can be dragged to new location:
                boolean canMove = dragSourceFrames.stream().allMatch(src -> dragTarget.getParentCanvas().acceptsType(src));

                if (canMove && !FXTabbedEditor.isUselessDrag(dragTarget, dragSourceFrames, copying))
                {
                    beginRecordingState(dragTarget);
                    performDrag(dragSourceFrames, fromShelf, copying);
                    endRecordingState(dragTarget);
                }
                selection.clear();

                // Then stop showing cursor as drag target:
                dragTarget.stopShowAsDropTarget();
                dragTarget = null;
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private void performDrag(List<Frame> dragSourceFrames, boolean fromShelf, boolean copying)
    {
        boolean shouldDisable = !dragTarget.getParentCanvas().getParent().getFrame().isFrameEnabled();

        editor.recordEdits(StrideEditReason.FLUSH);

        // We must add blocks in reverse order after cursor:
        Collections.reverse(dragSourceFrames);
        List<CodeElement> elements = GreenfootFrameUtil.getElementsForMultipleFrames(dragSourceFrames);
        for (CodeElement codeElement : elements) {
            final Frame frame = codeElement.createFrame(this);
            dragTarget.insertBlockAfter(frame);
            if (shouldDisable)
                frame.setFrameEnabled(false);
        }
        if (!copying)
            dragSourceFrames.forEach(src -> src.getParentCanvas().removeBlock(src));

        editor.recordEdits(fromShelf ? StrideEditReason.FRAMES_DRAG_SHELF : StrideEditReason.FRAMES_DRAG);
    }

    /**
     * Called by TabbedEditor when the drag location is updated, and we are the
     * currently active tab.
     */
    //package-visible
    @OnThread(Tag.FXPlatform)
    void draggedToTab(List<Frame> dragSourceFrames, double sceneX, double sceneY, boolean copying)
    {
        Bounds scrollBounds = scroll.localToScene(scroll.getBoundsInLocal());
        if (sceneX < scrollBounds.getMinX() || sceneX > scrollBounds.getMaxX())
        {
            // Drag has moved out of editor section, don't show any drag target for now:
            if (dragTarget != null) {
                dragTarget.stopShowAsDropTarget();
                dragTarget = null;
            }
        }
        else
        {
            FrameCursor newDragTarget = getTopLevelFrame().findCursor(sceneX, sceneY, null, null, dragSourceFrames, true, true);
            if (newDragTarget != null && dragTarget != newDragTarget)
            {
                if (dragTarget != null) {
                    dragTarget.stopShowAsDropTarget();
                    dragTarget = null;
                }
                boolean src = FXTabbedEditor.isUselessDrag(newDragTarget, dragSourceFrames, copying);
                boolean acceptsAll = true;
                for (Frame srcFrame : dragSourceFrames) {
                    acceptsAll &= newDragTarget.getParentCanvas().acceptsType(srcFrame);
                }
                newDragTarget.showAsDropTarget(src, acceptsAll, copying);
                dragTarget = newDragTarget;
            }
        }
        
        if (dragTarget != null)
        {
            dragTarget.updateDragCopyState(copying);
        }
    }

    /**
     * Called by TabbedEditor when we are no longer the target tab during a drag.
     * 
     * We just have to tidy up any display of potential drag targets.
     */
    //package-visible
    @OnThread(Tag.FXPlatform)
    void draggedToAnotherTab()
    {
        if (dragTarget != null)
        {
            dragTarget.stopShowAsDropTarget();
            dragTarget = null;
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void clickNearestCursor(double sceneX, double sceneY, boolean shiftDown)
    {
        FrameCursor target = getTopLevelFrame().findCursor(sceneX, sceneY, null, null, null, false, true);
        if (target != null) {
            if (shiftDown && viewProperty.get() != View.JAVA_PREVIEW)
            {
                // We need to calculate the other end of the selection.
                // If there's no selection, it's the currently focused cursor
                // If there is a selection, it's the end which is not the currently focused cursor
                FrameCursor anchor;
                if (selection.getSelected().size() == 0)
                {
                    anchor = getFocusedCursor();
                }
                else
                {
                    anchor = (selection.getCursorAfter() == getFocusedCursor()) ? selection.getCursorBefore() : selection.getCursorAfter();
                }

                if (getFocusedCursor() == null || target.getParentCanvas() != anchor.getParentCanvas())
                {
                    return; // Ignore the click; invalid action
                }

                selection.set(target.getParentCanvas().framesBetween(anchor, target));
                target.requestFocus();
            }
            else
            {
                target.requestFocus();
                // Since it wasn't a shift-click, clear the selection:
                selection.clear();
            }
        }
    }

    public FrameDictionary<StrideCategory> getDictionary()
    {
        return StrideDictionary.getDictionary();
    }

    @Override
    public void setupFrameCursor(FrameCursor f)
    {
        // We use "simple press-drag-release" here, so the events are all delivered to the original cursor:

        f.getNode().setOnDragDetected(e -> {
            selectingByDrag = true;
            e.consume();
        });

        f.getNode().setOnMouseDragged(e -> {
            if (!selectingByDrag || viewProperty.get() == View.JAVA_PREVIEW)
                return;

            FrameCanvas fCanvas = f.getParentCanvas();

            FrameCursor closest = fCanvas.getParent().findCursor(e.getSceneX(), e.getSceneY(), fCanvas.getFirstCursor(), fCanvas.getLastCursor(), null, true, false);
            if (closest != null)
                selection.set(fCanvas.framesBetween(closest, f));

            e.consume();
        });

        f.getNode().setOnMouseReleased(e -> {
            if (selectingByDrag)
            {
                selectingByDrag = false;
                e.consume();
            }
        });

        JavaFXUtil.addFocusListener(f.getNode(), new FXPlatformConsumer<Boolean>()
        {
            private FXRunnable cancelTimer;

            @OnThread(Tag.FXPlatform)
            public void accept(Boolean focused)
            {
                if (getParent() != null)
                    getParent().scheduleUpdateCatalogue(FrameEditorTab.this, focused ? f : null, CodeCompletionState.NOT_POSSIBLE, !selection.getSelected().isEmpty(), getView(), Collections.emptyList(), Collections.emptyList());

                if (cancelTimer != null)
                {
                    cancelTimer.run();
                    cancelTimer = null;
                }

                if (!focused)
                {
                    hideError();
                }
                else
                {
                    cancelTimer = JavaFXUtil.runRegular(Duration.millis(1000), this::updateFocusedDisplay);
                }
            }

            @OnThread(Tag.FXPlatform)
            private void hideError()
            {
                if (cursorErrorDisplay != null)
                {
                    cursorErrorDisplay.hide();
                    cursorErrorDisplay = null;
                }
            }

            @OnThread(Tag.FXPlatform)
            private void updateFocusedDisplay()
            {
                if (!f.getNode().focusedProperty().get())
                {
                    // Must have lost focus while we were being scheduled
                    hideError();
                    return;
                }

                // We favour the frame after the cursor, in the case that they both have errors:
                Optional<CodeError> maybeErr = Optional.ofNullable(f.getFrameAfter()).flatMap(fr -> fr.getCurrentErrors().findFirst());
                if (maybeErr.isPresent())
                {
                    if (cursorErrorDisplay != null && maybeErr.get() == cursorErrorDisplay.getError())
                    {
                        return;
                    } else
                    {
                        hideError();
                    }

                    cursorErrorDisplay = new ErrorAndFixDisplay(FrameEditorTab.this, "Below: ", maybeErr.get(), null);
                    cursorErrorDisplay.showAbove(f.getNode());
                } else
                {
                    // If not after, then check before:
                    maybeErr = Optional.ofNullable(f.getFrameBefore()).flatMap(fr -> fr.getCurrentErrors().findFirst());
                    if (maybeErr.isPresent())
                    {
                        if (cursorErrorDisplay != null && maybeErr.get() == cursorErrorDisplay.getError())
                        {
                            return;
                        } else
                        {
                            hideError();
                        }

                        cursorErrorDisplay = new ErrorAndFixDisplay(FrameEditorTab.this, "Above: ", maybeErr.get(), null);
                        cursorErrorDisplay.showBelow(f.getNode());
                    } else
                    {
                        // And if neither before nor after, check the enclosing frame:
                        maybeErr = Optional.ofNullable(f.getParentCanvas().getParent()).map(CanvasParent::getFrame).flatMap(fr -> fr.getCurrentErrors().findFirst());
                        // Only show the error if it's visible (it may not be, if the enclosing frame is still fresh):
                        if (maybeErr.isPresent() && maybeErr.get().visibleProperty().get())
                        {
                            if (cursorErrorDisplay != null && maybeErr.get() == cursorErrorDisplay.getError())
                            {
                                return;
                            } else
                            {
                                hideError();
                            }

                            cursorErrorDisplay = new ErrorAndFixDisplay(FrameEditorTab.this, "Enclosing frame: ", maybeErr.get(), null);
                            cursorErrorDisplay.showBelow(f.getNode());
                        } else
                        {
                            hideError();
                        }
                    }
                }
            }
        });
        
        // Add same focus listeners as slot component:
        setupFocusable(new CursorOrSlot(f), f.getNode());
    }

    @Override
    public void setupFrame(final Frame f)
    {
        JavaFXUtil.listenForContextMenu(f.getNode(), (x, y) -> {
            if (viewProperty.get() != View.NORMAL)
                return true;

            if (!selection.contains(f)) {
                selection.set(Arrays.asList(f));
            }

            if (menu != null) {
                menu.hide();
            }
            menu = selection.getContextMenu();
            if (menu != null) {
                menu.show(f.getNode(), x, y);
                return true;
            }
            return false;
        });

        // If we don't consume mouse pressed events, then the mouse pressed can get transferred
        // to the scroll pane, which then also messes up the click handling.  So this is needed:
        f.getNode().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (getFocusedCursor() != null && e.isShiftDown())
                e.consume();
        });


        f.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.isStillSincePress())
            {
                if (f.leftClicked(event.getSceneX(), event.getSceneY(), event.isShiftDown())) {
                    event.consume();
                }
            }
        });
        
        
        // We use "simple press-drag-release" here, so the events are all delivered to the original cursor:
        
        FXTabbedEditor.setupFrameDrag(f, false, this::getParent, () -> {
            if (dragTarget != null)
            {
                throw new IllegalStateException("Drag begun while drag in progress");
            }

            // No dragging allowed while showing Java preview:
            return viewProperty.get() != Frame.View.JAVA_PREVIEW;
        }, this::getSelection);
        
    }

    @Override
    public FrameCursor createCursor(FrameCanvas parent)
    {
        return new FrameCursor(this, parent);
    }

    public TopLevelCodeElement getSource()
    {
        if (getTopLevelFrame() == null) {
            // classFrame can be null at points in the loading sequence,
            // so just return null if we are called at that awkward time:
            return null;
        }
        return getTopLevelFrame().getCode();
    }
    
    private void regenerateCode()
    {
        if (getTopLevelFrame() != null)
            getTopLevelFrame().regenerateCode();
    }

    // Flag existing errors as old, generally happens just prior to compilation
    // They will be removed by a later call to removeOldErrors
    @OnThread(Tag.FXPlatform)
    public void flagErrorsAsOld()
    {
        if (getTopLevelFrame() != null)
            getTopLevelFrame().flagErrorsAsOld();
    }

    @OnThread(Tag.FXPlatform)
    public void removeOldErrors()
    {
        if (getTopLevelFrame() != null)
            getTopLevelFrame().removeOldErrors();
        errors = null;
        // TODO we should only really update the state when late errors arrive: 
        updateErrorOverviewBar(false);
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    void updateErrorOverviewBar(boolean waitingForCompile)
    {
        if (getTopLevelFrame() == null)
            return; // Still loading
        
        List<ErrorInfo> errors = getAllErrors()
                .filter(e -> e.getRelevantNode() != null)
                .map(e -> new ErrorInfo(e.getMessage(), e.getRelevantNode(), e.visibleProperty(), e.focusedProperty(),
                        () -> {
                            if (getView().isBirdseye()) {
                                // This case is not practically reachable, as the mouse click should have already exited
                                // the Birdseye view.
                                disableBirdseyeView(e.getRelevantNode(), Frame.ViewChangeReason.MOUSE_CLICKED,
                                        () -> e.jumpTo(this));
                            }
                            else {
                                e.jumpTo(this);
                            }
                        }))
                .collect(Collectors.toList());
        ErrorState state;
        if (waitingForCompile || getTopLevelFrame().getAllFrames().anyMatch(Frame::isFresh))
        {
            state = ErrorState.EDITING;
        }
        else
        {
            state = errors.stream().filter(e -> e.isVisible()).count()  == 0 ? ErrorState.NO_ERRORS : ErrorState.ERRORS;
        }
        
        errorOverviewBar.update(errors, state);
    }

    @OnThread(Tag.FXPlatform)
    private Stream<CodeError> getAllErrors() {
        return Stream.concat(
            getTopLevelFrame().getEditableSlots().flatMap(EditableSlot::getCurrentErrors)
            , getTopLevelFrame().getAllFrames().flatMap(Frame::getCurrentErrors));
    }

    @Override
    public void modifiedFrame(Frame f, boolean force)
    {
        if (f != null)
            f.trackBlank(); // Do this even if loading


        // If we are loading, we'll thread hop
        if (!isLoading() || force)
        {
            // After loading, this should always be on the FXPlatform thread,
            // so should always run now:
            JavaFXUtil.runNowOrLater(() ->
            {
                if ((!isLoading() && getParent() != null) || force)
                {
                    editor.codeModified();
                    registerStackHighlight(null);
                    JavaFXUtil.runNowOrLater(() -> updateErrorOverviewBar(true));
                    SuggestedFollowUpDisplay.modificationIn(this);
                }
            });
        }
    }

    @OnThread(Tag.FXPlatform)
    public void setWindowVisible(boolean vis, boolean bringToFront)
    {
        if (getParent() == null)
            // We were previously in a closed window, need to add ourselves again:
            parent.setValue(project.getDefaultFXTabbedEditor());

        // Add ourselves, in case we were closed previously (no harm in calling twice)
        if (vis)
            getParent().addTab(this, vis, bringToFront);
        getParent().setWindowVisible(vis, this);
        if (bringToFront)
            getParent().bringToFront(this);
    }

    @OnThread(Tag.FXPlatform)
    public boolean isWindowVisible()
    {
        return getParent() != null && getParent().containsTab(this) && getParent().isWindowVisible();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withCompletions(PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        withTopLevelFrame(_frame -> JavaFXUtil.runNowOrLater(() -> {
            TopLevelCodeElement allCode = getSource();
            handler.accept(Utility.mapList(Arrays.asList(
                editor.getCompletions(allCode, pos, completing, codeEl)
            ), AssistContentThreadSafe::copy));
        }));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withSuperConstructors(FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        TopLevelCodeElement codeEl = getSource();
        handler.accept(Utility.mapList(codeEl.getSuperConstructors(),
                c -> new AssistContentThreadSafe(new ConstructorCompletion(c, Collections.emptyMap(),
                        editor.getJavadocResolver()))));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<AssistContentThreadSafe> getThisConstructors()
    {
        TopLevelCodeElement codeEl = getSource();
        return codeEl.getThisConstructors();
    }
    
    @Override
    public void beginRecordingState(RecallableFocus f)
    {
        undoRedoManager.beginFrameState(getCurrentState(f));
    }

    @Override
    public void endRecordingState(RecallableFocus f)
    {
        undoRedoManager.endFrameState(getCurrentState(f));
    }
    
    private FrameState getCurrentState(RecallableFocus f)
    {
        regenerateCode();
        return new FrameState(getTopLevelFrame(), getSource(), f);
    }

    @OnThread(Tag.FXPlatform)
    public void undo()
    {
        if (undoRedoManager.isRecording()) {
            CursorOrSlot cursorOrSlot = focusedItem.get();
            endRecordingState(cursorOrSlot != null ? cursorOrSlot.getRecallableFocus() : null);
        }

        editor.recordEdits(StrideEditReason.FLUSH);
        undoRedoManager.startRestoring();
        updateClassContents(undoRedoManager.undo());
        undoRedoManager.stopRestoring();
        editor.recordEdits(StrideEditReason.UNDO_GLOBAL);
    }

    @OnThread(Tag.FXPlatform)
    public void redo()
    {
        editor.recordEdits(StrideEditReason.FLUSH);
        undoRedoManager.startRestoring();
        updateClassContents(undoRedoManager.redo());
        undoRedoManager.stopRestoring();
        editor.recordEdits(StrideEditReason.REDO_GLOBAL);
    }

    @OnThread(Tag.FXPlatform)
    private void updateClassContents(FrameState state)
    {
        if (state != null) {
            final ClassElement classElement = state.getClassElement(projectResolver,
                    editor.getPackage().getQualifiedName());
            getTopLevelFrame().restoreCast(classElement);
            getTopLevelFrame().regenerateCode();
            Node n = state.recallFocus(getTopLevelFrame());
            if (n != null)
            {
                ensureNodeVisible(n);
            }
        }
    }

    @Override
    public void scrollTo(Node n, double yOffsetFromTop, Duration duration /* instant if null */)
    {
        // prevent re-entrance from listeners looking to scroll to keep position:
        if (inScrollTo)
            return;
        inScrollTo = true;

        Bounds totalBound = scroll.getContent().localToScene(scroll.getContent().getBoundsInLocal());
        Bounds targetBound = n.localToScene(n.getBoundsInLocal());
        // Presumably 1.0 means that the bottom edge of the scroll pane is at the end,
        //   and the top edge is viewportHeight from the end, or (totalHeight - viewportHeight) from the beginning
        // While 0.0 means that the top edge of the scroll pane is at the beginning
        // So I figure that the coordinate of the top edge is given by:
        //   vvalue * (totalHeight - viewportHeight)
        //
        // Thus if we want to set the top edge to be at a given Y:
        //   Y = vvalue * (totalHeight - viewportHeight)
        //   vvalue = Y / (totalHeight - viewportHeight)
        
        double totalMinusView = totalBound.getHeight() - scroll.getHeight();
        double targetV = Math.max(0.0, Math.min(1.0, (targetBound.getMinY() + yOffsetFromTop - totalBound.getMinY()) / totalMinusView));
        
        //Debug.message("Scrolling to target: " + targetV + " on the basis of: " + targetBound + " and: " + totalBound + " and: " + scroll.getHeight());
        
        // targetV is a value from 0 to 1.  Technically, the vvalue for a scroll pane
        // can be between vmin and vmax.  Practically, vmin and vmax always seem to be
        // 0 and 1, but in case that changes:
        targetV = scroll.getVmin() + targetV * (scroll.getVmax() - scroll.getVmin());

        if (duration == null)
        {
            // Instant:
            scroll.setVvalue(targetV);
        }
        else
        {
            // Animate:
            animatingScroll  = true;
            Timeline t = new Timeline(new KeyFrame(duration, new KeyValue(scroll.vvalueProperty(), targetV)));
            t.setOnFinished(e -> { animatingScroll = false; });
            t.play();
        }
        inScrollTo = false;
    }

    @Override
    public FrameSelection getSelection()
    {
        return selection;
    }

    @OnThread(Tag.FXPlatform)
    public void saved()
    {
        getTopLevelFrame().saved();
        getTopLevelFrame().getEditableSlots().forEach(EditableSlot::saved);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withAccessibleMembers(PosInSourceDoc pos,
            Set<CompletionKind> kinds, boolean includeOverriden, FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        TopLevelCodeElement allCode = getSource();
        handler.accept(Utility.mapList(editor.getAvailableMembers(allCode, pos, kinds, includeOverriden)
                , AssistContentThreadSafe::copy));
    }

    @OnThread(Tag.FXPlatform)
    //package-visible
    void regenerateAndReparse()
    {
        regenerateCode();
        // Update positions in Java source file:
        if (getTopLevelFrame() != null)
        {
            TopLevelCodeElement code = getTopLevelFrame().getCode();
            code.updateSourcePositions();
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void afterRegenerateAndReparse(FXPlatformRunnable action)
    {
        withTopLevelFrame(f -> {
            regenerateAndReparse();
            if (action != null)
            {
                action.run();
            }
        });
    }

    @OnThread(Tag.FXPlatform)
    private void updateDisplays()
    {
        // Go through methods and set override tags:
        CodeElement el;
        if (getTopLevelFrame() != null && (el = getTopLevelFrame().getCode()) != null)
        {
            getTopLevelFrame().getAllFrames().forEach(f -> {
                if (f instanceof NormalMethodFrame) {
                    ((NormalMethodFrame) f).updateOverrideDisplay((ClassElement) el);
                }
            });
        }
    }

    @OnThread(Tag.FXPlatform)
    private void updateFontSize()
    {
        // We don't bind because topLevelFrame may change
        getTopLevelFrame().getNode().setStyle(getFontCSS().get());

        JavaFXUtil.runAfter(Duration.millis(500),
            () -> getTopLevelFrame().getAllFrames().forEach(Frame::fontSizeChanged));
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    void decreaseFontSize()
    {
        final IntegerProperty fontSize = PrefMgr.strideFontSizeProperty();
        Utility.decreaseFontSize(fontSize);
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    void increaseFontSize()
    {
        final IntegerProperty fontSize = PrefMgr.strideFontSizeProperty();
        Utility.increaseFontSize(fontSize);
    }

    @Override
    public StringExpression getFontCSS()
    {
        if (strideFontSizeAsString == null)
            strideFontSizeAsString = PrefMgr.strideFontSizeProperty().asString();
        if (strideFontCSS == null)
            strideFontCSS = Bindings.concat("-fx-font-size:", strideFontSizeAsString, "pt;");
        return strideFontCSS;
    }

    @Override
    public double getFontSize()
    {
        return PrefMgr.strideFontSizeProperty().get();
    }

    private void calculateBirdseyeRectangle()
    {
        Node n = birdseyeManager.getNodeForRectangle();
        Point2D scene = n.localToScene(n.getBoundsInLocal().getMinX(), n.getBoundsInLocal().getMinY());
        // We can't ask birdseyeSelectionPane to transform because it is not visible yet
        // Ask the class frame instead:
        Point2D onPane = getTopLevelFrame().getNode().sceneToLocal(scene);
        
        birdseyeSelection.setX(onPane.getX());
        birdseyeSelection.setY(onPane.getY() + 1.5);
        birdseyeSelection.setWidth(n.getBoundsInLocal().getWidth());
        birdseyeSelection.setHeight(n.getBoundsInLocal().getHeight() - 1.5);
        
        birdseyeSelection.setFocusTraversable(true);
        birdseyeSelection.requestFocus();
        
        ensureNodeVisible(birdseyeManager.getNodeForVisibility());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds, BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler)
    {
        final Map<String, AssistContentThreadSafe> r = new HashMap<>();

        if (kinds.contains(Kind.PRIMITIVE))
            addAllToMap(r,editor.getEditorFixesManager().getPrimitiveTypes());
        addAllToMap(r, editor.getLocalTypes(superType, kinds));
        FrameEditor frameEditor = getFrameEditor();
        Utility.runBackground(() -> {
            addAllToMap(r, frameEditor.getEditorFixesManager().getImportedTypes(superType, includeSelf, kinds));
            handler.accept(r);
        });
    }

    @OnThread(Tag.Any)
    private static void addAllToMap(Map<String, AssistContentThreadSafe> r, List<AssistContentThreadSafe> acs)
    {
        for (AssistContentThreadSafe ac : acs)
            r.put(ac.getName(), ac);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void withTypes(BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler)
    {
        withTypes(null, true, Kind.all(), handler);
    }
    
    @OnThread(Tag.FXPlatform)
    public void removeImports(List<String> importTargets)
    {
        withTopLevelFrame(topLevelFrame -> JavaFXUtil.runNowOrLater(() -> {
            // Take a copy because we're going to remove:
            FrameCanvas importCanvas = topLevelFrame.getImportCanvas();
            List<Frame> frames = new ArrayList<>(importCanvas.getBlockContents());
            for (Frame frame : frames)
            {
                if (frame instanceof ImportFrame)
                {
                    ImportFrame importFrame = (ImportFrame)frame;
                    
                    if (importTargets.contains(importFrame.getImport()))
                    {
                        importCanvas.removeBlock(importFrame);
                    }
                }
            }
        }));
    }
    
    @OnThread(Tag.FXPlatform)
    public void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after)
    {
        withTopLevelFrame(topLevelFrame -> {
            for (NormalMethodFrame normalMethodFrame : (List<NormalMethodFrame>) topLevelFrame.getMethods()) {
                // Check if it already exists
                if (normalMethodFrame.getName().equals(method.getName())) {
                    insertMethodContentsIntoMethodFrame(method, normalMethodFrame);
                    after.accept(false);
                    return;
                }
            }
            // method not found, create it
            insertMethodElementAtTheEnd(method);
            after.accept(true);
        });
    }
    
    @OnThread(Tag.FXPlatform)
    public void insertMethodCallInConstructor(CallElement methodCall, FXPlatformConsumer<Boolean> after)
    {
        // TODO maybe we have to insert it into the element not the frames.
        withTopLevelFrame(topLevelFrame -> {
            if (topLevelFrame.getConstructors().isEmpty())
            {
                topLevelFrame.addDefaultConstructor();
            }
            for (ConstructorFrame constructorFrame : topLevelFrame.getConstructors())
            {
                for (CodeFrame<?> innerFrame : constructorFrame.getMembersFrames())
                {
                    if (innerFrame instanceof CallFrame)
                    {
                        CallFrame doFrame = (CallFrame) innerFrame;
                        if (doFrame.getCode().toJavaSource().toTemporaryJavaCodeString().equals(methodCall.toJavaSource().toTemporaryJavaCodeString()))
                        {
                            after.accept(true);
                            return;
                        }
                    }
                }
                // Constructor found, but doesn't contain the method call
                insertElementIntoMethod(methodCall, constructorFrame);
            }
            after.accept(false);
        });
    }

    @OnThread(Tag.FXPlatform)
    public void insertElementIntoMethod(CodeElement element, MethodFrameWithBody<? extends MethodWithBodyElement> methodFrame)
    {
        methodFrame.getLastInternalCursor().insertBlockAfter(element.createFrame(this));
    }

    @OnThread(Tag.FXPlatform)
    private void insertMethodContentsIntoMethodFrame(MethodWithBodyElement methodElement, MethodFrameWithBody<? extends MethodWithBodyElement> methodFrame)
    {
        for (CodeElement element : methodElement.getContents())
        {
            methodFrame.getLastInternalCursor().insertBlockAfter(element.createFrame(this));
        }
    }

    @OnThread(Tag.FXPlatform)
    private void insertMethodElementAtTheEnd(MethodWithBodyElement method)
    {
        getTopLevelFrame().insertAtEnd(method.createFrame(this));
    }

    public boolean containsImport(String importSrc)
    {
        return getTopLevelFrame().getImports().contains(importSrc);
    }

    public void addImport(String importSrc)
    {
        getTopLevelFrame().addImport(importSrc);
    }
    
    @Override
    public List<FileCompletion> getAvailableFilenames()
    {
        List<FileCompletion> r = new ArrayList<>();
        if (Config.isGreenfoot())
        {
            File imageDir = new File(project.getProjectDir(), "images");
            if (imageDir.exists()) {
                File[] files = imageDir.listFiles(name -> name.getName().toLowerCase().endsWith(".png")
                        || name.getName().toLowerCase().endsWith(".jpg")
                        || name.getName().toLowerCase().endsWith(".jpeg"));

                r.addAll(Utility.mapList(Arrays.asList(files), ImageCompletion::new));
            }
            File soundDir = new File(project.getProjectDir(), "sounds");
            if (soundDir.exists()) {
                File[] files = soundDir.listFiles(name -> name.getName().toLowerCase().endsWith(".wav"));

                r.addAll(Utility.mapList(Arrays.asList(files), SoundCompletion::new));
            }
        }
        else
        {
            File[] files = project.getProjectDir().listFiles(name -> name.getName().toLowerCase().endsWith(".css"));
            r.addAll(Utility.<File,FileCompletion>mapList(Arrays.asList(files), file -> new FileCompletion() {
                @Override
                public File getFile()
                {
                    return file;
                }

                @Override
                public String getType()
                {
                    return "CSS";
                }

                @Override
                public Node getPreview(double maxWidth, double maxHeight)
                {
                    return null;
                }

                @Override
                public Map<KeyCode, Runnable> getShortcuts()
                {
                    return Collections.emptyMap();
                }
            }));
        }
        return r;
    }

    @OnThread(Tag.FXPlatform)
    public void nextError()
    {
        // If we don't have an iterator, or we've reached the end, restart:
        if (errors == null || !errors.hasNext())
        {
            errors = getAllErrors().iterator();
        }
        
        if (!errors.hasNext())
        {
            // If there are no errors, we perform a real compilation, keeping class files:
            editor.getWatcher().scheduleCompilation(true, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
            return;
        }
        
        while (errors.hasNext())
        {
            CodeError e = errors.next();
            if (e.visibleProperty().get())
            {
                e.jumpTo(this);
                return;
            }
        }
        // If we still didn't find one, cancel our fresh state:
        cancelFreshState();
    }
    
    // You can pass null.
    @Override
    public void registerStackHighlight(Frame frame)
    {
        if (stackHighlight != null && stackHighlight != frame) {
            stackHighlight.removeStackHighlight();
        }
        stackHighlight = frame;
    }

    public ObservableBooleanValue initialisedProperty()
    {
        return initialised;
    }

    @Override
    public ObservableStringValue nameProperty()
    {
        // We don't just return the class frame's name property direct because
        // this method will get called by the constructor frame initialisation, before
        // the class frame has finished initialisation.  So nameProperty acts as a 
        // delay to the binding, which will be completed once the class frame has initialised.
        return nameProperty;
    }

    @Override
    public void setupFocusableSlotComponent(EditableSlot parent, Node node, boolean canCodeComplete, FXSupplier<List<ExtensionDescription>> getExtensions, List<Hint> hints)
    {
        JavaFXUtil.addFocusListener(node, focused -> {
            if (focused)
            {
                selection.clear();
            }
            getParent().scheduleUpdateCatalogue(FrameEditorTab.this, null, focused && canCodeComplete ? CodeCompletionState.POSSIBLE : CodeCompletionState.NOT_POSSIBLE, false, getView(), getExtensions.get(), hints);
        });

        node.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (dragTarget == null && e.isShortcutDown())
            {
                if (showingUnderlinesFor != parent)
                {
                    if (showingUnderlinesFor != null)
                        showingUnderlinesFor.removeAllUnderlines();
                    showingUnderlinesFor = parent;
                    parent.findLinks().stream().forEach(l -> searchLink(l, olid -> olid.ifPresent(lid -> lid.show())));
                }
            }
            else if (showingUnderlinesFor == parent)
            {
                showingUnderlinesFor = null;
                parent.removeAllUnderlines();
            }
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (showingUnderlinesFor == parent)
            {
                showingUnderlinesFor = null;
                parent.removeAllUnderlines();
            }
        });

        setupFocusable(new CursorOrSlot(parent), node);
    }
    
    private void setupFocusable(CursorOrSlot parent, Node node)
    {
        FXRunnable checkPositionChange = new FXRunnable() {
            Bounds lastBounds = boundsInScrollContent(node);
            @Override
            public void run()
            {
                if (node.isFocused())
                {
                    Bounds boundsInScroll = boundsInScrollContent(node);
                    // Only scroll if our vertical position relative to scroll has changed:
                    if (Math.abs(boundsInScroll.getMinY() - lastBounds.getMinY()) >= 1.0
                        || Math.abs(boundsInScroll.getMaxY() - lastBounds.getMaxY()) >= 1.0)
                    {
                        //Debug.message("Position changed from " + lastBounds + " to " + boundsInScroll);
                        // Must change before calling ensureNodeVisible, as we may get re-triggered by
                        // consequent changes, and need to prevent infinite loop:
                        lastBounds = boundsInScroll;
                        if (!anyButtonsPressed)
                            ensureNodeVisible(node);
                    }
                    else
                    {
                        lastBounds = boundsInScroll;
                    }
                }
            }
        };

        ChangeListener<Object> listener = (a, b, c) -> JavaFXUtil.runPlatformLater(checkPositionChange);

        // When we detect focus gain, or whenever the size/position changes and node is focused,
        // make sure we remain visible:
        JavaFXUtil.addFocusListener(node, focused -> {
            if (focused)
            {
                node.localToSceneTransformProperty().addListener(listener);
                node.boundsInLocalProperty().addListener(listener);

                focusedItem.set(parent);

                if (menu != null)
                {
                    menu.hide();
                }

                if (parent.isInsideCanvas(getTopLevelFrame().getImportCanvas()))
                {
                    getTopLevelFrame().ensureImportCanvasShowing();
                }
                
                if (!animatingScroll && !anyButtonsPressed)
                {
                    // focusedItem.set above, will have set this to false if it's a new owner.
                    if (!manualScrolledSinceLastFocusChange)
                        ensureNodeVisible(node);
                }
                
                if (getTopLevelFrame() != null)
                {
                    // Have to take it into a list, as some slots vanish when they lose focus, which
                    // causes an exception in the underlying stream iterator:
                    List<EditableSlot> lostFocusSlots = getTopLevelFrame().getEditableSlots().filter(s -> !parent.matchesSlot(s)).collect(Collectors.toList());
                    lostFocusSlots.forEach(EditableSlot::lostFocus);
                    
                    // We need to find the focused frame.  That frame, and its direct slots,
                    // and all its ancestor frames and their direct slots, if fresh, should not show errors.
                    // Every other frame and slot become no longer fresh, and thus will show errors.
                    
                    Frame focusedFrame = parent.getParentFrame();
                    HashSet<Frame> frameAndAncestors = new HashSet<>();
                    for (Frame f = focusedFrame; f != null; f = f.getParentCanvas() == null ? null : f.getParentCanvas().getParent().getFrame())
                    {
                        frameAndAncestors.add(f);
                    }
                    
                    // Now go through all frames, and if they are not in frameAndAncestors set,
                    // mark them as non-fresh.
                    for (Frame f : Utility.iterableStream(getTopLevelFrame().getAllFrames()))
                    {
                        if (!frameAndAncestors.contains(f))
                        {
                            f.markNonFresh();
                        }
                        // We tell all frames which are not the focused frame
                        // that they lost focus in order to do actions such as save-recent
                        if (f != focusedFrame)
                            f.lostFocus();
                    }
                    
                    
                }
            }
            else
            {
                node.localToSceneTransformProperty().removeListener(listener);
                node.boundsInLocalProperty().removeListener(listener);
                if (parent.equals(focusedItem.get()))
                    focusedItem.set(null);
            }
        });
    }

    @Override
    public void ensureNodeVisible(Node node)
    {
        Bounds boundsInScroll = boundsInScroll(node);
        
        if (boundsInScroll == null)
            return;

        /*
         * There is a problem to do with focusing items just added to the scene.  When an item is just added,
         * its position is usually invalid nonsense (zero, height, positioned high up in the scene.
         * If we try to requestFocus on it, we'll end up here, trying to scroll towards this invalid
         * position, which we don't want to do; we want to wait until the position is valid
         * before deciding whether to scroll.
         *
         * A reasonable test for "valid position" appears to be whether the height is at least one pixel.
         * I've seen it zero and 0.4 with an invalid position, but after that it seems to become valid.
         *
         * What we do in the case the position is invalid is line up a one-time listener on the bounds
         * which will call this method again.  If they still aren't valid we'll go into the if-statement again,
         * add a listener, come back, etc, until they are valid.
         */
        if (boundsInScroll.getHeight() < 1.0)
        {
            JavaFXUtil.addSelfRemovingListener(node.boundsInLocalProperty(), x -> ensureNodeVisible(node));
            return;
        }

        final double MIN = 75; // Minimum pixels from edge of scroll view
        final Duration SCROLL_TIME = Duration.millis(150);

        if (boundsInScroll.getMaxY() < MIN) {
            scrollTo(node, -MIN, SCROLL_TIME);
        }
        else if (boundsInScroll.getMinY() > scroll.heightProperty().get() - MIN) {
            scrollTo(node, -(scroll.heightProperty().get() - MIN), SCROLL_TIME);
        }
    }

    // Returns null if the position does not seem to be valid yet
    // (we detect this by seeing if scene Y position is different to local Y position yet
    // -- it should be for everything except the very outermost node, which we
    // aren't concerned with here
    private Bounds boundsInScroll(Node node)
    {
        Bounds local = node.getBoundsInLocal();
        Bounds scene = node.localToScene(local);
        // It's only valid if local is different from scene in Y:
        if (local.getMinY() != scene.getMinY() && local.getMaxY() != scene.getMaxY())
            return scroll.sceneToLocal(scene);
        else
            return null;
    }

    private Bounds boundsInScrollContent(Node node)
    {
        return scrollContent.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }

    @Override
    @OnThread(Tag.FX)
    public boolean isLoading()
    {
        return loading;
    }
    
    @Override
    public boolean isEditable()
    {
        return viewProperty.get() != View.JAVA_PREVIEW;
    }

    @Override
    public void setupSuggestionWindow(Stage window) {
        JavaFXUtil.addFocusListener(window, focused ->
                        getParent().scheduleUpdateCatalogue(FrameEditorTab.this, null, focused ? CodeCompletionState.SHOWING : CodeCompletionState.NOT_POSSIBLE, false, View.NORMAL, Collections.emptyList(), Collections.emptyList())
        );
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Pane getDragTargetCursorPane()
    {
        return getParent().getDragCursorPane();
    }

    @OnThread(Tag.FXPlatform)
    public void compiled()
    {
        if (getTopLevelFrame() != null)
            getTopLevelFrame().getAllFrames().forEach(Frame::compiled);
        updateDisplays();
    }

    @Override
    public void ensureImportsVisible()
    {
        if (getTopLevelFrame() != null)
            getTopLevelFrame().ensureImportCanvasShowing();
    }

    @OnThread(Tag.FXPlatform)
    void ignoreEdits(FXPlatformRunnable during)
    {
        loading = true;
        during.run();
        loading = false;
    }

    @OnThread(Tag.FXPlatform)
    public void updateCatalog(FrameCursor f)
    {
        getParent().scheduleUpdateCatalogue(FrameEditorTab.this, f, CodeCompletionState.NOT_POSSIBLE, !selection.getSelected().isEmpty(), getView(), Collections.emptyList(), Collections.emptyList());
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    List<Menu> getMenus()
    {
        return menuManager.getMenus();
    }

    @Override
    public WindowOverlayPane getWindowOverlayPane()
    {
        return windowOverlayPane;
    }

    @Override
    public CodeOverlayPane getCodeOverlayPane()
    {
        return codeOverlayPane;
    }

    @Override
    public Observable getObservableScroll()
    {
        return observableScroll;
    }

    @Override
    public DoubleExpression getObservableViewportHeight()
    {
        return viewportHeight;
    }

    View getView()
    {
        return viewProperty.get();
    }

    @Override
    public ReadOnlyObjectProperty<View> viewProperty()
    {
        return viewProperty;
    }

    @Override
    public FrameCursor getFocusedCursor()
    {
        if (focusedItem.get() == null)
            return null;
        else
            return focusedItem.get().getCursor();
    }

    //package-visible
    public Observable focusedItemObservable()
    {
        return focusedItem;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void updateErrorOverviewBar()
    {
        // This method is called as a canvas begins to unfold/fold.  So we add a delay
        // before we recalculate the positions, to make sure the canvas has reached
        // its final size.  At worst, we recalculate twice; no big deal:
        JavaFXUtil.runAfter(Duration.millis(500), () -> updateErrorOverviewBar(false));
    }

    @Override
    public Paint getHighlightColor()
    {
        return contentRoot.cssHighlightColorProperty().get();
    }

    @SuppressWarnings("unchecked")
    public void focusMethod(String methodName)
    {
        if (getTopLevelFrame() != null) {
            for (NormalMethodFrame normalMethodFrame :
                    (List<NormalMethodFrame>) getTopLevelFrame().getMethods())
            {
                // TODO include the params no etc to increase accuracy
                if (normalMethodFrame.getName().equals(methodName)) {
                    normalMethodFrame.focusName();
                }
            }
        }
        else {
            Debug.message("focusMethod @ FrameEditorTab: " + "class frame is null!" );
        }
    }

    @OnThread(Tag.FXPlatform)
    public void setParent(FXTabbedEditor parent, boolean partOfMove)
    {
        if (!partOfMove && parent != null)
            editor.getWatcher().recordOpen();
        else if (!partOfMove && parent == null)
            editor.getWatcher().recordClose();

        this.parent.set(parent);

    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    FXTabbedEditor getParent()
    {
        return parent.get();
    }

    public ObservableValue<FXTabbedEditor> windowProperty()
    {
        return parent;
    }

    //package-visible
    Project getProject()
    {
        return project;
    }

    @Override
    public ObservableStringValue windowTitleProperty()
    {
        return nameProperty();
    }

    @Override
    String getWebAddress()
    {
        return null;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void notifySelected()
    {
        editor.getWatcher().recordSelected();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void notifyUnselected()
    {
        cancelFreshState();
    }

    @Override
    @OnThread(Tag.Any)
    public FrameEditor getFrameEditor()
    {
        return editor;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Class loadClass(String className)
    {
        return project.loadClass(className);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordEdits(StrideEditReason reason)
    {
        editor.recordEdits(reason);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordCodeCompletionStarted(SlotFragment element, int index, String stem, int codeCompletionId)
    {
        recordEdits(StrideEditReason.FLUSH);
        editor.getWatcher().recordCodeCompletionStarted(null, null,
                getLocationMap().locationFor(element), index, stem, codeCompletionId);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordCodeCompletionEnded(SlotFragment element, int index, String stem, String replacement, int codeCompletionId)
    {
        recordEdits(StrideEditReason.CODE_COMPLETION);
        editor.getWatcher().recordCodeCompletionEnded(null, null,
                getLocationMap().locationFor(element), index, stem, replacement, codeCompletionId);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordUnknownCommandKey(Frame enclosingFrame, int cursorIndex, char key)
    {
        if (key < 32 || key == 127)
            return; // Don't worry about command keys

        recordEdits(StrideEditReason.FLUSH);
        editor.getWatcher().recordUnknownCommandKey(getXPath(enclosingFrame), cursorIndex, key);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordShowHideFrameCatalogue(boolean show, FrameCatalogue.ShowReason reason)
    {
        FrameCursor focusedCursor = getFocusedCursor();
        editor.getWatcher().recordShowHideFrameCatalogue(
                focusedCursor != null ? getXPath(focusedCursor.getEnclosingFrame()) : null,
                focusedCursor != null ? focusedCursor.getCursorIndex() : -1,
                show,
                reason);
    }

    @Override
    @OnThread(Tag.FX)
    public ImageView makeClassImageView()
    {
        return makeClassGraphicIcon(imageProperty, 48, true);
    }

    /**
     * Records the reason, the old view and the current one, when switching between different show modes in the Stride editor.
     *
     * @param oldView              The old view mode that been switch from.
     * @param newView              The new view mode that been switch to.
     * @param reason               The user interaction which triggered the change.
     */
    @OnThread(Tag.FXPlatform)
    public void recordViewChange(View oldView, View newView, Frame.ViewChangeReason reason)
    {
        FrameCursor focusedCursor = getFocusedCursor();
        editor.getWatcher().recordViewModeChange(
                focusedCursor != null ? getXPath(focusedCursor.getEnclosingFrame()) : null,
                focusedCursor != null ? focusedCursor.getCursorIndex() : -1,
                oldView,
                newView,
                reason);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void recordErrorIndicatorShown(int identifier)
    {
        editor.getWatcher().recordShowErrorIndicators(Collections.singletonList(identifier));
    }

    @Override
    public void showUndoDeleteBanner(int totalEffort)
    {
        //Debug.message("Total effort: " + totalEffort);
        if (totalEffort >= 15 && !undoBannerShowing)
        {
            undoBannerShowing = true;
            CircleCountdown countdown = new CircleCountdown(40, Color.BLACK, Duration.seconds(15));
            TextFlow bannerText = new TextFlow();
            BorderPane banner = new BorderPane(bannerText);
            BorderPane.setAlignment(bannerText, Pos.TOP_LEFT);

            final FrameState restoreTarget = undoRedoManager.getCurrent();
            // Must use anonymous class to have "this" reference:
            undoRedoManager.addListener(new FXRunnable() {
                @Override
                public void run()
                {
                    if (!undoRedoManager.canUndoToReference(restoreTarget, 2))
                    {
                        undoRedoManager.removeListener(this);
                        countdown.stop();
                        bannerPane.getChildren().remove(banner);
                        undoBannerShowing = false;
                    }
                }
            });
                
            JavaFXUtil.addStyleClass(bannerText, "banner-undo-delete-text");
            Button undoButton = new Button(Config.getString("frame.undobanner.button"));
            JavaFXUtil.addStyleClass(undoButton, "banner-undo-delete-button");
            undoButton.setOnAction(e -> {
                while (undoRedoManager.canUndoToReference(restoreTarget, 3))
                    undo();
            });
            bannerText.getChildren().addAll(new Text(Config.getString("frame.undobanner.text") + " "), undoButton);
            Button close = new Button(Config.getString("frame.undobanner.close"));
            JavaFXUtil.addStyleClass(close, "banner-undo-delete-close");
            
            countdown.addOnFinished(() -> {
                bannerPane.getChildren().remove(banner);
                undoBannerShowing = false;
            });
            banner.setRight(new VBox(close, countdown));
            JavaFXUtil.addStyleClass(banner, "banner-undo-delete");
            //bannerText.styleProperty().bind(new ReadOnlyStringWrapper("-fx-font-size:").concat(PrefMgr.strideFontSizeProperty().multiply(4).divide(3).asString()).concat("pt;"));
            banner.styleProperty().bind(new ReadOnlyStringWrapper("-fx-font-size:").concat(PrefMgr.strideFontSizeProperty().asString()).concat("pt;"));
            bannerPane.getChildren().add(0, banner);
            close.setOnAction(e -> {
                countdown.stop();
                bannerPane.getChildren().remove(banner);
                undoBannerShowing = false;
                // If we don't explicitly focus something, focus will vanish.  Ideally we'd refocus
                // the last item, but it's too late here to know what that is, focus is already on the button.
                focusWhenShown();
            });
        }
    }

    /**
     * It returns the path for a code frame. If it's not a code frame, then null is returned.
     *
     * @param frame The frame that its path is needed.
     * @return      If the frame is a code frame, an XPath String identifying the location of that frame.
     *              Otherwise, null.
     */
    @SuppressWarnings("unchecked")
    private String getXPath(Frame frame)
    {
        return (frame instanceof CodeFrame)
                ? getLocationMap().locationFor(((CodeFrame<? extends CodeElement>)frame).getCode())
                : null;
    }

    /**
     * It invokes the buildLocationMap method in the LocatableElement to build a location map.
     * This location map is dependent on the latest version of the code, and will invalid as soon as the code changes in future.
     *
     * @return A map from JavaFragment to XPath String identifying the location of that fragment.
     */
    private LocationMap getLocationMap()
    {
        return getTopLevelFrame().getCode().toXML().buildLocationMap();
    }

    /**
     * Set the header image (in the tab header)
     * @param image The image to use (any size).
     */
    protected void setHeaderImage(Image image)
    {
        imageProperty.set(image);
    }
}
