/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.compiler.Diagnostic;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.editor.EditorWatcher;
import bluej.parser.AssistContent;
import bluej.parser.CodeSuggestions;
import bluej.parser.ParseUtils;
import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedCUNode;
import bluej.pkgmgr.JavadocResolver;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.GradientFillPanel;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingWorker;

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

public final class MoeEditor extends JFrame
    implements bluej.editor.Editor, BlueJEventListener, HyperlinkListener, DocumentListener, MouseListener
{
    // -------- CONSTANTS --------

    // version number
    final static int version = 300;
    final static String versionString = "3.0.0";

    // colours
    final static Color cursorColor = new Color(255, 0, 100);                 // cursor

    final static Color infoColor = new Color(240, 240, 240);
    final static Color lightGrey = new Color(224, 224, 224);
    final static Color selectionColour = Config.getSelectionColour();
    final static Color envOpColour = Config.ENV_COLOUR;

    // Fonts
    public static int printFontSize = Config.getPropInteger("bluej.fontsize.printText", 10);
    public static Font printFont = new Font("Monospaced", Font.PLAIN, printFontSize);

    // Strings
    private final String implementationString = Config.getString("editor.implementationLabel");
    private final String interfaceString = Config.getString("editor.interfaceLabel");

    // suffixes for resources
    final static String LabelSuffix = "Label";
    final static String ActionSuffix = "Action";
    final static String TooltipSuffix = "Tooltip";
    final static String AcceleratorSuffix = "Accelerator";

    // file suffixes
    private final static String CRASHFILE_SUFFIX = "#";
    private final static String BACKUP_SUFFIX = "~";

    // other
    final static String COMPILED = "compiled";
    private final static int NAVIVIEW_WIDTH = 90;       // width of the "naviview" (min-source) box

    // -------- CLASS VARIABLES --------

    private static boolean matchBrackets = false;
    
    private static final Color highlightBorderColor = new Color(212, 172,45);
    
    protected static AdvancedHighlightPainter searchHighlightPainter =
        new MoeBorderHighlighterPainter(highlightBorderColor, Config.getHighlightColour(),
                Config.getHighlightColour2(), Config.getSelectionColour2(),
                Config.getSelectionColour());

    // -------- INSTANCE VARIABLES --------

    private EditorWatcher watcher;
    private Properties resources;

    private AbstractDocument document;
    private MoeSyntaxDocument sourceDocument;
    private HTMLDocument htmlDocument;

    private MoeActions actions;
    public MoeUndoManager undoManager;

    private JEditorPane currentTextPane;    // text component currently displayed
    private JEditorPane sourcePane;         // the component holding the source text

    private JEditorPane htmlPane;           // the component holding the javadoc html
    private MoeCaret moeCaret;

    private Info info;                      // the info number label
    private JPanel statusArea;              // the status area
    private StatusLabel saveState;          // the status label
    private JComboBox interfaceToggle;
    private GoToLineDialog goToLineDialog;

    // find functionality
    private FindPanel finder;
    private ReplacePanel replacer;

    private JScrollPane scrollPane;
    private NaviView naviView;              // Navigation view (mini-source view)
    private EditorDividerPanel dividerPanel;  // Divider Panel to indicate separation between the
                                            // editor and navigation view
    private JComponent toolbar;             // The toolbar
    private JPopupMenu popup;               // Popup menu options

    private String filename;                // name of file or null
    private long lastModified;              // time of last modification of file
    private String windowTitle;             // title of editor window
    private String docFilename;             // path to javadoc html file
    private Charset characterSet;           // character set of the file

    private boolean sourceIsCode;           // true if current buffer is code
    private boolean viewingHTML;

    private int currentStepPos;             // position of step mark (or -1)
    private boolean mayHaveBreakpoints;     // true if there were BP here
    private boolean ignoreChanges = false;
    private boolean tabsAreExpanded = false;

    private MoePrinter printer;
    private PrintDialog printDialog;

    private TextInsertNotifier doTextInsert = new TextInsertNotifier();

    /**
     * list of actions that are dis/enabled depending on the selected view
     * (source/documentation)
     */
    private static ArrayList<String> editActions;
    /**
     * list of actions that are disabled in the readme text file
     */
    private static ArrayList<String> readMeActions;

    /** Used to obtain javadoc for arbitrary methods */
    private JavadocResolver javadocResolver;
    private ReparseRunner reparseRunner;

    /** Search highlight tags for both text panes */
    private List<Object> sourceSearchHighlightTags = new ArrayList<Object>();
    private List<Object> htmlSearchHighlightTags = new ArrayList<Object>();
    
    /** Manages display of compiler and parse errors */
    private MoeErrorManager errorManager = new MoeErrorManager(this);
    
    /**
     * Property map, allows BlueJ extensions to associate property values with
     * this editor instance; otherwise unused.
     */
    private HashMap<String,Object> propertyMap = new HashMap<String,Object>();
    
    // Blackbox data recording:
    private int oldCaretLineNumber = -1;


    /**
     * Constructor. Title may be null.
     */
    public MoeEditor(MoeEditorParameters parameters)
    {
        super("Moe");
        watcher = parameters.getWatcher();
        resources = parameters.getResources();
        javadocResolver = parameters.getJavadocResolver();

        filename = null;
        windowTitle = parameters.getTitle();
        sourceIsCode = parameters.isCode();
        viewingHTML = false;
        currentStepPos = -1;
        mayHaveBreakpoints = false;
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);
        undoManager = new MoeUndoManager(this);

        initWindow(parameters.getProjectResolver());
    }

    // --------------------------------------------------------------------

    /*
     * Load the file "filename" and show the editor window.
     */
    @Override
    public boolean showFile(String filename, Charset charset, boolean compiled,
            String docFilename, Rectangle bounds)
    {
        this.filename = filename;
        this.docFilename = docFilename;
        this.characterSet = charset;

        if (bounds != null) {
            if (bounds.x > (Config.screenBounds.width - 80))
                bounds.x = Config.screenBounds.width - 80;
            
            if (bounds.y > (Config.screenBounds.height - 80))
                bounds.y = Config.screenBounds.height - 80;
            
            if (bounds.width > 0 && bounds.height > 0) {
                setBounds(bounds);
            }
            else {
                setLocation(bounds.x, bounds.y);
            }
        }

        boolean loaded = false;

        if (filename != null) {
            try {
                // check for crash file
                String crashFilename = filename + CRASHFILE_SUFFIX;
                String backupFilename = crashFilename + "backup";
                File crashFile = new File(crashFilename);
                if (crashFile.exists()) {
                    File backupFile = new File(backupFilename);
                    backupFile.delete();
                    crashFile.renameTo(backupFile);
                    DialogManager.showMessage(this, "editor-crashed");
                }

                // FileReader reader = new FileReader(filename);
                FileInputStream inputStream = new FileInputStream(filename);
                Reader reader = new InputStreamReader(inputStream, charset);
                sourcePane.read(reader, null);
                try {
                    reader.close();
                    inputStream.close();
                }
                catch (IOException ioe) {}
                File file = new File(filename);
                lastModified = file.lastModified();
                
                sourcePane.addMouseListener(this);
                sourceDocument = (MoeSyntaxDocument) sourcePane.getDocument();
                naviView.setDocument(sourceDocument);
                sourceDocument.addDocumentListener(this);
                sourceDocument.addUndoableEditListener(undoManager);
                document = sourceDocument;
                
                sourceDocument.enableParser(false);
                loaded = true;
                
                scheduleReparseRunner();
            }
            catch (FileNotFoundException ex) {
                clear();
            }
            catch (IOException ex) {
                Debug.reportError("Couldn't open file", ex);
            }
        }
        else {
            if (docFilename != null) {
                if (new File(docFilename).exists()) {
                    showInterface(true);
                    loaded = true;
                    interfaceToggle.setEnabled(false);
                }
            }
        }

        if (!loaded) {
            // should exist, but didn't
            return false;
        }

        info.message(Config.getString("editor.info.version") + " " + versionString);

        setWindowTitle();
        sourcePane.setFont(PrefMgr.getStandardEditorFont());

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
     */
    @Override
    public void setVisible(boolean vis)
    {
        if (vis) {
            sourcePane.setFont(PrefMgr.getStandardEditorFont());
            checkBracketStatus();
        }

        super.setVisible(vis);              // show the window

        if(vis) {
            setState(Frame.NORMAL);         // de-iconify
            toFront();                      // window to front  
            Utility.bringToFront(this);        
        }
    }

    /**
     * Refresh the editor window.
     */
    @Override
    public void refresh()
    {
        sourcePane.setFont(PrefMgr.getStandardEditorFont());
        checkBracketStatus();
        currentTextPane.repaint();
        
        Info.resetFont();
        info.refresh();
        StatusLabel.resetFont();
        saveState.refresh();
    }

    /*
     * Save the buffer to disk under current filename, if there any changes.
     * This method may be called often.
     */
    @Override
    public void save()
        throws IOException
    {
        IOException failureException = null;
        if (saveState.isChanged()) {
            // Record any edits with the data collection system:
            recordEdit(true);
            
            Writer writer = null;
            try {
                // The crash file is used during writing and will remain in
                // case of a crash during the write operation. The backup
                // file always contains the last version.
                String crashFilename = filename + CRASHFILE_SUFFIX;
                String backupFilename = filename + BACKUP_SUFFIX;

                // make a backup to the crash file
                FileUtility.copyFile(filename, crashFilename);

                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(filename));
                writer = new OutputStreamWriter(ostream, characterSet);
                sourcePane.write(writer);
                writer.close(); writer = null;
                setSaved();
                lastModified = new File(filename).lastModified();

                if (PrefMgr.getFlag(PrefMgr.MAKE_BACKUP)) {
                    // if all went well, rename the crash file as a normal
                    // backup
                    File crashFile = new File(crashFilename);
                    File backupFile = new File(backupFilename);
                    backupFile.delete();
                    crashFile.renameTo(backupFile);
                }
                else {
                    File crashFile = new File(crashFilename);
                    crashFile.delete();
                }
            }
            catch (IOException ex) {
                failureException = ex;
                info.warning(Config.getString("editor.info.errorSaving") + " - " + ex.getLocalizedMessage());
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
            info.warning(Config.getString("editor.info.errorSaving")
                    + " - " + failureException.getLocalizedMessage());
            throw failureException;
        }
    }

    /**
     * The editor wants to close. Do this through the EditorManager so that we
     * can be removed from the list of open editors.
     */
    @Override
    public void close()
    {
        try {
            save();
        }
        catch (IOException ioe) {}
        // TODO should really be done by watcher from outside
        doClose();
    }

    /**
     * Display a message (used for compile/runtime errors). An editor must
     * support at least two lines of message text, so the message can contain a
     * newline character.
     * 
     * @param message  the message to be displayed
     * @param lineNumber  The line to highlight
     * @param column   the column to move the cursor to
     * @param beep   if true, do a system beep
     * @param setStepMark  if true, set step mark (for single stepping)
     * @param help  name of help group (may be null)
     */
    @Override
    public void displayMessage(String message, int lineNumber, int column, boolean beep, 
            boolean setStepMark, String help)
    {
        switchToSourceView();

        Element line = getSourceLine(lineNumber);
        int pos = line.getStartOffset();

        if (setStepMark) {
            setStepMark(pos);
        }

        // highlight the line

        sourcePane.setCaretPosition(pos);
        sourcePane.moveCaretPosition(line.getEndOffset() - 1);  // w/o line break
        moeCaret.setPersistentHighlight();

        // display the message

        if (beep) {
            info.warningImportant(message);
        }
        else {
            info.messageImportant(message);
        }

        if (help != null) {
            info.setHelp(help);
        }
    }
    
    @Override
    public void displayDiagnostic(Diagnostic diagnostic)
    {
        switchToSourceView();
        
        Element line = getSourceLine((int) diagnostic.getStartLine());
        if (line != null) {
            int pos = line.getStartOffset();

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

            errorManager.removeErrorHighlight();
            errorManager.addErrorHighlight(startPos, endPos);

            sourcePane.setCaretPosition(pos);
            sourcePane.moveCaretPosition(line.getEndOffset() - 1); // w/o line break
            moeCaret.setPersistentHighlight();
        }

        // display the message

        info.messageImportant(diagnostic.getMessage());
        info.setHelp("javac"); // TODO the compiler name, or the additional help text,
                               // should really be a property of the diagnostic object.
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
        if (testPos == 0) {
            return spos;
        }
        
        try {
            int cpos = 0; // what the actual column is so far
            int tpos = 0; // where we are in the string
            String lineText = sourceDocument.getText(spos, testPos);
            
            while (cpos < column - 1) {
                int tabPos = lineText.indexOf('\t', tpos);
                if (tabPos == -1) {
                    // No more tabs...
                    tpos += column - cpos - 1;
                    return Math.min(spos + tpos, epos - 1);
                }
                
                int newcpos = cpos + (tabPos - tpos);
                if (newcpos >= column) {
                    tpos += column - cpos - 1;
                    return spos + tpos;
                }

                cpos = newcpos;
                
                cpos += 8; // hit tab
                cpos -= cpos % 8;  // back to tab stop

                tpos = tabPos + 1; // skip over the tab char
            }
        }
        catch (BadLocationException ble) {
            // Shouldn't happen.
            throw new RuntimeException(ble);
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
    public void removeStepMark()
    {
        if (currentStepPos != -1) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttribute(MoeSyntaxView.STEPMARK, Boolean.FALSE);
            sourceDocument.setParagraphAttributes(currentStepPos, a);
            currentStepPos = -1;
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
    public void changeName(String title, String filename, String docFilename)
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
            errorManager.removeErrorHighlight();
        }
    }
    
    @Override
    public void compileFinished(boolean successful)
    {
        // Compilation requested via the editor interface has completed
        if (successful && isVisible()) {
            info.messageImportant(Config.getString("editor.info.compiled"));
        }
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
        getSourceDocument().scheduleUpdate(new Runnable() {
            @Override
            public void run()
            {
                clearAllBreakpoints();
            }
        });
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
                        watcher.breakpointToggleEvent(this, i, true);
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
     * Set this editor to read-only.
     * 
     * @param readOnly  The new readOnly value
     */
    @Override
    public void setReadOnly(boolean readOnly)
    {
        if (readOnly) {
            saveState.setState(StatusLabel.READONLY);
            updateUndoControls();
            updateRedoControls();
        }
        sourcePane.setEditable(!readOnly);
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
     * Set this editor to display either the interface or the source code of
     * this class
     * 
     * @param interfaceStatus  If true, display class interface, otherwise source.
     */
    @Override
    public void showInterface(boolean interfaceStatus)
    {
        interfaceToggle.setSelectedIndex(interfaceStatus ? 1 : 0);
    }

    /**
     * Tell whether the editor is currently displaying the interface or the
     * source of the class.
     * 
     * @return True, if interface is currently shown, false otherwise.
     */
    @Override
    public boolean isShowingInterface()
    {
        return viewingHTML;
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
     * Returns the SourceLocation object corresponding to the given offset in the
     * source text.
     * 
     * @param offset  The number of characters from the beginning of text (starting
     *                from zero)
     * @return the SourceLocation object or null if the offset points outside the
     *         text.
     */
    @Override
    public SourceLocation getLineColumnFromOffset(int offset)
    {
        if (offset < 0) {
            return null;
        }
        
        Element map = sourceDocument.getDefaultRootElement();
        int lineNumber = map.getElementIndex(offset);

        Element lineElement = map.getElement(lineNumber);
        if (offset >= lineElement.getEndOffset()) {
            return null;
        }
        
        int column = offset - lineElement.getStartOffset();

        return new SourceLocation(lineNumber+1, column+1);
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
    public void setCaretLocation(SourceLocation location)
    {
        sourcePane.setCaretPosition(getOffsetFromLineColumn(location));
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
        Caret aCaret = sourcePane.getCaret();

        // If the dot is == as the mark then there is no selection.
        if (aCaret.getDot() == aCaret.getMark()) {
            return null;
        }

        int beginOffset = Math.min(aCaret.getDot(), aCaret.getMark());

        return getLineColumnFromOffset(beginOffset);
    }

    /**
     * Returns the location where the current selection (in the
     * source pane) ends.
     * 
     * @return the current end of the selection or null if no text is selected.
     */
    @Override
    public SourceLocation getSelectionEnd()
    {
        Caret aCaret = sourcePane.getCaret();

        // If the dot is == as the mark then there is no selection.
        if (aCaret.getDot() == aCaret.getMark()) {
            return null;
        }

        int endOffset = Math.max(aCaret.getDot(), aCaret.getMark());

        return getLineColumnFromOffset(endOffset);
    }

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
    public String getText(SourceLocation begin, SourceLocation end)
    {
        int first = getOffsetFromLineColumn(begin);
        int last = getOffsetFromLineColumn(end);
        int beginOffset = Math.min(first, last);
        int endOffset = Math.max(first, last);

        try {
            return sourceDocument.getText(beginOffset, endOffset - beginOffset);
        }
        catch (BadLocationException exc) {
            throw new IllegalArgumentException(exc.getMessage());
        }
    }

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
     * @throws BadLocationException
     *             if internally the text points outside a location in the text.
     */
    @Override
    public void setText(SourceLocation begin, SourceLocation end, String newText) throws BadLocationException
    {
        int start = getOffsetFromLineColumn(begin);
        int finish = getOffsetFromLineColumn(end);

        int beginOffset = Math.min(start, finish);
        int endOffset = Math.max(start, finish);

        if (beginOffset != endOffset) {
            sourceDocument.remove(beginOffset, endOffset - beginOffset);
        }

        sourceDocument.insertString(beginOffset, newText, null);
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

        if (col >= lineLen) {
            throw new IllegalArgumentException("column=" + location.getColumn() + " greater than line len=" + lineLen);
        }

        return lineOffset + col;
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

    /**
     * Returns the length of the line indicated in the edited text.
     * Zero is a valid value if the given line has no characters in it.
     *
     * @param  line  the line in the text for which the length should be calculated, starting from 0
     * @return       the length of the line, -1 if line is invalid
     */
    @Override
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

    /**
     * Returns the length of the source document.
     *
     * It is possible to obtain the line and column of the last character of text by using
     * this method together with the getLineColumnFromOffset() method.
     *
     * @return the source length (>= 0)
     */
    @Override
    public int getTextLength ()
    {
        return sourceDocument.getLength();
    }

    /**
     * Return the number of lines in the source document.
     */
    @Override
    public int numberOfLines()
    {
        return sourceDocument.getDefaultRootElement().getElementCount();
    }

    /*
     * @see bluej.editor.Editor#getParsedNode()
     */
    @Override
    public ParsedCUNode getParsedNode()
    {
        return sourceDocument.getParser();
    }
    
    // --------------------------------------------------------------------
    // ------------ end of interface inherited from Editor ----------------
    // --------------------------------------------------------------------

    /**
     * Update the state of controls bound to "undo".
     */
    public void updateUndoControls()
    {
        boolean canUndo = undoManager.canUndo();
        displayMenuItem("undo", canUndo);
        displayToolbarItem("undo", canUndo);
    }

    /**
     * Update the state of controls bound to "redo".
     */
    public void updateRedoControls()
    {
        boolean canRedo = undoManager.canRedo();
        displayMenuItem("redo", canRedo);
        displayToolbarItem("redo", canRedo);
    }
    
    /**
     * Check whether the source file has changed on disk. If it has, reload.
     */
    private void checkForChangeOnDisk()
    {
        if (filename == null) {
            return;
        }
        File file = new File(filename);
        long modified = file.lastModified();
        if(modified != lastModified) {
            if (saveState.isChanged()) {
                int answer = DialogManager.askQuestion(this, "changed-on-disk");
                if (answer == 0)
                    doReload();
                else
                    lastModified = modified; // don't ask again for this change
            }
            else {
                doReload();
            }
        }
    }
    
    /**
     * Schedule the ReparseRunner on the AWT event queue, if it is not already scheduled.
     */
    private void scheduleReparseRunner() {
        if (reparseRunner == null) {
            reparseRunner = new ReparseRunner(this);
            EventQueue.invokeLater(reparseRunner);
        }
    }
    /**
     * Informs the editor that the re-parse runner has de-scheduled itself due to lack
     * of work.
     */
    public void reparseRunnerFinished()
    {
        reparseRunner = null;
    }
    
    // ---- BlueJEventListener interface ----

    /**
     * A BlueJEvent was raised. Check whether it is one that we're interested in.
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        switch(eventId) {
            case BlueJEvent.DOCU_GENERATED :
                BlueJEvent.removeListener(this);
                refreshHtmlDisplay();
                break;
            case BlueJEvent.DOCU_ABORTED :
                BlueJEvent.removeListener(this);
                info.warning(Config.getString("editor.info.docAborted"));
                break;
        }
    }

    // -------- DocumentListener interface --------

    /**
     * A text insertion has taken place.
     */
    @Override
    public void insertUpdate(DocumentEvent e)
    {
        //errorManager.insertUpdate(e);
        clearMessage();
        removeSearchHighlights();
        errorManager.removeErrorHighlight();
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
        doTextInsert.setEvent(e, sourcePane);
        
        // This may handle re-indentation; as this mutates the
        // document, it must be done outside the notification.
        ((MoeSyntaxDocument) e.getDocument()).scheduleUpdate(doTextInsert);
        
        recordEdit(false);        
        
        scheduleReparseRunner();
    }

    /**
     * A text removal has taken place.
     */
    @Override
    public void removeUpdate(DocumentEvent e)
    {
        //errorManager.removeUpdate(e);
        clearMessage();
        removeSearchHighlights();
        errorManager.removeErrorHighlight();
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
        
        recordEdit(false);
        
        scheduleReparseRunner();
    }

    /**
     * Document properties have changed
     */
    @Override
    public void changedUpdate(DocumentEvent e) { }
    
    // --------------------------------------------------------------------
    /**
     * Clear the message in the info area.
     */
    public void clearMessage()
    {
        info.clear();
    }

    /**
     * Display a message into the info area.
     * 
     * @param msg  the message to display
     */
    @Override
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
        info.warning(msg);
    }

    // ==================== USER ACTION IMPLEMENTATIONS ===================

    // --------------------------------------------------------------------
    
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

    // --------------------------------------------------------------------
    
    /**
     * User requests "reload"
     */
    public void reload()
    {
        if (filename == null) {
            info.warning(Config.getString("editor.info.cannotReload"), 
                    Config.getString("editor.info.reload"));
        }
        else if (saveState.isChanged()) {
            int answer = DialogManager.askQuestion(this, "really-reload");
            if (answer == 0)
                doReload();
        }
        else {
            doReload();
        }
    }

    // --------------------------------------------------------------------

    /**
     * Prints source code from Editor
     * 
     * @param printerJob  A PrinterJob to print to.
     */
    @Override
    public void print(PrinterJob printerJob)
    {
        if (printDialog == null) {
            printDialog = new PrintDialog(this);
        }

        if (printDialog.display()) {
            PrintHandler pt = new PrintHandler(printerJob, getPageFormat(printerJob), printDialog.printLineNumbers(), printDialog.printHighlighting());
            pt.print();
        }
    }

    /**
     * Return a validated version of the global PageFormat for BlueJ
     */
    public static PageFormat getPageFormat(PrinterJob job)
    {
        return job.validatePage(PkgMgrFrame.getPageFormat());
    }

    /**
     * Generalised version of print function. This is what is typically called
     * when print is initiated from within the source code editor menu. This
     * sets up and runs the print process as a separate lower priority thread.
     */
    public void print()
    {
        if (printDialog == null)
            printDialog = new PrintDialog(this);

        if (printDialog.display()) {
            // create a printjob
            PrinterJob job = PrinterJob.getPrinterJob();
            if (job.printDialog()) {
                PrintHandler pt = new PrintHandler(job, getPageFormat(job), printDialog.printLineNumbers(), printDialog.printHighlighting());
                Thread printJobThread = new Thread(pt);
                printJobThread.setPriority((Thread.currentThread().getPriority() - 1));
                printJobThread.start();
            }
        }
    }

    /**
     * Implementation of the "page setup" user function. This provides a dialog
     * for print page setup. PageSetup is global to BlueJ. Calling this from the 
     * Editor is effectively the same as calling from PkgMgrFrame as this saves 
     * back to PkgMgrFrame's global page format object.
     */
    public static void pageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pageFormat = job.pageDialog(PkgMgrFrame.getPageFormat());
        PkgMgrFrame.setPageFormat(pageFormat);
    }

    // --------------------------------------------------------------------
    
    /**
     * The editor has been closed. Hide the editor window now.
     */
    public void doClose()
    {
        setVisible(false);
        if (watcher != null) {
            //setting the naviview visible property when an editor is closed
            watcher.setProperty(EditorWatcher.NAVIVIEW_EXPANDED_PROPERTY, String.valueOf(dividerPanel.isExpanded()));
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
     * toggleReplacePanelVisible sets the replace panel editor in/visible
     * if visible sets the necessary other values
     */
    public void toggleReplacePanelVisible()
    {
        if (replacer.isVisible() || !finder.isVisible()) {
            replacer.setVisible(false);
            return;
        }
        replacer.setVisible(true);
        finder.requestFindfieldFocus();
    }

    /**
     * Opens or close the replace panel (and if opening it, set the focus into
     * the find field).
     */
    protected void setReplacePanelVisible(boolean visible)
    {
        if (visible) {
            if (!finder.isVisible()) {
                finder.setVisible(visible);
            }
            replacer.setVisible(visible);
            finder.requestFindfieldFocus();
            finder.setFindReplaceIcon(true);
        }
        else {
            replacer.setVisible(false);
            finder.setFindReplaceIcon(false);
        }
    }

    // --------------------------------------------------------------------

    /**
     * Replaces the selected text with the replaceString; moves the caret to the 
     * position it was in before the replace was requested and writes a message
     */
    public void replace(String replaceString)
    {
        int caretPos = sourcePane.getCaretPosition();
        String searchString=finder.getSearchString();
        if (getSourcePane().getSelectedText()==null|| getSourcePane().getSelectedText().length()<=0){
            //in case the selection has been lost due to moving it in the editor
            if (finder.getSearchString()!=null && finder.getSearchString().length()>0)
                searchString=finder.getSearchTextfield();
            else {
                writeMessage("Invalid search string ");
                return;
            }
        }
        String replaceText = smartFormat(searchString, replaceString);
        insertText(replaceText, true);
        //move the caret back to where it was before the replace
        sourcePane.setCaretPosition(caretPos);
        finder.find(true);
        //editor.writeMessage("Replaced " + count + " instances of " + searchString);
        writeMessage("Replaced an instance of " + 
                searchString);
    }

    // --------------------------------------------------------------------
    
    /**
     * Implementation of "find-next" user function.
     */
    public void findNext(boolean backwards)
    {
        String selection= currentTextPane.getSelectedText();
        if (selection==null){
            selection=finder.getSearchString();
        }
        //if the find panel is open- next/backwards should function like prev, next
        //if it is not open, it should do a single find forward or backwards
        if (finder.isVisible()){
            finder.setSearchString(selection);
            if (backwards) {
                finder.getPrev();
            }
            else {
                finder.getNext();
            }
        }
        else {
            removeSearchHighlights();
            removeSelection(currentTextPane);
            findString(selection, backwards, !finder.getMatchCase(), true);
        }
    }

    // --------------------------------------------------------------------    
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
            found = doFindBackward(s, ignoreCase, wrap);
        }
        else {
            setCaretPositionForward(1);
            found = doFind(s, ignoreCase, wrap);
        }

        StringBuffer msg = new StringBuffer(Config.getString("editor.find.find.label") + " ");
        msg.append(backward ? Config.getString("editor.find.backward") : Config.getString("editor.find.forward"));
        if (ignoreCase || wrap) {
            msg.append(" (");
        }
        if (ignoreCase) {
            msg.append(Config.getString("editor.find.ignoreCase").toLowerCase() + ", ");
        }
        if (wrap) { 
            msg.append(Config.getString("editor.find.wrapAround").toLowerCase() + ", ");
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
            info.warning(msg.toString(), Config.getString("editor.info.notFound"));
        }

        return found;
    }

    // --------------------------------------------------------------------

    /**
     * Search for and select the given search string forwards from
     * the current caret position. Returns false if not found.
     */
    boolean doFind(String s, boolean ignoreCase, boolean wrap)
    {
        int docLength = document.getLength();
        int startPosition = currentTextPane.getCaretPosition();
        int endPos = docLength;

        boolean found = false;
        boolean finished = false;

        // first line searched starts from current caret position
        int start = startPosition;
        Element line = getLineAt(start);
        int lineEnd = Math.min(line.getEndOffset(), endPos);

        // following lines search from start of line
        try {
            while (!found && !finished) {
                String lineText = document.getText(start, lineEnd - start);

                if (lineText != null && lineText.length() > 0) {
                    int foundPos = findSubstring(lineText, s, ignoreCase, false);
                    if (foundPos != -1) {
                        currentTextPane.select(start + foundPos, start + foundPos + s.length());
                        currentTextPane.getCaret().setSelectionVisible(true);
                        found = true;
                    }
                }
                if (lineEnd >= endPos) {
                    if (wrap) {
                        // do the wrapping
                        endPos = startPosition;
                        line = document.getParagraphElement(0);
                        start = line.getStartOffset();
                        lineEnd = Math.min(line.getEndOffset(), endPos);
                        wrap = false;
                        // don't wrap again
                    }
                    else {
                        finished = true;
                    }
                }
                else {
                    // go to next line
                    line = document.getParagraphElement(lineEnd + 1);
                    start = line.getStartOffset();
                    lineEnd = Math.min(line.getEndOffset(), endPos);
                }
            }
        }
        catch (BadLocationException ex) {
            Debug.reportError("Error in editor find operation", ex);
        }
        return found;
    }

    /**
     * Do a find backwards without visible feedback. Returns
     * false if not found.
     */
    boolean doFindBackward(String s, boolean ignoreCase, boolean wrap)
    {
        int docLength = document.getLength();
        int startPosition = currentTextPane.getCaretPosition() - 1;
        if (startPosition < 0) {
            startPosition = docLength;
        }
        int endPos = 0;                   // where the search ends

        boolean found = false;
        boolean finished = false;

        int start = startPosition;        // start of next partial search
        Element line = getLineAt(start);
        int lineStart = Math.max(line.getStartOffset(), endPos);

        try {
            while (!found && !finished) {
                String lineText = document.getText(lineStart, start - lineStart);
                if (lineText != null && lineText.length() > 0) {
                    int foundPos = findSubstring(lineText, s, ignoreCase, true);
                    if (foundPos != -1) {
                        currentTextPane.select(lineStart + foundPos, lineStart + foundPos + s.length());
                        currentTextPane.getCaret().setSelectionVisible(true);
                        found = true;
                    }
                }
                if (lineStart <= endPos) {            // reached end of search
                    if (wrap) {                       // do the wrapping around
                        endPos = startPosition;
                        line = document.getParagraphElement(docLength);
                        start = line.getEndOffset();
                        lineStart = Math.max(line.getStartOffset(), endPos);
                        wrap = false;                 // don't wrap again
                    }
                    else {
                        finished = true;
                    }
                }
                else {                                // go to next line
                    line = document.getParagraphElement(lineStart - 1);
                    start = line.getEndOffset();
                    lineStart = Math.max(line.getStartOffset(), endPos);
                }
            }
        }
        catch (BadLocationException ex) {
            Debug.reportError("Error in editor find operation", ex);
        }
        return found;
    }

    /**
     * doFindSelect - finds all the instances in the document from where the 
     * caret position is, optionally selects the first one and highlights all others
     * @param select indicates whether the first occurrence should be selected or only highlighted
     * @param s search string 
     * 
     * @return Returns false if not found.
     */
    int doFindSelect(String s, boolean ignoreCase, boolean wrap)
    {
        boolean select=true; //first item found should be selected so initialised to true
        int docLength = document.getLength();
        int startPosition = currentTextPane.getCaretPosition();
        int endPos = docLength;
        int highlightCount = 0;

        boolean finished = false;

        int start = startPosition;
        Element line = document.getParagraphElement(start);
        int lineEnd = line.getEndOffset();   
        int foundPos =0; 
        try {
            while (!finished) {
                String lineText = document.getText(start, lineEnd - start);
                while (lineText != null && lineText.length() > 0) {
                    foundPos = findSubstring(lineText, s, ignoreCase, false, foundPos);
                    if (foundPos != -1) {
                        addSearchHighlight(start + foundPos, start + foundPos + s.length());
                        highlightCount++;
                        if (select) {
                            currentTextPane.select(start + foundPos, start + foundPos + s.length());
                            setSelectionVisible();  
                            //reset the start position to the first selection start
                            //in order to ensure that none are missed
                            startPosition=start+foundPos;
                            select=false;
                        }
                        foundPos=foundPos+s.length();
                    } else {
                        lineText=null;
                    }
                }
                if (lineEnd >= endPos) {
                    if (wrap) {
                        // do the wrapping
                        endPos = Math.min(startPosition + s.length() - 1, document.getLength());
                        line = document.getParagraphElement(0);
                        start = line.getStartOffset();
                        lineEnd = Math.min(line.getEndOffset(), endPos);
                        wrap = false;
                        // don't wrap again
                    }
                    else {
                        finished = true;
                    }
                }
                else {
                    // go to next line
                    line = document.getParagraphElement(lineEnd + 1);
                    start = line.getStartOffset();
                    lineEnd = Math.min(line.getEndOffset(), endPos);
                }
            }
        }
        catch (BadLocationException ex) {
            Debug.reportError("Error in editor find operation", ex);
        }
        return highlightCount;
    }
    
    /**
     * Add a search highlight to the currently displayed pane.
     */
    private void addSearchHighlight(int startPos, int endPos)
    {
        try {
            MoeHighlighter highlighter = (MoeHighlighter) currentTextPane.getHighlighter();
            Object tag = highlighter.addHighlight(startPos, endPos, searchHighlightPainter);
            if (currentTextPane == sourcePane) {
                sourceSearchHighlightTags.add(tag);
            }
            else {
                htmlSearchHighlightTags.add(tag);
            }
        }
        catch (BadLocationException ble) {
            Debug.reportError("Error adding search highlight", ble);
        }
    }

    /**
     * Transfers caret to user specified line number location.
     */
    public void goToLine()
    {
        if (goToLineDialog == null) {
            goToLineDialog = new GoToLineDialog(this);
        }

        DialogManager.centreDialog(goToLineDialog);
        goToLineDialog.showDialog(numberOfLines());
        int newPosition = goToLineDialog.getLineNumber();
        if (newPosition > 0) {
            setSelection(newPosition, 1, 0);
        }
    }

    /**
     * Find the position of a substring in a given string, 
     * can specify direction and whether the search should ignore case
     * Return the position of the substring or -1.
     *
     * @param  text        the full string to be searched
     * @param  sub         the substring that we're looking for
     * @param  ignoreCase  if true, case is ignored
     * @param  backwards   Description of the Parameter
     * @return             Description of the Return Value
     * @returns            the index of the substring, or -1 if not found
     */
    private static int findSubstring(String text, String sub, boolean ignoreCase, boolean backwards)
    {
        int strlen = text.length();
        int sublen = sub.length();

        if (sublen == 0) {
            return -1;
        }

        boolean found = false;
        int pos = (backwards ? strlen - sublen : 0);
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
    // --------------------------------------------------------------------
    
    /**
     * Implementation of "compile" user function.
     */
    public void compile()
    {
        if (watcher == null) {
            return;
        }
        if (!viewingCode()) {
            info.warning(" ");
            return;
        }

        info.message(Config.getString("editor.info.compiling"));
        watcher.compile(this);
    }

    // --------------------------------------------------------------------
    
    /**
     * Toggle the interface popup menu. This is used when using keys to toggle
     * the interface view. Toggling the menu will result in invoking the action.
     */
    public void toggleInterfaceMenu()
    {
        if (!sourceIsCode)
            return;

        if (interfaceToggle.getSelectedIndex() == 0) {
            interfaceToggle.setSelectedIndex(1);
        }
        else {
            interfaceToggle.setSelectedIndex(0);
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Implementation of "toggle-interface-view" user function. The menu has
     * already been changed - now see what it is and do it.
     */
    public void toggleInterface()
    {
        if (!sourceIsCode) {
            return;
        }

        boolean wantHTML = (interfaceToggle.getSelectedItem() == interfaceString);
        if (wantHTML && !viewingHTML) {
            switchToInterfaceView();
        }
        else if (!wantHTML && viewingHTML) {
            switchToSourceView();
        }
    }

    /**
     * Allow the enabling/disabling of print menu option. Added to disable the
     * printing og javadoc html for the time being until until implemented.
     * (This is reliant on the use of j2sdk1.4 and Java Unified Print Service
     * implementation JSR 6)
     * 
     * @param flag  true to enable printing from menu.
     */
    public void enablePrinting(boolean flag)
    {
        Action printAction = actions.getActionByName("print");
        if (printAction != null) {
            printAction.setEnabled(flag);
        }
        Action pageSetupAction = actions.getActionByName("page-setup");
        if (pageSetupAction != null) {
            pageSetupAction.setEnabled(flag);
        }

    }   

    /**
     * Check if an item is in the reserved list for disabled interface options
     *  
     * @return boolean reflects if it is enabled ie false=disabled
     * @param buttonText  String with button text name
     */
    private static boolean isEditAction(String text)
    {       
        ArrayList<String> editActions = getEditActions();
        if (editActions!=null && editActions.contains(text)) {
            return true;
        }
        return false;
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
        ArrayList<String> flaggedActions = getNonReadmeActions();
        if (flaggedActions!=null && flaggedActions.contains(actionName)) {
            return true;
        }
        return false;
    }

    /**
     * Get a list of actions not applicable in the readme.txt file
     */
    private static ArrayList<String> getNonReadmeActions ()
    {
        if (readMeActions==null) {
            readMeActions=new ArrayList<String>();
            readMeActions.add("compile");
            readMeActions.add("autoindent");
            readMeActions.add("insert-method");
            readMeActions.add("toggle-interface-view");
        }
        return readMeActions;
    }
    /**
     * Returns a list of names for the actions which are only valid in an editing
     * context, that is, when the display shows the source and not the documentation.
     *  
     * @return list of editing action names
     */
    private static ArrayList<String> getEditActions()
    {
        if (editActions == null) {
            editActions=new ArrayList<String>();
            editActions.add("save");
            editActions.add("reload");
            editActions.add("print");
            editActions.add("page-setup");
            editActions.add("compile");
            editActions.add("cut-to-clipboard");
            editActions.add("indent-block");
            editActions.add("deindent-block");
            editActions.add("comment-block");
            editActions.add("uncomment-block");
            editActions.add("insert-method");
            editActions.add("replace");
            editActions.add("go-to-line");
            editActions.add("paste-from-clipboard");
            editActions.add("toggle-breakpoint");
            editActions.add("autoindent");
        }

        return editActions;
    }
    
    /**
     * Sets the search start to the beginning of the document/current pos in the
     * sourcepane; removes all the selections and highlights; resets search string 
     * and initiates a search (if the find panel is visible)
     */
    private void initSearch()
    {
        //current caret position may be invalid in the new view
        //so reset it to the current pos in that pane/0 in the documentation
        if (isShowingInterface()){
            finder.setSearchStart(0);
        }    
        else{
            finder.setSearchStart(getCurrentTextPane().getCaretPosition());
        }
        //reset the search string to null
        finder.setSearchString(null);
        //as the search is cleared between switches in the view
        //there should be no selections/highlights from the previous search
        removeSearchHighlights();
        removeSelections();
        //reset the search and replace strings
        finder.setSearchString(null);
        replacer.setReplaceString(null);
        replacer.populateReplaceField(null);
        if (finder.isVisible()){
            initFindPanel();
        }
    }
    
    // --------------------------------------------------------------------
    
    /**
     * Switch on the source view (if it isn't showing already).
     */
    private void switchToSourceView()
    {
        if (!viewingHTML) {
            return;
        }
        resetMenuToolbar(true);
        document = sourceDocument;
        currentTextPane = sourcePane;
        viewingHTML = false;
        scrollPane.setViewportView(currentTextPane);
        dividerPanel.endTemporaryHide();
        currentTextPane.requestFocus();
        initSearch();
    }

    // --------------------------------------------------------------------
    
    /**
     * Switch on the javadoc interface view (it it isn't showing already). If
     * necessary, generate it first.
     */
    private void switchToInterfaceView()
    {
        if (viewingHTML) {
            return;
        }
        resetMenuToolbar(false);
        dividerPanel.beginTemporaryHide();
        try {
            save();
            displayInterface();
        }
        catch (IOException ioe) {
            // Could display a dialog here. However, the error message
            // (from save() call) will already be displayed in the editor
            // status bar.
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Refresh the HTML display.
     */
    private void refreshHtmlDisplay()
    {
        FileInputStream fis = null;
        try {
            File urlFile = new File(getDocPath());
            URL myURL = urlFile.toURI().toURL();
            
            HTMLEditorKit ekit = new HTMLEditorKit();
            htmlDocument = (HTMLDocument) ekit.createDefaultDocument();
            htmlDocument.setBase(myURL);
            // Must ignore character set META tag, otherwise an exception will be thrown;
            // HTMLEditorKit doesn't support changing character set:
            htmlDocument.putProperty("IgnoreCharsetDirective", true);
            htmlDocument.putProperty(Document.StreamDescriptionProperty, myURL);
            
            fis = new FileInputStream(urlFile);
            Reader r = new InputStreamReader(fis, characterSet);
            ekit.read(r, htmlDocument, 0);
            r.close();
            
            htmlPane.setDocument(htmlDocument);
            
            info.message(Config.getString("editor.info.docLoaded"));
            if (isShowingInterface()){
                document=htmlDocument;
            }
        }
        catch (Exception exc) {
            info.warning(Config.getString("editor.info.docDisappeared"), getDocPath());
            Debug.reportError("loading class interface failed: " + exc);
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (Exception e) {}
            }
        }
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
        boolean canUndo=false;
        boolean canRedo=false;
        displayMenubar(sourceView);
        displayToolbar(sourceView);
        //if the view is source view need to decide whether to display 
        //the undo and redo according to the undoManager; if it
        //is the documentation view then they are always disabled
        if (sourceView){
            if (undoManager.canUndo())
                canUndo=true;   
            if (undoManager.canRedo())
                canRedo=true;
        }
        displayMenuItem("undo", canUndo);
        displayToolbarItem("undo", canUndo);
        displayMenuItem("redo", canRedo);
        displayToolbarItem("redo", canRedo);
    }

    /**
     * This method changes the display of the menubar based on the
     * view (source/documentation) that is selected.
     * 
     * @param sourceView true if viewing source; false if viewing documentation
     */
    private void displayMenubar(boolean sourceView)
    {
        JMenuBar menuBar = getJMenuBar(); 
        JMenu menu=null;
        Component[] menubarComponent = menuBar.getComponents();
        for (int i=0;i<menubarComponent.length; i++ ){
            if (menubarComponent[i] instanceof JMenu){
                menu=(JMenu)menubarComponent[i]; 
                for (int j=0; j<menu.getMenuComponentCount(); j++){
                    if (menu.getMenuComponent(j) instanceof JMenuItem){
                        if (isEditAction(((JMenuItem)menu.getMenuComponent(j)).getName())){                  
                            ((JMenuItem)menu.getMenuComponent(j)).setEnabled(sourceView);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method changes the display of the toolbar based on the view
     * (source/documentation) that is selected
     * 
     * @param sourceView true if viewing source; false if viewing documentation
     */
    private void displayToolbar(boolean sourceView)
    {
        JPanel toolbar=null;
        Component contentPaneItem;
        JButton actionButton;
        Component[] c = getContentPane().getComponents();
        for (int i=0;i<c.length; i++ ){
            contentPaneItem=c[i];
            if(contentPaneItem.getName()!=null && contentPaneItem.getName().equals("toolbar")) {
                toolbar=(JPanel)contentPaneItem;
            }
        }

        if (toolbar==null) {
            return;
        }
        
        Component[] toolbarComponent = toolbar.getComponents();
        for (int i=0;i<toolbarComponent.length; i++ ) {
            if (toolbarComponent[i] instanceof JButton) {                   
                actionButton=(JButton)toolbarComponent[i];
                if (isEditAction(actionButton.getName())) {
                    actionButton.setEnabled(sourceView);
                }
            }
        }
    }

    /**
     * This method changes the display of the menubar based on the interface that is selected
     * 
     * @param sourceView true if called from sourceView setup; false from documentation View setup
     */
    private void displayMenuItem(String itemName, boolean sourceView)
    {
        JMenuBar menuBar = getJMenuBar(); 
        JMenu menu=null;
        JMenuItem menuItem;
        Component[] menubarComponent = menuBar.getComponents();
        for (int i=0;i<menubarComponent.length; i++ ){
            if (menubarComponent[i] instanceof JMenu){
                menu=(JMenu)menubarComponent[i]; 
                for (int j=0; j<menu.getMenuComponentCount(); j++){
                    if (menu.getMenuComponent(j) instanceof JMenuItem){
                        menuItem=(JMenuItem)menu.getMenuComponent(j);
                        if (menuItem.getName().equals(itemName)){                   
                            menuItem.setEnabled(sourceView);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * This method enables/disables the display of the toolbar item specified
     * 
     * @param sourceView true if called from sourceView setup; false from documentation View setup
     */
    private void displayToolbarItem(String itemName, boolean sourceView)
    {
        JPanel toolbar=null;
        Component contentPaneItem;
        Component[] c = getContentPane().getComponents();
        for (int i=0;i<c.length; i++ ){
            contentPaneItem=c[i];
            if(contentPaneItem.getName()!=null && contentPaneItem.getName().equals("toolbar")) { 
                toolbar=(JPanel)contentPaneItem;
            }
        }

        if (toolbar==null) {
            return;
        }

        Component[] toolbarComponent = toolbar.getComponents();
        for (int i=0;i<toolbarComponent.length; i++ ){
            if (toolbarComponent[i] instanceof JButton){                
                JButton actionButton=(JButton)toolbarComponent[i];
                if (actionButton.getName().equals(itemName)){
                    actionButton.setEnabled(sourceView);
                    return;
                }
            }
        }
    }

    /**
     * We want to display the interface view. This will generate the
     * documentation if necessary.
     * 
     * Don't call this directly to switch to the interface view. Call
     * switchToInterfaceView() instead.
     */
    private void displayInterface()
    {
        info.message(Config.getString("editor.info.loadingDoc"));
        boolean generateDoc = ! docUpToDate();

        // The following all used to be done in a separate thread, but this is not
        // necessary - setPage() operates asynchronously anyway.
        if (htmlPane == null) {
            createHTMLPane();
            if (! generateDoc) {
                refreshHtmlDisplay();
            }
        }
        else if (! generateDoc) {
            info.message(Config.getString("editor.info.docLoaded"));
        }

        if (generateDoc) {
            // clear the existing document
            htmlDocument = new HTMLDocument();
            htmlPane.setDocument(htmlDocument);

            // interface needs to be re-generated
            info.message(Config.getString("editor.info.generatingDoc"));
            BlueJEvent.addListener(this);
            if (watcher != null) {
                watcher.generateDoc();
            }
        }

        document = htmlDocument;
        currentTextPane = htmlPane;
        viewingHTML = true;
        scrollPane.setViewportView(htmlPane);
        currentTextPane.requestFocus();
        initSearch();
    }

    // --------------------------------------------------------------------

    /**
     * Create the HTML plane used to display javadoc.
     */
    private void createHTMLPane()
    {
        htmlPane = new JEditorPane();
        htmlPane.setHighlighter(new MoeHighlighter());
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(this);
        htmlPane.setInputMap(JComponent.WHEN_FOCUSED, new InputMap() {
            @Override
            public Object get(KeyStroke keyStroke)
            {
                // Define no action for up/down, which allows the parent scroll
                // pane to process the keys instead. This means the view will scroll,
                // rather than just moving an invisible cursor.
                Object action = super.get(keyStroke);
                if ("caret-up".equals(action) || "caret-down".equals(action)) {
                    return null;
                }
                return action;
            }
        });
    }

    // --------------------------------------------------------------------

    /**
     * A hyperlink was activated in the document. Do something appropriate.
     */
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        info.clear();
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else {
                try {
                    pane.setPage(e.getURL());
                }
                catch (Throwable t) {
                    info.warning("cannot display hyperlink: " + e.getURL());
                    Debug.reportError("hyperlink failed: " + t);
                }
            }
        }
    }

    // --------------------------------------------------------------------
    
    /**
     * Implementation of "toggle-breakpoint" user function.
     */
    public void toggleBreakpoint()
    {
        if (!viewingCode()) {
            info.warning(" ");            // cause a beep
            return;
        }
        toggleBreakpoint(sourcePane.getCaretPosition());
    }

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

    /**
     * Clear all known breakpoints.
     */
    private void clearAllBreakpoints()
    {
        if (mayHaveBreakpoints) {

            for (int i = 1; i <= numberOfLines(); i++) {
                if (lineHasBreakpoint(i)) {
                    doRemoveBreakpoint(getPositionInLine(i));
                }
            }
            mayHaveBreakpoints = false;
        }
    }

    /**
     * Check whether a position in the current document has a breakpoint set
     * (should only be called after a check that the current document is the source doc)
     */
    private boolean positionHasBreakpoint(int pos)
    {
        Element line = getSourceLine(getLineNumberAt(pos));
        return Boolean.TRUE.equals(line.getAttributes().getAttribute(MoeSyntaxView.BREAKPOINT));
    }
    
    /**
     * Check whether a line in the source document has a breakpoint set
     */
    private boolean lineHasBreakpoint(int lineNo)
    {
        Element line = getSourceLine(lineNo);
        return (Boolean.TRUE.equals(line.getAttributes().getAttribute(MoeSyntaxView.BREAKPOINT)));
    }

    /**
     * Try to set or remove a breakpoint (depending on the parameter) at the
     * given position in the source document. Informs the watcher.
     */
    private void setUnsetBreakpoint(int pos, boolean set)
    {
        if (watcher != null) {
            int line = getLineNumberAt(pos);
            String result = watcher.breakpointToggleEvent(this, line, set);

            if (result == null) {
                // no problem, go ahead
                SimpleAttributeSet a = new SimpleAttributeSet();
                if (set) {
                    a.addAttribute(MoeSyntaxView.BREAKPOINT, Boolean.TRUE);
                    mayHaveBreakpoints = true;
                }
                else {
                    a.addAttribute(MoeSyntaxView.BREAKPOINT, Boolean.FALSE);
                }

                sourceDocument.setParagraphAttributes(pos, a);
            }
            else {
                info.warning(result);
            }

            // force an update of UI
            repaint();
        }
        else {
            info.warning(Config.getString("editor.info.cannotSetBreak"));
        }
    }

    /**
     * Remove a breakpoint without question.
     */
    private void doRemoveBreakpoint(int pos)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(MoeSyntaxView.BREAKPOINT, Boolean.FALSE);
        sourceDocument.setParagraphAttributes(pos, a);
        repaint();
    }

    // --------------------------------------------------------------------

    /**
     * Try to set or remove a step mark (depending on the parameter) at the
     * given position.
     * 
     * @param pos  A position in the line where we'd like the step mark.
     */
    private void setStepMark(int pos)
    {
        removeStepMark();
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(MoeSyntaxView.STEPMARK, Boolean.TRUE);
        sourceDocument.setParagraphAttributes(pos, a);
        currentStepPos = pos;
        // force an update of UI
        repaint();
    }

    // ========================= SUPPORT ROUTINES ==========================

    // --------------------------------------------------------------------
    /**
     * Return a boolean representing whether in source editing view
     */
    private boolean viewingCode()
    {
        return sourceIsCode && (!viewingHTML);
    }

    // --------------------------------------------------------------------
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
     * Find and return a line by text position in the current document
     */
    private Element getLineAt(int pos)
    {
        return document.getParagraphElement(pos);
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a position in the source document.
     */
    private int getPositionInLine(int lineNo)
    {
        return getSourceLine(lineNo).getStartOffset();
    }

    // --------------------------------------------------------------------
    /**
     * Return the number of the line containing position 'pos' in the source document.
     */
    private int getLineNumberAt(int pos)
    {
        return sourceDocument.getDefaultRootElement().getElementIndex(pos) + 1;
    }

    // --------------------------------------------------------------------
    
    /**
     * Revert the buffer contents to the last saved version. Do not ask any
     * question - just do it. Must have a file name.
     */
    public void doReload()
    {
        removeSearchHighlights();
        Reader reader = null;
        boolean isShowingSrc = sourceDocument == document;
        
        try {
            FileInputStream inputStream = new FileInputStream(filename);
            reader = new InputStreamReader(inputStream, characterSet);
            sourcePane.read(reader, null);
            try {
                reader.close();
                inputStream.close();
            }
            catch (IOException ioe) {}
            File file = new File(filename);
            lastModified = file.lastModified();
            
            sourceDocument = (MoeSyntaxDocument) sourcePane.getDocument();
            sourceDocument.enableParser(false);
            naviView.setDocument(sourceDocument);

            // flag document type as a java file by associating a
            // JavaTokenMarker for syntax colouring if specified
            sourceDocument.addDocumentListener(this);
            sourceDocument.addUndoableEditListener(undoManager);

            // We want to inform the watcher that the editor content has changed,
            // and then inform it that we are in "saved" state (synced with file).
            // But first set state to saved to avoid unnecessary writes to disk.
            saveState.setState(StatusLabel.SAVED);
            setChanged(); // contents may have changed - notify watcher
            setSaved();  // notify watcher that we are saved
            
            scheduleReparseRunner();
        }
        catch (FileNotFoundException ex) {
            info.warning(Config.getString("editor.info.fileDisappeared"));
        }
        catch (IOException ex) {
            info.warning(Config.getString("editor.info.fileReadError"));
            setChanged();
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException ioe) {}
            
            if (isShowingSrc) {
                document = sourceDocument;
            }
            
            if (finder != null && finder.isVisible()) {
               finder.find(true);
            }
        }
    }

    // --------------------------------------------------------------------

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
            moeCaret.removeBracket();
        }
    }

    /**
     * Tell whether we are currently matching brackets.
     * 
     * @return True, if we are matching brackets, otherwise false.
     */
    public static boolean matchBrackets()
    {
        return matchBrackets;
    }
    
    /**
     * Set the caret inactive or active. When "inactive" the standard Moe caret is replaced
     * with a dummy caret which does not paint or do anything which the default caret does.
     * This makes it much, much faster to move around (any text insert or remove operation
     * moves the caret, so these become faster as well).
     * 
     * <p>Calls to setCaretActive(false) should always be paired with calls to
     * setCaretActive(true).
     * 
     * @param active  True, if the Moe caret should be active; false to replace it temporarily
     *                with a fast dummy caret.
     */
    public void setCaretActive(boolean active)
    {
        if (! active) {
            //moeCaret.deinstall(currentTextPane);
            currentTextPane.setCaret(new NullCaret(moeCaret.getMark(), moeCaret.getDot()));
        }
        else {
            //moeCaret.install(currentTextPane);
            Caret caret = currentTextPane.getCaret();
            currentTextPane.setCaret(moeCaret);
            moeCaret.setDot(caret.getMark());
            moeCaret.moveDot(caret.getDot());
        }
    }

    // --------------------------------------------------------------------
    /**
     * Toggle the editor's 'compiled' status. If compiled, enable the breakpoint
     * function.
     */
    private void setCompileStatus(boolean compiled)
    {
        actions.getActionByName("toggle-breakpoint").setEnabled(compiled && viewingCode());
        if (compiled) {
            sourceDocument.putProperty(COMPILED, Boolean.TRUE);
        }
        else {
            sourceDocument.putProperty(COMPILED, Boolean.FALSE);
        }

        currentTextPane.repaint();
    }

    // --------------------------------------------------------------------
    /**
     * Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
        info.message(Config.getString("editor.info.saved"));
        saveState.setState(StatusLabel.SAVED);
        if (watcher != null) {
            watcher.saveEvent(this);
        }
    }

    // --------------------------------------------------------------------
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
        String errCode = errorManager.getErrorAtPosition(caretPos);
        if (errCode != null) {
            info.message(ParserMessageHandler.getMessageForCode(errCode));
        }
        
        // the selection may have changed and therefore need to determine
        // whether it is logical to have the buttons enabled/disabled
        enableReplaceButtons();
        if (matchBrackets) {
            doBracketMatch();
        }
        actions.userAction();
        
        if (oldCaretLineNumber != getLineNumberAt(caretPos))
        {
            recordEdit(true);
        }
        oldCaretLineNumber = getLineNumberAt(caretPos);
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
        try {
            int caretPos = sourcePane.getCaretPosition();
            if (caretPos != 0) {
                caretPos--;
            }
            pos = TextUtilities.findMatchingBracket(sourceDocument, caretPos);
        }
        catch (BadLocationException ble) {
            Debug.reportError("Bad document location reached while trying to match brackets");
        }
        return pos;
    }

    /**
     * delegates bracket matching to the source pane's caret
     */
    private void doBracketMatch()
    {
        moeCaret.paintMatchingBracket();
    }

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
        setTitle(title);
    }

    // --------------------------------------------------------------------

    /**
     * Return the path to the class documentation.
     */
    private String getDocPath()
    {
        return docFilename;
    }

    // --------------------------------------------------------------------

    /**
     * Gets the resource attribute of the MoeEditor object
     */
    private String getResource(String name)
    {
        return Config.getPropString(name, null, resources);
    }



    // ======================= WINDOW INITIALISATION =======================

    /**
     * Create all the Window components.
     * 
     * @param projectResolver  the entity resolver for the project. If this is null
     *   then it is assumed that this editor is for a README or other plain text file.
     */
    private void initWindow(EntityResolver projectResolver)
    {
        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            setIconImage(icon);
        }

        // prepare the content pane

        JPanel contentPane;
        if (!Config.isRaspberryPi()){
            contentPane = new GradientFillPanel(new BorderLayout(6,6));
        }else{
            contentPane = new JPanel(new BorderLayout(6,6));
        }
        
        contentPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setContentPane(contentPane);

        // create and add info and status areas

        JPanel bottomArea = new JPanel();

        // create panel for info/status
        bottomArea.setLayout(new BorderLayout(6, 1));
        if (!Config.isRaspberryPi()) bottomArea.setOpaque(false);
        
        JPanel finderPanel = new JPanel(new DBoxLayout(DBox.Y_AXIS, 0, 0));
        finderPanel.setBorder(BorderFactory.createEmptyBorder(0, BlueJTheme.componentSpacingLarge, 0, 0));
        if (!Config.isRaspberryPi()) finderPanel.setOpaque(false);
        
        int smallSpc = BlueJTheme.componentSpacingSmall;
        
        //area for new find functionality
        finder=new FindPanel(this);
        finder.setVisible(false);
        finder.setBorder(BorderFactory.createEmptyBorder(0, 0, smallSpc, 0));
        finder.setName("FinderPanel");
        finder.setAlignmentX(0.0f);
        if (!Config.isRaspberryPi()) finder.setOpaque(false);
        finderPanel.add(finder);

        replacer=new ReplacePanel(this, finder);
        replacer.setVisible(false);
        replacer.setBorder(BorderFactory.createEmptyBorder(0, 0, smallSpc, 0));
        replacer.setAlignmentX(0.0f);
        if (!Config.isRaspberryPi()) replacer.setOpaque(false);
        finderPanel.add(replacer);
        
        bottomArea.add(finderPanel, BorderLayout.NORTH);

        statusArea = new JPanel();
        statusArea.setLayout(new GridLayout(0, 1));
        // one column, many rows
        statusArea.setBackground(infoColor);
        statusArea.setBorder(BorderFactory.createLineBorder(Color.black));

        saveState = new StatusLabel(StatusLabel.SAVED);
        statusArea.add(saveState);
        //bottomArea.add(statusArea, BorderLayout.EAST);

        info = new Info();
        JPanel commentsPanel=new JPanel(new BorderLayout(6,1));
        if (!Config.isRaspberryPi()) commentsPanel.setOpaque(false);
        commentsPanel.add(info, BorderLayout.CENTER);
        commentsPanel.add(statusArea, BorderLayout.EAST);

        bottomArea.add(commentsPanel, BorderLayout.SOUTH);

        contentPane.add(bottomArea, BorderLayout.SOUTH);

        // create the text document

        if (projectResolver != null) {
            sourceDocument = new MoeSyntaxDocument(projectResolver, errorManager);
        }
        else {
            sourceDocument = new MoeSyntaxDocument();  // README file
        }
        sourceDocument.addDocumentListener(this);
        sourceDocument.addUndoableEditListener(undoManager);               

        // create the text pane

        EditorKit kit;
        if (projectResolver != null) {
            kit = new MoeSyntaxEditorKit(projectResolver, errorManager);
        }
        else {
            kit = new ReadmeEditorKit();
        }
        //MoeSyntaxEditorKit kit = new MoeSyntaxEditorKit(false, projectResolver);
        sourcePane = new MoeEditorPane();
        sourcePane.setDocument(sourceDocument);
        sourcePane.setCaretPosition(0);
        sourcePane.setMargin(new Insets(2, 0, 2, 0));
        sourcePane.setOpaque(true);
        sourcePane.setEditorKit(kit);
        moeCaret = new MoeCaret(this);
        sourcePane.setCaret(moeCaret);
        sourcePane.setBackground(MoeSyntaxDocument.getBackgroundColor());
        
        // *** Disabled due to Java bug - see http://davmac.wordpress.com/2014/05/13/javas-nimbus-look-and-feel-and-custom-keymaps/ ***
        // The Nimbus look-and-feel doesn't normally respect the background colour setting;
        // try to encourage it to do so:
        //UIDefaults defaults = new UIDefaults();
        // We can set this to anything as long it's not actually a background painter. In that case
        // Nimbus will use the background color that's been set with setBackground(...) - which is
        // what we want.
        //defaults.put("EditorPane[Enabled].backgroundPainter", versionString);
        //sourcePane.putClientProperty("Nimbus.Overrides", defaults);
        //sourcePane.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        
        sourcePane.setSelectionColor(selectionColour);
        sourcePane.setCaretColor(cursorColor);

        // default showing:
        currentTextPane = sourcePane;

        JPanel editorPane = new JPanel();
        editorPane.setLayout(new BoxLayout(editorPane, BoxLayout.X_AXIS));
        if (!Config.isRaspberryPi()) editorPane.setOpaque(false);
        scrollPane = new JScrollPane(currentTextPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        naviView = new NaviView(sourceDocument, scrollPane.getVerticalScrollBar());
        naviView.setPreferredSize(new Dimension(NAVIVIEW_WIDTH, 0));
        naviView.setMaximumSize(new Dimension(NAVIVIEW_WIDTH, Integer.MAX_VALUE));
        naviView.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        
        dividerPanel=new EditorDividerPanel(naviView, getNaviviewExpandedProperty());
        if (!Config.isRaspberryPi()) dividerPanel.setOpaque(false);
      
        editorPane.add(scrollPane);
        editorPane.add(dividerPanel);
        editorPane.add(naviView);

        contentPane.add(editorPane, BorderLayout.CENTER);

        // get table of edit actions

        actions = MoeActions.getActions(sourcePane);
        actions.setUndoEnabled(false);
        actions.setRedoEnabled(false);

        // create menubar and menus

        JMenuBar menubar = createMenuBar();
        menubar.setName("menubar");
        setJMenuBar(menubar);

        // create toolbar

        toolbar = createToolbar();
        toolbar.setName("toolbar");
        if (!Config.isRaspberryPi()) toolbar.setOpaque(false);
        contentPane.add(toolbar, BorderLayout.NORTH);
        
        //add popup menu
        
        popup= createPopupMenu();

        // add event listener to handle the window close requests

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
            @Override
            public void windowActivated(WindowEvent e) {
                checkForChangeOnDisk();
            }
        });

        setFocusTraversalPolicy(new MoeFocusTraversalPolicy());

        setWindowTitle();
        pack();
        
        // Set the size, respecting the current environment maximums.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = ge.getMaximumWindowBounds();
        int myWidth = Math.min(900, (int) maxBounds.getWidth());
        int myHeight = Math.min(700, (int) maxBounds.getHeight());
        setSize(myWidth, myHeight);
    }

    // --------------------------------------------------------------------

    /**
     * Create the editor's menu bar.
     */
    private JMenuBar createMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = null;

        String[] menuKeys = getResource("menubar").split(" ");
        for (int i = 0; i < menuKeys.length; i++) {
            menu = createMenu(menuKeys[i]);
            if (menu != null) {
                menubar.add(menu);
            }
        }
        return menubar;
    }

    // --------------------------------------------------------------------
    
    /**
     * Create the pop up menu bar
     */
    private JPopupMenu createPopupMenu()
    {
        JMenuItem menuItem;
        Action action;
        String label;
        String actionName;
        popup = new JPopupMenu();
        String [] popupKeys=getResource("popupmenu").split(" ");
        for (int i=0; i< popupKeys.length; i++){
            label = Config.getString("editor." + popupKeys[i] + LabelSuffix);
            actionName = getResource(popupKeys[i] + ActionSuffix);
            action = actions.getActionByName(actionName);
            if (action == null) {               
                Debug.message("Moe: cannot find action " + popupKeys[i]);
            }
            else {
                menuItem=new JMenuItem(action);
                menuItem.setText(label);
                popup.add(menuItem);
            }
        }      
        return popup;

    }
    
    // --------------------------------------------------------------------

    /**
     * Create a single menu for the editor's menu bar. The key for the menu (as
     * defined in moe.properties) is supplied.
     */
    private JMenu createMenu(String key)
    {
        JMenuItem item;
        String label;

        // get menu title
        JMenu menu = new JMenu(Config.getString("editor." + key + LabelSuffix));
        int mnemonic = Config.getMnemonicKey("editor." + key + LabelSuffix);
        menu.setMnemonic(mnemonic);

        // get menu definition
        String itemString = getResource(key);
        if (itemString == null) {
            Debug.message("Moe: cannot find menu definition for " + key);
            return null;
        }

        // cut menu definition into separate items
        String[] itemKeys = itemString.split(" ");

        // create menu item for each item
        for (int i = 0; i < itemKeys.length; i++) {
            if (itemKeys[i].equals("-")) {
                menu.addSeparator();
            }
            else {
                Action action = actions.getActionByName(itemKeys[i]);
                if (action == null) {
                    Debug.message("Moe: cannot find action " + itemKeys[i]);
                }
                else {
                    item = menu.add(action);
                    label = Config.getString("editor." + itemKeys[i] + LabelSuffix);
                    if (label != null) {
                        item.setText(label);
                    }
                    KeyStroke[] keys = actions.getKeyStrokesForAction(action);
                    if (keys != null) {
                        item.setAccelerator(chooseKey(keys));
                    }
                    item.setName(itemKeys[i]); 
                    if (isNonReadmeAction(itemKeys[i])){
                        item.setEnabled(sourceIsCode);
                    }
                }               
            }
        }
        return menu;
    }

    /**
     * Choose a key to use in the menu from all defined keys.
     */
    private static KeyStroke chooseKey(KeyStroke[] keys)
    {
        if (keys.length == 1) {
            return keys[0];
        }
        KeyStroke key = keys[0];
        // give preference to shortcuts using letter keys (CTRL-V, rather than F2)
        for (int i = 1; i < keys.length; i++) {
            if (keys[i].getKeyCode() >= 'A' && keys[i].getKeyCode() <= 'Z') {
                key = keys[i];
            }
        }
        return key;
    }


    /**
     * Create the toolbar.
     * 
     * @return The toolbar component, ready made.
     */
    private JComponent createToolbar()
    {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));

        String[] toolGroups = getResource("toolbar").split(" ");
        for (String group : toolGroups) {
            addToolbarGroup(toolbar, group);
        }

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(createInterfaceSelector());

        return toolbar;
    }



    // --------------------------------------------------------------------

    /**
     * Create the toolbar.
     * 
     * @return The toolbar component, ready made.
     */
    private void addToolbarGroup(JComponent toolbar, String group)
    {
        String[] toolKeys = group.split(":");
        for (int i = 0; i < toolKeys.length; i++) {
            toolbar.add(createToolbarButton(toolKeys[i]));
            if(!Config.isMacOSLeopard()) toolbar.add(Box.createHorizontalStrut(3));
        }
    }



    // --------------------------------------------------------------------

    /**
     * Create a button on the toolbar.
     * 
     * @param key  The internal key identifying the action and label
     * @param position  The position in the button group. One of "first", 
     *                  "middle", "last", "only". Only used on MacOS.
     */
    private AbstractButton createToolbarButton(String key)
    {
        final String label = Config.getString("editor." + key + LabelSuffix);
        AbstractButton button;

        String actionName = getResource(key + ActionSuffix);
        if (actionName == null) {
            actionName = key;
        }
        Action action = actions.getActionByName(actionName);
        
        if (action != null) {
            Action tbAction = new ToolbarAction(action, label);
            button = new JButton(tbAction);
        }
        else {
            button = new JButton("Unknown");
        }

        button.setName(actionName);

        if (action == null) {
            button.setEnabled(false);
            Debug.message("Moe: action not found for button " + label);
        }
        
        if (isNonReadmeAction(actionName) && !sourceIsCode){
            button.setEnabled(false);
        }

        button.setRequestFocusEnabled(false);
        // never get keyboard focus

        if (!Config.isMacOS()) {
            // on all other platforms than MacOS, the default insets needs to
            // be changed to make the buttons smaller
            Insets margin = button.getMargin();
            button.setMargin(new Insets(margin.top, 3, margin.bottom, 3));
        }
        else {
            Utility.changeToMacButton(button);
        }
        button.setFont(PrefMgr.getStandardFont());
        return button;
    }




    // --------------------------------------------------------------------

    /**
     * Create a combo box for the toolbar
     */
    private JComboBox createInterfaceSelector()
    {
        String[] choiceStrings = {implementationString, interfaceString};
        interfaceToggle = new JComboBox(choiceStrings);

        interfaceToggle.setRequestFocusEnabled(false);
        interfaceToggle.setFont(PrefMgr.getStandardFont());
        interfaceToggle.setBorder(new EmptyBorder(2, 2, 2, 2));
        interfaceToggle.setForeground(envOpColour);
        if (!Config.isRaspberryPi()) interfaceToggle.setOpaque(false);

        String actionName = "toggle-interface-view";
        Action action = actions.getActionByName(actionName);
        if (action != null) {           // should never be null...
            interfaceToggle.setAction(action);
        }
        else {
            interfaceToggle.setEnabled(false);
            Debug.message("Moe: action not found: " + actionName);
        }
        if (!sourceIsCode) {
            interfaceToggle.setEnabled(false);
        }
        return interfaceToggle;
    }

    // --------------------------------------------------------------------

    /**
     * Inner class for printing thread to allow printing to occur as a
     * background operation.
     * 
     * @author Bruce Quig
     */
    class PrintHandler
    implements Runnable
    {
        PrinterJob printJob;
        PageFormat pageFormat;
        boolean lineNumbers;
        boolean syntaxHighlighting;

        /**
         * Construct the PrintHandler.
         */
        public PrintHandler(PrinterJob pj, PageFormat format, boolean lineNumbers, boolean syntaxHighlighting)
        {
            super();
            printJob = pj;
            pageFormat = format;
            this.lineNumbers = lineNumbers;
            this.syntaxHighlighting = syntaxHighlighting;
        }

        /**
         * Implementation of Runnable interface
         */
        @Override
        public void run()
        {
            print();
        }

        /**
         * Create MoePrinter and then invoke print method
         */
        public void print()
        {
            if (printer == null) {
                printer = new MoePrinter();
            }

            // print document, using new pageformat object at present
            info.message(Config.getString("editor.info.printing"));
            if (printer.printDocument(printJob, sourceDocument, lineNumbers, syntaxHighlighting, windowTitle, printFont, pageFormat)) {
                info.message(Config.getString("editor.info.printed"));
            }
            else {
                info.message(Config.getString("editor.info.cancelled"));
            }
        }
    }

    // --------------------------------------------------------------------

    /**
     * Class for thread listening to edit changes.
     */
    class TextInsertNotifier
    implements Runnable
    {
        private DocumentEvent evt;
        private JEditorPane editorPane;

        /**
         * Sets the event attribute of the TextInsertNotifier object
         */
        public void setEvent(DocumentEvent e, JEditorPane editorPane)
        {
            evt = e;
            this.editorPane = editorPane;
        }

        /**
         * Main processing method for the TextInsertNotifier object
         */
        @Override
        public void run()
        {
            actions.textInsertAction(evt, editorPane);
        }
    }

    /**
     * Custom focus traversal implementation to make sure that the text area
     * gets and never loses focus.
     */
    class MoeFocusTraversalPolicy extends FocusTraversalPolicy
    {
        @Override
        public Component getComponentAfter(Container focusCycleRoot,  Component aComponent) {
            if (aComponent.equals(finder.getFindTField())) {
                if (replacer.isVisible()){
                    return replacer.getReplaceText();
                }
            } 
            return currentTextPane;
        }

        @Override
        public Component getComponentBefore(Container focusCycleRoot,  Component aComponent) {
            if (aComponent.equals(replacer.getReplaceText())) {
                return finder.getFindTField();
            } 
            return currentTextPane;
        }

        @Override
        public Component getDefaultComponent(Container focusCycleRoot) {
            return currentTextPane;
        }

        @Override
        public Component getFirstComponent(Container focusCycleRoot) {
            return currentTextPane;
        }

        @Override
        public Component getInitialComponent(Window window) {
            return currentTextPane;
        }

        @Override
        public Component getLastComponent(Container focusCycleRoot) {
            return currentTextPane;
        }
    }

    /**
     * An abstract action which delegates to a sub-action, and which
     * mirrors the "enabled" state of the sub-action. This allows having
     * actions with alternative labels.
     * 
     * @author Davin McCall
     */
    class ToolbarAction extends AbstractAction implements PropertyChangeListener
    {
        private Action subAction;

        public ToolbarAction(Action subAction, String label)
        {
            super(label);
            this.subAction = subAction;
            subAction.addPropertyChangeListener(this);
            setEnabled(subAction.isEnabled());
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            subAction.actionPerformed(e);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            // If the enabled state of the sub-action changed,
            // then we should change our own state.
            if (evt.getPropertyName().equals("enabled")) {
                Object newVal = evt.getNewValue();
                if (newVal instanceof Boolean) {
                    boolean state = ((Boolean) newVal).booleanValue();
                    setEnabled(state);
                }
            }
        }
    }

    /**
     * Sets the find panel to be visible and if there is a selection/or previous search 
     * it starts a automatic find of what was selected in the text/or previous search. If 
     * it is the source pane then the replace button is enabled; if it is the interface pane 
     * then the replace button and replace panel are set to disabled and invisible
     */
    public void initFindPanel()
    {
        finder.displayFindPanel(currentTextPane.getSelectedText());
        //functionality for the replace button to be enabled/disabled according to view
        if (isShowingInterface())
        {
            finder.setReplaceEnabled(false);
            replacer.setVisible(false);
        }
        else
        {
            finder.setReplaceEnabled(true);
        }
    }

    /**
     * Sets the caret forward by the value indicated if this does not 
     * exceed the document length; Else it sets it to the document length
     */
    public void setCaretPositionForward (int caretPos)
    {
        int docLength = document.getLength();
        if (currentTextPane.getCaretPosition() + caretPos <= docLength) {
            currentTextPane.setCaretPosition(currentTextPane.getCaretPosition() + caretPos);
        } else { 
            currentTextPane.setCaretPosition(docLength);
        }
    }

    /**
     * Get the source pane.
     */
    public JEditorPane getSourcePane()
    {
        return sourcePane;
    }
    
    /**
     * Get the current pane.
     */
    public JEditorPane getCurrentTextPane()
    {
        return currentTextPane;
    }    
    
    /**
     * Get the source document that this editor is editing.
     */
    @Override
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
        for (Object tag : sourceSearchHighlightTags) {
            sourcePane.getHighlighter().removeHighlight(tag);
        }
        sourceSearchHighlightTags.clear();
        
        for (Object tag : htmlSearchHighlightTags) {
            htmlPane.getHighlighter().removeHighlight(tag);
        }
        htmlSearchHighlightTags.clear();
    }
    
    /**
     * Removes the selection in the textpane specified
     * @param textPane specified textpane (source/html)
     */
    private static void removeSelection(JEditorPane textPane)
    {
        if (textPane != null) {
            textPane.setSelectionEnd(textPane.getSelectionStart());
        }
    }
    
    /**
     * Removes the selections 
     */
    public void removeSelections()
    {
        removeSelection(sourcePane);
        removeSelection(htmlPane);
    }
 
    /*
     * PopulateCompletionsWorker creates a thread that searches for code completion suggestions and populate 
     * the JList interactively.
     * This change of behaviour was introduced to improve usability on the Raspberry Pi when dealing with methods
     * with many suggestions.
     */
    class PopulateCompletionsWorker extends SwingWorker<AssistContent[], AssistContent>
    {

        CodeCompletionDisplay codeCompletionDlg;
        MoeEditor moe;
        CodeSuggestions suggests;
        LocatableToken suggestToken;
        int xpos = 0, ypos = 0;

        public PopulateCompletionsWorker(MoeEditor m, CodeSuggestions sug, LocatableToken sugT, int x, int y)
        {
            this.moe = m;
            this.suggests = sug;
            this.suggestToken = sugT;
            this.xpos = x;
            this.ypos = y;
        }

        /*
         * This method is based on the ParseUtils.getPossibleCompletions. 
         * However it has to be inside the doInBackground() method in order to populate the code completion
         * JList while still looking for completions. 
         * This change was introduced in order to improve feedback to the user when using a Raspberry Pi.
         */
        @Override
        protected AssistContent[] doInBackground() throws Exception
        {
            GenTypeClass exprType = ParseUtils.initGetPossibleCompletions(suggests);
            if (exprType != null) {
                //process queue and publish partial results.
                List<AssistContent> completions = processQueueInBackground(exprType, suggests, javadocResolver);

                return completions.toArray(new AssistContent[completions.size()]);
            }
            return null;
        }

        /*
         * This method is adpted from ParseUtils.processQueue, however the partial results are published.
         */
        private List<AssistContent> processQueueInBackground(GenTypeClass exprType, CodeSuggestions suggests,
                JavadocResolver javadocResolver)
        {
            GenTypeClass accessType = suggests.getAccessType();
            Reflective accessReflective = (accessType != null) ? accessType.getReflective() : null;

        // Use two sets, one to keep track of which types we have already processed,
            // another for individual methods.
            Set<String> contentSigs = new HashSet<String>();
            Set<String> typesDone = new HashSet<String>();
            List<AssistContent> completions = new ArrayList<AssistContent>();

            LinkedList<GenTypeClass> typeQueue = new LinkedList<GenTypeClass>();
            typeQueue.add(exprType);
            GenTypeClass origExprType = exprType;

            while (!typeQueue.isEmpty()) {
                exprType = typeQueue.removeFirst();

                if (!typesDone.add(exprType.getReflective().getName())) {
                    // we've already done this type...
                    continue;
                }
                Map<String, Set<MethodReflective>> methods = exprType.getReflective().getDeclaredMethods();
                Map<String, GenTypeParameter> typeArgs = exprType.getMap();

                for (String name : methods.keySet()) {
                    Set<MethodReflective> mset = methods.get(name);
                    for (MethodReflective method : mset) {
                        if (accessReflective != null
                                && !JavaUtils.checkMemberAccess(method.getDeclaringType(),
                                        origExprType,
                                        suggests.getAccessType().getReflective(),
                                        method.getModifiers(), suggests.isStatic())) {
                            continue;
                        }
                        AssistContent completion = ParseUtils.discoverElement(javadocResolver, contentSigs, completions, typeArgs, method);
                        if (completion != null) {
                            publish(completion);
                        }

                        for (GenTypeClass stype : exprType.getReflective().getSuperTypes()) {
                            if (typeArgs != null) {
                                typeQueue.add(stype.mapTparsToTypes(typeArgs));
                            } else {
                                typeQueue.add(stype.getErasedType());
                            }
                        }

                        Reflective outer = exprType.getReflective().getOuterClass();
                        if (outer != null) {
                            typeQueue.add(new GenTypeClass(outer));
                        }
                    }
                }
            }
            return completions;

        }

        /*
         * displays the values published by processQueueInBackground.
         */
        @Override
        protected void process(List<AssistContent> chunks)
        {
            if (chunks != null && !chunks.isEmpty()) {
                //there are elements to show
                if (codeCompletionDlg == null) {
                    AssistContent[] initialElements = chunks.toArray(new AssistContent[chunks.size()]);
                    codeCompletionDlg = new CodeCompletionDisplay(this.moe,
                            suggests.getSuggestionType().toString(false),
                            initialElements, suggestToken);
                    codeCompletionDlg.setLocation(xpos, ypos);
                    codeCompletionDlg.setVisible(true);
                    codeCompletionDlg.requestFocus();
                } else {
                    //component was already created. update it.
                    codeCompletionDlg.addElements(chunks);
                }
            }
        }

        /*
         * This method is called when processing is done.
         */
        @Override
        protected void done()
        {
            try {
                AssistContent[] result = get();
                if (result != null && result.length == 0) {
                    //set message on status bar
                    info.warning("No completions available.");
                } else {
                    //update the JList.
                    codeCompletionDlg.addElements(Arrays.asList(result));
                }
            } catch (Exception ie) {
            }
        }
        
    };
    
    /**
     * Create and pop up the content assist (code completion) dialog.
     */
    protected void createContentAssist() {
        //need to recreate the dialog each time it is pressed as the values may be different 
        CodeSuggestions suggests = sourceDocument.getParser().getExpressionType(sourcePane.getCaretPosition(),
                sourceDocument);
        LocatableToken suggestToken;
         int cpos;
        Rectangle pos;
        Point spLoc;
        int xpos = 0, ypos = 0;
        //get screen positioning too.
            cpos = sourcePane.getCaretPosition();
            try {
                pos = sourcePane.modelToView(cpos);
                spLoc = sourcePane.getLocationOnScreen();
                xpos = pos.x + spLoc.x;
                ypos = pos.y + pos.height + spLoc.y;
            } catch (BadLocationException ble) {
            }
        if (suggests != null) {
            suggestToken = suggests.getSuggestionToken();
            PopulateCompletionsWorker worker = new PopulateCompletionsWorker(this, suggests, suggestToken, xpos, ypos);
            worker.execute();
            } else {
            //no completions found. no need to search.
             info.warning("No completions available.");
             CodeCompletionDisplay codeCompletionDlg = new CodeCompletionDisplay(this,
                            null, new AssistContent[0], null);
            codeCompletionDlg.setLocation(xpos, ypos);
            codeCompletionDlg.setVisible(true);
            codeCompletionDlg.requestFocus();
        }
    }

    /**
     * Does some clever formatting to ensure that the replacement matches
     * the original on the formatting eg upper/lower case
     */
    private static String smartFormat(String original, String replacement)
    {
        if(original == null || replacement == null) {
            return replacement;
        }

        // only do smart stuff if search and replace strings were entered in lowercase.
        // check here. if not lowercase, just return.

        if( !isLowerCase(replacement) || !isLowerCase(original)) {
            return replacement;
        }
        if(isUpperCase(original)) {
            return replacement.toUpperCase();
        }
        if(isTitleCase(original)) {
            return Character.toTitleCase(replacement.charAt(0)) + 
                replacement.substring(1);
        }
        
        return replacement;
    }

    /**
     * True if the string is in lower case.
     */
    public static boolean isLowerCase(String s)
    {
        for(int i=0; i<s.length(); i++) {
            if(! Character.isLowerCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the string is in Upper case.
     */
    public static boolean isUpperCase(String s)
    {
        for(int i=0; i<s.length(); i++) {
            if(! Character.isUpperCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the string is in title case.
     */
    public static boolean isTitleCase(String s)
    {
        if(s.length() < 2) {
            return false;
        }
        return Character.isUpperCase(s.charAt(0)) &&
                Character.isLowerCase(s.charAt(1));
    }

    /**
     * Sets the find panel to be visible
     */
    public void setFindPanelVisible()
    {
        finder.setVisible(true);
    }

    /**
     * Replace all instances of the search String with a replacement.
     * -check for valid search criteria
     * -start at beginning
     * -do initial find
     * -replace until not found, no wrapping!
     * -print out number of replacements (?)
     */
    public void replaceAll(String replaceString)
    {
        //remove selection and remove highlighting 
        int caretPos = sourcePane.getCaretPosition();
        if (getSelectionBegin()!=null) {
            sourcePane.setCaretPosition(getSelectionBegin().getColumn());
        }
        removeSearchHighlights();
        String searchString = finder.getSearchString();
        boolean isMatchCase=finder.getMatchCase();
        int count = 0;
        while(doFindBackward(searchString, !isMatchCase, false)) {
            insertText(smartFormat(searchString, replaceString), true);
            count++;
        }        
        while(doFind(searchString, !isMatchCase,false)) 
        {
            insertText(smartFormat(searchString, replaceString), false);
            count++;
        }

        removeSearchHighlights();
        sourcePane.setCaretPosition(caretPos);


        if(count > 0) {
            writeMessage(Config.getString("editor.replaceAll.replaced").trim() + " " +
                    count + " " + Config.getString("editor.replaceAll.intancesOf").trim() + " " +
                    searchString);
        }
        else {
            writeMessage(Config.getString("editor.replaceAll.string").trim() + " " +
                    searchString + " " + Config.getString("editor.replaceAll.notFoundNothingReplaced"));
        }
    }

    /**
     * Sets the caret selection visible. The visibility will be persistent,
     * until the caret is repositioned.
     */
    protected void setSelectionVisible()
    {
        Caret caret = currentTextPane.getCaret();
        caret.setSelectionVisible(true);
        if (caret instanceof MoeCaret) {
            MoeCaret mcaret = (MoeCaret) caret;
            mcaret.setPersistentHighlight();
        }
    }

    /**
     * getFindSearchString() returns the search string in the find panel
     */
    protected String getFindSearchString()
    {
        return finder.getSearchString();
    }

    /**
     * Enables/disables the once and all buttons on the replace panel
     * @param enable  True to enable; false to disable
     */
    protected void enableReplaceButtons(boolean enable)
    {
        replacer.enableButtons(enable);
    }
    
    /**
     * Enables/disables the once and all buttons on the replace panel
     */
    protected void enableReplaceButtons()
    {
        replacer.enableButtons();
    }

    /**
     * When the mouse is clicked away from the selected text, 
     * the replace buttons need to be disabled
     */
    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e)
    {
        showPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        showPopup(e);
    }
    
    /**
     * Displays the popup menu, if triggered by the given mouse event
     */
    private void showPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            popup.show(e.getComponent(), e.getX(), e.getY());
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
            try {
                watcher.recordEdit(sourceDocument.getText(0, sourceDocument.getLength()), includeOneLineEdits);
            }
            catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}
