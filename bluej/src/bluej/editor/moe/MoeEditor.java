/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg

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
package bluej.editor.moe;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.EditorWatcher;
import bluej.editor.moe.BlueJSyntaxView.ParagraphAttribute;
import bluej.editor.moe.MoeActions.MoeAbstractAction;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.editor.moe.MoeErrorManager.ErrorDetails;
import bluej.editor.moe.MoeSyntaxDocument.Element;
import bluej.editor.moe.PrintDialog.PrintChoices;
import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.FrameEditor;
import bluej.editor.stride.MoeFXTab;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.ImportsCollection;
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
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.JavadocResolver;
import bluej.prefmgr.PrefMgr;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.ExpressionCompletionCalculator;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.slots.SuggestionList;
import bluej.stride.slots.SuggestionList.SuggestionDetails;
import bluej.stride.slots.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.stride.slots.SuggestionList.SuggestionListListener;
import bluej.stride.slots.SuggestionList.SuggestionListParent;
import bluej.stride.slots.SuggestionList.SuggestionShown;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.fxmisc.wellbehaved.event.*;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Moe is the editor of the BlueJ environment. This class is the main class of
 * this editor and implements the top-level functionality.
 * 
 * <p>MoeEditor implements the Editor interface, which defines the interface to the
 * rest of the BlueJ system.
 * 
 * @author Michael Kolling
 * @author Bruce Quig
 * @author Damiano Bolla
 */
