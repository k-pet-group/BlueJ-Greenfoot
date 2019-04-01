package bluej.editor.flow;

import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.EditorWatcher;
import bluej.editor.TextEditor;
import bluej.editor.flow.FlowErrorManager.ErrorDetails;
import bluej.editor.flow.JavaSyntaxView.ParagraphAttribute;
import bluej.editor.flow.StatusLabel.Status;
import bluej.editor.moe.Info;
import bluej.editor.moe.ParserMessageHandler;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.FlowFXTab;
import bluej.editor.stride.FrameEditor;
import bluej.parser.SourceLocation;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.symtab.ClassInfo;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.PopupWindow;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FlowEditor extends ScopeColorsBorderPane implements TextEditor
{
    private final FlowEditorPane flowEditorPane = new FlowEditorPane("");
    private final HoleDocument document = flowEditorPane.getDocument();
    private final JavaSyntaxView javaSyntaxView = new JavaSyntaxView(flowEditorPane, this);
    private final FetchTabbedEditor fetchTabbedEditor;
    private final FlowFXTab fxTab = new FlowFXTab(this, "TODOFLOW Title");
    private final FlowActions actions;
    /** Watcher - provides interface to BlueJ core. May be null (eg for README.txt file). */
    private final EditorWatcher watcher;

    private final boolean sourceIsCode = true /*TODOFLOW*/;           // true if current buffer is code
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


    // TODOFLOW handle the interface-only case
    public boolean containsSourceCode()
    {
        return true;
    }

    public static interface FetchTabbedEditor
    {
        FXTabbedEditor getFXTabbedEditor(boolean newWindow);
    }
    
    // TODOFLOW implement undo and redo
    class UndoManager
    {
        public void compoundEdit(FXPlatformRunnable action)
        {
            action.run();
        }
    }
    
    // package-visible:
    final UndoManager undoManager;

    // TODOFLOW remove this once all its callers are implemented.
    private final class UnimplementedException extends RuntimeException {}


    public FlowEditor(FetchTabbedEditor fetchTabbedEditor, EditorWatcher editorWatcher)
    {
        this.flowEditorPane.setErrorQuery(errorManager);
        this.undoManager = new UndoManager();
        this.fetchTabbedEditor = fetchTabbedEditor;
        this.watcher = editorWatcher;
        info = new Info();
        saveState = new StatusLabel(Status.SAVED, this, errorManager);
        setCenter(flowEditorPane);
        actions = FlowActions.getActions(this);
        flowEditorPane.addCaretListener(this::caretMoved);
        Nodes.addInputMap(this, InputMap.consume(MouseEvent.MOUSE_MOVED, this::mouseMoved));
    }

    private void mouseMoved(MouseEvent event)
    {
        flowEditorPane.getCaretPositionForMouseEvent(event).ifPresent(pos -> showErrorPopupForCaretPos(pos, true));
    }

    /**
     * Notification (from the caret) that the caret position has moved.
     */
    private void caretMoved(int caretPos)
    {
        showErrorPopupForCaretPos(caretPos, false);
        
        /*TODOFLOW
        if (matchBrackets) {
            doBracketMatch();
        }
        actions.userAction();

        // Only send caret moved event if we are open; caret moves while loading
        // but we don't want to send an edit event because of that:
        if (oldCaretLineNumber != getLineNumberAt(caretPos) && isOpen())
        {
            recordEdit(true);

            cancelFreshState();

            // This is a workaround to overcome a bug in RichTextFX lib,
            // which in some cases used to cause the editor to not scroll
            // to follow cursor.
            // The re-layout enforcing is inside a runAfterCurrent to avoid
            // an IllegalArgumentException caused by state inconsistency.
            JavaFXUtil.runAfterCurrent(() -> {
                ensureCaretVisible();
                layout();
            });
        }
        oldCaretLineNumber = getLineNumberAt(caretPos);
        */
    }

    private void showErrorPopupForCaretPos(int caretPos, boolean mousePosition)
    {
        ErrorDetails err = caretPos == -1 ? null : errorManager.getErrorAtPosition(caretPos);
        if (err != null)
        {
            showErrorOverlay(err, caretPos);
        }
        else
        {
            // Only hide if it was a keyboard move,
            // or it was a mouse move but there is no error at the keyboard position
            if (errorDisplay != null && (!mousePosition || !errorDisplay.details.containsPosition(caretPos)))
            {
                showErrorOverlay(null, caretPos);
            }
        }
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
                errorDisplay = new ErrorDisplay(details);
                ErrorDisplay newDisplay = errorDisplay;

                newDisplay.createPopup();
                newDisplay.popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
                newDisplay.popup.setAnchorX(xpos);
                newDisplay.popup.setAnchorY(ypos);
                newDisplay.popup.show(getWindow());

                if (watcher != null) {
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


    private void checkForChangeOnDisk()
    {
        // TODOFLOW
    }

    /**
     * Schedule compilation, if any edits have occurred since last compile.
     */
    @OnThread(Tag.FXPlatform)
    public void cancelFreshState()
    {
        if (sourceIsCode && saveState.isChanged())
        {
            scheduleCompilation(CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
        }

        // Save will occur as part of compilation scheduled above.
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
    private void scheduleCompilation(CompileReason reason, CompileType ctype)
    {
        if (watcher != null)
        {
            // We can collapse multiple compiles, but we cannot collapse an explicit compilation
            // (resulting class files kept) into a non-explicit compilation (result discarded).
            if (! compilationQueued )
            {
                watcher.scheduleCompilation(true, reason, ctype);
                compilationQueued = true;
            }
            else if (compilationStarted ||
                    (ctype != CompileType.ERROR_CHECK_ONLY && ! compilationQueuedExplicit))
            {
                // Either: a previously queued compilation has already started
                // Or: we have queued an error-check-only compilation, but are being asked to
                //     schedule a full (explicit) compile which keeps the resulting classes.
                //
                // In either case, we need to queue a second compilation after the current one
                // finishes. We override any currently queued ERROR_CHECK_ONLY since explicit
                // compiles should take precedence:
                if (! requeueForCompilation || ctype == CompileType.ERROR_CHECK_ONLY)
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
        //TODOFLOW
        return List.of(); 
    }

    @Override
    public boolean showFile(String filename, Charset charset, boolean compiled, String docFilename)
    {
        // TODOFLOW add the rest from MoeEditor
        try
        {
            document.replaceText(0, document.getLength(), Files.readString(new File(filename).toPath(), charset));
            javaSyntaxView.enableParser(false);
            setCompileStatus(compiled);
            return true;
        }
        catch (IOException e)
        {
            Debug.reportError(e);
            return false;
        }
    }

    @Override
    public void clear()
    {
        document.replaceText(0, document.getLength(), "");
    }

    @Override
    public void insertText(String text, boolean caretBack)
    {
        int startPos = Math.min(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition());
        flowEditorPane.replaceSelection(text);
        if (caretBack)
        {
            flowEditorPane.positionCaret(startPos);
        }
    }

    @Override
    public void setSelection(SourceLocation begin, SourceLocation end)
    {
        flowEditorPane.positionCaret(document.getPosition(end));
        flowEditorPane.positionAnchor(document.getPosition(begin));
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
        return document.makeSourceLocation(Math.min(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition()));
    }

    @Override
    public SourceLocation getSelectionEnd()
    {
        return document.makeSourceLocation(Math.max(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition()));
    }

    @Override
    public String getText(SourceLocation begin, SourceLocation end)
    {
        return document.getContent(document.getPosition(begin), document.getPosition(end));
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
        return javaSyntaxView.getRootNode();
    }

    @Override
    public ReparseableDocument getSourceDocument()
    {
        return javaSyntaxView;
    }

    @Override
    public void reloadFile()
    {
        throw new UnimplementedException();
    }

    @Override
    public void setEditorVisible(boolean vis, boolean openInNewWindow)
    {
        // TODOFLOW put pack the commented parts of this method.
        
        if (vis)
        {
            //checkBracketStatus();

            /*
            if (sourceIsCode && !compiledProperty.get() && sourceDocument.notYetShown)
            {
                // Schedule a compilation so we can find and display any errors:
                scheduleCompilation(CompileReason.LOADED, CompileType.ERROR_CHECK_ONLY);
            }
            */

            // Make sure caret is visible after open:
            //sourcePane.requestFollowCaret();
            //sourcePane.layout();
        }
        FXTabbedEditor fxTabbedEditor = fetchTabbedEditor.getFXTabbedEditor(false);
        /*
        if (fxTabbedEditor == null)
        {
            if (openInNewWindow)
            {
                fxTabbedEditor = defaultFXTabbedEditor.get().getProject().createNewFXTabbedEditor();
            }
            else
            {
                fxTabbedEditor = defaultFXTabbedEditor.get();
            }
        }
        else
        {
            // Checks if the editor of the selected target is already opened in a tab inside another window,
            // then do not open it in a new window unless the tab is closed.
            if (openInNewWindow && !fxTabbedEditor.containsTab(fxTab) )
            {
                fxTabbedEditor = defaultFXTabbedEditor.get().getProject().createNewFXTabbedEditor();
            }
        }
        */

        if (vis)
        {
            fxTabbedEditor.addTab(fxTab, vis, true);
        }
        fxTabbedEditor.setWindowVisible(vis, fxTab);
        if (vis)
        {
            fxTabbedEditor.bringToFront(fxTab);
            /*
            if (callbackOnOpen != null)
            {
                callbackOnOpen.run();
            }
            */

            // Allow recalculating the scopes:
            //sourceDocument.notYetShown = false;

            // Make sure caret is visible after open:
            //sourcePane.requestFollowCaret();
            //sourcePane.layout();
        }
    }

    @Override
    public boolean isOpen()
    {
        throw new UnimplementedException();
    }

    @Override
    public void save() throws IOException
    {
        // TODOFLOW don't want to save until we stop throwing exceptions everywhere...
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
        throw new UnimplementedException();
    }

    @Override
    public void displayMessage(String message, int lineNumber, int column)
    {
        throw new UnimplementedException();
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
                if (endPos < getTextLength() - 1 && !document.getContent(endPos, 1).equals("\n"))
                {
                    endPos += 1;
                }
                else if (startPos > 0 && !document.getContent(startPos - 1, 1).equals("\n"))
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
        //TODOFLOW
    }

    @Override
    public boolean setStepMark(int lineNumber, String message, boolean isBreak, DebuggerThread thread)
    {
        throw new UnimplementedException();
    }

    @Override
    public void writeMessage(String msg)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeStepMark()
    {
        throw new UnimplementedException();
    }

    @Override
    public void changeName(String title, String filename, String javaFilename, String docFileName)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setCompiled(boolean compiled)
    {
        setCompileStatus(compiled);
        if (compiled) {
            errorManager.removeAllErrorHighlights();
        }
    }

    /**
     * Toggle the editor's 'compiled' status. This affects display (left-hand margin colour)
     * and whether breakpoints can be set.
     */
    private void setCompileStatus(boolean compiled)
    {
        //TODOFLOW uncomment this once toggle-breakpoint is implemented.
//        actions.getActionByName("toggle-breakpoint").setEnabled(compiled && viewingCode());
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
        errorManager.removeAllErrorHighlights();
        return false;
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
        throw new UnimplementedException();
    }

    @Override
    public void reInitBreakpoints()
    {
        if (mayHaveBreakpoints) {
            mayHaveBreakpoints = false;
            for (int i = 1; i <= numberOfLines(); i++) {
                if (lineHasBreakpoint(i)) {
                    if (watcher != null)
                        watcher.breakpointToggleEvent(i, true);
                    mayHaveBreakpoints = true;
                }
            }
        }
    }

    private boolean lineHasBreakpoint(int i)
    {
        return document.hasLineAttribute(i, ParagraphAttribute.BREAKPOINT);
    }

    @Override
    public boolean isModified()
    {
        //TODOFLOW need to implement saving first
        return false;
    }

    @Override
    public FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printBackground)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setReadOnly(boolean readOnly)
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean isReadOnly()
    {
        throw new UnimplementedException();
    }

    @Override
    public void showInterface(boolean interfaceStatus)
    {
        if (interfaceStatus)
            throw new UnimplementedException();
    }

    @Override
    public Object getProperty(String propertyKey)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setProperty(String propertyKey, Object value)
    {
        throw new UnimplementedException();
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
        throw new UnimplementedException();
    }

    @Override
    public void insertMethodCallInConstructor(String className, CallElement methodCall, FXPlatformConsumer<Boolean> after)
    {
        throw new UnimplementedException();
    }

    @Override
    public void focusMethod(String methodName, List<String> paramTypes)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setExtendsClass(String className, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeExtendsClass(ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void addImplements(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeImports(List<String> importTargets)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setHeaderImage(Image image)
    {
        throw new UnimplementedException();
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
    public javafx.stage.Window getWindow()
    {
        return fxTabbedEditor.getWindow();
    }

    // package visible
    void updateHeaderHasErrors(boolean hasErrors)
    {
        fxTab.setErrorStatus(hasErrors);
    }

    private static class ErrorDisplay
    {
        @OnThread(Tag.Swing)
        private final ErrorDetails details;
        private PopupControl popup;

        public ErrorDisplay(ErrorDetails details)
        {
            this.details = details;
        }

        @OnThread(Tag.FXPlatform)
        public void createPopup()
        {
            this.popup = new PopupControl();

            Text text = new Text(ParserMessageHandler.getMessageForCode(details.message));
            TextFlow flow = new TextFlow(text);
            flow.setMaxWidth(600.0);
            JavaFXUtil.addStyleClass(text, "java-error");
            text.styleProperty().bind(PrefMgr.getEditorFontCSS(true));
            Pane p = new BorderPane(flow);
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
                    return p;
                }

                @Override
                @OnThread(Tag.FX)
                public void dispose()
                {

                }
            });

            p.getStyleClass().add("java-error-popup");
            Config.addPopupStylesheets(p);
            //org.scenicview.ScenicView.show(this.popup.getScene());
        }
    }
}
