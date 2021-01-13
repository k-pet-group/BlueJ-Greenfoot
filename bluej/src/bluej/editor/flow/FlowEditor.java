/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020,2021  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.EditorWatcher;
import bluej.editor.TextEditor;
import bluej.editor.fixes.EditorFixesManager;
import bluej.editor.fixes.FixDisplayManager;
import bluej.editor.flow.FlowActions.FlowAbstractAction;
import bluej.editor.flow.FlowEditorPane.FlowEditorPaneListener;
import bluej.editor.flow.FlowEditorPane.LineContainer;
import bluej.editor.flow.FlowEditorPane.LineStyler;
import bluej.editor.flow.FlowEditorPane.SelectionListener;
import bluej.editor.flow.FlowEditorPane.StyledLines;
import bluej.editor.flow.FlowErrorManager.ErrorDetails;
import bluej.editor.flow.JavaSyntaxView.Display;
import bluej.editor.flow.JavaSyntaxView.ParagraphAttribute;
import bluej.editor.flow.LineDisplay.LineDisplayListener;
import bluej.editor.flow.MarginAndTextLine.MarginDisplay;
import bluej.editor.flow.StatusLabel.Status;
import bluej.editor.flow.TextLine.HighlightType;
import bluej.editor.flow.TextLine.StyledSegment;
import bluej.editor.flow.PrintDialog.PrintChoices;
import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.FlowFXTab;
import bluej.editor.stride.FrameEditor;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.ImportsCollection.LocatableImport;
import bluej.parser.ParseUtils;
import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.JavadocResolver;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.print.PrintProgressDialog;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.ExpressionCompletionCalculator;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.fixes.SuggestionList;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.editor.fixes.SuggestionList.SuggestionListParent;
import bluej.editor.fixes.SuggestionList.SuggestionShown;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.text.DefaultEditorKit;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class FlowEditor extends ScopeColorsBorderPane implements TextEditor, FlowEditorPaneListener, SelectionListener, BlueJEventListener, DocumentListener
{
    // version number
    public final static int VERSION = 400;
    // file suffixes
    public final static String CRASHFILE_SUFFIX = "#";
    // suffixes for resources
    final static String LabelSuffix = "Label";
    final static String ActionSuffix = "Action";

    private final FlowEditorPane flowEditorPane;
    private final HoleDocument document;
    private final JavaSyntaxView javaSyntaxView;
    private final FetchTabbedEditor fetchTabbedEditor;
    private final FlowFXTab fxTab;
    private final FlowActions actions;
    /** Watcher - provides interface to BlueJ core. May be null (eg for README.txt file). */
    private final EditorWatcher watcher;
    /** The Editor Quick Fixes manager associated with this Editor */
    private final EditorFixesManager editorFixesMgr;
    
    private final boolean sourceIsCode;           // true if current buffer is code
    private final List<Menu> fxMenus;
    private boolean compilationStarted;
    private boolean requeueForCompilation;
    private boolean compilationQueued;
    private boolean compilationQueuedExplicit;
    private CompileReason requeueReason;
    private CompileType requeueType;
    private final Info info;
    private final StatusLabel saveState;          // the status label
    private FlowErrorManager errorManager = new FlowErrorManager(this, enable -> {});
    private FXTabbedEditor fxTabbedEditor;
    private boolean mayHaveBreakpoints;
    private final BooleanProperty compiledProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty viewingHTML = new SimpleBooleanProperty(false); // changing this alters the interface accordingly
    private ErrorDisplay errorDisplay;
    private final BitSet breakpoints = new BitSet();
    private int currentStepLineIndex = -1;
    private ComboBox<String> interfaceToggle;
    private final WebView htmlPane;
    private String filename;                // name of file or null
    private String docFilename;             // path to javadoc html file
    private Charset characterSet;           // character set of the file
    private String windowTitle;
    private final Properties resources = Config.moeUserProps;
    // Each element is size 2: beginning (incl) and end (excl)
    private final ArrayList<int[]> findResults = new ArrayList<>();
    /**
     * list of actions that are disabled in the readme text file
     */
    private static ArrayList<String> readMeActions;

    // find functionality
    private final FindPanel finder;
    // The most recent active FindNavigator.  Returns null if there has been no search,
    // or if the document has been modified since the last search.
    private final ObjectProperty<FindNavigator> currentSearchResult = new SimpleObjectProperty<>(null);
    private String lastSearchString = "";

    /** Used to obtain javadoc for arbitrary methods */
    private final JavadocResolver javadocResolver;
    private boolean matchBrackets;
    // Each element is size 2: beginning (incl) and end (excl)
    private final ArrayList<int[]> bracketMatches = new ArrayList<>();
    /**
     * Property map, allows BlueJ extensions to associate property values with
     * this editor instance; otherwise unused.
     */
    private final HashMap<String,Object> propertyMap = new HashMap<>();
    // Blackbox data recording:
    private int oldCaretLineNumber = -1;
    private long lastModified;
    private boolean respondingToChange = false;
    private boolean ignoreChanges = false;
    private boolean showingChangedOnDiskDialog = false;
    private FXPlatformRunnable callbackOnOpen;

    private final ContextMenu editorContextMenu;

    public boolean containsSourceCode()
    {
        return sourceIsCode;
    }

    // Used during testing
    public void enableParser(boolean force)
    {
        javaSyntaxView.enableParser(force);
    }

    @Override
    public boolean marginClickedForLine(int lineIndex)
    {
        return toggleBreakpointForLine(lineIndex);
    }

    @Override
    public ContextMenu getContextMenuToShow()
    {
        // It may already be showing; if so, hide and re-show at new click position:
        editorContextMenu.hide();
        return editorContextMenu;
    }

    // Returns true if successfully flipped, false if not.
    private boolean toggleBreakpointForLine(int lineIndex)
    {
        if (watcher.breakpointToggleEvent(lineIndex + 1, !breakpoints.get(lineIndex)) == null)
        {
            breakpoints.flip(lineIndex);
            if(breakpoints.get(lineIndex))
            {
                mayHaveBreakpoints = true;
            }
            flowEditorPane.setLineMarginGraphics(lineIndex, calculateMarginDisplay(lineIndex));
            // We also reapply scopes:
            flowEditorPane.applyScopeBackgrounds(javaSyntaxView.getScopeBackgrounds());
            return true;
        }
        return false;
    }

    public void toggleBreakpoint()
    {
        toggleBreakpointForLine(flowEditorPane.getDocument().getLineFromPosition(flowEditorPane.getCaretPosition()));
    }

    @Override
    public Set<Integer> getBreakpointLines()
    {
        return breakpoints.stream().mapToObj(Integer::valueOf).collect(Collectors.toSet());
    }

    @Override
    public int getStepLine()
    {
        return currentStepLineIndex;
    }

    public FlowActions getActions()
    {
        return actions;
    }

    public Project getProject()
    {
        if (fxTabbedEditor != null)
            return fxTabbedEditor.getProject();
        return null;
    }

    public static interface FetchTabbedEditor
    {
        FXTabbedEditor getFXTabbedEditor(boolean newWindow);
    }

    /**
     * An implementation of FlowEditorPaneListener that is only used for printing.
     */
    public static class OffScreenFlowEditorPaneListener extends ScopeColorsBorderPane implements FlowEditorPaneListener
    {
        @Override
        public boolean marginClickedForLine(int lineIndex)
        {
            return false;
        }

        @Override
        public Set<Integer> getBreakpointLines()
        {
            return ImmutableSet.of();
        }

        @Override
        public int getStepLine()
        {
            return -1;
        }

        @Override
        public void showErrorPopupForCaretPos(int caretPos, boolean mousePosition)
        {
        }

        @Override
        public String getErrorAtPosition(int caretPos)
        {
            return null;
        }

        @Override
        public ContextMenu getContextMenuToShow()
        {
            return null;
        }
    }

    class UndoManager
    {
        private final DocumentUndoStack undoStack;
        private final BooleanProperty cannotRedo = new SimpleBooleanProperty(true);
        private final BooleanProperty cannotUndo = new SimpleBooleanProperty(true);

        public UndoManager(Document document)
        {
            undoStack = new DocumentUndoStack(document);
            undoStack.setStateListener(this::updateState);
        }

        private void updateState()
        {
            cannotUndo.setValue(undoStack.canUndoCount() == 0);
            cannotRedo.setValue(undoStack.canRedoCount() == 0);
        }

        public BooleanExpression cannotUndo()
        {
            return cannotUndo;
        }

        public BooleanExpression cannotRedo()
        {
            return cannotRedo;
        }

        public void undo()
        {
            int pos = undoStack.undo();
            if (pos >= 0)
            {
                flowEditorPane.positionCaret(pos);
            }
        }

        public void redo()
        {
            int pos = undoStack.redo();
            if (pos >= 0)
            {
                flowEditorPane.positionCaret(pos);
            }
        }

        public void forgetHistory()
        {
            undoStack.clear();
        }

        public void compoundEdit(FXPlatformRunnable action)
        {
            undoStack.compoundEdit(action);
        }
    }

    // package-visible:
    final UndoManager undoManager;

    public FlowEditor(FetchTabbedEditor fetchTabbedEditor, String title, EditorWatcher editorWatcher, EntityResolver parentResolver, JavadocResolver javadocResolver, FXPlatformRunnable openCallback, @OnThread(Tag.FXPlatform) BooleanExpression syntaxHighlighting, boolean sourceIsCode)
    {
        this.fxTab = new FlowFXTab(this, title);
        this.javadocResolver = javadocResolver;
        this.windowTitle = title;
        this.callbackOnOpen = openCallback;
        this.flowEditorPane = new FlowEditorPane("", this);
        this.document = flowEditorPane.getDocument();
        this.document.addListener(false, this);
        this.javaSyntaxView = new JavaSyntaxView(document, flowEditorPane, this, parentResolver, syntaxHighlighting);
        this.flowEditorPane.setErrorQuery(errorManager);
        this.undoManager = new UndoManager(document);
        this.fetchTabbedEditor = fetchTabbedEditor;
        this.watcher = editorWatcher;
        this.info = new Info();
        this.saveState = new StatusLabel(Status.SAVED, this, errorManager);
        this.actions = new FlowActions(this);
        this.htmlPane = new WebView();
        this.sourceIsCode = sourceIsCode;
        this.editorFixesMgr = new EditorFixesManager(watcher == null || watcher.getPackage() == null ? new CompletableFuture<>() : watcher.getPackage().getProject().getImports());
        htmlPane.visibleProperty().bind(viewingHTML);
        setCenter(new StackPane(flowEditorPane, htmlPane));
        this.interfaceToggle = createInterfaceSelector();
        interfaceToggle.setDisable(!sourceIsCode);
        Region toolbar = createToolbar(interfaceToggle.heightProperty());
        setTop(JavaFXUtil.withStyleClass(new BorderPane(toolbar, null, interfaceToggle, null, null), "flow-top-bar"));
        flowEditorPane.addSelectionListener(this);
        flowEditorPane.addLineDisplayListener(new LineDisplayListener()
        {
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            @Override
            public void renderedLines(int fromIncl, int toIncl)
            {
                for (int i = fromIncl; i <= toIncl; i++)
                {
                    flowEditorPane.setLineMarginGraphics(i, FlowEditor.this.calculateMarginDisplay(i));
                }
                flowEditorPane.showHighlights(HighlightType.FIND_RESULT, findResults);
                flowEditorPane.showHighlights(HighlightType.BRACKET_MATCH, bracketMatches);
            }
        });

        // create menubar and menus

        fxMenus = createMenus();
        // Must update keymap after making menu to remove shortcuts which are now handled by menu:
        actions.updateKeymap();
        //fxMenus.setAll(menus);

        BorderPane bottomArea = new BorderPane();
        JavaFXUtil.addStyleClass(bottomArea, "moe-bottom-bar");

        // create panel for info/status

        //area for new find functionality
        finder=new FindPanel(this);
        finder.setVisible(false);
        BorderPane commentsPanel = new BorderPane();
        commentsPanel.setCenter(info);
        commentsPanel.setRight(saveState);
        BorderPane.setAlignment(info, Pos.TOP_LEFT);
        BorderPane.setAlignment(saveState, Pos.CENTER_RIGHT);
        JavaFXUtil.addStyleClass(commentsPanel, "moe-bottom-status-row");
        commentsPanel.styleProperty().bind(PrefMgr.getEditorFontCSS(false));

        bottomArea.setBottom(commentsPanel);
        bottomArea.setTop(finder);
        setBottom(bottomArea);

        this.editorContextMenu = new ContextMenu(
            getActions().getActionByName(DefaultEditorKit.cutAction).makeContextMenuItem(Config.getString("editor.cutLabel")),
            getActions().getActionByName(DefaultEditorKit.copyAction).makeContextMenuItem(Config.getString("editor.copyLabel")),
            getActions().getActionByName(DefaultEditorKit.pasteAction).makeContextMenuItem(Config.getString("editor.pasteLabel"))
        );

        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getEditorFontSize(), s -> {
            javaSyntaxView.fontSizeChanged();
            flowEditorPane.fontSizeChanged();
        });

        // Repaint the flowEditorPane whenever the line numbers are shown or hidden
        JavaFXUtil.addChangeListenerPlatform(PrefMgr.flagProperty(PrefMgr.LINENUMBERS), b -> {
            flowEditorPane.repaint();
        });
    }

    /**
     * Gets the resource attribute of the MoeEditor object
     */
    private String getResource(String name)
    {
        return Config.getPropString(name, null, resources);
    }

    /**
     * Create the toolbar.
     *
     * @return The toolbar component, ready made.
     */
    private Region createToolbar(DoubleExpression buttonHeight)
    {
        // We use a tile pane because it makes all the buttons the same size as each other.
        // But we don't actually want any wrapping that TilePane gives us.  The easiest way to
        // prevent this is to make it vertical, and then restrict it to one row in height.
        // This means it "wraps" all the buttons horizontally, and won't change it's behaviour
        // if the window gets too narrow to display all the buttons.
        TilePane tilePane = new TilePane(Orientation.VERTICAL,
            createToolbarButton("compile", buttonHeight),
            createToolbarButton("undo", buttonHeight),
            createToolbarButton("cut", buttonHeight),
            createToolbarButton("copy", buttonHeight),
            createToolbarButton("paste", buttonHeight),
            createToolbarButton("find", buttonHeight),
            createToolbarButton("close", buttonHeight)
        );
        tilePane.setPrefRows(1);
        tilePane.setPrefColumns(tilePane.getChildren().size());
        tilePane.prefWidthProperty().bind(tilePane.maxWidthProperty());
        tilePane.prefHeightProperty().bind(tilePane.minHeightProperty());
        return JavaFXUtil.withStyleClass(tilePane, "flow-top-bar-buttons");
    }

    /**
     * Create a button on the toolbar.
     *  @param key  The internal key identifying the action and label
     *  @param buttonHeight The height of the buttons
     *
     */
    private ButtonBase createToolbarButton(String key, DoubleExpression buttonHeight)
    {
        final String label = Config.getString("editor." + key + LabelSuffix);
        ButtonBase button;

        String actionName = getResource(key + ActionSuffix);
        if (actionName == null) {
            actionName = key;
        }
        FlowAbstractAction action = actions.getActionByName(actionName);

        if (action != null) {
            button = action.makeButton();
            button.setText(label);
        }
        else {
            button = new Button("Unknown");
        }

        if (action == null) {
            button.setDisable(true);
            Debug.message("Moe: action not found for button " + label);
        }

        if (isNonReadmeAction(actionName) && !sourceIsCode){
            action.setEnabled(false);
        }

        // never get keyboard focus:
        button.setFocusTraversable(false);

        // Let it resize to width of other buttons:
        button.setMaxWidth(Double.MAX_VALUE);

        button.prefHeightProperty().bind(buttonHeight);
        button.setMaxHeight(Double.MAX_VALUE);

        button.getStyleClass().add("toolbar-" + key + "-button");

        return button;
    }

    /**
     * Check whether an action is not valid for the project "readme" (i.e. if it is only
     * valid for source files).
     *
     * @param actionName String representing the action name
     * @return true if it is an action that should be disabled while editing the readme file,
     *         or false otherwise
     */
    private static boolean isNonReadmeAction(String actionName)
    {
        List<String> flaggedActions = getNonReadmeActions();
        return flaggedActions.contains(actionName);
    }

    /**
     * Get a list of actions not applicable in the readme.txt file
     */
    private static ArrayList<String> getNonReadmeActions ()
    {
        if (readMeActions == null) {
            readMeActions = new ArrayList<>();
            readMeActions.add("compile");
            readMeActions.add("autoindent");
            readMeActions.add("insert-method");
            readMeActions.add("add-javadoc");
            readMeActions.add("toggle-interface-view");
        }
        return readMeActions;
    }


    /**
     * Create a combo box for the toolbar
     */
    private ComboBox<String> createInterfaceSelector()
    {
        final String interfaceString = Config.getString("editor.interfaceLabel");
        final String implementationString = Config.getString("editor.implementationLabel");
        String[] choiceStrings = {implementationString, interfaceString};
        ComboBox<String> interfaceToggle = new ComboBox<String>(FXCollections.observableArrayList(choiceStrings));

        interfaceToggle.setFocusTraversable(false);
        JavaFXUtil.addChangeListenerPlatform(interfaceToggle.valueProperty(), v -> {
            if (v.equals(interfaceString))
                switchToInterfaceView();
            else
                switchToSourceView();
        });

        return interfaceToggle;
    }

    /**
     * Create the editor's menu bar.
     */
    private List<Menu> createMenus()
    {
        return List.of(
                createMenu("class", "save - print - close"),
                createMenu("edit", "undo redo - cut-to-clipboard copy-to-clipboard paste-from-clipboard - indent-block deindent-block comment-block uncomment-block autoindent - insert-method add-javadoc"),
                createMenu("tools", "find find-next find-next-backward replace go-to-line - compile toggle-breakpoint - toggle-interface-view"),
                createMenu("option", "increase-font decrease-font reset-font - key-bindings preferences")
        );
    }

    /**
     * Create a single menu for the editor's menu bar. The key for the menu (as
     * defined in moe.properties) is supplied.
     */
    private Menu createMenu(String titleKey, String itemList)
    {
        MenuItem item;
        String label;

        // get menu title
        Menu menu = new Menu(Config.getString("editor." + titleKey + LabelSuffix));

        // cut menu definition into separate items
        String[] itemKeys = itemList.split(" ");

        // create menu item for each item
        for (String itemKey : itemKeys) {
            if (itemKey.equals("-")) {
                menu.getItems().add(new SeparatorMenuItem());
            } else {
                FlowAbstractAction action = actions.getActionByName(itemKey);
                if (action == null) {
                    Debug.message("Moe: cannot find action " + itemKey);
                }
                // Forbid Preferences from being added to the Options menu when using
                // Mac screen menubar, as it is already exist in the Application menu.
                else if ( !( Config.isMacOS() &&
                        titleKey.toLowerCase().equals("option") &&
                        itemKey.toLowerCase().equals("preferences") )
                )
                {
                    item = action.makeMenuItem();
                    menu.getItems().add(item);
                    label = Config.getString("editor." + itemKey + LabelSuffix);
                    item.setText(label);
                }
            }
        }
        return menu;
    }

    /**
     * Gets the watcher associated with this editor
     */
    public EditorWatcher getWatcher(){
        return watcher;
    }

    @Override
    public String getErrorAtPosition(int caretPos)
    {
        String errorMessage = (errorManager.getErrorAtPosition(caretPos) != null)
            ? errorManager.getErrorAtPosition(caretPos).message
            : null;
        return errorMessage;
    }

    /**
     * Notification (from the caret) that the caret position has moved.
     */
    public void selectionChanged(int caretPos, int anchorPos)
    {
        showErrorPopupForCaretPos(caretPos, false);

        actions.userAction();

        if (matchBrackets)
        {
            doBracketMatch();
        }

        // Only send caret moved event if we are open; caret moves while loading
        // but we don't want to send an edit event because of that:
        if (oldCaretLineNumber != document.getLineFromPosition(caretPos) && isOpen())
        {
            recordEdit(true);

            cancelFreshState();

            // This is a workaround to overcome a bug in RichTextFX lib,
            // which in some cases used to cause the editor to not scroll
            // to follow cursor.
            // The re-layout enforcing is inside a runAfterCurrent to avoid
            // an IllegalArgumentException caused by state inconsistency.
            JavaFXUtil.runAfterCurrent(() -> {
                getSourcePane().ensureCaretShowing();
                layout();
            });
        }
        oldCaretLineNumber = document.getLineFromPosition(caretPos);
    }

    /**
     * delegates bracket matching to the source pane's caret
     */
    private void doBracketMatch()
    {
        int originalPos = getSourcePane().getCaretPosition();
        bracketMatches.clear();
        for (Integer position : getBracketMatchPositions())
        {
            bracketMatches.add(new int[] {position, position + 1});
        }
        flowEditorPane.showHighlights(HighlightType.BRACKET_MATCH, bracketMatches);
    }

    private void removeBracketHighlight()
    {
        bracketMatches.clear();
        flowEditorPane.showHighlights(HighlightType.BRACKET_MATCH, bracketMatches);
    }

    /**
     * Returns the positions of the brackets adjacent to the caret position, and their matching bracket positions. Returns empty list if not found or not valid/appropriate
     *
     * @return the int representing bracket positions to highlight
     */
    private List<Integer> getBracketMatchPositions()
    {
        int actualCaretPos = flowEditorPane.getCaretPosition();
        ArrayList<Integer> matches = new ArrayList<>();
        for (int caretPos = Math.max(0, actualCaretPos - 1); caretPos <= Math.min(actualCaretPos, getTextLength() - 1); caretPos++)
        {
            int pos = TextUtilities.findMatchingBracket(document, caretPos);
            if (pos != -1)
            {
                matches.add(caretPos);
                matches.add(pos);
            }
        }
        return matches;
    }

    public boolean hasQuickFixShown()
    {
        return errorDisplay != null && errorDisplay.hasFixes() && errorDisplay.popup.isShowing();
    }

    public boolean hasQuickFixSelected()
    {
        return errorDisplay.hasQuickFixSelected();
    }

    @Override
    public void showErrorPopupForCaretPos(int caretPos, boolean mousePosition)
    {
        ErrorDetails err = caretPos == -1 ? null : errorManager.getErrorAtPosition(caretPos);

        // Indicator for knowing if a popup is opened and displays quick fixes
        boolean isPopupOpenedWithFixes = errorDisplay != null && errorDisplay.hasFixes() && errorDisplay.popup.isShowing();
        // Indicator for knowing if the error at the next location is the same the current error
        boolean isStillSameError = errorDisplay != null && caretPos >= errorDisplay.details.startPos && caretPos <= errorDisplay.details.endPos;
        if (err != null && !isStillSameError)
        {
            showErrorOverlay(err, caretPos);
        }
        else if (!mousePosition && !isStillSameError)
        {
            // If the keyboard moves to a different error (or out of an error)
            // always do that update:
            showErrorOverlay(err, caretPos);
        }
        else
        {
            // Only hide if it was a keyboard move
            // or it was a mouse move but there is no error at the keyboard position
            // and if the popup with fixes is not opened
            if (!isPopupOpenedWithFixes && errorDisplay != null && (!mousePosition || !errorDisplay.details.containsPosition(caretPos)))
            {
                showErrorOverlay(null, caretPos);
            }
        }
    }

    public void changeQuickFixSelection(boolean changeDownwards)
    {
        if (changeDownwards)
        {
            errorDisplay.down();
        }
        else
        {
            errorDisplay.up();
        }
    }

    void executeQuickFix(){
        errorDisplay.executeQuickFix();
    }

    public void requestEditorFocus()
    {
        flowEditorPane.requestFocus();
    }

    /**
     * Notify this editor that it has gained focus, either because its tab was selected or it is the
     * currently selected tab in a window that gained focus, or it has lost focus for the opposite
     * reasons.
     *
     * @param visible   true if the editor has focus, false otherwise
     */
    public void notifyVisibleTab(boolean visible)
    {
        if (visible) {
            if (watcher != null) {
                watcher.recordSelected();
            }
            if(PrefMgr.getFlag(PrefMgr.CHECK_DISKFILECHANGES))
                checkForChangeOnDisk();
        }
        else
        {
            // Hide any error tooltip:
            showErrorOverlay(null, 0);
        }
    }

    /**
     * Show the given error overlay, or hide the existing overlay
     * @param details If non-null, show the given error details.  If null, hide existing overlay.
     * @param displayPosition The character position at which to show.
     */
    private void showErrorOverlay(ErrorDetails details, int displayPosition)
    {
        //Debug.message("Showing error at " + displayPosition + ": " + details);
        if (details != null)
        {
            if (errorDisplay == null || errorDisplay.details != details)
            {
                // First, hide existing display:
                if (errorDisplay != null)
                {
                    ErrorDisplay old = errorDisplay;
                    old.popup.hide();
                }

                Bounds pos = null;
                boolean before = false;
                try
                {
                    // First, try to get the character after the caret:
                    pos = getSourcePane().getCaretBoundsOnScreen(displayPosition).orElse(null);

                    // That may be null if caret was at end of line, in which case try character before:
                    if (pos == null)
                    {
                        pos = getSourcePane().getCaretBoundsOnScreen(displayPosition - 1).orElse(null);
                        before = true;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    // Can happen if display position is out of bounds (while pending updates get flushed)
                    // Will fall through to null case below...
                }

                // If that still doesn't work, give up (may not be on screen)
                if (pos == null)
                    return;
                int xpos = (int)(before ? pos.getMaxX() : pos.getMinX());
                int ypos = (int)(pos.getMinY() + (4*pos.getHeight()/3));
                errorDisplay = new ErrorDisplay(this, () -> this.getWatcher(), details);
                ErrorDisplay newDisplay = errorDisplay;

                newDisplay.createPopup();
                newDisplay.popup.setAnchorLocation(AnchorLocation.WINDOW_TOP_LEFT);
                newDisplay.popup.setAnchorX(xpos);
                newDisplay.popup.setAnchorY(ypos);
                newDisplay.popup.show(getWindow());

                if (watcher != null)
                {
                    watcher.recordShowErrorMessage(details.identifier, Collections.emptyList());
                }
            }
        }
        else if (errorDisplay != null)
        {
            ErrorDisplay old = errorDisplay;
            old.popup.hide();
            errorDisplay = null;
        }
    }

    /**
     * Check whether the source file has changed on disk. If it has, reload.
     */
    private void checkForChangeOnDisk()
    {
        if (filename == null)
        {
            return;
        }
        File file = new File(filename);
        long modified = file.lastModified();
        // Prevent infinite loop which can occur when we re-enter
        // this method while regaining focus from the modal dialog.
        if (modified > lastModified + 1000 && !showingChangedOnDiskDialog)
        {
            Debug.message("File " + filename + " changed on disk; our record is " + lastModified + " but file was " + modified);
            if (saveState.isChanged())
            {
                showingChangedOnDiskDialog = true;
                int answer = DialogManager.askQuestionFX(getWindow(), "changed-on-disk");
                if (answer == 0)
                {
                    doReload();
                }
                else
                {
                    setLastModified(modified); // don't ask again for this change
                }
                showingChangedOnDiskDialog = false;
            }
            else
            {
                doReload();
            }
        }
    }


    /**
     * Schedule compilation, if any edits have occurred since last compile.
     */
    @OnThread(Tag.FXPlatform)
    public void cancelFreshState()
    {
        if (saveState.isChanged())
        {
            if (sourceIsCode)
            {
                // Save will occur as part of the future compilation:
                scheduleCompilation(CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
            }
            else
            {
                userSave();
            }
        }
    }

    public void setParent(FXTabbedEditor parent, boolean partOfMove)
    {
        if (watcher != null)
        {
            if (!partOfMove && parent != null)
            {
                watcher.recordOpen();
            }
            else if (!partOfMove && parent == null)
            {
                watcher.recordClose();
            }

            // If we are closing, force a compilation in case there are pending changes:
            if (parent == null && saveState.isChanged())
            {
                scheduleCompilation(CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
            }
        }

        this.fxTabbedEditor = parent;
    }

    /**
     * Schedule an immediate compilation for the specified reason and of the specified type.
     * @param reason  The reason for compilation
     * @param ctype   The type of compilation
     */
    public void scheduleCompilation(CompileReason reason, CompileType ctype)
    {
        if (watcher != null)
        {
            // We can collapse multiple compiles, but we cannot collapse an explicit compilation
            // (resulting class files kept) into a non-explicit compilation (result discarded).
            if (!compilationQueued)
            {
                watcher.scheduleCompilation(true, reason, ctype);
                compilationQueued = true;
            }
            else if (compilationStarted ||
                    (ctype != CompileType.ERROR_CHECK_ONLY && !compilationQueuedExplicit))
            {
                // Either: a previously queued compilation has already started
                // Or: we have queued an error-check-only compilation, but are being asked to
                //     schedule a full (explicit) compile which keeps the resulting classes.
                //
                // In either case, we need to queue a second compilation after the current one
                // finishes. We override any currently queued ERROR_CHECK_ONLY since explicit
                // compiles should take precedence:
                if (!requeueForCompilation || ctype == CompileType.ERROR_CHECK_ONLY)
                {
                    requeueForCompilation = true;
                    requeueReason = reason;
                    requeueType = ctype;
                }
            }
        }
    }


    public List<Menu> getFXMenu()
    {
        return fxMenus;
    }

    @Override
    public boolean showFile(String filename, Charset charset, boolean compiled, String docFilename)
    {
        this.filename = filename;
        this.docFilename = docFilename;
        this.characterSet = charset;

        boolean loaded = false;

        File file = new File(filename);
        if (filename != null)
        {
            setupJavadocMangler();
            try
            {
                // check for crash file
                String crashFilename = filename + FlowEditor.CRASHFILE_SUFFIX;
                String backupFilename = crashFilename + "backup";
                File crashFile = new File(crashFilename);
                if (crashFile.exists())
                {
                    File backupFile = new File(backupFilename);
                    backupFile.delete();
                    crashFile.renameTo(backupFile);
                    DialogManager.showMessageFX(fxTabbedEditor.getWindow(), "editor-crashed");
                }

                ignoreChanges = true;
                document.replaceText(0, document.getLength(), Files.readString(file.toPath(), charset).replace("\r", "").replace("\t", "    "));
                setLastModified(file.lastModified());
                // Position caret at start, not the end:
                getSourcePane().positionCaret(0);
                undoManager.forgetHistory();

                if (sourceIsCode)
                {
                    javaSyntaxView.enableParser(false);
                }
                loaded = true;
            }
            catch (IOException ex) {
                // TODO display user-visible error
                Debug.reportError("Couldn't open file", ex);
            }
            finally
            {
                ignoreChanges = false;
            }
        }
        else {
            if (docFilename != null) {
                if (new File(docFilename).exists()) {
                    showInterface(true);
                    loaded = true;
                    interfaceToggle.setDisable(true);
                }
            }
        }

        if (!loaded) {
            // should exist, but didn't
            return false;
        }

        setCompileStatus(compiled);

        return true;
    }

    /**
     * Sets up the processor for loaded Javdoc.  Currently this inserts a link
     * next to a method name to allow you to jump back to the BlueJ source, if
     * there is source code available.
     */
    private void setupJavadocMangler()
    {
        JavaFXUtil.addChangeListenerPlatform(htmlPane.getEngine().documentProperty(), doc -> {
            if (doc != null)
            {
                /* Javadoc looks like this:
                <a id="sampleMethod(java.lang.String)">
                <!--   -->
                </a>
                <ul>
                <li>
                <h4>sampleMethod</h4>
                 */

                // First find the anchor.  Ignore anchors with ids that do not end in a closing bracket (they are not methods):
                NodeList anchors = doc.getElementsByTagName("a");
                for (int i = 0; i < anchors.getLength(); i++)
                {
                    org.w3c.dom.Node anchorItem = anchors.item(i);
                    org.w3c.dom.Node anchorName = anchorItem.getAttributes().getNamedItem("id");
                    if (anchorName != null && anchorName.getNodeValue() != null && anchorName.getNodeValue().endsWith(")"))
                    {
                        // Then find the ul child, then the li child of that, then the h4 child of that:
                        org.w3c.dom.Node ulNode = findHTMLNode(anchorItem, org.w3c.dom.Node::getNextSibling, n -> "ul".equals(n.getLocalName()));
                        if (ulNode == null)
                            continue;
                        org.w3c.dom.Node liNode = findHTMLNode(ulNode.getFirstChild(), org.w3c.dom.Node::getNextSibling, n -> "li".equals(n.getLocalName()));
                        if (liNode == null)
                            continue;
                        org.w3c.dom.Node headerNode = findHTMLNode(liNode.getFirstChild(), org.w3c.dom.Node::getNextSibling, n -> "h4".equals(n.getLocalName()));
                        if (headerNode != null)
                        {
                            // Make a link, and set a listener for it:
                            Element newLink = doc.createElement("a");
                            newLink.setAttribute("style", "padding-left: 2em;cursor:pointer;");
                            newLink.insertBefore(doc.createTextNode("[Show source in BlueJ]"), null);
                            headerNode.insertBefore(newLink, null);

                            ((EventTarget) newLink).addEventListener("click", e ->
                            {
                                String[] tokens = anchorName.getNodeValue().split("[(,)]");
                                List<String> paramTypes = new ArrayList<>();
                                for (int t = 1; t < tokens.length; t++)
                                {
                                    paramTypes.add(tokens[t]);
                                }
                                focusMethod(tokens[0].equals("<init>") ? windowTitle : tokens[0], paramTypes);
                            }, false);
                        }
                    }
                }
            }
        });
    }

    /**
     * Traverses the document using the given traversal operation (next parameter),
     * until the stopWhen test returns true, beginning at the start node.  The start
     * node is not tested.  Once the traversal returns null, this method returns null.
     */
    private static org.w3c.dom.Node findHTMLNode(org.w3c.dom.Node start, UnaryOperator<org.w3c.dom.Node> next, Predicate<org.w3c.dom.Node> stopWhen)
    {
        org.w3c.dom.Node n = start;
        while (n != null)
        {
            n = next.apply(n);
            if (n != null && stopWhen.test(n))
                return n;
        }
        return null;
    }


    @Override
    public void clear()
    {
        document.replaceText(0, document.getLength(), "");
    }

    @Override
    public void insertText(String text, boolean caretBack)
    {
        int startPos = flowEditorPane.getSelectionStart();
        flowEditorPane.replaceSelection(text);
        if (caretBack)
        {
            flowEditorPane.positionCaret(startPos);
        }
    }

    @Override
    public void setSelection(SourceLocation begin, SourceLocation end)
    {
        flowEditorPane.select(document.getPosition(begin), document.getPosition(end));
    }

    @Override
    public SourceLocation getCaretLocation()
    {
        return document.makeSourceLocation(flowEditorPane.getCaretPosition());
    }

    @Override
    public void setCaretLocation(SourceLocation location)
    {
        flowEditorPane.positionCaret(document.getPosition(location));
    }

    @Override
    public SourceLocation getSelectionBegin()
    {
        return document.makeSourceLocation(flowEditorPane.getSelectionStart());
    }

    @Override
    public SourceLocation getSelectionEnd()
    {
        return document.makeSourceLocation(flowEditorPane.getSelectionEnd());
    }

    @Override
    public String getText(SourceLocation begin, SourceLocation end)
    {
        return document.getContent(document.getPosition(begin), document.getPosition(end)).toString();
    }

    @Override
    public void setText(SourceLocation begin, SourceLocation end, String newText)
    {
        document.replaceText(document.getPosition(begin), document.getPosition(end), newText);
    }

    @Override
    public SourceLocation getLineColumnFromOffset(int offset)
    {
        return document.makeSourceLocation(offset);
    }

    @Override
    public int getOffsetFromLineColumn(SourceLocation location)
    {
        return document.getPosition(location);
    }

    @Override
    public int getLineLength(int line)
    {
        return document.getLineLength(line);
    }

    @Override
    public int numberOfLines()
    {
        return document.getLineCount();
    }

    @Override
    public int getTextLength()
    {
        return document.getLength();
    }

    @Override
    public ParsedCUNode getParsedNode()
    {
        javaSyntaxView.flushReparseQueue();
        return javaSyntaxView.getParser();
    }

    @Override
    public ReparseableDocument getSourceDocument()
    {
        return javaSyntaxView;
    }

    @Override
    public void reloadFile()
    {
        doReload();
    }

    private void read(Reader reader) throws IOException
    {
        document.replaceText(0, document.getLength(), CharStreams.toString(reader).replace("\r", ""));
        // Position caret at start, not the end:
        getSourcePane().positionCaret(0);

        undoManager.forgetHistory();
    }

    /**
     * Revert the buffer contents to the last saved version. Do not ask any
     * question - just do it. Must have a file name.
     */
    public void doReload()
    {
        removeSearchHighlights();
        Reader reader = null;

        try
        {
            FileInputStream inputStream = new FileInputStream(filename);
            reader = new InputStreamReader(inputStream, characterSet);
            read(reader);
            try
            {
                reader.close();
                inputStream.close();
            }
            catch (IOException ioe)
            {
            }
            File file = new File(filename);
            setLastModified(file.lastModified());

            if (sourceIsCode)
            {
                enableParser(false);
            }

            // We want to inform the watcher that the editor content has changed,
            // and then inform it that we are in "saved" state (synced with file).
            // But first set state to saved to avoid unnecessary writes to disk.
            saveState.setState(Status.SAVED);
            setChanged(); // contents may have changed - notify watcher
            setSaved();  // notify watcher that we are saved

            scheduleCompilation(CompileReason.LOADED, CompileType.ERROR_CHECK_ONLY);
        }
        catch (FileNotFoundException ex) {
            info.message (Config.getString("editor.info.fileDisappeared"));
        }
        catch (IOException ex) {
            info.message (Config.getString("editor.info.fileReadError"));
            setChanged();
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException ioe) {}

            if (finder != null && finder.isVisible()) {
                findNext(false);
            }
        }
    }

    @Override
    public void setEditorVisible(boolean vis, boolean openInNewWindow)
    {
        FXTabbedEditor fxTabbedEditor = fetchTabbedEditor.getFXTabbedEditor(openInNewWindow);

        if (vis)
        {
            fxTabbedEditor.addTab(fxTab, vis, true);
        }
        boolean hadEffect = fxTabbedEditor.setWindowVisible(vis, fxTab);

        if (vis)
        {
            fxTabbedEditor.bringToFront(fxTab);
            if (hadEffect)
            {
                if (callbackOnOpen != null)
                {
                    callbackOnOpen.run();
                }
                
                checkBracketStatus();

                if (sourceIsCode && !compiledProperty.get())
                {
                    // Schedule a compilation so we can find and display any errors:
                    scheduleCompilation(CompileReason.LOADED, CompileType.ERROR_CHECK_ONLY);
                }
            }
            // Make sure caret is visible after open:
            getSourcePane().ensureCaretShowing();
            requestLayout();
        }
    }

    /**
     * Checks that current status of syntax highlighting option is consistent
     * with desired option eg off/on. Called when refreshing or making visible
     * to pick up any Preference Manager changes to this functionality
     */
    private void checkBracketStatus()
    {
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);
        // tidies up leftover highlight if matching is switched off
        // while highlighting a valid bracket or refreshes bracket in open
        // editor
        if (matchBrackets) {
            doBracketMatch();
        }
        else {
            removeBracketHighlight();
        }
    }

    @Override
    public boolean isOpen()
    {
        return fxTabbedEditor != null && fxTabbedEditor.isWindowVisible();
    }

    @Override
    public void save() throws IOException
    {
        IOException failureException = null;
        if (saveState.isChanged() && filename != null)
        {
            // Record any edits with the data collection system:
            recordEdit(true);

            // Play it safe and avoid overwriting code that has been changed outside BlueJ (or at least,
            // outside *this* instance of BlueJ):
            if(PrefMgr.getFlag(PrefMgr.CHECK_DISKFILECHANGES))
                checkForChangeOnDisk();
            if (!saveState.isChanged())
            {
                return;
            }

            Writer writer = null;
            try
            {
                // The crash file is used during writing and will remain in
                // case of a crash during the write operation.
                String crashFilename = filename + FlowEditor.CRASHFILE_SUFFIX;

                // make a backup to the crash file
                FileUtility.copyFile(filename, crashFilename);

                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(filename));
                writer = new OutputStreamWriter(ostream, characterSet);
                getSourcePane().write(writer);
                writer.close();
                writer = null;
                setLastModified(new File(filename).lastModified());
                File crashFile = new File(crashFilename);
                crashFile.delete();

                // Do this last, as it may trigger further actions in the watcher:
                setSaved();
            }
            catch (IOException ex)
            {
                failureException = ex;
                info.message (Config.getString("editor.info.errorSaving") + " - " + ex.getLocalizedMessage());
            }
            finally
            {
                try
                {
                    if (writer != null)
                        writer.close();
                }
                catch (IOException ex)
                {
                    failureException = ex;
                }
            }
        }

        // If an error occurred, set a message in the editor status bar, and
        // re-throw the exception.
        if (failureException != null)
        {
            info.message(Config.getString("editor.info.errorSaving")
                    + " - " + failureException.getLocalizedMessage());
            throw failureException;
        }
    }

    /**
     * Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
        // Don't need to say saved twice:
        //info.message(Config.getString("editor.info.saved"));
        saveState.setState(Status.SAVED);
        if (watcher != null)
        {
            watcher.saveEvent(this);
        }
    }

    /**
     * Notify the editor watcher of an edit (or save).
     *
     * @param includeOneLineEdits - will be true if it is considered unlikely that further edits will
     *                     be localised to previous edit locations (line), or if the file has been saved.
     */
    private void recordEdit(boolean includeOneLineEdits)
    {
        if (watcher != null)
        {
            watcher.recordJavaEdit(document.getFullContent(), includeOneLineEdits);
        }
    }

    /**
     * The editor wants to close. Do this through the EditorManager so that we
     * can be removed from the list of open editors.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void close()
    {
        cancelFreshState();

        try
        {
            save();
        }
        catch (IOException ioe)
        {
            DialogManager.showErrorTextFX(getWindow(), "Error saving source code");
        }

        doClose();
    }

    /**
     * The editor has been closed. Hide the editor window now.
     */
    public void doClose()
    {
        setEditorVisible(false, false);
        if (watcher != null)
        {
            watcher.closeEvent(this);
        }
    }

    @Override
    public void refresh()
    {
        checkBracketStatus();
        javaSyntaxView.recalculateAndApplyAllScopes(); //whenever we change the scope highlighter, call scheduleReparseRunner to create a reparser to that file: if the scope highlighter is 0, it will do nothing. However, if it is not zero, it will ensure the editor is updated accordingly.
    }

    @Override
    public void displayMessage(String message, int lineNumber, int column)
    {
        switchToSourceView();

        // highlight the line (parameter is one-based so convert to zero-based):
        int lineStart = document.getLineStart(lineNumber - 1);
        int lineEnd = document.getLineEnd(lineNumber - 1);
        getSourcePane().positionCaret(lineStart);
        getSourcePane().moveCaret(lineEnd - 1);

        // display the message
        info.messageImportant(message);
    }

    @Override
    public boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        if (compileType.showEditorOnError())
        {
            setEditorVisible(true, false);
        }

        switchToSourceView();

        if (diagnostic.getStartLine() >= 0 && diagnostic.getStartLine() < document.getLineCount())
        {
            // Limit diagnostic display to a single line.
            int startPos = document.getPosition(new SourceLocation((int)diagnostic.getStartLine(), (int) diagnostic.getStartColumn()));
            int endPos;
            if (diagnostic.getStartLine() != diagnostic.getEndLine())
            {
                endPos = document.getLineEnd((int)diagnostic.getStartLine());
            }
            else
                {
                endPos = document.getPosition(new SourceLocation((int)diagnostic.getStartLine(), (int) diagnostic.getEndColumn()));
            }

            // highlight the error and the line on which it occurs
            // If error is zero-width, make it one character wide:
            if (endPos == startPos)
            {
                // By default, extend one char right, unless that would encompass a newline:
                if (endPos < getTextLength() - 1 && !document.getContent(endPos, endPos + 1).equals("\n"))
                {
                    endPos += 1;
                }
                else if (startPos > 0 && !document.getContent(startPos - 1, startPos).equals("\n"))
                {
                    startPos -= 1;
                }
            }
            errorManager.addErrorHighlight(startPos, endPos, diagnostic.getMessage(), diagnostic.getIdentifier());
        }

        return true;
    }

    private void switchToSourceView()
    {
        if (!viewingHTML.get()) {
            return;
        }
        resetMenuToolbar(true);
        viewingHTML.set(false);
        interfaceToggle.getSelectionModel().selectFirst();
        watcher.showingInterface(false);
        clearMessage();
        flowEditorPane.requestFocus();
    }

    /**
     * Switch on the javadoc interface view (it it isn't showing already). If
     * necessary, generate it first.
     */
    private void switchToInterfaceView()
    {
        if (viewingHTML.get()) {
            return;
        }
        resetMenuToolbar(false);
        //NAVIFX
        //dividerPanel.beginTemporaryHide();
        try {
            save();
            info.message(Config.getString("editor.info.loadingDoc"));
            boolean generateDoc = !docUpToDate();

            if (generateDoc)
            {
                // interface needs to be re-generated
                info.message(Config.getString("editor.info.generatingDoc"));
                BlueJEvent.addListener(this);
                if (watcher != null) {
                    watcher.generateDoc();
                }
            }
            else
            {
                // Only bother to refresh if we're not about to generate
                // (if we do generate, we will refresh once completed)
                refreshHtmlDisplay();
            }

            interfaceToggle.getSelectionModel().selectLast();
            viewingHTML.set(true);
            watcher.showingInterface(true);
        }
        catch (IOException ioe) {
            // Could display a dialog here. However, the error message
            // (from save() call) will already be displayed in the editor
            // status bar.
        }
    }

    /**
     * A BlueJEvent was raised. Check whether it is one that we're interested in.
     */
    @Override
    public void blueJEvent(int eventId, Object arg, Project prj)
    {
        switch(eventId) {
            case BlueJEvent.DOCU_GENERATED :
                BlueJEvent.removeListener(this);
                refreshHtmlDisplay();
                break;
            case BlueJEvent.DOCU_ABORTED :
                BlueJEvent.removeListener(this);
                info.message (Config.getString("editor.info.docAborted"));
                break;
        }
    }

    /**
     * Check whether javadoc file is up to date.
     *
     * @return True is the currently existing documentation is up-to-date.
     */
    private boolean docUpToDate()
    {
        if (filename == null) {
            return true;
        }
        try {
            File src = new File(filename);
            File doc = new File(docFilename);

            if (!doc.exists() || (src.exists() && (src.lastModified() > doc.lastModified()))) {
                return false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Refresh the HTML display.
     */
    private void refreshHtmlDisplay()
    {
        try {
            File urlFile = new File(docFilename);

            // Check if docs file exists before attempting to load it.  There is a JDK behaviour where
            // if you load a non-existent file in a webview, all future attempts to reload the page will
            // fail even once the file exists.  So the file must be present before we attempt to load.
            //
            // There is an seeming timing hazard here where we could be called just at the moment the file
            // is created but before it is finished.  In fact, we are called in one of two cases:
            //  - One is where the interface is being switched to.  This method is called only if the
            //    docs won't be regenerated, so no race hazard there.
            //  - The other case is when the doc generation has definitely finished, so again we won't be in a
            //    race with the generation:
            if (!urlFile.exists())
            {
                return;
            }

            URL myURL = urlFile.toURI().toURL();

            // We must use reload here if applicable, as that forces reloading the stylesheet.css asset
            // (which may have changed if we initially loaded docs from a version older than 4.1.0,
            // but have now regenerated them).  We compare URLs, not String versions, because you may
            // get difference between e.g. file:/Users... and file:///Users... which URL comparison
            // properly takes care of:
            String location = htmlPane.getEngine().getLocation();
            if (Objects.equals(location == null ? null : new URL(location), myURL))
            {
                htmlPane.getEngine().reload();
            }
            else
            {
                htmlPane.getEngine().load(myURL.toString());
            }

            info.message(Config.getString("editor.info.docLoaded"));
        }
        catch (IOException exc) {
            info.message (Config.getString("editor.info.docDisappeared"), docFilename);
            Debug.reportError("loading class interface failed: " + exc);
        }
    }



    /**
     * Clear the message in the info area.
     */
    public void clearMessage()
    {
        info.clear();
    }

    /**
     * This method resets the value of the menu and toolbar according to the view
     *
     * @param sourceView true if called from source view setup; false from documentation view setup
     */
    private void resetMenuToolbar(boolean sourceView)
    {
        if (sourceView)
            actions.makeAllAvailable();
        else
            actions.makeAllUnavailableExcept("close", "toggle-interface-view");
    }


    @Override
    public boolean setStepMark(int lineNumber, String message, boolean isBreak, DebuggerThread thread)
    {
        switchToSourceView();

        if (isBreak)
        {
            removeStepMark();
            currentStepLineIndex = lineNumber - 1;
            flowEditorPane.setLineMarginGraphics(currentStepLineIndex, calculateMarginDisplay(currentStepLineIndex));
            // We also reapply scopes:
            flowEditorPane.applyScopeBackgrounds(javaSyntaxView.getScopeBackgrounds());
        }

        // Scroll to the line:
        flowEditorPane.positionCaret(getOffsetFromLineColumn(new SourceLocation(lineNumber, 1)));

        // display the message

        if (message != null) {
            info.messageImportant(message);
        }

        return false;
    }

    private EnumSet<MarginDisplay> calculateMarginDisplay(int lineIndex)
    {
        EnumSet<MarginDisplay> r = EnumSet.noneOf(MarginDisplay.class);
        if (PrefMgr.getFlag(PrefMgr.LINENUMBERS))
            r.add(MarginDisplay.LINE_NUMBER);
        if (breakpoints.get(lineIndex))
            r.add(MarginDisplay.BREAKPOINT);
        if (lineIndex == currentStepLineIndex)
            r.add(MarginDisplay.STEP_MARK);
        if (errorManager.getErrorOnLine(lineIndex) != null)
            r.add(MarginDisplay.ERROR);
        if (!compiledProperty.get())
            r.add(MarginDisplay.UNCOMPILED);
        return r;
    }

    @Override
    public void writeMessage(String msg)
    {
        info.message(msg);
    }

    /**
     * Remove the step mark (the mark that shows the current line when
     * single-stepping through code). If it is not currently displayed, do
     * nothing.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void removeStepMark()
    {
        if (currentStepLineIndex != -1)
        {
            int oldStepLine = currentStepLineIndex;
            currentStepLineIndex = -1;
            flowEditorPane.setLineMarginGraphics(oldStepLine, calculateMarginDisplay(oldStepLine));
            // We also reapply scopes:
            flowEditorPane.applyScopeBackgrounds(javaSyntaxView.getScopeBackgrounds());
        }
    }

    /**
     * Change class name.
     *
     * @param title  new window title
     * @param filename  new file name
     */
    @Override
    public void changeName(String title, String filename, String javaFilename, String docFilename)
    {
        this.filename = filename;
        this.docFilename = docFilename;
        windowTitle = title;
        setWindowTitle();
    }

    /**
     * Set the window title to show the defined title, or else the file name.
     */
    private void setWindowTitle()
    {
        String title = windowTitle;

        if (title == null) {
            if (filename == null) {
                title = "<no name>";
            }
            else {
                title = filename;
            }
        }
        fxTab.setWindowTitle(title);
    }

    @Override
    public void setCompiled(boolean compiled)
    {
        setCompileStatus(compiled);
        if (compiled) {
            removeErrorHighlights();
        }
    }

    /**
     * Toggle the editor's 'compiled' status. This affects display (left-hand margin colour)
     * and whether breakpoints can be set.
     */
    private void setCompileStatus(boolean compiled)
    {
        actions.getActionByName("toggle-breakpoint").setEnabled(compiled && viewingCode());
        compiledProperty.set(compiled);
    }

    /**
     * Return a boolean representing whether in source editing view
     */
    private boolean viewingCode()
    {
        return sourceIsCode && (!viewingHTML.get());
    }

    @Override
    public boolean compileStarted(int compilationSequence)
    {
        compilationStarted = true;
        removeErrorHighlights();
        return false;
    }

    @Override
    public void removeErrorHighlights()
    {
        errorManager.removeAllErrorHighlights();
    }

    @Override
    public void compileFinished(boolean successful, boolean classesKept)
    {
        compilationStarted = false;
        if (requeueForCompilation) {
            requeueForCompilation = false;
            if (classesKept)
            {
                // If the classes were kept, that means the compilation is valid and the source
                // hasn't changed since. There is then no need for another recompile, even if
                // we thought we needed one before.
                compilationQueued = false;
            }
            else
            {
                compilationQueuedExplicit = (requeueType != CompileType.ERROR_CHECK_ONLY);
                watcher.scheduleCompilation(true, requeueReason, requeueType);
            }
        }
        else {
            compilationQueued = false;
        }

        if (classesKept)
        {
            // Compilation requested via the editor interface has completed
            if (successful)
            {
                info.messageImportant(Config.getString("editor.info.compiled"));
            }
            else
            {
                info.messageImportant(getCompileErrorLabel());
            }
        }
    }

    private String getCompileErrorLabel()
    {
        return Config.getString("editor.info.compileError").replace("$", actions.getKeyStrokesForAction("compile").stream().map(KeyCodeCombination::getDisplayText).collect(Collectors.joining(" " + Config.getString("or") + " ")));
    }

    @Override
    public void removeBreakpoints()
    {
        // This may be a callback in response to a modification event.
        // If we try to remove breakpoints during the modification notification,
        // AbstractDocument throws an exception.
        JavaFXUtil.runAfterCurrent(() -> clearAllBreakpoints());
    }

    /**
     * Clear all known breakpoints.
     */
    private void clearAllBreakpoints()
    {
        if (mayHaveBreakpoints)
        {
            breakpoints.clear();
            for (int lineIndex = 0; lineIndex < document.getLineCount(); lineIndex++)
            {
                flowEditorPane.setLineMarginGraphics(lineIndex, calculateMarginDisplay(lineIndex));
            }
            mayHaveBreakpoints = false;
        }
    }

    @Override
    public void reInitBreakpoints()
    {
        if (mayHaveBreakpoints) {
            mayHaveBreakpoints = false;
            for (int i = 1; i <= numberOfLines(); i++)
            {
                if (breakpoints.get(i))
                {
                    if (watcher != null)
                        watcher.breakpointToggleEvent(i + 1, true);
                    mayHaveBreakpoints = true;
                }
            }
        }
    }

     /**
     * User requests "save"
     */
    public void userSave()
    {
        if (saveState.isSaved())
            info.message(Config.getString("editor.info.noChanges"));
        else {
            try {
                save();
            }
            catch (IOException ioe) {}
            // Note we can safely ignore the exception here: a message has
            // already been displayed in the editor status bar
        }
    }

    /**
     * A change has been made to the source code content.
     */
    public void textReplaced(int origStartIncl, String replaced, String replacement, int linesRemoved, int linesAdded)
    {
        // Prevent re-entry to this method.  In theory this shouldn't happen as we
        // shouldn't modify the document in this function.  But it seems like sometimes
        // the styled changes we make cause RichTextFX to generate a plain text change event:
        if (respondingToChange)
        {
            return;
        }
        respondingToChange = true;

        if (!saveState.isChanged()) {
            saveState.setState(Status.CHANGED);
            setChanged();
        }

        if (linesRemoved > 0 || linesAdded > 0) // For a multi-line change, always compile:
        {
            saveState.setState(Status.CHANGED);
            setChanged();

            // Note that this compilation will cause a save:
            if (sourceIsCode && watcher != null) {
                scheduleCompilation(CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
            }
        }

        clearMessage();
        // Calling the methods to remove the error/search stylings from within this 
        // document-changed callback causes an extra change notification 
        // to be regenerated by RichTextFX, which is unwanted.
        // So we must run those later:
        JavaFXUtil.runAfterCurrent(() -> {
            removeSearchHighlights();
            currentSearchResult.setValue(null);
            removeErrorHighlights();
            errorManager.documentContentChanged();
            showErrorOverlay(null, 0);
        });
        actions.userAction();

        // This may handle re-indentation; as this mutates the
        // document, it must be done outside the notification.
        if ("}".equals(replacement) && PrefMgr.getFlag(PrefMgr.AUTO_INDENT))
        {
            JavaFXUtil.runAfterCurrent(() -> {
                // It's possible, e.g. due to de-indenting, that by the time we
                // get here, the offset won't be valid any more, in which case don't
                // worry about it:
                if (origStartIncl + replacement.length() <= document.getLength() && replacement.equals("}"))
                {
                    actions.closingBrace(origStartIncl);
                }
            });
        }

        recordEdit(false);

        respondingToChange = false;
        flowEditorPane.textChanged();
    }

    private void setChanged()
    {
        if (ignoreChanges) {
            return;
        }
        setCompileStatus(false);
        if (watcher != null) {
            watcher.modificationEvent(this);
        }
    }

    @Override
    public boolean isModified()
    {
        return saveState.isChanged();
    }

    /**
     * Returns if this editor is read-only. Accessor for the setReadOnly
     * property.
     *
     * @return a boolean indicating whether the editor is read-only.
     */
    @Override
    public boolean isReadOnly()
    {
        return !getSourcePane().isEditable();
    }

    /**
     * Set this editor to read-only.
     *
     * @param readOnly  The new readOnly value
     */
    @Override
    public void setReadOnly(boolean readOnly)
    {
        if (readOnly) {
            saveState.setState(Status.READONLY);
        }
        getSourcePane().setEditable(!readOnly);
    }

    @Override
    public void showInterface(boolean interfaceStatus)
    {
        interfaceToggle.getSelectionModel().select(interfaceStatus ? 1 : 0);
    }

    /**
     * Returns a property of the current editor.
     *
     * @param  propertyKey  The propertyKey of the property to retrieve.
     * @return              the property value or null if it is not found
     */
    @Override
    public Object getProperty(String propertyKey)
    {
        return propertyMap.get(propertyKey);
    }

    /**
     * Set a property for the current editor. Any existing property with
     * this key will be overwritten.
     *
     * @param  propertyKey  The property key of the new property
     * @param  value        The new property value
     */
    @Override
    public void setProperty(String propertyKey, Object value)
    {
        if ( propertyKey == null ) {
            return;
        }

        propertyMap.put(propertyKey,value);
    }

    @Override
    public TextEditor assumeText()
    {
        return this;
    }

    @Override
    public FrameEditor assumeFrame()
    {
        return null;
    }

    @Override
    public void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after)
    {
        NodeAndPosition<ParsedNode> classNode = findClassNode();
        if (classNode != null) {
            NodeAndPosition<ParsedNode> existingMethodNode = findMethodNode(method.getName(), classNode);

            if (existingMethodNode != null) {
                //Append to existing method:
                String text = "";
                for (CodeElement codeElement : method.getContents()) {
                    text += codeElement.toJavaSource().toTemporaryJavaCodeString();
                }
                appendTextToNode(existingMethodNode, text);
                after.accept(false);
                return;
            }

            //Make a new method:
            appendTextToNode(classNode, method.toJavaSource().toTemporaryJavaCodeString());
            after.accept(true);
        }
        after.accept(false);
    }

    @Override
    public void insertMethodCallInConstructor(String className, CallElement callElement, FXPlatformConsumer<Boolean> after)
    {
        NodeAndPosition<ParsedNode> classNode = findClassNode();
        if (classNode != null) {
            NodeAndPosition<ParsedNode> constructor = findMethodNode(className, classNode);
            if (constructor == null) {
                addDefaultConstructor(className, callElement);
            }
            else {
                String methodName = callElement.toJavaSource().toTemporaryJavaCodeString();
                methodName = methodName.substring(0, methodName.indexOf('('));
                if (!hasMethodCall(methodName, constructor, true)) {
                    //Add at the end of the constructor:
                    appendTextToNode(constructor, callElement.toJavaSource().toTemporaryJavaCodeString());
                    after.accept(true);
                    return;
                }
            }
        }
        after.accept(false);
    }

    private void addDefaultConstructor(String className, CallElement callElement)
    {
        NodeAndPosition<ParsedNode> classNode = findClassNode();
        if (classNode != null) {
            //Make a new method:
            appendTextToNode(classNode, "public " + className + "()\n{\n" + callElement.toJavaSource().toTemporaryJavaCodeString() + "}\n");
        }
    }

    /**
     * Appends text to a node that ends in a curly bracket
     */
    private void appendTextToNode(NodeAndPosition<ParsedNode> node, String text)
    {
        //The node may have whitespace at the end, so we look for the last closing brace and
        //insert before that:
        for (int pos = node.getEnd() - 1; pos >= 0; pos--) {
            if ("}".equals(getText(getLineColumnFromOffset(pos), getLineColumnFromOffset(pos+1)))) {
                int posFinal = pos;
                undoManager.compoundEdit(() -> {
                    int originalLength = node.getSize();
                    // First insert the text:
                    setText(getLineColumnFromOffset(posFinal), getLineColumnFromOffset(posFinal), text);
                    // Then auto-indent the method to make sure our indents were correct:
                    int oldPos = getSourcePane().getCaretPosition();
                    FlowIndent.calculateIndentsAndApply(getSourceDocument(), document, node.getPosition(),
                        node.getPosition() + originalLength + text.length(), oldPos);
                });
                setCaretLocation(getLineColumnFromOffset(pos));
                return;
            }
        }
        Debug.message("Could not find end of node to append to: \"" + getText(getLineColumnFromOffset(
            node.getPosition()), getLineColumnFromOffset(node.getEnd())) + "\"");
    }

    private NodeAndPosition<ParsedNode> findClassNode()
    {
        NodeAndPosition<ParsedNode> root = new NodeAndPosition<>(getParsedNode(), 0,
            getParsedNode().getSize());
        for (NodeAndPosition<ParsedNode> nap : iterable(root)) {
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_TYPEDEF)
                return nap;
        }
        return null;
    }

    private NodeAndPosition<ParsedNode> findMethodNode(String methodName, NodeAndPosition<ParsedNode> start)
    {
        for (NodeAndPosition<ParsedNode> nap : iterable(start)) {
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_NONE) {
                NodeAndPosition<ParsedNode> r = findMethodNode(methodName, nap);
                if (r != null)
                    return r;
            }
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF && nap.getNode().getName().equals(methodName)) {
                return nap;
            }
        }

        return null;
    }

    private boolean hasMethodCall(String methodName, NodeAndPosition<ParsedNode> methodNode, boolean root)
    {
        for (NodeAndPosition<ParsedNode> nap : iterable(methodNode)) {
            // Method nodes have comments as children, and the body:
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_NONE && root) {
                return hasMethodCall(methodName, nap, false);
            }

            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION && document.getContent(
                nap.getPosition(), nap.getPosition() + nap.getSize()).toString().startsWith(methodName)) {
                return true;
            }
        }

        return false;
    }

    private Iterable<NodeAndPosition<ParsedNode>> iterable(final NodeAndPosition<ParsedNode> parent)
    {
        return () -> parent.getNode().getChildren(parent.getPosition());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void focusMethod(String methodName, List<String> paramTypes)
    {
        focusMethod(methodName, paramTypes, new NodeAndPosition<ParsedNode>(getParsedNode(), 0, 0), 0);
    }

    private boolean focusMethod(String methodName, List<String> paramTypes, NodeAndPosition<ParsedNode> tree, int offset)
    {
        // This is a fairly naive traversal, which may find methods in inner classes rather
        // than one in the outer class; but then we don't actually pass which class we are interested in,
        // so it may be right to pick the one in the inner class anyway:
        if (tree.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF && methodName.equals(tree.getNode().getName())
            && paramsMatch(tree.getNode(), paramTypes))
        {
            switchToSourceView();
            flowEditorPane.positionCaret(offset);
            return true;
        }
        else
        {
            for (NodeAndPosition<ParsedNode> child : (Iterable<NodeAndPosition<ParsedNode>>)(() -> tree.getNode().getChildren(0)))
            {
                if (focusMethod(methodName, paramTypes, child, offset + child.getPosition()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks if the parameter types match the parameters of the given node,
     * if it is a method node.  (If not a method node, false is returned)
     * @param node The node which should be a MethodNode.
     * @param paramTypes Parameter types.  null matches anything.
     * @return
     */
    private boolean paramsMatch(ParsedNode node, List<String> paramTypes)
    {
        if (paramTypes == null)
            return true;
        if (node instanceof MethodNode)
        {
            MethodNode methodNode = (MethodNode)node;
            if (methodNode.getParamTypes().size() != paramTypes.size())
                return false;
            for (int i = 0; i < paramTypes.size(); i++)
            {
                JavaEntity paramType = methodNode.getParamTypes().get(i);
                if (!paramType.getName().equals(paramTypes.get(i)))
                    return false;
            }
            // If we get here, all paramTypes must have matched:
            return true;
        }
        return false;
    }

    @Override
    public void setExtendsClass(String className, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                if (info.getSuperclass() == null) {
                    Selection s1 = info.getExtendsInsertSelection();

                    setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
                    insertText(" extends " + className, false);
                }
                else {
                    Selection s1 = info.getSuperReplaceSelection();

                    setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
                    insertText(className, false);
                }
                save();
            }
        }
        catch (IOException ioe) {
            DialogManager.showMessageWithTextFX(getWindow(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }

    @Override
    public void removeExtendsClass(ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                Selection s1 = info.getExtendsReplaceSelection();
                s1.combineWith(info.getSuperReplaceSelection());

                if (s1 != null) {
                    setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
                    insertText("", false);
                }
                save();
            }
        }
        catch (IOException ioe) {
            DialogManager.showMessageWithTextFX(getWindow(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }

    @Override
    public void addImplements(String interfaceName, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                Selection s1 = info.getImplementsInsertSelection();
                setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));

                if (info.hasInterfaceSelections()) {
                    // if we already have an implements clause then we need to put a
                    // comma and the interface name but not before checking that we
                    // don't already have it

                    List<String> exists = getInterfaceTexts(info.getInterfaceSelections());

                    // XXX make this equality check against full package name
                    if (!exists.contains(interfaceName))
                        insertText(", " + interfaceName, false);
                }
                else {
                    // otherwise we need to put the actual "implements" word
                    // and the interface name
                    insertText(" implements " + interfaceName, false);
                }
                save();
            }
        }
        catch (IOException ioe) {
            DialogManager.showMessageWithTextFX(getWindow(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }

    /**
     * Using a list of selections, retrieve a list of text strings from the editor which
     * correspond to those selections.
     * TODO this is usually used to get the implemented interfaces, but it is a clumsy way
     *      to do that.
     */
    private List<String> getInterfaceTexts(List<Selection> selections)
    {
        List<String> r = new ArrayList<String>(selections.size());
        for (Selection sel : selections)
        {
            String text = getText(new SourceLocation(sel.getLine(), sel.getColumn()),
                new SourceLocation(sel.getEndLine(), sel.getEndColumn()));

            // check for type arguments: don't include them in the text
            int taIndex = text.indexOf('<');
            if (taIndex != -1)
                text = text.substring(0, taIndex);
            text = text.trim();

            r.add(text);
        }
        return r;
    }

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                Selection s1 = info.getExtendsInsertSelection();
                setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));

                if (info.hasInterfaceSelections()) {
                    // if we already have an extends clause then we need to put a
                    // comma and the interface name but not before checking that we
                    // don't
                    // already have it

                    List<String> exists = getInterfaceTexts(info.getInterfaceSelections());

                    // XXX make this equality check against full package name
                    if (!exists.contains(interfaceName))
                        insertText(", " + interfaceName, false);
                }
                else {
                    // otherwise we need to put the actual "extends" word
                    // and the interface name
                    insertText(" extends " + interfaceName, false);
                }
                save();
            }
        }
        catch (IOException ioe) {
            DialogManager.showMessageWithTextFX(getWindow(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                Selection s1 = null;

                List<Selection> vsels;
                List<String> vtexts;

                vsels = info.getInterfaceSelections();
                vtexts = getInterfaceTexts(vsels);
                int where = vtexts.indexOf(interfaceName);

                // we have a special case if we deleted the first bit of an
                // "implements" clause, yet there are still clauses left.. we have
                // to delete the following "," instead of the preceding one.
                if (where == 1 && vsels.size() > 2)
                    where = 2;

                if (where > 0) { // should always be true
                    s1 = vsels.get(where - 1);
                    s1.combineWith(vsels.get(where));
                }

                // delete the text from the end backwards so that our
                if (s1 != null) {
                    setSelection(new SourceLocation(s1.getLine(), s1.getColumn()), new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
                    insertText("", false);
                }

                save();
            }
        }
        catch (IOException ioe) {
            DialogManager.showMessageWithTextFX(getWindow(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }

    @Override
    public EditorFixesManager getEditorFixesManager(){
        return editorFixesMgr;
    }

    @Override
    public void addImportFromQuickFix(String importName)
    {
        int importOffset = 0;
        int currentCaretPos = flowEditorPane.getSelectionStart();
        List<CharSequence> docLines = document.getLines();
        boolean isLineAfterImportBlank = docLines.get(0).toString().isBlank();

        // Look for existing imports and package statement to find where the new import needs to be inserted.
        // We suppose package and import statements may be placed anywhere if the class contains comments...
        boolean hasImports = docLines.stream().filter(charSequence -> charSequence.toString().startsWith("import ")).count() > 0;
        boolean hasPackage = docLines.stream().filter(charSequence -> charSequence.toString().startsWith("package ")).count() > 0;
        if (hasImports || hasPackage)
        {
            boolean passedImport = false, passedPackage = false;
            for (CharSequence charseq : docLines)
            {
                boolean isLineImport = charseq.toString().startsWith("import ");
                boolean isLinePackage = charseq.toString().startsWith("package ");
                passedImport |= isLineImport;
                passedPackage |= isLinePackage;

                // We found the place to add the import when we've passed all imports or when we only search for the package and passed it
                if ((hasImports && !isLineImport && passedImport) || (!hasImports && passedPackage && !isLinePackage))
                {
                    isLineAfterImportBlank = charseq.toString().isBlank();
                    break;
                }

                // Update the offset when we continue in the loop
                importOffset += charseq.length() + 1;
            }
        }

        String fullImportStr = "import " + importName + ((isLineAfterImportBlank) ? ";\n" : ";\n\n");
        flowEditorPane.select(importOffset, importOffset);
        insertText(fullImportStr, false);
        int newCaretPos = (currentCaretPos >= importOffset) ? (currentCaretPos + fullImportStr.length()) : currentCaretPos;
        flowEditorPane.select(newCaretPos, newCaretPos);

        refresh();
    }

    @Override
    public void removeImports(List<String> importTargets)
    {
        List<LocatableImport> toRemove = new ArrayList<>();
        for (String importTarget : importTargets)
        {
            LocatableImport details = getParsedNode().getImports().getImportInfo(importTarget);

            if (details != null)
            {
                toRemove.add(details);
            }
        }

        // Sort in reverse order of position, so that we can go down the list
        // and remove in turn without a removal affecting a later removal.
        // Hence we sort by negative start value:
        Collections.sort(toRemove, Comparator.<LocatableImport>comparingInt(t -> -t.getStart()));

        for (LocatableImport locatableImport : toRemove)
        {
            if (locatableImport.getStart() != -1)
            {
                document.replaceText(locatableImport.getStart(), locatableImport.getStart() + locatableImport.getLength(), "");
            }
        }
    }

    @Override
    public void setHeaderImage(Image image)
    {
        fxTab.setHeaderImage(image);
    }

    /**
     * Implementation of "toggle-interface-view" user function. The menu has
     * already been changed - now see what it is and do it.
     */
    public void toggleInterface()
    {
        if (viewingHTML.get())
            switchToSourceView();
        else
            switchToInterfaceView();
    }

    @Override
    public void setLastModified(long millisSinceEpoch)
    {
        this.lastModified = millisSinceEpoch;
    }

    public FlowEditorPane getSourcePane()
    {
        return flowEditorPane;
    }

    public void compileOrShowNextError()
    {
        if (watcher != null) {
            if (saveState.isChanged() || !errorManager.hasErrorHighlights())
            {
                if (! saveState.isChanged())
                {
                    if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT))
                    {
                        // Pop up in a dialog:
                        DialogManager.showTextWithCopyButtonFX(getWindow(), Config.getString("pkgmgr.accessibility.compileDone"), "BlueJ");
                    }
                }
                scheduleCompilation(CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
            }
            else
            {
                ErrorDetails err = errorManager.getNextErrorPos(flowEditorPane.getCaretPosition());
                if (err != null)
                {
                    flowEditorPane.positionCaret(err.startPos);

                    if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT))
                    {
                        // Pop up in a dialog:
                        DialogManager.showTextWithCopyButtonFX(getWindow(), err.message, "BlueJ");
                    }
                }
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public Window getWindow()
    {
        return fxTabbedEditor.getWindow();
    }

    /**
     * Shows the preferences pane, and makes the given pane index (i.e. given tab index
     * in the preferences) the active showing tab.  0 is general, 1 is key bindings, and so on.
     * If in doubt, pass 0.
     */
    public void showPreferences(int paneIndex)
    {
        watcher.showPreferences(paneIndex);
    }

    /**
     * Transfers caret to user specified line number location.
     */
    public void goToLine()
    {
        final int numberOfLines = numberOfLines();
        GoToLineDialog goToLineDialog = new GoToLineDialog(fxTabbedEditor.getWindow());
        goToLineDialog.setRangeMax(numberOfLines);
        Optional<Integer> o = goToLineDialog.showAndWait();
        o.ifPresent(n -> {
            setSelection(new SourceLocation(n , 1), new SourceLocation(n, 1));
        });
    }

    // package visible
    void updateHeaderHasErrors(boolean hasErrors)
    {
        fxTab.setErrorStatus(hasErrors);
    }

    /**
     * Do a find forwards or backwards, and highlight all cases.
     *
     * The case after the cursor (if backwards is false) or before it (if
     * backwards is true) is given a special highlight.
     *
     * Returns null if nothing was found.  If something was found, gives
     * you back a class you can use to cycle between search results.  It
     * becomes invalid next time doFind is called, or if the document is modified.
     */
    FindNavigator doFind(String searchFor, boolean ignoreCase)
    {
        removeSearchHighlights();
        // Deselect existing selection in case it's no longer a valid search result.
        // Move back to beginning of selection:
        flowEditorPane.positionCaret(flowEditorPane.getSelectionStart());
        lastSearchString = searchFor;
        String content = document.getFullContent();

        int curPosition = 0;
        boolean finished = false;

        List<Integer> foundStarts = new ArrayList<>();

        while (!finished)
        {
            int foundPos = FindNavigator.findSubstring(content, searchFor, ignoreCase, false, curPosition);
            if (foundPos != -1)
            {
                foundStarts.add(foundPos);
                curPosition = foundPos + searchFor.length();
            }
            else
            {
                finished = true;
            }
        }
        currentSearchResult.set(foundStarts.isEmpty() ? null : new FindNavigator()
        {
            @Override
            public void highlightAll()
            {
                findResults.clear();
                findResults.addAll(Utility.mapList(foundStarts, foundPos -> new int[] {foundPos, foundPos + searchFor.length()}));
                flowEditorPane.showHighlights(HighlightType.FIND_RESULT, findResults);
            }

            @Override
            public FindNavigator replaceCurrent(String replacement)
            {
                if (!flowEditorPane.getSelectedText().equals(searchFor))
                {
                    selectNext(true);
                }
                int pos = flowEditorPane.getSelectionStart();
                document.replaceText(pos, pos + searchFor.length(), replacement);
                flowEditorPane.positionCaret(pos + searchFor.length());
                return doFind(searchFor, ignoreCase);
            }

            public void replaceAll(String replacement)
            {
                // Sort all the found positions in descending order, so we can replace them
                // in order without affecting the later positions in the list (earlier in file):
                foundStarts.stream().sorted(Comparator.reverseOrder()).forEach(pos ->
                    document.replaceText(pos, pos + searchFor.length(), replacement)
                );
            }

            @Override
            public void selectNext(boolean canBeAtCurrentPos)
            {
                if (validProperty().get())
                {
                    int selStart = flowEditorPane.getSelectionStart();
                    int position = foundStarts.stream()
                        .filter(pos -> pos > selStart || (canBeAtCurrentPos && pos == selStart))
                        .findFirst()
                        .orElse(foundStarts.get(0));
                    select(position);
                }
            }

            private void select(int position)
            {
                flowEditorPane.select(position, position + searchFor.length());
            }

            @Override
            public void selectPrev()
            {
                if (validProperty().get())
                {
                    int selStart = flowEditorPane.getSelectionStart();
                    int position = Utility.streamReversed(foundStarts)
                        .filter(pos -> pos < selStart)
                        .findFirst()
                        .orElse(foundStarts.get(foundStarts.size() - 1));
                    select(position);
                }
            }

            @Override
            public BooleanExpression validProperty()
            {
                return currentSearchResult.isEqualTo(this);
            }
        });
        return currentSearchResult.get();
    }

    /**
     * Removes the selected highlights (in both the source/doc pane)
     * Note: the other highlights such as the brackets etc remain
     */
    public void removeSearchHighlights()
    {
        findResults.clear();
        flowEditorPane.showHighlights(HighlightType.FIND_RESULT, List.of());
    }

    /**
     * Sets the find panel to be visible and if there is a selection/or previous search
     * it starts a automatic find of what was selected in the text/or previous search. If
     * it is the source pane then the replace button is enabled; if it is the interface pane
     * then the replace button and replace panel are set to disabled and invisible
     */
    public void initFindPanel()
    {
        finder.displayFindPanel(flowEditorPane.getSelectedText());
    }

    /**
     * Implementation of "find-next" user function.
     */
    public void findNext(boolean backwards)
    {
        if (currentSearchResult.get() == null || !currentSearchResult.get().validProperty().get())
        {
            String search = flowEditorPane.getSelectedText();
            if (search.isEmpty())
                search = lastSearchString;
            doFind(search, true);
        }
        if (currentSearchResult.get() != null)
        {
            if (backwards)
                currentSearchResult.get().selectPrev();
            else
                currentSearchResult.get().selectNext(false);
        }
    }

    /**
     * Opens or close the replace panel (and if opening it, set the focus into
     * the find field).
     */
    protected void showReplacePanel()
    {
        if (!finder.isVisible()) {
            finder.setVisible(true);
        }
        finder.requestFindfieldFocus();
        finder.setReplaceEnabled(true);
    }

    /**
     * Populates the find field and requests focus
     */
    public void setFindTextfield(String text)
    {
        finder.populateFindTextfield(text);
    }

    /**
     * Create and pop up the content assist (code completion) dialog.
     */
    protected void createContentAssist()
    {
        //need to recreate the dialog each time it is pressed as the values may be different
        javaSyntaxView.flushReparseQueue();
        ParsedCUNode parser = getParsedNode();
        ExpressionTypeInfo suggests = parser == null ? null : parser.getExpressionType(flowEditorPane.getCaretPosition(),
                javaSyntaxView);
        if (suggests != null)
        {
            LocatableToken suggestToken = suggests.getSuggestionToken();
            AssistContent[] possibleCompletions = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null, parser.getContainingMethodOrClassNode(flowEditorPane.getCaretPosition()));
            Arrays.sort(possibleCompletions, AssistContent.getComparator());
            List<SuggestionDetails> suggestionDetails = Arrays.stream(possibleCompletions)
                    .map(AssistContentThreadSafe::new)
                    .map(ac -> new SuggestionDetailsWithHTMLDoc(ac.getName(), ExpressionCompletionCalculator.getParamsCompletionDisplay(ac), ac.getType(), SuggestionShown.COMMON, ac.getDocHTML()))
                    .collect(Collectors.toList());

            int originalPosition = suggestToken == null ? flowEditorPane.getCaretPosition() : suggestToken.getPosition();
            Bounds screenPos;
            // First, try to get the character after the caret:
            screenPos = flowEditorPane.getCaretBoundsOnScreen(originalPosition).orElse(null);

            // That may be null if caret was at end of line, in which case try character before:
            if (screenPos == null && originalPosition > 0)
            {
                screenPos = flowEditorPane.getCaretBoundsOnScreen(originalPosition - 1).orElse(null);;
                // Adjust to move to RHS of the rectangle:
                screenPos = new BoundingBox(screenPos.getMaxX(), screenPos.getMinY(), 0, screenPos.getHeight());
            }
            if (screenPos == null)
                return;
            Bounds spLoc = flowEditorPane.screenToLocal(screenPos);

            StringExpression editorFontCSS = PrefMgr.getEditorFontCSS(true);
            SuggestionList suggestionList = new SuggestionList(new SuggestionListParent()
            {
                @Override
                @OnThread(Tag.FX)
                public StringExpression getFontCSS()
                {
                    return editorFontCSS;
                }

                @Override
                public double getFontSize()
                {
                    return PrefMgr.getEditorFontSize().get();
                }

                @Override
                public void setupSuggestionWindow(Stage window)
                {
                    flowEditorPane.setFakeCaret(true);
                }
            }, suggestionDetails, null, SuggestionShown.RARE, i ->
            {
            }, new SuggestionListListener()
            {
                @Override
                public @OnThread(Tag.FXPlatform) void suggestionListChoiceClicked(SuggestionList suggestionList, int highlighted)
                {
                    if (highlighted != -1)
                    {
                        codeComplete(possibleCompletions[highlighted], originalPosition, flowEditorPane.getCaretPosition(), suggestionList);
                    }
                }

                @Override
                public Response suggestionListKeyTyped(SuggestionList suggestionList, KeyEvent event, int highlighted)
                {
                    if (event.getCharacter().equals("\b") || event.getCharacter().equals("\u007F"))
                    {
                        // Backspace/delete; handled by key pressed event, lower down
                    }
                    else if (event.getCharacter().equals("\n"))
                    {
                        suggestionListChoiceClicked(suggestionList, highlighted);
                        return Response.DISMISS;
                    }
                    else
                    {
                        document.replaceText(flowEditorPane.getCaretPosition(), flowEditorPane.getCaretPosition(), event.getCharacter());
                    }

                    String prefix = document.getContent(originalPosition, flowEditorPane.getCaretPosition()).toString();
                    suggestionList.calculateEligible(prefix, true, false);
                    suggestionList.updateVisual(prefix);
                    return Response.CONTINUE;
                }

                @Override
                public @OnThread(Tag.FXPlatform) SuggestionList.SuggestionListListener.Response suggestionListKeyPressed(SuggestionList suggestionList, KeyEvent event, int highlighted)
                {
                    switch (event.getCode())
                    {
                        case ESCAPE:
                            return Response.DISMISS;
                        case ENTER:
                        case TAB:
                            suggestionListChoiceClicked(suggestionList, highlighted);
                            return Response.DISMISS;
                        case BACK_SPACE:
                            actions.getActionByName(DefaultEditorKit.deletePrevCharAction).actionPerformed(false);
                            break;
                        case DELETE:
                            actions.getActionByName(DefaultEditorKit.deleteNextCharAction).actionPerformed(false);
                            break;
                    }
                    // If they delete to before the original position then
                    // not only does it make sense to dismiss, but in fact
                    // we must dismiss or we will encounter an exception:
                    if (flowEditorPane.getCaretPosition() < originalPosition)
                    {
                        return Response.DISMISS;
                    }
                    else
                    {
                        return Response.CONTINUE;
                    }
                }

                @Override
                public @OnThread(Tag.FXPlatform) void hidden()
                {
                    flowEditorPane.setFakeCaret(false);
                }
            });
            String prefix = document.getContent(originalPosition, flowEditorPane.getCaretPosition()).toString();
            suggestionList.calculateEligible(prefix, true, false);
            suggestionList.updateVisual(prefix);
            suggestionList.highlightFirstEligible();
            suggestionList.show(flowEditorPane, spLoc);
            watcher.recordCodeCompletionStarted(document.getLineFromPosition(originalPosition) + 1, document.getColumnFromPosition(originalPosition) + 1, null, null, prefix, suggestionList.getRecordingId());

        } else {
            /*
            //no completions found. no need to search.
            info.message ("No completions available.");
            CodeCompletionDisplay codeCompletionDlg = new CodeCompletionDisplay(this, watcher,
                            null, new AssistContent[0], null);

            initialiseContentAssist(codeCompletionDlg, xpos, ypos);
            */
        }
    }

    /**
     * codeComplete prints the selected text in the editor
     */
    private void codeComplete(AssistContent selected, int prefixBegin, int prefixEnd, SuggestionList suggestionList)
    {
        String start = selected.getName();
        List<ParamInfo> params = selected.getParams();
        if (params != null)
            start += "(";
        // Replace prefix with the full name:
        flowEditorPane.select(prefixBegin, prefixEnd);
        String prefix = flowEditorPane.getSelectedText();
        insertText(start, false);
        String inserted = start;

        if (params != null)
        {
            // Record position before we add first parameter,
            // so that we can come back and select it:
            int selLoc = flowEditorPane.getCaretPosition();
            // Put all available params in, separated by ", "
            if (!params.isEmpty())
            {
                final String joinedParams = params.stream().map(ParamInfo::getDummyName).collect(Collectors.joining(", "));
                insertText(joinedParams, false);
                inserted += joinedParams;
            }

            insertText(")", false);
            inserted += ")";

            // If there were any dummy parameters, go back and select first one:
            if (params.size() > 0)
                flowEditorPane.select(selLoc, selLoc + params.get(0).getDummyName().length());
        }
        watcher.recordCodeCompletionEnded(document.getLineFromPosition(prefixBegin) + 1, document.getColumnFromPosition(prefixBegin) + 1, null, null, prefix, inserted, suggestionList.getRecordingId());
        try
        {
            save();
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        }
    }

    /**
     * Prints source code from Editor
     *
     * @param printerJob  A PrinterJob to print to.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printScopeBackgrounds, PrintProgressUpdate progressUpdate)
    {
        SimpleDoubleProperty height = new SimpleDoubleProperty();
        Document doc = new HoleDocument();
        doc.replaceText(0, 0, document.getFullContent());
        OffScreenFlowEditorPaneListener flowEditorPaneListener = new OffScreenFlowEditorPaneListener();
        final String fontCSS;
        // These sizes are picked by hand.  They are small because the Roboto Mono font
        // is very large for its point size
        String fontSize = "7pt";
        switch (printSize)
        {
            case SMALL:
                fontSize = "5pt";
                break;
            case STANDARD:
                fontSize = "7pt";
                break;
            case LARGE:
                fontSize = "9pt";
                break;
        }
        fontCSS = "-fx-font-size: " + fontSize + ";" + PrefMgr.getEditorFontFamilyCSS();
        LineDisplay lineDisplay = new LineDisplay(height::get, new ReadOnlyDoubleWrapper(0), new ReadOnlyStringWrapper(fontCSS), flowEditorPaneListener);
        // TODO apply syntax highlighting
        LineContainer lineContainer = new LineContainer(lineDisplay, true);
        LineStyler[] lineStylerWrapper = new LineStyler[] {(i, s) -> Collections.singletonList(new StyledSegment(Collections.emptyList(), s.toString()))};
        JavaSyntaxView javaSyntaxView = new JavaSyntaxView(doc, new Display()
        {
            @Override
            public ReadOnlyObjectProperty<Scene> sceneProperty()
            {
                return lineContainer.sceneProperty();
            }

            @Override
            public ReadOnlyDoubleProperty widthProperty()
            {
                return lineContainer.widthProperty();
            }

            @Override
            public ReadOnlyDoubleProperty heightProperty()
            {
                return lineContainer.heightProperty();
            }

            @Override
            public boolean isPrinting()
            {
                return true;
            }

            @Override
            public void requestLayout()
            {
                lineContainer.requestLayout();
                lineContainer.layout();
            }

            @Override
            public boolean isLineVisible(int lineIndex)
            {
                return lineDisplay.isLineVisible(lineIndex);
            }

            @Override
            public Optional<Double> getLeftEdgeX(int charIndex)
            {
                return FlowEditorPane.getLeftEdgeX(charIndex, doc, lineDisplay);
            }

            @Override
            public void addLineDisplayListener(LineDisplayListener lineDisplayListener)
            {
                lineDisplay.addLineDisplayListener(lineDisplayListener);
            }

            @Override
            public void setLineStyler(LineStyler lineStyler)
            {
                lineStylerWrapper[0] = lineStyler;
            }

            @Override
            public double getTextDisplayWidth()
            {
                return lineContainer.getWidth() - MarginAndTextLine.TEXT_LEFT_EDGE;
            }

            @Override
            public void applyScopeBackgrounds(Map<Integer, List<BackgroundItem>> scopeBackgrounds)
            {
                if (printScopeBackgrounds)
                {
                    lineDisplay.applyScopeBackgrounds(scopeBackgrounds);
                }
            }

            @Override
            public void repaint()
            {
                // Nothing to do?
            }
        }, flowEditorPaneListener, this.javaSyntaxView.getEntityResolver(), PrefMgr.flagProperty(PrefMgr.HIGHLIGHTING));
        javaSyntaxView.enableParser(true);
        StyledLines allLines = new StyledLines(doc, lineStylerWrapper[0]);
        lineContainer.getChildren().setAll(lineDisplay.recalculateVisibleLines(allLines, Math::ceil, 0, printerJob.getJobSettings().getPageLayout().getPrintableWidth(), height.get(), true));

        // Note: very important we make this call before copyFrom, as copyFrom is what triggers
        // the run-later that marking as printing suppresses:
        //doc.markAsForPrinting();
        //doc.copyFrom(sourceDocument);
        //MoeEditorPane editorPane = doc.makeEditorPane(null, null);
        Label pageNumberLabel = new Label("");
        String timestamp = new SimpleDateFormat("yyyy-MMM-dd HH:mm").format(new Date());
        BorderPane header = new BorderPane(new Label(timestamp), null, pageNumberLabel, null, new Label(windowTitle));
        // If we let labels be default font, it can cause weird font corruption when printing.
        // But setting labels to same font as editor seems to avoid the issue:
        for (Node node : header.getChildren())
        {
            node.setStyle(PrefMgr.getEditorFontFamilyCSS());
        }
        header.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));
        header.setPadding(new Insets(5));

        BorderPane rootPane = new BorderPane(lineContainer, header, null, flowEditorPaneListener, null);
        // The scopeColorsPane needs to be in the scene to access the CSS colors.
        // But we don't actually want it visible:
        flowEditorPaneListener.setManaged(false);
        flowEditorPaneListener.setVisible(false);
        rootPane.setBackground(null);
        // JavaFX seems to always print at 72 DPI, regardless of printer DPI:
        // This means that that the point width (1/72 of an inch) is actually the pixel width, too:
        double pixelWidth = printerJob.getJobSettings().getPageLayout().getPrintableWidth();
        double pixelHeight = printerJob.getJobSettings().getPageLayout().getPrintableHeight();
        Scene scene = new Scene(rootPane, pixelWidth, pixelHeight, Color.GRAY);
        Config.addEditorStylesheets(scene);

        //editorPane.setPrinting(true, printSize, printLineNumbers);
        //editorPane.setWrapText(true);
        lineContainer.applyCss();
        rootPane.requestLayout();
        rootPane.layout();
        rootPane.applyCss();
        /*
        if (!printBackground)
        {
            // Remove styles:
            for (int i = 0; i < doc.getDocument().getParagraphs().size(); i++)
            {
                doc.getDocument().setParagraphStyle(i, null);
            }

        }
        else
        {
            // We have to reparse and recalculate scopes

            // We must mark document as shown or else parsing terminates early:
            doc.notYetShown = false;
            doc.enableParser(true);
            doc.getParser();
            doc.recalculateAllScopes();
        }
        */
        FXConsumer<Integer> updatePageNumber = n -> {
            pageNumberLabel.setText("Page " + n);
            rootPane.requestLayout();
            rootPane.layout();
            rootPane.applyCss();
        };
        // Run printing in another thread:
        return () -> printPages(printerJob, rootPane, updatePageNumber, lineContainer, lineDisplay, allLines, printLineNumbers, progressUpdate);
    }

    /**
     * Prints the editor, using multiple pages if necessary
     *
     * @param printerJob The overall printer job
     * @param printNode The node to print, each page.  This may just be the
     *                  editor pane, or it may be a wrapper around the editor
     *                  pane that also shows a header and/or footer.
     * @param updatePageNumber A callback to update the header/footer each time
     *                         the page number changes.  Cannot be null.
     * @param lineContainer The line container to change the lines for
     * @param lineDisplay The line display used to show the line container
     * @param allLines The full list of lines to print, which will be split up into pages for printing
     * @param printLineNumbers Whether to print line numbers at the side of the page
     * @param progressUpdate The callback to call with the current progress after each page
     */
    @OnThread(Tag.FX)
    public static void printPages(PrinterJob printerJob, Node printNode, FXConsumer<Integer> updatePageNumber,
        LineContainer lineContainer, LineDisplay lineDisplay, List<List<StyledSegment>> allLines, boolean printLineNumbers, PrintProgressUpdate progressUpdate)
    {
        // We must manually scroll down the editor, one page's worth at a time.  We keep track of the top line visible:
        int topLine = 0;
        boolean lastPage = false;
        int editorLines = allLines.size();
        int pageNumber = 1;
        int lastTopLine = topLine;
        while (topLine < editorLines && !lastPage)
        {
            if (!progressUpdate.printProgress(topLine, editorLines))
            {
                return;
            }

            // Scroll to make topLine actually at the top:
            lineDisplay.scrollTo(topLine, 0);
            List<MarginAndTextLine> lines = lineDisplay.recalculateVisibleLines(allLines, Math::ceil, 0, printerJob.getJobSettings().getPageLayout().getPrintableWidth(), lineContainer.getHeight(), true);
            for (MarginAndTextLine line : lines)
            {
                line.setMarginGraphics(printLineNumbers ? EnumSet.of(MarginDisplay.LINE_NUMBER) : EnumSet.noneOf(MarginDisplay.class));
            }
            lineContainer.getChildren().setAll(lines);
            lineContainer.layout();
            lineContainer.applyCss();

            // We need a double layout because the first one will have applied all the styles,
            // which means the second can now wrap long lines at the right points:
            updatePageNumber.accept(pageNumber);
            lineContainer.requestLayout();
            lineContainer.layout();
            lineContainer.applyCss();

            // Take a copy to avoid any update problems:
            List<Node> visibleCells = new ArrayList<>(lineContainer.getChildren());

            // Previously, we had a problem where a run-later task could race us and alter
            // the scrolling which caused us to try to print an empty page.  That shouldn't happen
            // any more, but we still guard against this just in case; better to handle it gracefully
            // than risk running into an exception or infinite loop:
            if (visibleCells.isEmpty())
            {
                // If visible cells empty, just give up gracefully rather than encounter an exception:
                return;
            }

            Node lastCell = visibleCells.get(visibleCells.size() - 1);
            // Last page if we can see the last editor line:
            lastPage = lineDisplay.getLineRangeVisible()[1] >= editorLines - 1 && lastCell.getLayoutY() < lineContainer.getHeight();

            if (!lastPage)
            {
                // If it's not the last page, we crop so that we don't see a partial line at the end of the page
                // We crop to leave out the last visible cell.  Even if it is fully visible, we remove it
                // (too hard to determine if it's fully visible):
                double limitY = lastCell.getLayoutY();
                //Debug.message("Limit Y: " + limitY + " vs " + lineContainer.getHeight());
                lineContainer.setClip(new Rectangle(lineContainer.getWidth(), limitY));
                topLine += visibleCells.size() - 1;
            }
            else
            {
                // No need to clip on last page, but we use translateY to move the content we want
                // up to the top.  (The editor pane won't show empty space beyond the bottom, so we cannot
                // scroll as far as we would like.  Instead, we have the bottom of the content at the bottom
                // of the window, and use translateY to do a fake scroll to move it up to the top of the page.)
                lineContainer.setClip(new Rectangle(lineContainer.getWidth(), lineContainer.getHeight()));
                //lineContainer.setTranslateY(-las);
            }

            // NCCB: I have investigated the printing bug seen in 4.1.2rc2 for
            // a long time, but my best guess is that the bug is not in our code.
            // The way it now manifests, with the page cut off at an arbitrary
            // point halfway through a line, just doesn't seem like it can be
            // the fault of our code or RichTextFX (having seen it occur with the
            // clip above disabled).  Putting a Thread.sleep here seems to avoid
            // the problem, which I think suggests it may be a race hazard in the
            // JDK which uses threading for JavaFX printing.  Not nice, but it
            // does seem to fix the issue:
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
            }
            printerJob.printPage(printNode);
            pageNumber += 1;
            // Failsafe:
            if (topLine <= lastTopLine)
            {
                break;
            }

            lastTopLine = topLine;
        }
    }

    /**
     * Generalised version of print function. This is what is typically called
     * when print is initiated from within the source code editor menu. This
     * sets up and runs the print process as a separate lower priority thread.
     */
    public void print()
    {
        Optional<PrintChoices> choices = new PrintDialog(getWindow(), null, document.getLineCount() >= 200).showAndWait();
        if (!choices.isPresent())
            return;
        PrinterJob job = JavaFXUtil.createPrinterJob();
        if (job == null)
        {
            DialogManager.showErrorFX(getWindow(),"print-no-printers");
        }
        else if (job.showPrintDialog(getWindow()))
        {
            PrintProgressDialog printProgressDialog = new PrintProgressDialog(getWindow(), false);
            FXRunnable printAction = printTo(job, choices.get().printSize, choices.get().printLineNumbers, choices.get().printHighlighting, printProgressDialog.getWithinFileUpdater());
            new Thread("PRINT")
            {
                @Override
                @OnThread(value = Tag.FX, ignoreParent = true)
                public void run()
                {
                    printAction.run();
                    job.endJob();
                    printProgressDialog.finished();
                }
            }.start();
            printProgressDialog.showAndWait();
        }
    }

    private static class ErrorDisplay extends FixDisplayManager
    {
        private final ErrorDetails details;
        private final Supplier<EditorWatcher> editorWatcherSupplier;
        private PopupControl popup;
        private final FlowEditor flowEditor;

        public ErrorDisplay(FlowEditor flowEditor, Supplier<EditorWatcher> editorWatcherSupplier, ErrorDetails details)
        {
            this.details = details;
            this.editorWatcherSupplier = editorWatcherSupplier;
            this.flowEditor = flowEditor;
        }

        public boolean hasQuickFixSelected()
        {
            return highlighted > -1;
        }

        @OnThread(Tag.FXPlatform)
        void executeQuickFix(){
            super.executeSelectedFix();
        }

        @Override
        @OnThread(value=Tag.FX,ignoreParent = true)
        protected void hide()
        {
            this.popup.hide();
        }

        @Override
        @OnThread(value=Tag.FXPlatform,ignoreParent = true)
        protected void postFixError()
        {
            flowEditor.compileOrShowNextError();
        }

        @OnThread(Tag.FXPlatform)
        public void createPopup()
        {
            this.popup = new PopupControl();
            VBox errorVBox = new VBox();

            String errorMessage = ParserMessageHandler.getMessageForCode(details.message);
            TextFlow tf = null;
            if (details.italicMessageStartIndex == -1 || details.italicMessageEndIndex == -1)
            {
                tf = new TextFlow(new Label(errorMessage));
            } else
            {
                Label beforeItalicText = (details.italicMessageStartIndex > 0) ? new Label(errorMessage.substring(0, details.italicMessageStartIndex)) : new Label("");
                Label italicText = new Label(errorMessage.substring(details.italicMessageStartIndex, details.italicMessageEndIndex));
                JavaFXUtil.withStyleClass(italicText, "error-fix-display-italic");
                Label afterItalicText = (details.italicMessageEndIndex < errorMessage.length() - 1) ? new Label(errorMessage.substring(details.italicMessageEndIndex)) : new Label("");
                tf = new TextFlow(beforeItalicText, italicText, afterItalicText);
            }

            errorVBox.getChildren().add(tf);
            prepareFixDisplay(errorVBox, details.corrections, editorWatcherSupplier, details.identifier);

            JavaFXUtil.addStyleClass(tf, "error-label");
            this.popup.setSkin(new Skin<Skinnable>()
            {
                @Override
                @OnThread(Tag.FX)
                public Skinnable getSkinnable()
                {
                    return popup;
                }

                @Override
                @OnThread(Tag.FX)
                public Node getNode()
                {
                    return errorVBox;
                }

                @Override
                @OnThread(Tag.FX)
                public void dispose()
                {

                }
            });
            errorVBox.getStyleClass().add("java-error-popup");
            // No need to bind as only matters if user increases font size while error showing:
            tf.setStyle(PrefMgr.getEditorFontCSS(false).get());
            Config.addPopupStylesheets(errorVBox);
        }
    }
}