@OnThread(Tag.FXPlatform)
public final class MoeEditor extends ScopeColorsBorderPane
    implements bluej.editor.TextEditor, BlueJEventListener
{
    // -------- CONSTANTS --------

    // version number
    final static int version = 400;
    final static String versionString = "3.3.0";

    // suffixes for resources
    final static String LabelSuffix = "Label";
    final static String ActionSuffix = "Action";
    final static String TooltipSuffix = "Tooltip";
    final static String AcceleratorSuffix = "Accelerator";
    // other
    final static String COMPILED = "compiled";
    // file suffixes
    private final static String CRASHFILE_SUFFIX = "#";

    // -------- CLASS VARIABLES --------
    private static boolean matchBrackets = false;

    /**
     * list of actions that are disabled in the readme text file
     */
    private static ArrayList<String> readMeActions;

    // -------- INSTANCE VARIABLES --------  //
    // Strings                               //
    private final String interfaceString = Config.getString("editor.interfaceLabel");
    private final String implementationString = Config.getString("editor.implementationLabel");

    // The code to ask for the default editor to be added to, for the case
    // where we have been hidden but want to reshow ourselves again:
    private final FXSupplier<FXTabbedEditor> defaultFXTabbedEditor;
    @OnThread(Tag.Any) // Should really be Tag.FX
    private FXTabbedEditor fxTabbedEditor;
    private @OnThread(Tag.FX) MoeFXTab fxTab;
    //private StringProperty titleProperty;
    //private final AtomicBoolean panelOpen = new AtomicBoolean();
    public MoeUndoManager undoManager;
    private boolean showingChangedOnDiskDialog = false;

    /** Watcher - provides interface to BlueJ core. May be null (eg for README.txt file). */
    private final EditorWatcher watcher;
    
    private final Properties resources;
    private MoeSyntaxDocument sourceDocument;
    private MoeActions actions;
    private MoeEditorPane sourcePane;         // the component holding the source text
    private WebView htmlPane;           // the component holding the javadoc html
    private Info info;                      // the info number label
    private StatusLabel saveState;          // the status label
    private ComboBox<String> interfaceToggle;

    // find functionality
    private FindPanel finder;
    // The most recent active FindNavigator.  Returns null if there has been no search,
    // or if the document has been modified since the last search.
    private final ObjectProperty<FindNavigator> currentSearchResult = new SimpleObjectProperty<>(null);

    private MenuBar menubar;
    private String filename;                // name of file or null
    private long lastModified;              // time of last modification of file
    private String windowTitle;             // title of editor window
    private String docFilename;             // path to javadoc html file
    private Charset characterSet;           // character set of the file
    private final boolean sourceIsCode;           // true if current buffer is code
    private final BooleanProperty viewingHTML; // changing this alters the interface accordingly
    private int currentStepLineNumber;             // position of step mark (or -1)
    private boolean mayHaveBreakpoints;     // true if there were BP here
    private boolean ignoreChanges = false;
    private boolean tabsAreExpanded = false;

    /** Used to obtain javadoc for arbitrary methods */
    private final JavadocResolver javadocResolver;
    private ReparseRunner reparseRunner;

    /**
     * Property map, allows BlueJ extensions to associate property values with
     * this editor instance; otherwise unused.
     */
    private final HashMap<String,Object> propertyMap = new HashMap<>();
    // Blackbox data recording:
    private int oldCaretLineNumber = -1;
    private ErrorDisplay errorDisplay;

    // These variables track the validity of our compiled state.
    private boolean compilationQueued = false;    // queued for compilation?
    private boolean compilationQueuedExplicit = false;  // explicit compilation?
    private boolean compilationStarted = false;
    private boolean requeueForCompilation = false; // re-queue after current compile?
    private CompileReason requeueReason;
    private CompileType requeueType;

    /** Manages display of compiler and parse errors */
    private final MoeErrorManager errorManager = new MoeErrorManager(this, enable -> {});
    private int mouseCaretPos = -1;


    /**
     * A callback to call (on the Swing thread) when this editor is opened.
     */
    private final FXPlatformRunnable callbackOnOpen;
    @OnThread(Tag.FX)
    private final List<Menu> fxMenus = new ArrayList<>();
    private final BooleanProperty compiledProperty = new SimpleBooleanProperty(true);
    @OnThread(Tag.FXPlatform)
    private boolean respondingToChange;
    private String lastSearchString = "";

    /**
     * Constructor. Title may be null.
     */
    public MoeEditor(MoeEditorParameters parameters, FXSupplier<FXTabbedEditor> getDefaultEditor)
    {
        super();
        this.defaultFXTabbedEditor = getDefaultEditor;
        final String fxWindowTitle = parameters.getTitle();
        watcher = parameters.getWatcher();
        resources = parameters.getResources();
        javadocResolver = parameters.getJavadocResolver();

        filename = null;
        windowTitle = parameters.getTitle();
        sourceIsCode = parameters.isCode();
        viewingHTML = new SimpleBooleanProperty(false);
        currentStepLineNumber = -1;
        mayHaveBreakpoints = false;
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);

        initWindow(parameters.getProjectResolver());
        callbackOnOpen = parameters.getCallbackOnOpen();

        this.fxTabbedEditor = getDefaultEditor.get();
        this.fxTab = new MoeFXTab(this, fxWindowTitle);
    }

    /**
     * Find the position of a substring in a given string, 
     * can specify direction and whether the search should ignoring case
     * Return the position of the substring or -1.
     *
     * @param  text        the full string to be searched
     * @param  sub         the substring that we're looking for
     * @param  ignoreCase  if true, case is ignored
     * @param  backwards   Description of the Parameter
     * @param  foundPos   Offset for the string search
     * @return             Description of the Return Value
     * @returns            the index of the substring, or -1 if not found
     */
    private static int findSubstring(String text, String sub, boolean ignoreCase, boolean backwards, int foundPos)
    {
        int strlen = text.length();
        int sublen = sub.length();

        if (sublen == 0) {
            return -1;
        }

        boolean found = false;
        int pos = foundPos;
        boolean itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));
        while (!found && !itsOver) {
            found = text.regionMatches(ignoreCase, pos, sub, 0, sublen);                
            if (found) {
                return pos;
            }
            if (!found) {
                pos = (backwards ? pos - 1 : pos + 1);
                itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));
            }
        }      
        return -1;
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

    /*
     * Load the file "filename" and show the editor window.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean showFile(String filename, Charset charset, boolean compiled, String docFilename)
    {
        this.filename = filename;
        this.docFilename = docFilename;
        this.characterSet = charset;

        boolean loaded = false;

        if (filename != null) {
            setupJavadocMangler();
            try {
                // check for crash file
                String crashFilename = filename + CRASHFILE_SUFFIX;
                String backupFilename = crashFilename + "backup";
                File crashFile = new File(crashFilename);
                if (crashFile.exists()) {
                    File backupFile = new File(backupFilename);
                    backupFile.delete();
                    crashFile.renameTo(backupFile);
                    DialogManager.showMessageFX(fxTabbedEditor.getWindow(), "editor-crashed");
                }

                // FileReader reader = new FileReader(filename);
                FileInputStream inputStream = new FileInputStream(filename);
                Reader reader = new InputStreamReader(inputStream, charset);
                ignoreChanges = true;
                sourcePane.read(reader);
                ignoreChanges = false;
                try {
                    reader.close();
                    inputStream.close();
                }
                catch (IOException ioe) {}
                File file = new File(filename);
                setLastModified(file.lastModified());

                listenToChanges(sourceDocument);
                //NAVIFX
                //naviView.setDocument(sourceDocument);
                
                sourceDocument.enableParser(false);
                loaded = true;
                
                scheduleReparseRunner();
            }
            catch (IOException ex) {
                // TODO display user-visible error
                Debug.reportError("Couldn't open file", ex);
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

    /*
     * Reload the editor content from the associated file, discarding unsaved
     * edits.
     */
    @Override
    public void reloadFile()
    {
        doReload();
    }
    
    /*
     * Wipe out contents of the editor.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void clear()
    {
        ignoreChanges = true;
        sourcePane.setText("");
        ignoreChanges = false;
    }

    /**
     * Insert a string into the buffer. The editor is not immediately
     * redisplayed. This function is typically used in a sequence "clear;
     * [insertText]*; setVisible(true)". If the selection is on, it is replaced
     * by the new text.
     * 
     * @param text  the text to be inserted
     * @param caretBack  move the caret to the beginning of the inserted text
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void insertText(String text, boolean caretBack)
    {
        sourcePane.replaceSelection(text);
        if (caretBack) {
            sourcePane.setCaretPosition(sourcePane.getCaretPosition() - text.length());
        }
    }

    /**
     * Show the editor window. This includes whatever is necessary of the
     * following: make visible, de-iconify, bring to front of window stack.
     *
     * @param vis  The new visible value
     * @param openInNewWindow if this is true, the editor opens in a new window
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setEditorVisible(boolean vis, boolean openInNewWindow)
    {
        if (vis)
        {
            checkBracketStatus();
            
            if (sourceIsCode && !compiledProperty.get() && sourceDocument.notYetShown)
            {
                // Schedule a compilation so we can find and display any errors:
                scheduleCompilation(CompileReason.LOADED, CompileType.ERROR_CHECK_ONLY);
            }

            // Make sure caret is visible after open:
            //sourcePane.requestFollowCaret();
            //sourcePane.layout();
        }
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

        if (vis)
        {
            fxTabbedEditor.addTab(fxTab, vis, true);
        }
        fxTabbedEditor.setWindowVisible(vis, fxTab);
        if (vis)
        {
            fxTabbedEditor.bringToFront(fxTab);
            if (callbackOnOpen != null)
            {
                callbackOnOpen.run();
            }

            // Allow recalculating the scopes:
            sourceDocument.notYetShown = false;
            
            // Make sure caret is visible after open:
            sourcePane.requestFollowCaret();
            sourcePane.layout();
        }
    }

    /**
     * Refresh the editor window.
     */
    @Override
    public void refresh()
    {
        checkBracketStatus();
        scheduleReparseRunner(); //whenever we change the scope highlighter, call scheduleReparseRunner to create a reparser to that file: if the scope highlighter is 0, it will do nothing. However, if it is not zero, it will ensure the editor is updated accordingly.
    }

    /*
     * Save the buffer to disk under current filename, if there any changes.
     * This method may be called often.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void save()
        throws IOException
    {
        IOException failureException = null;
        if (saveState.isChanged()) {
            // Record any edits with the data collection system:
            recordEdit(true);
            
            // Play it safe and avoid overwriting code that has been changed outside BlueJ (or at least,
            // outside *this* instance of BlueJ):
            checkForChangeOnDisk();
            if (! saveState.isChanged())
            {
                return;
            }
            
            Writer writer = null;
            try {
                // The crash file is used during writing and will remain in
                // case of a crash during the write operation.
                String crashFilename = filename + CRASHFILE_SUFFIX;

                // make a backup to the crash file
                FileUtility.copyFile(filename, crashFilename);

                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(filename));
                writer = new OutputStreamWriter(ostream, characterSet);
                sourcePane.write(writer);
                writer.close(); writer = null;
                setLastModified(new File(filename).lastModified());
                File crashFile = new File(crashFilename);
                crashFile.delete();

                // Do this last, as it may trigger further actions in the watcher:
                setSaved();
            }
            catch (IOException ex) {
                failureException = ex;
                info.message (Config.getString("editor.info.errorSaving") + " - " + ex.getLocalizedMessage());
            }
            finally {
                try {
                    if(writer != null)
                        writer.close();
                }
                catch (IOException ex) {
                    failureException = ex;
                }
            }
        }

        // If an error occurred, set a message in the editor status bar, and
        // re-throw the exception.
        if (failureException != null) {
            info.message (Config.getString("editor.info.errorSaving")
                        + " - " + failureException.getLocalizedMessage());
            throw failureException;
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
        
        try {
            save();
        }
        catch (IOException ioe) {
            // TODO we should definitely show dialog here.
        }

        doClose();
    }

    /**
     * Display a message (used for compile/runtime errors). An editor must
     * support at least two lines of message text, so the message can contain a
     * newline character.
     *  @param message  the message to be displayed
     * @param lineNumber  The line to highlight
     * @param column   the column to move the cursor to
     */
    @Override
    public void displayMessage(String message, int lineNumber, int column)
    {
        switchToSourceView();

        Element line = getSourceLine(lineNumber);
        int pos = line.getStartOffset();

        // highlight the line

        sourcePane.setCaretPosition(pos);
        sourcePane.moveCaretPosition(line.getEndOffset() - 1);  // w/o line break
        sourcePane.requestFollowCaret();

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
        
        Element line = getSourceLine((int) diagnostic.getStartLine());
        if (line != null) {
            // Limit diagnostic display to a single line.
            int startPos = getPosFromColumn(line, (int) diagnostic.getStartColumn());
            int endPos;
            if (diagnostic.getStartLine() != diagnostic.getEndLine()) {
                endPos = line.getEndOffset() - 1;
            }
            else {
                endPos = getPosFromColumn(line, (int) diagnostic.getEndColumn());
            }

            // highlight the error and the line on which it occurs
            // If error is zero-width, make it one character wide:
            if (endPos == startPos)
            {
                // By default, extend one char right, unless that would encompass a newline:
                if (endPos < getTextLength() - 1 && !sourceDocument.getText(endPos, 1).equals("\n"))
                {
                    endPos += 1;
                }
                else if (startPos > 0 && !sourceDocument.getText(startPos - 1, 1).equals("\n"))
                {
                    startPos -= 1;
                }
            }
            errorManager.addErrorHighlight(startPos, endPos, diagnostic.getMessage(), diagnostic.getIdentifier());
            repaint();
            //NAVIFX
            //naviView.repaintLines((int)diagnostic.getStartLine(), (int)diagnostic.getEndLine());
        }

        return true;
    }
    
    @Override
    public boolean setStepMark(int lineNumber, String message, boolean isBreak,
            DebuggerThread thread)
    {
        switchToSourceView();

        if (isBreak) {
            setStepMark(lineNumber);
        }

        // highlight the line
        sourceDocument.showStepLine(lineNumber);

        // Scroll to the line:
        sourcePane.setCaretPosition(getOffsetFromLineColumn(new SourceLocation(lineNumber, 1)));
        sourcePane.requestFollowCaret();

        // display the message

        if (message != null) {
            info.messageImportant(message);
        }
        
        return false;
    }

    /**
     * Get a position in a line from a column number, where the column number assumes
     * tab stops are every 8 spaces.
     */
    private int getPosFromColumn(Element line, int column)
    {
        int spos = line.getStartOffset();
        int epos = line.getEndOffset();
        int testPos = Math.min(epos - spos - 1, column - 1);
        if (testPos <= 0)
        {
            return spos;
        }

        int cpos = 0; // what the actual column is so far
        int tpos = 0; // where we are in the string
        String lineText = sourceDocument.getText(spos, testPos);

        while (cpos < column - 1)
        {
            int tabPos = lineText.indexOf('\t', tpos);
            if (tabPos == -1)
            {
                // No more tabs...
                tpos += column - cpos - 1;
                return Math.min(spos + tpos, epos - 1);
            }

            int newcpos = cpos + (tabPos - tpos);
            if (newcpos >= column)
            {
                tpos += column - cpos - 1;
                return spos + tpos;
            }

            cpos = newcpos;

            cpos += 8; // hit tab
            cpos -= cpos % 8;  // back to tab stop

            tpos = tabPos + 1; // skip over the tab char
        }

        return spos;
    }

    /**
     * Set the selection of the editor (in the source pane) to be {@code len} characters on
     * line {@code lineNumber}, starting with column {@code columnNumber}.
     * 
     * @param lineNumber  the line to select characters on
     * @param columnNumber  the column to start selection at (1st column is 1 - not 0)
     * @param len         the number of characters to select
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setSelection(int lineNumber, int columnNumber, int len)
    {
        Element line = getSourceLine(lineNumber);

        sourcePane.select(line.getStartOffset() + columnNumber - 1, 
                line.getStartOffset() + columnNumber + len - 1);
    }

    /**
     * Select a specified area of text in the source pane.
     * 
     * @param lineNumber1  The new selection value
     * @param columnNumber1  The new selection value
     * @param lineNumber2  The new selection value
     * @param columnNumber2  The new selection value
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setSelection(int lineNumber1, int columnNumber1, int lineNumber2, int columnNumber2)
    {
        /*
         * if (lineNumber2 < lineNumber1) return; if (lineNumber2 == lineNumber1 &&
         * (columnNumber2 < columnNumber1)) return;
         */
        Element line1 = getSourceLine(lineNumber1);
        Element line2 = getSourceLine(lineNumber2);

        sourcePane.select(line1.getStartOffset() + columnNumber1 - 1, line2.getStartOffset() + columnNumber2 - 1);
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
        if (currentStepLineNumber != -1) {
            sourceDocument.setParagraphAttributesForLineNumber(currentStepLineNumber, Collections.singletonMap(ParagraphAttribute.STEP_MARK, false));
            currentStepLineNumber = -1;
            // remove highlight as well
            sourcePane.setCaretPosition(sourcePane.getCaretPosition());
            // force an update of UI
            repaint();
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
     * Set the "compiled" status
     * 
     * @param compiled  True if the class has been compiled.
     */
    @Override
    public void setCompiled(boolean compiled)
    {
        setCompileStatus(compiled);
        if (compiled) {
            errorManager.removeAllErrorHighlights();
        }
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

        if (isVisible() && classesKept)
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

    /**
     * Called when all the breakpoints have been cleared. The editor should
     * update its display to show that no breakpoints are set.
     */
    @Override
    public void removeBreakpoints()
    {
        // This may be a callback in response to a modification event.
        // If we try to remove breakpoints during the modification notification,
        // AbstractDocument throws an exception.
        JavaFXUtil.runAfterCurrent(() -> clearAllBreakpoints());
    }

    /**
     * The editor must re-set all its breakpoints via the EditorWatcher
     * interface.
     */
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

    /**
     *  Determine whether this buffer has been modified.
     *
     * @return    a boolean indicating whether the file is modified
     */
    @Override
    public boolean isModified()
    {
        return (saveState.isChanged());
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
        return !sourcePane.isEditable();
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
            saveState.setState(StatusLabel.Status.READONLY);
        }
        sourcePane.setEditable(!readOnly);
    }

    /**
     * Set this editor to display either the interface or the source code of
     * this class
     * 
     * @param interfaceStatus  If true, display class interface, otherwise source.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void showInterface(boolean interfaceStatus)
    {
        interfaceToggle.getSelectionModel().select(interfaceStatus ? 1 : 0);
    }

    /**
     * Tell whether the editor is currently displaying the interface or the
     * source of the class.
     * 
     * @return True, if interface is currently shown, false otherwise.
     */
    public boolean isShowingInterface()
    {
        return viewingHTML.get();
    }
    
    /**
     * Returns the current caret location within the edited text.
     * 
     * @return An object describing the current caret location.
     */
    @Override
    public SourceLocation getCaretLocation()
    {
        int caretOffset = sourcePane.getCaretPosition();
        return getLineColumnFromOffset(caretOffset);
    }

    /**
     * Sets the current Caret location within the edited text (source pane).
     * 
     * @param location  The location in the text to set the Caret to.
     * @throws IllegalArgumentException
     *             if the specified TextLocation represents a position which
     *             does not exist in the text.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setCaretLocation(SourceLocation location)
    {
        sourcePane.setCaretPosition(getOffsetFromLineColumn(location));
    }
    
    /**
     * Returns the SourceLocation object corresponding to the given offset in the
     * source text.
     * 
     * @param offset  The number of characters from the beginning of text (starting
     *                from zero)
     * @return the SourceLocation object or null if the offset points outside the
     *         text.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public SourceLocation getLineColumnFromOffset(int offset)
    {
        if (offset < 0) {
            return null;
        }
        
        Element map = sourceDocument.getDefaultRootElement();
        int lineNumber = map.getElementIndex(offset);

        Element lineElement = map.getElement(lineNumber);
        if (offset > lineElement.getEndOffset()) {
            return null;
        }
        
        int column = offset - lineElement.getStartOffset();

        return new SourceLocation(lineNumber+1, column+1);
    }
    
    /**
     * Returns the location where the current selection (in the source pane)
     * begins.
     * 
     * @return the current beginning of the selection or null if no text is
     *         selected.
     */
    @Override
    public SourceLocation getSelectionBegin()
    {
        // If the dot is == as the mark then there is no selection.
        if (sourcePane.getCaretDot() == sourcePane.getCaretMark()) {
            return null;
        }

        int beginOffset = Math.min(sourcePane.getCaretDot(), sourcePane.getCaretMark());

        return getLineColumnFromOffset(beginOffset);
    }

    /**
     * Returns the location where the current selection (in the
     * source pane) ends.
     * 
     * @return the current end of the selection or null if no text is selected.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public SourceLocation getSelectionEnd()
    {
        // If the dot is == as the mark then there is no selection.
        if (sourcePane.getCaretDot() == sourcePane.getCaretMark()) {
            return null;
        }

        int endOffset = Math.max(sourcePane.getCaretDot(), sourcePane.getCaretMark());

        return getLineColumnFromOffset(endOffset);
    }
    
    // ---- BlueJEventListener interface ----

    /**
     * Returns the source text between two locations as a string.
     * 
     * @param begin  The beginning of the text to get
     * @param end    The end of the text to get
     * @return  The text between the 'begin' and 'end' positions.
     * @throws IllegalArgumentException
     *             if either of the specified TextLocations represent a position
     *             which does not exist in the text.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public String getText(SourceLocation begin, SourceLocation end)
    {
        int first = getOffsetFromLineColumn(begin);
        int last = getOffsetFromLineColumn(end);
        int beginOffset = Math.min(first, last);
        int endOffset = Math.max(first, last);

        return sourceDocument.getText(beginOffset, endOffset - beginOffset);
    }

    // -------- DocumentListener interface --------

    /**
     * Request to the editor to replace the text between 'begin' and 'end' with
     * the given newText. If begin and end point to the same location, the text
     * is inserted.
     * 
     * @param begin  The start position of text to replace
     * @param end    The end position of text to replace
     * @param newText  The text to insert
     * @throws IllegalArgumentException
     *             if either of the specified SourceLocation represent a position
     *             which does not exist in the text.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setText(SourceLocation begin, SourceLocation end, String newText)
    {
        int start = getOffsetFromLineColumn(begin);
        int finish = getOffsetFromLineColumn(end);

        int beginOffset = Math.min(start, finish);
        int endOffset = Math.max(start, finish);

        if (beginOffset != endOffset) {
            sourceDocument.remove(beginOffset, endOffset - beginOffset);
        }

        sourceDocument.insertString(beginOffset, newText);
    }

    /**
     * Request to the editor to mark the source text between two positions as selected.
     * 
     * @param begin  The start position of the selection
     * @param end  The end position of the selection
     * @throws IllegalArgumentException
     *             if either of the specified TextLocations represent a position
     *             which does not exist in the text.
     */
    @Override
    public void setSelection(SourceLocation begin, SourceLocation end)
    {
        int start = getOffsetFromLineColumn(begin);
        int finish = getOffsetFromLineColumn(end);

        int selectionStart = Math.min(start, finish);
        int selectionEnd = Math.max(start, finish);

        sourcePane.setCaretPosition(selectionStart);
        sourcePane.moveCaretPosition(selectionEnd);
    }

    /**
     * Translates a SourceLocation into an offset into the source text
     * held by the editor.
     * 
     * @param location  position to be translated
     * @return the offset into the content of this editor
     * @throws IllegalArgumentException
     *             if the specified SourceLocation represent a position which does
     *             not exist in the text.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public int getOffsetFromLineColumn(SourceLocation location)
    {
        int col = location.getColumn() - 1;
        int line = location.getLine() - 1;
        
        if (line < 0 || col < 0) {
            throw new IllegalArgumentException("line or column < 1");
        }
        
        Element map = sourceDocument.getDefaultRootElement();
        if (line >= map.getElementCount()) {
            throw new IllegalArgumentException("line=" + location.getLine()
                    + " is out of bound");
        }

        Element lineElement = sourceDocument.getDefaultRootElement()
                .getElement(line);

        int lineOffset = lineElement.getStartOffset();
        int lineLen = lineElement.getEndOffset() - lineOffset;

        if (col > lineLen) {
            throw new IllegalArgumentException("column=" + location.getColumn() + " greater than line len=" + lineLen);
        }

        return lineOffset + col;
    }
    
    // --------------------------------------------------------------------

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

    /**
     * Returns the length of the line indicated in the edited text.
     * Zero is a valid value if the given line has no characters in it.
     *
     * @param  line  the line in the text for which the length should be calculated, starting from 0
     * @return       the length of the line, -1 if line is invalid
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public int getLineLength(int line)
    {
        if (line < 0) {
            return -1;
        }

        Element lineElement = sourceDocument.getDefaultRootElement().getElement(line);
        if (lineElement == null) {
            return -1;
        }

        int startOffset = lineElement.getStartOffset();

        return lineElement.getEndOffset() - startOffset;
    }

    // --------------------------------------------------------------------
    
    /**
     * Returns the length of the source document.
     *
     * It is possible to obtain the line and column of the last character of text by using
     * this method together with the getLineColumnFromOffset() method.
     *
     * @return the source length (>= 0)
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public int getTextLength ()
    {
        return sourceDocument.getLength();
    }

    // --------------------------------------------------------------------
    
    /**
     * Return the number of lines in the source document.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public int numberOfLines()
    {
        return sourceDocument.getDefaultRootElement().getElementCount();
    }

    // --------------------------------------------------------------------

    /*
     * @see bluej.editor.Editor#getParsedNode()
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public ParsedCUNode getParsedNode()
    {
        return sourceDocument.getParser();
    }


    // --------------------------------------------------------------------
    
    /**
     * Schedule the ReparseRunner on the FX Platform queue, if it is not already scheduled.
     */
    private void scheduleReparseRunner() {
        if (reparseRunner == null) {
            reparseRunner = new ReparseRunner(this);
            JavaFXUtil.runPlatformLater(reparseRunner);
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Informs the editor that the re-parse runner has de-scheduled itself due to lack
     * of work.
     */
    public void reparseRunnerFinished()
    {
        reparseRunner = null;
    }

    // --------------------------------------------------------------------

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

    private void listenToChanges(MoeSyntaxDocument msd)
    {
        msd.getDocument().plainChanges().subscribe(c -> {
            if (!ignoreChanges)
            {
                boolean singleLineChange = !c.getInserted().contains("\n") && !c.getRemoved().contains("\n");
                boolean inserted = !c.getInserted().isEmpty();
                documentContentChanged(singleLineChange, inserted, c.getPosition(), c.getInsertionEnd() - c.getPosition(), c.getInserted());
            }
        });
    }

    /**
     * A change has been made to the source code content.
     */
    private void documentContentChanged(boolean singleLineChange, boolean inserted, int offset, int insertionLength, String insertedContent)
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
            saveState.setState(StatusLabel.Status.CHANGED);
            setChanged();
        }

        if (!singleLineChange) // For a multi-line change, always compile:
        {
            saveState.setState(StatusLabel.Status.CHANGED);
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
            errorManager.removeAllErrorHighlights();
            errorManager.documentContentChanged();
            showErrorOverlay(null, 0);
        });
        actions.userAction();
        
        // This may handle re-indentation; as this mutates the
        // document, it must be done outside the notification.
        if (inserted && "}".equals(insertedContent) && PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
            JavaFXUtil.runAfterCurrent(() -> {
                // It's possible, e.g. due to de-indenting, that by the time we
                // get here, the offset won't be valid any more, in which case don't
                // worry about it:
                if (offset + insertionLength <= getSourcePane().getLength() && sourcePane.getText(offset, offset + insertionLength).equals("}"))
                {
                    actions.closingBrace(offset);
                }
            });
        }
        
        recordEdit(false);        
        
        scheduleReparseRunner();
        respondingToChange = false;
    }

    // --------------------------------------------------------------------

    /**
     * Clear the message in the info area.
     */
    public void clearMessage()
    {
        info.clear();
    }

    // --------------------------------------------------------------------

    /**
     * Display a message into the info area.
     * 
     * @param msg  the message to display
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void writeMessage(String msg)
    {
        info.message(msg);
    }

    /**
     * Write a warning message into the info area. Typically some form of
     * unexpected behaviour has occurred.
     * 
     * @param msg  Description of the Parameter
     */
    public void writeWarningMessage(String msg)
    {
        info.message (msg);
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
     * Prints source code from Editor
     * 
     * @param printerJob  A PrinterJob to print to.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printBackground)
    {
        ScopeColorsBorderPane scopeColorsPane = new ScopeColorsBorderPane();
        MoeSyntaxDocument doc = new MoeSyntaxDocument(scopeColorsPane);
        // Note: very important we make this call before copyFrom, as copyFrom is what triggers
        // the run-later that marking as printing suppresses:
        doc.markAsForPrinting();
        doc.copyFrom(sourceDocument);
        MoeEditorPane editorPane = doc.makeEditorPane(null, null);
        Label pageNumberLabel = new Label("");
        String timestamp = new SimpleDateFormat("yyyy-MMM-dd HH:mm").format(new Date());
        BorderPane header = new BorderPane(new Label(timestamp), null, pageNumberLabel, null, new Label(getTitle()));
        // If we let labels be default font, it can cause weird font corruption when printing.
        // But setting labels to same font as editor seems to avoid the issue:
        for (Node node : header.getChildren())
        {
            node.setStyle(PrefMgr.getEditorFontFamilyCSS());
        }
        header.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));
        header.setPadding(new Insets(5));
        BorderPane rootPane = new BorderPane(editorPane, header, null, scopeColorsPane, null);
        // The scopeColorsPane needs to be in the scene to access the CSS colors.
        // But we don't actually want it visible:
        scopeColorsPane.setManaged(false);
        scopeColorsPane.setVisible(false);
        rootPane.setBackground(null);
        // JavaFX seems to always print at 72 DPI, regardless of printer DPI:
        // This means that that the point width (1/72 of an inch) is actually the pixel width, too:
        double pixelWidth = printerJob.getJobSettings().getPageLayout().getPrintableWidth();
        double pixelHeight = printerJob.getJobSettings().getPageLayout().getPrintableHeight();
        Scene scene = new Scene(rootPane, pixelWidth, pixelHeight, Color.GRAY);
        Config.addEditorStylesheets(scene);

        // We could make page size match screen size by scaling font size by difference in DPIs:
        //editorPane.styleProperty().unbind();
        //editorPane.setStyle("-fx-font-size: " + PrefMgr.getEditorFontSize().getValue().doubleValue() * 0.75 + "pt;");
        editorPane.setPrinting(true, printSize, printLineNumbers);
        editorPane.setWrapText(true);
        editorPane.applyCss();
        // Make sure the scroll bars in the editor are invisible when printing:
        for (Node node : editorPane.lookupAll(".scroll-bar"))
        {
            node.setVisible(false);
            node.setManaged(false);
        }
        rootPane.requestLayout();
        rootPane.layout();
        rootPane.applyCss();
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
        VirtualFlow<?, ?> virtualFlow = (VirtualFlow<?, ?>) editorPane.lookup(".virtual-flow");
        FXConsumer<Integer> updatePageNumber = n -> {
            pageNumberLabel.setText("Page " + n);
            rootPane.requestLayout();
            rootPane.layout();
            rootPane.applyCss();
        };
        // Run printing in another thread:
        return () -> printPages(printerJob, rootPane, updatePageNumber, editorPane, virtualFlow);
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
     * @param editorPane The editor pane to print
     * @param virtualFlow The virtual flow inside the editor pane.
     * @param <T> Parameter type of the VirtualFlow.  Will be inferred.
     * @param <C> Parameter type of the VirtualFlow.  Will be inferred.
     */
    @OnThread(Tag.FX)
    public static <T, C extends org.fxmisc.flowless.Cell<T, ?>> void printPages(PrinterJob printerJob,
        Node printNode, FXConsumer<Integer> updatePageNumber,
        GenericStyledArea<?, ?, ?> editorPane, VirtualFlow<T, C> virtualFlow)
    {
        virtualFlow.scrollXToPixel(0);
        // We must manually scroll down the editor, one page's worth at a time.  We keep track of the top line visible:
        int topLine = 0;
        boolean lastPage = false;
        int editorLines = editorPane.getParagraphs().size();
        int pageNumber = 1;
        while (topLine < editorLines && !lastPage)
        {
            // Scroll to make topLine actually at the top:
            virtualFlow.showAsFirst(topLine);
            virtualFlow.requestLayout();
            virtualFlow.layout();
            virtualFlow.applyCss();

            // Take a copy to avoid any update problems:
            List<C> visibleCells = new ArrayList<>(virtualFlow.visibleCells());

            // Previously, we had a problem where a run-later task could race us and alter
            // the scrolling which caused us to try to print an empty page.  That shouldn't happen
            // any more, but we still guard against this just in case; better to handle it gracefully
            // than risk running into an exception or infinite loop:
            if (visibleCells.isEmpty())
            {
                // If visible cells empty, just give up gracefully rather than encounter an exception:
                return;
            }

            C lastCell = visibleCells.get(visibleCells.size() - 1);
            // Last page if we can see the last editor line:
            lastPage = virtualFlow.getCellIfVisible(editorLines - 1).isPresent();

            if (!lastPage)
            {
                // If it's not the last page, we crop so that we don't see a partial line at the end of the page
                // We crop to leave out the last visible cell.  Even if it is fully visible, we remove it
                // (too hard to determine if it's fully visible):
                double limitY = virtualFlow.cellToViewport(lastCell, 0, 0).getY();
                editorPane.setClip(new javafx.scene.shape.Rectangle(editorPane.getWidth(), limitY));
                topLine += visibleCells.size() - 1;
            }
            else
            {
                // No need to clip on last page, but we use translateY to move the content we want
                // up to the top.  (The editor pane won't show empty space beyond the bottom, so we cannot
                // scroll as far as we would like.  Instead, we have the bottom of the content at the bottom
                // of the window, and use translateY to do a fake scroll to move it up to the top of the page.)
                editorPane.setClip(new javafx.scene.shape.Rectangle(editorPane.getWidth(), editorPane.getHeight()));
                editorPane.setTranslateY(-virtualFlow.cellToViewport(virtualFlow.getCell(topLine), 0, 0).getY());
            }
            updatePageNumber.accept(pageNumber);
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
        }
    }

    /**
     * Generalised version of print function. This is what is typically called
     * when print is initiated from within the source code editor menu. This
     * sets up and runs the print process as a separate lower priority thread.
     */
    public void print()
    {
        Optional<PrintChoices> choices = new PrintDialog(getWindow(), null).showAndWait();
        if (!choices.isPresent())
            return;
        PrinterJob job = JavaFXUtil.createPrinterJob();
        if (job == null)
        {
            DialogManager.showErrorFX(getWindow(),"print-no-printers");
        }
        else if (job.showPrintDialog(getWindow()))
        {
            FXRunnable printAction = printTo(job, choices.get().printSize, choices.get().printLineNumbers, choices.get().printHighlighting);
            new Thread("PRINT")
            {
                @Override
                @OnThread(value = Tag.FX, ignoreParent = true)
                public void run()
                {
                    printAction.run();
                    job.endJob();
                }
            }.start();
        }
    }

    /**
     * The editor has been closed. Hide the editor window now.
     */
    public void doClose()
    {
        setEditorVisible(false, false);
        if (watcher != null) {
            //setting the naviview visible property when an editor is closed
            //NAVIFX
            //watcher.setProperty(EditorWatcher.NAVIVIEW_EXPANDED_PROPERTY, String.valueOf(dividerPanel.isExpanded()));
            watcher.closeEvent(this);
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Check whether TABs need expanding in this editor. If they do, return
     * true, and mark tabs as no longer needing expanding (i.e. subsequent
     * calls will return false).
     */
    public boolean checkExpandTabs()
    {
        if (tabsAreExpanded) {
            return false;
        }
        tabsAreExpanded = true;
        return true;
    }

    // --------------------------------------------------------------------

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
     * Implementation of "find-next" user function.
     */
    public void findNext(boolean backwards)
    {
        if (currentSearchResult.get() == null || !currentSearchResult.get().validProperty().get())
        {
            String search = sourcePane.getSelectedText();
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
     * Do a find with info in the info area.
     */
    boolean findString(String s, boolean backward,
            boolean ignoreCase, boolean wrap)
    {
        if (s.length() == 0) {
            info.message(" ");
            return false;
        }

        boolean found;
        if (backward){
            found = doFind(s, ignoreCase) != null;
        }
        else {
            setCaretPositionForward(1);
            found = doFind(s, ignoreCase) != null;
        }

        StringBuilder msg = new StringBuilder(Config.getString("editor.find.find.label") + " ");
        msg.append(backward ? Config.getString("editor.find.backward") : Config.getString("editor.find.forward"));
        if (ignoreCase || wrap) {
            msg.append(" (");
        }
        if (ignoreCase) {
            msg.append(Config.getString("editor.find.ignoreCase").toLowerCase()).append(", ");
        }
        if (wrap) { 
            msg.append(Config.getString("editor.find.wrapAround").toLowerCase()).append(", ");
        }
        if (ignoreCase || wrap) { 
            msg.replace(msg.length() - 2, msg.length(), "): ");
        }
        else { 
            msg.append(": ");
        }

        msg.append(s);
        if (found) {
            info.message(msg.toString());
        }
        else {
            info.message (msg.toString(), Config.getString("editor.info.notFound"));
        }

        return found;
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

    @Override
    @OnThread(Tag.FXPlatform)
    public void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }


    /**
     * An interface for dealing with search results.
     */
    public static interface FindNavigator
    {
        /**
         * Highlights all search results
         */
        public void highlightAll();

        /**
         * Selects the next search result, wrapping if necessary
         *
         * @param canBeAtCurrentPos If true, "next" result can include one beginning
         *                          at the start of the current selection (e.g. when
         *                          typing in search field).  If false, next result
         *                          must be beyond start of selection (e.g. when pressing
         *                          find next button).
         */
        public void selectNext(boolean canBeAtCurrentPos);

        /**
         * Selects the previous search result, wrapping backwards if necessary.
         */
        public void selectPrev();

        /**
         * Is this search result still valid?  Search results get invalidated
         * by modifying the document, or performing a new search.
         */
        public BooleanExpression validProperty();

        /**
         * Replaces the current selected search result with the given replacement
         * string, and returns the updated search.  THis search object will no longer
         * be valid, and you should switch to using the returned result.
         */
        public FindNavigator replaceCurrent(String replacement);

        /**
         * Replaces all search results with the given replacement string.
         * This search result will no longer be valid, but there's no point
         * searching again, as all instances will have been replaced.
         */
        public void replaceAll(String replacement);
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
        sourcePane.moveTo(Math.min(sourcePane.getAnchor(), sourcePane.getCaretPosition()));
        lastSearchString = searchFor;
        String content = sourcePane.getText();

        int curPosition = 0;
        boolean finished = false;

        List<Integer> foundStarts = new ArrayList<>();

        while (!finished)
        {
            int foundPos = findSubstring(content, searchFor, ignoreCase, false, curPosition);
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
                for (Integer foundPos : foundStarts)
                {
                    sourceDocument.markFindResult(foundPos, foundPos + searchFor.length());
                }
            }

            @Override
            public FindNavigator replaceCurrent(String replacement)
            {
                if (!sourcePane.getSelectedText().equals(searchFor))
                {
                    selectNext(true);
                }
                int pos = sourcePane.getSelection().getStart();
                sourceDocument.replace(pos, searchFor.length(), replacement);
                sourcePane.setCaretPosition(pos + searchFor.length());
                // For some reason, after a replacement, the request to follow caret doesn't
                // work.  I think it's because the document content has changed.  A simple
                // runAfterCurrent doesn't work either.  So although it's hacky, we use a delayed
                // action.  We can queue it up now:
                JavaFXUtil.runAfter(Duration.millis(200), () -> sourcePane.requestFollowCaret());
                return doFind(searchFor, ignoreCase);
            }

            public void replaceAll(String replacement)
            {
                // Sort all the found positions in descending order, so we can replace them
                // in order without affecting the later positions in the list (earlier in file):
                foundStarts.stream().sorted(Comparator.reverseOrder()).forEach(pos ->
                    sourceDocument.replace(pos, searchFor.length(), replacement)
                );
            }

            @Override
            public void selectNext(boolean canBeAtCurrentPos)
            {
                if (validProperty().get())
                {
                    int selStart = sourcePane.getSelection().getStart();
                    int position = foundStarts.stream()
                            .filter(pos -> pos > selStart || (canBeAtCurrentPos && pos == selStart))
                            .findFirst()
                            .orElse(foundStarts.get(0));
                    select(position);
                }
            }

            private void select(int position)
            {
                sourcePane.select(position, position + searchFor.length());
                sourcePane.requestFollowCaret();
            }

            @Override
            public void selectPrev()
            {
                if (validProperty().get())
                {
                    int selStart = sourcePane.getSelection().getStart();
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
     * Transfers caret to user specified line number location.
     */
    public void goToLine()
    {
        final int numberOfLines = numberOfLines();
        GoToLineDialog goToLineDialog = new GoToLineDialog(fxTabbedEditor.getWindow());
        goToLineDialog.setRangeMax(numberOfLines);
        Optional<Integer> o = goToLineDialog.showAndWait();
        o.ifPresent(n -> {
            setSelection(n , 1, 0);
            ensureCaretVisible();
        });
    }

    private void ensureCaretVisible()
    {
        sourcePane.requestFollowCaret();
    }

    // --------------------------------------------------------------------

    /**
     * Implementation of "toggle-interface-view" user function. The menu has
     * already been changed - now see what it is and do it.
     */
    public void toggleInterface()
    {
        if (isShowingInterface())
            switchToSourceView();
        else
            switchToInterfaceView();
    }

    /**
     * Switch on the source view (if it isn't showing already).
     */
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
        //NAVIFX
        //dividerPanel.endTemporaryHide();
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
            boolean generateDoc = ! docUpToDate();

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

    /**
     * Refresh the HTML display.
     */
    private void refreshHtmlDisplay()
    {
        try {
            File urlFile = new File(getDocPath());

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
            info.message (Config.getString("editor.info.docDisappeared"), getDocPath());
            Debug.reportError("loading class interface failed: " + exc);
        }
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
                            org.w3c.dom.Element newLink = doc.createElement("a");
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

    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------

    /**
     * Implementation of "toggle-breakpoint" user function.
     */
    public void toggleBreakpoint()
    {
        if (!viewingCode()) {
            info.message (" ");
            return;
        }
        toggleBreakpoint(sourcePane.getCaretPosition());
    }

    // --------------------------------------------------------------------

    /**
     * Toggle a breakpoint at a given position.
     */
    public void toggleBreakpoint(int pos)
    {
        if (positionHasBreakpoint(pos)) {
            setUnsetBreakpoint(pos, false);        // remove
        }
        else {
            setUnsetBreakpoint(pos, true);         // set
        }
    }

    // --------------------------------------------------------------------

    /**
     * Clear all known breakpoints.
     */
    private void clearAllBreakpoints()
    {
        if (mayHaveBreakpoints) {

            for (int i = 1; i <= numberOfLines(); i++) {
                if (lineHasBreakpoint(i)) {
                    doRemoveBreakpoint(i);
                }
            }
            mayHaveBreakpoints = false;
        }
    }

    // --------------------------------------------------------------------

    /**
     * Check whether a position in the current document has a breakpoint set
     * (should only be called after a check that the current document is the source doc)
     */
    private boolean positionHasBreakpoint(int pos)
    {
        return lineHasBreakpoint(getLineNumberAt(pos));
    }

    // --------------------------------------------------------------------

    /**
     * Check whether a line in the source document has a breakpoint set
     */
    private boolean lineHasBreakpoint(int lineNo)
    {
        return sourceDocument.getParagraphAttributes(lineNo).contains(ParagraphAttribute.BREAKPOINT);
    }

    // --------------------------------------------------------------------

    /**
     * Try to set or remove a breakpoint (depending on the parameter) at the
     * given position in the source document. Informs the watcher.
     */
    private void setUnsetBreakpoint(int pos, boolean set)
    {
        if (watcher != null) {
            int line = getLineNumberAt(pos);
            String result = watcher.breakpointToggleEvent(line, set);

            if (result == null) {
                // no problem, go ahead
                if (set) {
                    mayHaveBreakpoints = true;
                }

                sourceDocument.setParagraphAttributesForLineNumber(line, Collections.singletonMap(ParagraphAttribute.BREAKPOINT, set));
            }
            else {
                info.message (result);
            }

            // force an update of UI
            repaint();
        }
        else {
            info.message (Config.getString("editor.info.cannotSetBreak"));
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Remove a breakpoint without question.
     */
    private void doRemoveBreakpoint(int lineNumber)
    {
        sourceDocument.setParagraphAttributesForLineNumber(lineNumber, Collections.singletonMap(ParagraphAttribute.BREAKPOINT, false));
        repaint();
    }

    // --------------------------------------------------------------------

    /**
     * Try to set or remove a step mark (depending on the parameter) at the
     * given position.
     * 
     * @param pos  A position in the line where we'd like the step mark.
     */
    private void setStepMark(int lineNumber)
    {
        removeStepMark();
        sourceDocument.setParagraphAttributesForLineNumber(lineNumber, Collections.singletonMap(ParagraphAttribute.STEP_MARK, true));
        currentStepLineNumber = lineNumber;
        // force an update of UI
        repaint();
    }

    private void repaint()
    {
        // N/A in JavaFX
    }

    /**
     * Return a boolean representing whether in source editing view
     */
    private boolean viewingCode()
    {
        return sourceIsCode && (!viewingHTML.get());
    }
    
    /**
     * Find and return a line (by line number) in the source document
     */
    private Element getSourceLine(int lineNo)
    {
        Element map = sourceDocument.getDefaultRootElement();
        if (map.getElementCount() >= lineNo) {
            return sourceDocument.getDefaultRootElement().getElement(lineNo - 1);
        }
        else {
            return null;
        }
    }

    // --------------------------------------------------------------------

    /**
     * Return the number of the line containing position 'pos' in the source document.
     */
    private int getLineNumberAt(int pos)
    {
        return sourceDocument.getDefaultRootElement().getElementIndex(pos) + 1;
    }

    /**
     * Revert the buffer contents to the last saved version. Do not ask any
     * question - just do it. Must have a file name.
     */
    public void doReload()
    {
        removeSearchHighlights();
        Reader reader = null;
        
        try {
            FileInputStream inputStream = new FileInputStream(filename);
            reader = new InputStreamReader(inputStream, characterSet);
            sourcePane.read(reader);
            try {
                reader.close();
                inputStream.close();
            }
            catch (IOException ioe) {}
            File file = new File(filename);
            setLastModified(file.lastModified());

            sourceDocument.enableParser(false);
            //NAVIFX
            //naviView.setDocument(sourceDocument);

            // flag document type as a java file by associating a
            // JavaTokenMarker for syntax colouring if specified
            listenToChanges(sourceDocument);

            // We want to inform the watcher that the editor content has changed,
            // and then inform it that we are in "saved" state (synced with file).
            // But first set state to saved to avoid unnecessary writes to disk.
            saveState.setState(StatusLabel.Status.SAVED);
            setChanged(); // contents may have changed - notify watcher
            setSaved();  // notify watcher that we are saved
            
            scheduleReparseRunner();
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
     * Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
        // Don't need to say saved twice:
        //info.message(Config.getString("editor.info.saved"));
        saveState.setState(StatusLabel.Status.SAVED);
        if (watcher != null) {
            watcher.saveEvent(this);
        }
    }

    /**
     * Buffer just went from saved to changed state (called by StatusLabel)
     */
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

    /**
     * Notification (from the caret) that the caret position has moved.
     */
    public void caretMoved()
    {
        int caretPos = sourcePane.getCaretPosition();
        showErrorPopupForCaretPos(caretPos, false);
        
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
            if (errorDisplay != null && (!mousePosition || !errorDisplay.details.containsPosition(sourcePane.getCaretPosition())))
            {
                showErrorOverlay(null, caretPos);
            }
        }
    }

    // --------------------------------------------------------------------

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
            sourcePane.setCaretPosition(offset);
            sourcePane.requestFollowCaret();
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

    // --------------------------------------------------------------------

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
                    pos = sourcePane.getCharacterBoundsOnScreen(displayPosition, displayPosition + 1).orElse(null);

                    // That may be null if caret was at end of line, in which case try character before:
                    if (pos == null)
                    {
                        pos = sourcePane.getCharacterBoundsOnScreen(displayPosition - 1, displayPosition).orElse(null);
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

    /**
     * Returns the position of the matching bracket for the source pane's
     * current caret position. Returns -1 if not found or not valid/appropriate
     * 
     * @return the int representing bracket position
     */
    public int getBracketMatch()
    {
        int pos = -1;
        int caretPos = sourcePane.getCaretPosition();
        if (caretPos != 0) {
            caretPos--;
        }
        pos = TextUtilities.findMatchingBracket(sourceDocument, caretPos);
        return pos;
    }

    // --------------------------------------------------------------------

    /**
     * delegates bracket matching to the source pane's caret
     */
    private void doBracketMatch()
    {
        int originalPos = getSourcePane().getCaretPosition();
        int matchBracket = getBracketMatch();

        // This is a kludge.  Changing the style causes the node to be swapped out, which causes issues with mouse dragging
        // because the node is swapped as the drag begins.  So we wrap this in a run later
        // so that the drag can begin before the node is swapped.  It's ugly, but it seems
        // to work:
        JavaFXUtil.runPlatformLater(() ->
        {
            // remove existing bracket if needed
            removeBracketHighlight();
            // Only highlight if we found a match, and the cursor hasn't moved since
            // we started the run later:
            if (matchBracket != -1 && originalPos > 0 && originalPos == getSourcePane().getCaretPosition())
            {
                sourceDocument.addStyle(originalPos - 1, originalPos, MoeSyntaxDocument.MOE_BRACKET_HIGHLIGHT);
                sourceDocument.addStyle(matchBracket, matchBracket + 1, MoeSyntaxDocument.MOE_BRACKET_HIGHLIGHT);
            }
        });
    }

    private void removeBracketHighlight()
    {
        sourceDocument.removeStyleThroughout(MoeSyntaxDocument.MOE_BRACKET_HIGHLIGHT);
    }

    // --------------------------------------------------------------------
    
    /**
     * Set the window title to show the defined title, or else the file name.
     */
    private void setWindowTitle()
    {
        String title = windowTitle;

        if (title == null) {
            if (filename == null) {
                title = "Moe:  <no name>";
            }
            else {
                title = "Moe:  " + filename;
            }
        }
        fxTab.setWindowTitle(title);
    }
    
    // --------------------------------------------------------------------

    /**
     * Return the path to the class documentation.
     */
    private String getDocPath()
    {
        return docFilename;
    }

    /**
     * Gets the resource attribute of the MoeEditor object
     */
    private String getResource(String name)
    {
        return Config.getPropString(name, null, resources);
    }

    /**
     * Create all the Window components.
     * 
     * @param projectResolver  the entity resolver for the project. If this is null
     *   then it is assumed that this editor is for a README or other plain text file.
     */
    private void initWindow(EntityResolver projectResolver)
    {
        // prepare the content pane (us)

        // create and add info and status areas

        BorderPane bottomArea = new BorderPane();
        JavaFXUtil.addStyleClass(bottomArea, "moe-bottom-bar");

        // create panel for info/status

        //area for new find functionality
        finder=new FindPanel(this);
        finder.setVisible(false);

        saveState = new StatusLabel(StatusLabel.Status.SAVED, this, errorManager);

        info = new Info();
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

        // create the text document

        if (projectResolver != null) {
            sourceDocument = new MoeSyntaxDocument(projectResolver, this);
        }
        else {
            sourceDocument = new MoeSyntaxDocument((ScopeColors) null);  // README file
        }
        listenToChanges(sourceDocument);

        // create the text pane
        sourcePane = sourceDocument.makeEditorPane(this, compiledProperty);
        sourcePane.setCaretPosition(0);
        undoManager = new MoeUndoManager(sourcePane);
        sourcePane.setUndoManager(undoManager.getUndoManager());
        JavaFXUtil.addChangeListenerPlatform(sourcePane.caretPositionProperty(), e -> caretMoved());
        JavaFXUtil.addChangeListenerPlatform(sourcePane.estimatedScrollYProperty(), d -> {
            // The caret won't have actually moved within the document,
            // but its visibility on screen may well have changed, so we
            // make sure to redraw things (e.g. error popup) which may have
            // been affected:

            // It's important to use runAfterCurrent, because this listener
            // can get called during the layout pass, and altering the styles
            // during this pass can cause an exception, so we must do it later:
            JavaFXUtil.runAfterCurrent(() -> {
                showErrorPopupForCaretPos(sourcePane.getCaretPosition(), false);
            });
        });
        sourcePane.setMouseOverTextDelay(java.time.Duration.ofMillis(400));
        sourcePane.addEventHandler(MouseOverTextEvent.ANY, this::mouseOverText);
        Nodes.addInputMap(sourcePane, org.fxmisc.wellbehaved.event.InputMap.consume(EventPattern.keyPressed(KeyCode.ESCAPE), e -> {
            if (finder != null && finder.isVisible())
            {
                finder.close();
            }
        }));

        // default showing:
        //currentTextPane = sourcePane;

        HBox editorPane = new HBox();

        //NAVIFX: sourceDocument, scroll bar
        //naviView = new NaviView(null /*sourceDocument*/, errorManager, new JScrollPane().getVerticalScrollBar());
        //naviView.setPreferredSize(new Dimension(NAVIVIEW_WIDTH, 0));
        //naviView.setMaximumSize(new Dimension(NAVIVIEW_WIDTH, Integer.MAX_VALUE));
        //naviView.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        //dividerPanel=new EditorDividerPanel(naviView, getNaviviewExpandedProperty());

        //NAVIFX
        //editorPane.getChildren().add(scrollPane);
        //editorPane.add(dividerPanel);
        //editorPane.add(naviView);

        htmlPane = new WebView();
        htmlPane.visibleProperty().bind(viewingHTML);

        editorPane.setFillHeight(true);
        //HBox.setHgrow(scrollPane, Priority.ALWAYS);
        //setCenter(editorPane);
        BorderPane background = new BorderPane();
        JavaFXUtil.addStyleClass(background, "moe-background");
        setCenter(new StackPane(background, new VirtualizedScrollPane<>(sourcePane), htmlPane));

        // get table of edit actions

        actions = MoeActions.getActions(this);

        // create menubar and menus

        menubar = createMenuBar();
        // Must update keymap after making menu to remove shortcuts which are now handled by menu:
        actions.updateKeymap();
        fxMenus.clear();
        menubar.getMenus().forEach(fxMenus::add);

        // create toolbar
        ComboBox<String> interfaceSelector = createInterfaceSelector();
        interfaceSelector.setDisable(!sourceIsCode);
        Region toolbar = createToolbar(interfaceSelector.heightProperty());
        BorderPane topBar = new BorderPane(null, null, interfaceSelector, null, toolbar);
        topBar.getStyleClass().add("moe-top-bar");
        setTop(topBar);
        
        //add popup menu
        sourcePane.setContextMenu(createPopupMenu());
    }

    // --------------------------------------------------------------------

    /**
     * Create the editor's menu bar.
     */
    private MenuBar createMenuBar()
    {
        return new MenuBar(
            createMenu("class", "save - print - close"),
            createMenu("edit", "undo redo - cut-to-clipboard copy-to-clipboard paste-from-clipboard - indent-block deindent-block comment-block uncomment-block autoindent - insert-method add-javadoc"),
            createMenu("tools", "find find-next find-next-backward replace go-to-line - compile toggle-breakpoint - toggle-interface-view"),
            createMenu("option", "increase-font decrease-font reset-font - key-bindings preferences")
        );
    }

    // --------------------------------------------------------------------

    /**
     * Create the pop up menu bar
     */
    private ContextMenu createPopupMenu()
    {
        ContextMenu popup = new ContextMenu();
        String [] popupKeys="cut copy paste".split(" ");
        for (String popupKey : popupKeys) {
            String label = Config.getString("editor." + popupKey + LabelSuffix);
            String actionName = getResource(popupKey + ActionSuffix);
            MoeAbstractAction action = actions.getActionByName(actionName);
            if (action == null)
            {
                Debug.message("Moe: cannot find action " + popupKey);
            }
            else
            {
                MenuItem menuItem = action.makeContextMenuItem();
                menuItem.setText(label);
                popup.getItems().add(menuItem);
            }
        }      
        return popup;

    }



    // --------------------------------------------------------------------

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
                MoeAbstractAction action = actions.getActionByName(itemKey);
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

    // --------------------------------------------------------------------

    /**
     * Create the toolbar.
     *
     * @return The toolbar component, ready made.
     */
    private Region createToolbar(DoubleExpression buttonHeight)
    {
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL,
                createToolbarButton("compile", buttonHeight),
                createToolbarButton("undo", buttonHeight),
                createToolbarButton("cut", buttonHeight),
                createToolbarButton("copy", buttonHeight),
                createToolbarButton("paste", buttonHeight),
                createToolbarButton("find", buttonHeight),
                createToolbarButton("close", buttonHeight)
        );
        tilePane.setPrefColumns(tilePane.getChildren().size());
        return JavaFXUtil.withStyleClass(tilePane, "moe-top-bar-buttons");
    }

    // --------------------------------------------------------------------

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
        MoeAbstractAction action = actions.getActionByName(actionName);

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
     * Create a combo box for the toolbar
     */
    private ComboBox<String> createInterfaceSelector()
    {
        String[] choiceStrings = {implementationString, interfaceString};
        interfaceToggle = new ComboBox<String>(FXCollections.observableArrayList(choiceStrings));

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
     * Sets the find panel to be visible and if there is a selection/or previous search 
     * it starts a automatic find of what was selected in the text/or previous search. If 
     * it is the source pane then the replace button is enabled; if it is the interface pane 
     * then the replace button and replace panel are set to disabled and invisible
     */
    public void initFindPanel()
    {
        finder.displayFindPanel(sourcePane.getSelectedText());
    }

    /**
     * Sets the caret forward by the value indicated if this does not 
     * exceed the document length; Else it sets it to the document length
     */
    public void setCaretPositionForward (int caretPos)
    {
        int docLength = sourcePane.getLength();
        if (sourcePane.getCaretPosition() + caretPos <= docLength) {
            sourcePane.setCaretPosition(sourcePane.getCaretPosition() + caretPos);
        } else { 
            sourcePane.setCaretPosition(docLength);
        }
    }

    /**
     * Get the source pane.
     */
    public MoeEditorPane getSourcePane()
    {
        return sourcePane;
    }

    public WebView getHTMLPane()
    {
        return htmlPane;
    }
    
    /**
     * Get the current pane.
     */
    public MoeEditorPane getCurrentTextPane()
    {
        return sourcePane;
    }    
    
    /**
     * Get the source document that this editor is editing.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public MoeSyntaxDocument getSourceDocument()
    {
        return sourceDocument;
    }
    
    /**
     * Removes the selected highlights (in both the source/doc pane) 
     * Note: the other highlights such as the brackets etc remain
     */
    public void removeSearchHighlights()
    {
        sourceDocument.removeSearchHighlights();
    }

    /**
     * Create and pop up the content assist (code completion) dialog.
     */
    protected void createContentAssist()
    {
        //need to recreate the dialog each time it is pressed as the values may be different
        ParsedCUNode parser = sourceDocument.getParser();
        ExpressionTypeInfo suggests = parser == null ? null : parser.getExpressionType(sourcePane.getCaretPosition(),
                sourceDocument);
        if (suggests != null)
        {
            LocatableToken suggestToken = suggests.getSuggestionToken();
            /*
            PopulateCompletionsWorker worker = new PopulateCompletionsWorker(suggests, suggestToken, xpos, ypos);
            worker.execute();
            */
            AssistContent[] possibleCompletions = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null);
            Arrays.sort(possibleCompletions, AssistContent.getComparator());
            List<SuggestionDetails> suggestionDetails = Arrays.stream(possibleCompletions)
                .map(AssistContentThreadSafe::new)
                .map(ac -> new SuggestionDetailsWithHTMLDoc(ac.getName(), ExpressionCompletionCalculator.getParamsCompletionDisplay(ac), ac.getType(), SuggestionShown.COMMON, ac.getDocHTML()))
                .collect(Collectors.toList());

            int originalPosition = suggestToken == null ? sourcePane.getCaretPosition() : suggestToken.getPosition();
            Bounds screenPos;
            if (suggestToken == null)
            {
                screenPos = sourcePane.getCaretBounds().orElse(null);
            }
            else
            {
                // First, try to get the character after the caret:
                screenPos = sourcePane.getCharacterBoundsOnScreen(originalPosition, originalPosition + 1).orElse(null);

                // That may be null if caret was at end of line, in which case try character before:
                if (screenPos == null)
                {
                    screenPos = sourcePane.getCharacterBoundsOnScreen(originalPosition - 1, originalPosition).orElse(null);;
                    // Adjust to move to RHS of the rectangle:
                    screenPos = new BoundingBox(screenPos.getMaxX(), screenPos.getMinY(), 0, screenPos.getHeight());
                }
            }
            if (screenPos == null)
                return;
            Bounds spLoc = sourcePane.screenToLocal(screenPos);

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
                    sourcePane.setFakeCaret(true);
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
                        codeComplete(possibleCompletions[highlighted], originalPosition, sourcePane.getCaretPosition(), suggestionList);
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
                        sourcePane.insertText(sourcePane.getCaretPosition(), event.getCharacter());
                    }

                    String prefix = sourcePane.getText(originalPosition, sourcePane.getCaretPosition());
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
                            sourcePane.deletePreviousChar();
                            break;
                        case DELETE:
                            sourcePane.deleteNextChar();
                            break;
                    }
                    // If they delete to before the original position then
                    // not only does it make sense to dismiss, but in fact
                    // we must dismiss or we will encounter an exception:
                    if (sourcePane.getCaretPosition() < originalPosition)
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
                    sourcePane.setFakeCaret(false);
                }
            });
            String prefix = sourcePane.getText(originalPosition, sourcePane.getCaretPosition());
            suggestionList.calculateEligible(prefix, true, false);
            suggestionList.updateVisual(prefix);
            suggestionList.highlightFirstEligible();
            suggestionList.show(sourcePane, spLoc);
            Position pos = sourcePane.offsetToPosition(originalPosition, Bias.Forward);
            watcher.recordCodeCompletionStarted(pos.getMajor() + 1, pos.getMinor() + 1, null, null, prefix, suggestionList.getRecordingId());

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
        sourcePane.select(prefixBegin, prefixEnd);
        String prefix = sourcePane.getSelectedText();
        insertText(start, false);
        String inserted = start;

        if (params != null)
        {
            // Record position before we add first parameter,
            // so that we can come back and select it:
            int selLoc = sourcePane.getCaretPosition();
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
                sourcePane.select(selLoc, selLoc + params.get(0).getDummyName().length());
        }
        Position prefixBeginPos = sourcePane.offsetToPosition(prefixBegin, Bias.Forward);
        watcher.recordCodeCompletionEnded(prefixBeginPos.getMajor() + 1, prefixBeginPos.getMinor() + 1, null, null, prefix, inserted, suggestionList.getRecordingId());
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
     * Sets the find panel to be visible
     */
    public void setFindPanelVisible()
    {
        finder.setVisible(true);
    };

    public void mouseOverText(MouseOverTextEvent e)
    {
        final int caretPos = e.getCharacterIndex();
        // If the mouse has moved position, restart error show timer:
        if (caretPos != mouseCaretPos)
        {
            showErrorPopupForCaretPos(caretPos, true);
        }
    }

    /**
     * Populates the find field and requests focus
     */
    public void setFindTextfield(String text)
    {
        finder.populateFindTextfield(text);
    }

    /**
     * Determines whether the Naviview should initially be expanded or not.
     */
    protected boolean getNaviviewExpandedProperty()
    {
        if (watcher != null && watcher.getProperty(EditorWatcher.NAVIVIEW_EXPANDED_PROPERTY)!=null)
        {
            return Boolean.parseBoolean(watcher.getProperty(EditorWatcher.NAVIVIEW_EXPANDED_PROPERTY));
        }
        return PrefMgr.getNaviviewExpanded();
    }

    /**
     * Returns whether the editor text represents source code, or something else
     * (such as the README.txt file).
     * 
     * @return true if source code; 
     *         false if not
     */
    protected boolean containsSourceCode()
    {
        return sourceIsCode;
    }

    /**
     * Notify the editor watcher of an edit (or save).
     * @param includeOneLineEdits - will be true if it is considered unlikely that further edits will
     *                     be localised to previous edit locations (line), or if the file has been saved.
     */
    private void recordEdit(boolean includeOneLineEdits)
    {
        if (watcher != null)
        {
            watcher.recordJavaEdit(sourceDocument.getText(0, sourceDocument.getLength()), includeOneLineEdits);
        }
    }

    public bluej.editor.TextEditor assumeText()
    {
        return this;
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
                    MoeIndent.calculateIndentsAndApply(sourceDocument, node.getPosition(),
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
        NodeAndPosition<ParsedNode> root = new NodeAndPosition<>(sourceDocument.getParser(), 0, 
                sourceDocument.getParser().getSize());
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

            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION && sourceDocument.getText(
                    nap.getPosition(), nap.getSize()).startsWith(methodName)) {
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
    @OnThread(Tag.FX)
    public FrameEditor assumeFrame()
    {
        return null;
    }

    @Override
    public boolean compileStarted(int compilationSequence)
    {
        compilationStarted = true;
        errorManager.removeAllErrorHighlights();
        return false;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean isOpen()
    {
        return fxTabbedEditor != null && fxTabbedEditor.isWindowVisible();
    }
    
    public String getTitle()
    {
        return windowTitle;
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
                ErrorDetails err = errorManager.getNextErrorPos(sourcePane.getCaretPosition());
                if (err != null)
                {
                    sourcePane.setCaretPosition(err.startPos);
                    ensureCaretVisible();

                    if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT))
                    {
                        // Pop up in a dialog:
                        DialogManager.showTextWithCopyButtonFX(getWindow(), err.message, "BlueJ");
                    }
                }
            }
        }
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
     * Sets the parent SwingTabbedEditor reference.
     *
     * @param partOfMove True if this is part of a move to another window (and thus we shouldn't record
     *                   open or close)
     */
    @OnThread(Tag.FXPlatform)
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

    // package visible
    void updateHeaderHasErrors(boolean hasErrors)
    {
        fxTab.setErrorStatus(hasErrors);
    }

    @OnThread(Tag.FX)
    public List<Menu> getFXMenu()
    {
        return fxMenus;
    }

    @OnThread(Tag.FXPlatform)
    public EditorWatcher getWatcher()
    {
        return watcher;
    }

    @OnThread(Tag.FXPlatform)
    public void requestEditorFocus()
    {
        sourcePane.requestFocus();
    }

    @Override
    public void setExtendsClass(String className, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                if (info.getSuperclass() == null) {
                    Selection s1 = info.getExtendsInsertSelection();

                    setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                    insertText(" extends " + className, false);
                }
                else {
                    Selection s1 = info.getSuperReplaceSelection();

                    setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
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
                    setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
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
                setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());

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

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo info)
    {
        try {
            save();

            if (info != null) {
                Selection s1 = info.getExtendsInsertSelection();
                setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());

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
                    setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
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
    public void removeImports(List<String> importTargets)
    {
        List<ImportsCollection.LocatableImport> toRemove = new ArrayList<>();
        for (String importTarget : importTargets)
        {
            ImportsCollection.LocatableImport details = getParsedNode().getImports().getImportInfo(importTarget);
            
            if (details != null)
            {
                toRemove.add(details);
            }
        }
        
        // Sort in reverse order of position, so that we can go down the list
        // and remove in turn without a removal affecting a later removal.
        // Hence we sort by negative start value:
        Collections.sort(toRemove, Comparator.<LocatableImport>comparingInt(t -> -t.getStart()));

        for (ImportsCollection.LocatableImport locatableImport : toRemove)
        {
            if (locatableImport.getStart() != -1)
            {
                getSourcePane().replaceText(locatableImport.getStart(), locatableImport.getStart() + locatableImport.getLength(), "");
            }
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
            String text = getText(new bluej.parser.SourceLocation(sel.getLine(), sel.getColumn()),
                new bluej.parser.SourceLocation(sel.getEndLine(), sel.getEndColumn()));

            // check for type arguments: don't include them in the text
            int taIndex = text.indexOf('<');
            if (taIndex != -1)
                text = text.substring(0, taIndex);
            text = text.trim();

            r.add(text);
        }
        return r;
    }

    /**
     * Set the header image (in the tab header) for this editor
     * @param image The image to use (any size).
     */
    @Override
    public void setHeaderImage(Image image)
    {
        fxTab.setHeaderImage(image);
    }

    @OnThread(Tag.FXPlatform)
    public javafx.stage.Window getWindow()
    {
        return fxTabbedEditor.getWindow();
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
