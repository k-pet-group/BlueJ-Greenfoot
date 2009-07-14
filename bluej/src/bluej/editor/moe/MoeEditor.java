/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.syntax.jedit.tokenmarker.JavaTokenMarker;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.editor.EditorWatcher;
import bluej.editor.LineColumn;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;

/**
 * Moe is the editor of the BlueJ environment. This class is the main class of
 * this editor and implements the top-level functionality.
 * 
 * MoeEditor implements the Editor interface, which defines the interface to the
 * rest of the BlueJ system.
 * 
 * @author Michael Kolling
 * @author Bruce Quig
 * @author Damiano Bolla
 */

public final class MoeEditor extends JFrame
    implements bluej.editor.Editor, BlueJEventListener, HyperlinkListener, DocumentListener
{
    // -------- CONSTANTS --------

    // version number
    final static int version = 252;
    final static String versionString = "2.5.2";

    // colours
    final static Color cursorColor = new Color(255, 0, 100);                 // cursor

    final static Color frameBgColor = new Color(175, 175, 175);
    final static Color infoColor = new Color(240, 240, 240);
    final static Color lightGrey = new Color(224, 224, 224);
    final static Color selectionColour = Config.getSelectionColour();
    final static Color titleCol = Config.getItemColour("colour.text.fg");
    final static Color envOpColour = Config.getItemColour("colour.menu.environOp");

    // Icons
    final static Image iconImage = Config.getImageAsIcon("image.icon.editor").getImage();

    // Fonts
    public static int printFontSize = Config.getPropInteger("bluej.fontsize.printText", 10);
    public static Font printFont = new Font("Monospaced", Font.PLAIN, printFontSize);

    // Strings
    String implementationString = Config.getString("editor.implementationLabel");
    String interfaceString = Config.getString("editor.interfaceLabel");

    // suffixes for resources
    final static String LabelSuffix = "Label";
    final static String ActionSuffix = "Action";
    final static String TooltipSuffix = "Tooltip";
    final static String AcceleratorSuffix = "Accelerator";

    // file suffixes
    final static String CRASHFILE_SUFFIX = "#";
    final static String BACKUP_SUFFIX = "~";

    final static String spaces = "    ";

    final static String COMPILED = "compiled";

    private static boolean matchBrackets = false;

    // -------- INSTANCE VARIABLES --------

    private EditorWatcher watcher;
    private Properties resources;

    private AbstractDocument document;
    private MoeSyntaxDocument sourceDocument;
    private HTMLDocument htmlDocument;

    private MoeActions actions;
    public MoeUndoManager undoManager;

    JEditorPane currentTextPane;            // text component currently dislayed
    private JEditorPane sourcePane;         // the component holding the source text

    private JEditorPane htmlPane;           // the component holding the javadoc html
    private MoeCaret moeCaret;

    private Info info;                      // the info number label
    private JPanel statusArea;              // the status area
    private StatusLabel saveState;          // the status label
    private JComboBox interfaceToggle;
    private GoToLineDialog goToLineDialog;

    private JScrollPane scrollPane;
    private JComponent toolbar;             // The toolbar

    private String filename;                // name of file or null
    private long lastModified;              // time of last modification of file
    private String windowTitle;             // title of editor window
    private String docFilename;             // path to javadoc html file

    private boolean sourceIsCode;           // true if current buffer is code
    private boolean viewingHTML;

    private int currentStepPos;             // position of step mark (or -1)
    private boolean mayHaveBreakpoints;     // true if there were BP here
    private boolean ignoreChanges = false;
    private boolean tabsAreExpanded = false;

    private MoePrinter printer;

    private TextInsertNotifier doTextInsert = new TextInsertNotifier();
    
    //list of actions that are dis/enabled depending on the view
    private ArrayList<String> flaggedActions;

   
	/**
     * Property map, allows BlueJ extensions to assosciate property values with
     * this editor instance; otherwise unused.
     */
    private HashMap<String,Object> propertyMap = new HashMap<String,Object>();

    
    /**
     * Constructor. Title may be null
     */
    public MoeEditor(String title, boolean isCode, EditorWatcher watcher, boolean showToolbar, 
                     boolean showLineNum, Properties resources)
    {
        super("Moe");
        this.watcher = watcher;
        this.resources = resources;
        
        filename = null;
        windowTitle = title;
        sourceIsCode = isCode;
        viewingHTML = false;
        currentStepPos = -1;
        mayHaveBreakpoints = false;
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);
        undoManager = new MoeUndoManager(this);

        initWindow();
    }

    // --------------------------------------------------------------------

    /**
     * Update the state of controls bound to "undo".
     */
    public void updateUndoControls()
    {
        //actions.setUndoEnabled(undoManager.canUndo());
    	boolean canUndo=false;
    	if (undoManager.canUndo())
    		canUndo=true;
    	displayMenuItem("undo", canUndo);
    	displayToolbarItem("undo", canUndo);
    }

    /**
     * Update the state of controls bound to "redo".
     */
    public void updateRedoControls()
    {
        //actions.setRedoEnabled(undoManager.canRedo());
    	boolean canRedo=false;
    	if (undoManager.canRedo())
    		canRedo=true;
    	displayMenuItem("redo", canRedo);
    	displayToolbarItem("redo", canRedo);
    }
    
    /**
     * Load the file "filename" and show the editor window.
     */
    public boolean showFile(String filename, boolean compiled,       // inherited from Editor, redefined
                            String docFilename, Rectangle bounds)
    {
        this.filename = filename;
        this.docFilename = docFilename;

        if (bounds != null) {
            setBounds(bounds);
        }

        boolean loaded = false;
        boolean readError = false;

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

                FileReader reader = new FileReader(filename);
                sourcePane.read(reader, null);
                reader.close();
                File file = new File(filename);
                lastModified = file.lastModified();

                sourceDocument = (MoeSyntaxDocument) sourcePane.getDocument();

                // set TokenMarker for syntax highlighting if desired
                checkSyntaxStatus();

                sourceDocument.addDocumentListener(this);
                sourceDocument.addUndoableEditListener(undoManager);
                document = sourceDocument;
                loaded = true;
            }
            catch (FileNotFoundException ex) {
                clear();
            }
            catch (IOException ex) {
                readError = true;
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

        if (!loaded)             // should exist, but didn't
            return false;

        if (loaded)
            info.message(Config.getString("editor.info.version") + " " + versionString);
        else if (readError)
            info.warning(Config.getString("editor.info.readingProblem"), 
                         Config.getString("editor.info.regularFile"));
        else
            info.message(Config.getString("editor.info.version" + versionString), 
                         Config.getString("editor.info.newFile"));

        setWindowTitle();
        sourcePane.setFont(PrefMgr.getStandardEditorFont());
        sourcePane.setSelectionColor(selectionColour);

        setCompileStatus(compiled);

        return true;
    }

    /**
     * Reload the editor content from the associated file, discarding unsaved
     * edits.
     */
    public void reloadFile()       // inherited from Editor, redefined
    {
        doReload();
    }

    /**
     * Wipe out contents of the editor.
     */
    public void clear()       // inherited from Editor, redefined
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
    public void insertText(String text, boolean caretBack)       // inherited from Editor, redefined
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
    public void setVisible(boolean vis)       // inherited from Editor, redefined
    {
        if (vis) {
            sourcePane.setFont(PrefMgr.getStandardEditorFont());
            checkSyntaxStatus();
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
    public void refresh()       // inherited from Editor, redefined
    {
        sourcePane.setFont(PrefMgr.getStandardEditorFont());
        checkBracketStatus();
        checkSyntaxStatus();
        currentTextPane.repaint();
    }


    /**
     * Save the buffer to disk under current filename. This is often called from
     * the outside - just in case. Save only if really necessary, otherwise we
     * save much too often. PRE: filename != null
     */
    public void save()       // inherited from Editor, redefined
        throws IOException
    {
        IOException failureException = null;
        if (saveState.isChanged()) {
            BufferedWriter writer = null;
            try {
                // The crash file is used during writing and will remain in
                // case of a crash during the write operation. The backup
                // file always contains the last version.
                String crashFilename = filename + CRASHFILE_SUFFIX;
                String backupFilename = filename + BACKUP_SUFFIX;

                // make a backup to the crash file
                FileUtility.copyFile(filename, crashFilename);

                writer = new BufferedWriter(new FileWriter(filename));
                sourcePane.write(writer);
                writer.close();
                setSaved();
                File file = new File(filename);
                lastModified = file.lastModified();

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
    public void close()       // inherited from Editor, redefined
    {
        try {
            save();
        }
        catch (IOException ioe) {}
        // temporary - should really be done by watcher from outside
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
    public void displayMessage(String message, int lineNumber, int column, boolean beep, 
                               boolean setStepMark, String help)        // inherited from Editor
    {
        switchToSourceView();

        Element line = getLine(lineNumber);
        int pos = line.getStartOffset();

        if (setStepMark) {
            setStepMark(pos);
        }

        // highlight the line

        sourcePane.setCaretPosition(pos);
        sourcePane.moveCaretPosition(line.getEndOffset() - 1);
        moeCaret.setPersistentHighlight();
        // w/o line break

        // display the message

        if (beep)
            info.warning(message);
        else
            info.message(message);

        if (help != null)
            info.setHelp(help);
    }

    /**
     * Set the selection of the editor to be a len characters on the line
     * lineNumber, starting with column columnNumber
     * 
     * @param lineNumber  the line to select characters on
     * @param columnNumber  the column to start selection at (1st column is 1 - not 0)
     * @param len         the number of characters to select
     */
    public void setSelection(int lineNumber, int columnNumber, int len)
    {
        Element line = getLine(lineNumber);

        sourcePane.select(line.getStartOffset() + columnNumber - 1, 
                               line.getStartOffset() + columnNumber + len - 1);
    }

    /**
     * Select a specified area of text.
     * 
     * @param lineNumber1  The new selection value
     * @param columnNumber1  The new selection value
     * @param lineNumber2  The new selection value
     * @param columnNumber2  The new selection value
     */
    public void setSelection(int lineNumber1, int columnNumber1, int lineNumber2, int columnNumber2)
    {
        /*
         * if (lineNumber2 < lineNumber1) return; if (lineNumber2 == lineNumber1 &&
         * (columnNumber2 < columnNumber1)) return;
         */
        Element line1 = getLine(lineNumber1);
        Element line2 = getLine(lineNumber2);

        sourcePane.select(line1.getStartOffset() + columnNumber1 - 1, line2.getStartOffset() + columnNumber2 - 1);
    }

    /**
     * Get the text currently selected.
     * 
     * @return The selected text.
     */
    public String getSelectedText()
    {
        return sourcePane.getSelectedText();
    }

    /**
     * Remove the step mark (the mark that shows the current line when
     * single-stepping through code). If it is not currently displayed, do
     * nothing.
     */
    public void removeStepMark()        // inherited from Editor
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
    public void changeName(String title, String filename, String docFilename)
    {
        this.filename = filename;
        this.docFilename = docFilename;
        // error ## - need to add full path
        windowTitle = title;
        setWindowTitle();
    }

    /**
     * Set the "compiled" status
     * 
     * @param compiled  True if the class has been compiled.
     */
    public void setCompiled(boolean compiled)
    {
        setCompileStatus(compiled);
        if (compiled) {
            info.message(Config.getString("editor.info.compiled"));
        }
    }

    /**
     * Called when all the breakpoints have been cleared. The editor should
     * update its display to show that no breakpoints are set.
     */
    public void removeBreakpoints()
    {
        // This may be a callback in response to a modification event.
        // If we try to remove breakpoints during the modification notification,
        // AbstractDocument throws an exception.
        EventQueue.invokeLater(new Runnable() {
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
    public void reInitBreakpoints()
    {
        if (mayHaveBreakpoints) {
            mayHaveBreakpoints = false;
            for (int i = 1; i <= numberOfLines(); i++) {
                if (lineHasBreakpoint(i)) {
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
    public boolean isModified()        // inherited from Editor
    {
        return (saveState.isChanged());
    }

    /**
     * Set this editor to read-only.
     * 
     * @param readOnly  The new readOnly value
     */
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
    public boolean isShowingInterface()
    {
        return viewingHTML;
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
     * Returns the current caret location within the edited text.
     * 
     * @return An object describing the current caret location.
     */
    public LineColumn getCaretLocation()
    {
        int caretOffset = sourcePane.getCaretPosition();
        return getLineColumnFromOffset(caretOffset);
    }

    /**
     * Returns the LineColumn object from the given offset in the text.
     * 
     * @param offset  The number of characters from the beginning of text (startng
     *                from zero)
     * @return the LineColumn object or null if the offset points outside the
     *         text.
     */
    public LineColumn getLineColumnFromOffset(int offset)
    {
        int lineNumber = sourceDocument.getDefaultRootElement().getElementIndex(offset);

        if (lineNumber < 0) {
            return null;
        }

        Element lineElement = getLineAt(offset);
        int column = offset - lineElement.getStartOffset();

        if (column < 0) {
            return null;
        }

        return new LineColumn(lineNumber, column);
    }

    /**
     * Sets the current Caret location within the edited text.
     * 
     * @param location  The location in the text to set the Caret to.
     * @throws IllegalArgumentException
     *             if the specified TextLocation represents a position which
     *             does not exist in the text.
     */
    public void setCaretLocation(LineColumn location)
    {
        sourcePane.setCaretPosition(getOffsetFromLineColumn(location));
    }

    /**
     * Returns the location where the current selection begins.
     * 
     * @return the current beginning of the selection or null if no text is
     *         selected.
     */
    public LineColumn getSelectionBegin()
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
     * Returns the location where the current selection ends.
     * 
     * @return the current end of the selection or null if no text is selected.
     */
    public LineColumn getSelectionEnd()
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
     * Returns the text which lies between the two LineColumn.
     * 
     * @param begin  The beginning of the text to get
     * @param end    The end of the text to get
     * @return  The text between the 'begin' and 'end' positions.
     * @throws IllegalArgumentException
     *             if either of the specified TextLocations represent a position
     *             which does not exist in the text.
     */
    public String getText(LineColumn begin, LineColumn end)
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
     *             if either of the specified LineColumn represent a position
     *             which does not exist in the text.
     * @throws BadLocationException
     *             if internally the text points outside a location in the text.
     */
    public void setText(LineColumn begin, LineColumn end, String newText)
        throws BadLocationException
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
     * Request to the editor to mark the text between begin and end as selected.
     * 
     * @param begin  The start position of the selection
     * @param end  The end position of the selection
     * @throws IllegalArgumentException
     *             if either of the specified TextLocations represent a position
     *             which does not exist in the text.
     */
    public void setSelection(LineColumn begin, LineColumn end)
    {
        int start = getOffsetFromLineColumn(begin);
        int finish = getOffsetFromLineColumn(end);

        int selectionStart = Math.min(start, finish);
        int selectionEnd = Math.max(start, finish);

        sourcePane.setCaretPosition(selectionStart);
        sourcePane.moveCaretPosition(selectionEnd);
    }

    /**
     * Translates a LineColumn into an offset into the text held by the editor.
     * 
     * @param location  position to be translated
     * @return the offset into the content of this editor
     * @throws IllegalArgumentException
     *             if the specified LineColumn represent a position which does
     *             not exist in the text.
     */
    public int getOffsetFromLineColumn(LineColumn location)
    {
        if (location.getLine() < 0) {
            throw new IllegalArgumentException("line < 0");
        }

        Element lineElement = sourceDocument.getDefaultRootElement()
            .getElement(location.getLine());
        if (lineElement == null) {
            throw new IllegalArgumentException("line=" + location.getLine()
                    + " is out of bound");
        }

        int lineOffset = lineElement.getStartOffset();

        if (location.getColumn() < 0) {
            throw new IllegalArgumentException("column < 0 ");
        }

        int lineLen = lineElement.getEndOffset() - lineOffset;

        if (location.getColumn() >= lineLen) {
            throw new IllegalArgumentException("column=" + location.getColumn() + " greater than line len=" + lineLen);
        }

        return lineOffset + location.getColumn();
    }

    /**
     * Returns a property of the current editor.
     *
     * @param  propertyKey  The propertyKey of the property to retrieve.
     * @return              the property value or null if it is not found
     */
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
     * Returns the length of the data.  This is the number of
     * characters of content that represents the users data.
     *
     * It is possible to obtain the line and column of the last character of text by using
     * the getLineColumnFromOffset() method.
     *
     * @return the length >= 0
     */
    public int getTextLength ()
    {
        return sourceDocument.getLength();
    }
    
    /**
     * Return the number of lines in the documant.
     */
    public int numberOfLines()
    {
        return sourceDocument.getDefaultRootElement().getElementCount();
    }
    

    // --------------------------------------------------------------------
    // ------------ end of interface inherited from Editor ----------------
    // --------------------------------------------------------------------

    // ---- BlueJEventListener interface ----

    /**
     * A BlueJEvent was raised. Check whether it is one that we're interested
     * in.
     */
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
    public void insertUpdate(DocumentEvent e)
    {
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
        doTextInsert.setEvent(e, sourcePane);
        SwingUtilities.invokeLater(doTextInsert);
    }

    /**
     * A text removal has taken place.
     */
    public void removeUpdate(DocumentEvent e)
    {
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
    }

    /**
     * Document properties have changed - ignore
     */
    public void changedUpdate(DocumentEvent e)
    {}

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
    public void print(PrinterJob printerJob)
    {
        PrintHandler pt = new PrintHandler(printerJob, getPageFormat(printerJob));
        pt.print();
    }
    
    /**
     * Return a validated version of the global PageFormat for BlueJ
     */
    public PageFormat getPageFormat(PrinterJob job)
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
        // create a printjob
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.printDialog()) {
            PrintHandler pt = new PrintHandler(job, getPageFormat(job));
            Thread printJobThread = new Thread(pt);
            printJobThread.setPriority((Thread.currentThread().getPriority() - 1));
            printJobThread.start();
        }
    }

    // --------------------------------------------------------------------
    /**
     * Implementation of the "page setup" user function. This provides a dialog
     * for print page setup. PageSetup is global to BlueJ. Calling this from the 
     * Editor is effectively the same as calling from PkgMgrFrame as this saves 
     * back to PkgMgrFrame's global page format object.
     */
    public void pageSetup()
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
            watcher.closeEvent(this);
        }
    }

    // --------------------------------------------------------------------
    /**
     * Check whether TABs need expanding in this editor. If they do, return
     * true. At the same time, set this flag to true.
     * 
     * @return Description of the Return Value
     */
    public boolean checkExpandTabs()
    {
        if (tabsAreExpanded)
            return false;

        else {
            tabsAreExpanded = true;
            return true;
        }
    }

    // --------------------------------------------------------------------
    /**
     * Implementation of "find" user function.
     */
    public void find()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        finder.show(this, currentTextPane.getSelectedText(), false);
    }

    // --------------------------------------------------------------------
    /**
     * Implementation of "replace" user function. Replace adds extra
     * functionality to that of a find dialog, as well as altered behaviour. It
     * can remain open for multiple functions.
     */
    public void replace()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        finder.show(this, currentTextPane.getSelectedText(), true);
    }

    // --------------------------------------------------------------------
    /**
     * Implementation of "find-next" user function.
     */
    public void findNext()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        String s = currentTextPane.getSelectedText();
        if (s == null) {
            s = finder.getSearchString();
            if (s == null) {
                info.warning(DialogManager.getMessage("no-search-string"));
                return;
            }
        }
        findNextString(finder, s, false);
    }

    // --------------------------------------------------------------------
    /**
     * Implementation of "find-next-reverse" user function.
     */
    public void findNextBackward()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        String s = currentTextPane.getSelectedText();
        if (s == null) {
            s = finder.getSearchString();
            if (s == null) {
                info.warning(DialogManager.getMessage("no-search-string"));
                return;
            }
        }
        findNextString(finder, s, true);
    }

    // --------------------------------------------------------------------
    
    /**
     * Finds the first cvs-style conflict and selects it
     */
    public void findFirstConflict(){
    	setCaretLocation(new LineColumn(0,0));
    	findNextConflict();
    }
    
    private void findNextConflict(){
    	findString("=======", false, false, true, false);
    	findString("<<<<<<<", true, false, false, false);
    	LineColumn startPos = getCaretLocation();
    	findString(">>>>>>>", false, false, false, false);
    	LineColumn endPos = getCaretLocation();
    	setSelection(startPos, endPos);
    }
    
    /**
     * Do a find with info in the info area.
     */
    private void findNextString(Finder finder, String s, boolean backward)
    {
        boolean found = findString(s, backward, finder.getIgnoreCase(), 
                                   finder.getWholeWord(), (!finder.getSearchFound()));

        finder.setSearchString(s);
        finder.setSearchFound(found);
    }

    // --------------------------------------------------------------------
    /**
     * Do a find with info in the info area.
     */
    boolean findString(String s, boolean backward, boolean ignoreCase, 
                       boolean wholeWord, boolean wrap)
    {
        if (s.length() == 0) {
            info.warning(Config.getString("editor.info.emptySearchString"));
            return false;
        }
        
        boolean found;
        if (backward)
            found = doFindBackward(s, ignoreCase, wholeWord, wrap);
        else
            found = doFind(s, ignoreCase, wholeWord, wrap);
        
        StringBuffer msg = new StringBuffer(Config.getString("editor.find.find.label") + " ");
        msg.append(backward ? Config.getString("editor.find.backward") : Config.getString("editor.find.forward"));
        if (ignoreCase || wholeWord || wrap)
            msg.append(" (");

        if (ignoreCase)
        	msg.append(Config.getString("editor.find.ignoreCase").toLowerCase() + ", ");
        
        if (wholeWord)
        	msg.append(Config.getString("editor.find.wholeWord").toLowerCase() + ", ");
        
        if (wrap) 
        	msg.append(Config.getString("editor.find.wrapAround").toLowerCase() + ", ");
        
        if (ignoreCase || wholeWord || wrap) 
            msg.replace(msg.length() - 2, msg.length(), "): ");
        else 
            msg.append(": ");
        
        msg.append(s);

        if (found)
            info.message(msg.toString());
        else
            info.warning(msg.toString(), Config.getString("editor.info.notFound"));

        return found;
    }

    // --------------------------------------------------------------------
    /**
     * doFind - do a find without visible feedback. Returns false if not found.
     */
    boolean doFind(String s, boolean ignoreCase, boolean wholeWord, boolean wrap)
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
                    int foundPos = findSubstring(lineText, s, ignoreCase, wholeWord, false);
                    if (foundPos != -1) {
                        currentTextPane.select(start + foundPos, start + foundPos + s.length());
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
            Debug.message("error in editor find operation");
        }
        return found;
    }

    // --------------------------------------------------------------------
    /**
     * doFindBackward - do a find backwards without visible feedback. Returns
     * false if not found.
     */
    boolean doFindBackward(String s, boolean ignoreCase, boolean wholeWord, boolean wrap)
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
                    int foundPos = findSubstring(lineText, s, ignoreCase, wholeWord, true);
                    if (foundPos != -1) {
                        currentTextPane.select(lineStart + foundPos, lineStart + foundPos + s.length());
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
            Debug.reportError("error in editor find operation");
            ex.printStackTrace();
        }
        return found;
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
     * Find the position of a substring in a given string, ignoring case or searching for
     * whole words if desired. Return the position of the substring or -1.
     *
     * @param  text        the full string to be searched
     * @param  sub         the substring that we're looking for
     * @param  ignoreCase  if true, case is ignored
     * @param  wholeWord   if true, and the search string resembles something like a word,
     *                    find only whole-word ocurrences
     * @param  backwards   Description of the Parameter
     * @return             Description of the Return Value
     * @returns            the index of the substring, or -1 if not found
     */
    private int findSubstring(String text, String sub, boolean ignoreCase, 
                              boolean wholeWord, boolean backwards)
    {
        int strlen = text.length();
        int sublen = sub.length();

        if (sublen == 0) {
            return -1;
        }

        // 'wholeWord' search does not make much sense when the search string is
        // not a word
        // (ar at least the first and last character is a letter). Check that.
        if (!Character.isJavaIdentifierPart(sub.charAt(0)) || !Character.isJavaIdentifierPart(sub.charAt(sublen - 1))) {
            wholeWord = false;
        }

        boolean found = false;
        int pos = (backwards ? strlen - sublen : 0);
        boolean itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));

        while (!found && !itsOver) {
            found = text.regionMatches(ignoreCase, pos, sub, 0, sublen);
            if (found && wholeWord) {
                found = ((pos == 0) || !Character.isJavaIdentifierPart(text.charAt(pos - 1)))
                        && ((pos + sublen >= strlen) || !Character.isJavaIdentifierPart(text.charAt(pos + sublen)));
            }
            if (!found) {
                pos = (backwards ? pos - 1 : pos + 1);
                itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));
            }
        }
        if (found) {
            return pos;
        }
        else {
            return -1;
        }
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

        if (interfaceToggle.getSelectedIndex() == 0)
            interfaceToggle.setSelectedIndex(1);
        else
            interfaceToggle.setSelectedIndex(0);
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
    
    private boolean isFlaggedAction(String text){
    	
    	ArrayList flaggedActions=getFlaggedActions();
    	
    	if (flaggedActions!=null && flaggedActions.contains(text)){
    		return true;
    	}
    	return false;
    
    }
    
    /**
     * Returns a list of flagged items 
     *  
     * @return ArrayList list of flagged items
     */
    
    private ArrayList getFlaggedActions(){
    	
    	if (flaggedActions==null){
    		flaggedActions=new ArrayList<String>();
    		flaggedActions.add("save");
    		flaggedActions.add("reload");
    		flaggedActions.add("print");
    		flaggedActions.add("page-setup");
    		flaggedActions.add("compile");
    		flaggedActions.add("cut-to-clipboard");
    		flaggedActions.add("indent-block");
    		flaggedActions.add("deindent-block");
    		flaggedActions.add("comment-block");
    		flaggedActions.add("uncomment-block");
    		flaggedActions.add("insert-method");
    		flaggedActions.add("find");
    		flaggedActions.add("find-next");
    		flaggedActions.add("find-next-backward");
    		flaggedActions.add("replace");
    		flaggedActions.add("go-to-line");
    		flaggedActions.add("paste-from-clipboard");
    		flaggedActions.add("toggle-breakpoint");

    	
    	}
    	
    	
    	return flaggedActions;
    
    }
    // --------------------------------------------------------------------
    /**
     * Switch on the source view (it it isn't showing already).
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
        checkSyntaxStatus();
        currentTextPane.requestFocus();
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
        try {
            File urlFile = new File(getDocPath());
            URL myURL = urlFile.toURI().toURL();
            htmlPane.setPage(myURL);
            htmlDocument = (HTMLDocument) htmlPane.getDocument();
            htmlDocument.setBase(myURL);
            info.message(Config.getString("editor.info.docLoaded"));
        }
        catch (Exception exc) {
            info.warning(Config.getString("editor.info.docDisappeared"), getDocPath());
            Debug.reportError("loading class interface failed: " + exc);
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
    private void resetMenuToolbar(boolean sourceView){
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
     * This method changes the display of the menubar based on the interface that is selected
     * 
     * @param sourceView true if called from sourceView setup; false from documentation View setup
     */
    
    private void displayMenubar(boolean sourceView){
    	    	
    	JMenuBar menuBar=(JMenuBar) getJMenuBar(); 
    	JMenu menu=null;
    	Component[] menubarComponent = menuBar.getComponents();
    	for (int i=0;i<menubarComponent.length; i++ ){
    			if (menubarComponent[i] instanceof JMenu){
    				menu=(JMenu)menubarComponent[i]; 
    				for (int j=0; j<menu.getMenuComponentCount(); j++){
    					if (menu.getMenuComponent(j) instanceof JMenuItem){
    						if (isFlaggedAction(((JMenuItem)menu.getMenuComponent(j)).getName())){   				
    							((JMenuItem)menu.getMenuComponent(j)).setEnabled(sourceView);
    						}
        				}
    						
    				}
    				
    				
    			}
    				
    	}
    }
    
    /**
     * This method changes the display of the toolbar based on the interface that is selected
     * 
     * @param sourceView true if called from sourceView setup; false from documentation View setup
     */
    
    private void displayToolbar(boolean sourceView){
    	
    	JPanel toolbar=null;
    	Component contentPaneItem;
    	JButton actionButton;
    	Component[] c = getContentPane().getComponents();
    	for (int i=0;i<c.length; i++ ){
    		contentPaneItem=c[i];
    		if(contentPaneItem.getName()!=null && contentPaneItem.getName().equals("toolbar")) 
    			toolbar=(JPanel)contentPaneItem;
    	}
    
    	if (toolbar==null)
    		return;
    	Component[] toolbarComponent = toolbar.getComponents();
    	for (int i=0;i<toolbarComponent.length; i++ ){
    		if (toolbarComponent[i] instanceof JButton){   				
    			actionButton=(JButton)toolbarComponent[i];
    			if (isFlaggedAction(actionButton.getName())){
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
    
    
    /*
     * Finds the menu item specified and enables or disables
     */
    private void displayMenuItem(String itemName, boolean sourceView){
    	    	
    	JMenuBar menuBar=(JMenuBar) getJMenuBar(); 
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
    
    private void displayToolbarItem(String itemName, boolean sourceView){
    	
    	JPanel toolbar=null;
    	Component contentPaneItem;
    	Component[] c = getContentPane().getComponents();
    	for (int i=0;i<c.length; i++ ){
    		contentPaneItem=c[i];
    		if(contentPaneItem.getName()!=null && contentPaneItem.getName().equals("toolbar")) 
    			toolbar=(JPanel)contentPaneItem;
    	}
    
    	if (toolbar==null)
    		return;
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
            watcher.generateDoc();
        }

        document = htmlDocument;
        currentTextPane = htmlPane;
        viewingHTML = true;
        scrollPane.setViewportView(htmlPane);
        currentTextPane.requestFocus();
    }

    // --------------------------------------------------------------------
    /**
     */
    public void createHTMLPane()
    {
        htmlPane = new JEditorPane();
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(this);
        htmlPane.setInputMap(JComponent.WHEN_FOCUSED, new InputMap() {
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

    // --------------------------------------------------------------------
    /**
     * Toggle a breakpoint at a given position.
     */
    public void toggleBreakpoint(int pos)
    {
        if (positionHasBreakpoint(pos))
            setUnsetBreakpoint(pos, false);        // remove
        else
            setUnsetBreakpoint(pos, true);         // set
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
                    doRemoveBreakpoint(getPositionInLine(i));
                }
            }
            mayHaveBreakpoints = false;
        }
    }

    // --------------------------------------------------------------------
    /**
     * Check weather a position has a breakpoint set
     */
    private boolean positionHasBreakpoint(int pos)
    {
        Element line = getLineAt(pos);
        return Boolean.TRUE.equals(line.getAttributes().getAttribute(MoeSyntaxView.BREAKPOINT));
    }

    // --------------------------------------------------------------------
    /**
     * Check weather a line has a breakpoint set
     */
    private boolean lineHasBreakpoint(int lineNo)
    {
        Element line = getLine(lineNo);
        return (Boolean.TRUE.equals(line.getAttributes().getAttribute(MoeSyntaxView.BREAKPOINT)));
    }

    // --------------------------------------------------------------------
    /**
     * Try to set or remove a breakpoint (depending on the parameter) at the
     * given position. Informs the watcher.
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

    // --------------------------------------------------------------------
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
     * return a boolean representing whether in source editing view
     */
    private boolean viewingCode()
    {
        return sourceIsCode && (!viewingHTML);
    }


    // --------------------------------------------------------------------
    /**
     * Return the current line.
     */
    //    private Element getCurrentLine()
    //    {
    //        return document.getParagraphElement(currentTextPane.getCaretPosition());
    //    }

    // --------------------------------------------------------------------
    /**
     * Find and return a line by line number
     */
    private Element getLine(int lineNo)
    {
        return sourceDocument.getDefaultRootElement().getElement(lineNo - 1);
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a line by text position
     */
    private Element getLineAt(int pos)
    {
        return sourceDocument.getParagraphElement(pos);
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a position in a line.
     */
    private int getPositionInLine(int lineNo)
    {
        return getLine(lineNo).getStartOffset();
    }

    // --------------------------------------------------------------------
    /**
     * Return the number of the current line.
     */
    //    private int getCurrentLineNo()
    //    {
    //        return document.getDefaultRootElement().getElementIndex(
    //                                   currentTextPane.getCaretPosition()) + 1;
    //    }

    // --------------------------------------------------------------------
    /**
     * Return the number of the line containing position 'pos'.
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
        FileReader reader = null;
        try {
            reader = new FileReader(filename);
            sourcePane.read(reader, null);
            reader.close();
            File file = new File(filename);
            lastModified = file.lastModified();

            sourceDocument = (MoeSyntaxDocument) sourcePane.getDocument();
            
            // flag document type as a java file by associating a
            // JavaTokenMarker for syntax colouring if specified
            checkSyntaxStatus();
            sourceDocument.addDocumentListener(this);
            sourceDocument.addUndoableEditListener(undoManager);
            
            // We want to inform the watcher that the editor content has changed,
            // and then inform it that we are in "saved" state (synced with file).
            // But first set state to saved to avoid unnecessary writes to disk.
            saveState.setState(StatusLabel.SAVED);
            setChanged(); // contents may have changed - notify watcher
            setSaved();  // notify watcher that we are saved
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
                if (reader != null)
                    reader.close();
            }
            catch (IOException ioe) {}
        }
    }

    // --------------------------------------------------------------------
    /**
     * Checks that current status of syntax highlighting option is consistent
     * with desired option eg off/on.
     */
    private void checkSyntaxStatus()
    {
        if (sourceDocument != null) {

            // flag document type as a java file by associating a
            // JavaTokenMarker for syntax colouring if specified
            if (viewingCode() && PrefMgr.getFlag(PrefMgr.HILIGHTING)) {
                if (sourceDocument.getTokenMarker() == null) {
                    sourceDocument.setTokenMarker(new JavaTokenMarker());
                }
            }
            else {
                sourceDocument.setTokenMarker(null);
            }
        }
        // else ??
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
        if (matchBrackets)
            doBracketMatch();
        else
            moeCaret.removeBracket();
    }

    /**
     * Tell whether we are currently matching brackets.
     * 
     * @return True, if we are matching brackets, otherwise false.
     */
    public boolean matchBrackets()
    {
        return matchBrackets;
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

    // --------------------------------------------------------------------
    /**
     * Clear the message in the info area.
     */
    void caretMoved()
    {
        clearMessage();
        if (matchBrackets) {
            doBracketMatch();
        }
        actions.userAction();
    }

    /**
     * returns the position of the matching bracket for the source pane's
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
        Caret caret = sourcePane.getCaret();
        if (caret instanceof MoeCaret) {
            ((MoeCaret) caret).paintMatchingBracket();
        }
    }

    /**
     * Set the window title to show the defined title, or else the file name.
     */
    private void setWindowTitle()
    {
        String title = windowTitle;

        if (title == null) {
            if (filename == null)
                title = "Moe:  <no name>";
            else
                title = "Moe:  " + filename;
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

    // --------------------------------------------------------------------
    /**
     * Create all the Window components
     */
    private void initWindow()
    {
        setIconImage(iconImage);

        // prepare the content pane

        JPanel contentPane = new JPanel(new BorderLayout(6,6));
        contentPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        //contentPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        //contentPane.setBackground(frameBgColor);
        setContentPane(contentPane);

        // create and add info and status areas

        JPanel bottomArea = new JPanel();
        
        // create panel for info/status
        bottomArea.setLayout(new BorderLayout(6, 1));
        //bottomArea.setBackground(frameBgColor);
        
        info = new Info();
        bottomArea.add(info, BorderLayout.CENTER);

        statusArea = new JPanel();
        statusArea.setLayout(new GridLayout(0, 1));
        // one column, many rows
        statusArea.setBackground(infoColor);
        statusArea.setBorder(BorderFactory.createLineBorder(Color.black));

        saveState = new StatusLabel(StatusLabel.SAVED);
        statusArea.add(saveState);
        bottomArea.add(statusArea, BorderLayout.EAST);

        contentPane.add(bottomArea, BorderLayout.SOUTH);

        // create the text document

        sourceDocument = new MoeSyntaxDocument();
        sourceDocument.addDocumentListener(this);
        sourceDocument.addUndoableEditListener(undoManager);

        // create the text pane

        MoeSyntaxEditorKit kit = new MoeSyntaxEditorKit(false);
        sourcePane = new MoeEditorPane();

        sourcePane.setDocument(sourceDocument);
        sourcePane.setCaretPosition(0);
        sourcePane.setMargin(new Insets(2, 2, 2, 2));
        sourcePane.setOpaque(true);
        sourcePane.setEditorKit(kit);
        moeCaret = new MoeCaret(this);
        sourcePane.setCaret(moeCaret);
        sourcePane.setBackground(MoeSyntaxDocument.getBackgroundColor());
        //        sourcePane.setSelectionColor(selectionColour);
        sourcePane.setCaretColor(cursorColor);

        // default showing:
        currentTextPane = sourcePane;
        
        scrollPane = new JScrollPane(currentTextPane);
        scrollPane.setPreferredSize(new Dimension(598, 400));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // get table of edit actions

        actions = MoeActions.getActions(sourcePane);
        actions.setUndoEnabled(false);
        actions.setRedoEnabled(false);

        // **** temporary: disable all unimplemented actions ****
        actions.getActionByName("show-manual").setEnabled(false);
        // ****

        // create menubar and menus

        JMenuBar menubar = createMenuBar();
        setJMenuBar(menubar);

        // create toolbar

        toolbar = createToolbar();
        toolbar.setName("toolbar");
        contentPane.add(toolbar, BorderLayout.NORTH);

        // add event listener to handle the window close requests

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
            public void windowActivated(WindowEvent e) {
                checkForChangeOnDisk();
            }
        });

        setFocusTraversalPolicy(new MoeFocusTraversalPolicy());
        
        setWindowTitle();
        pack();
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
     * Create a single menu for the editor's menu bar. The key for the menu (as
     * defined in moe.properties) is supplied.
     */
    private JMenu createMenu(String key)
    {
        JMenuItem item;
        String label;

        // get menu title
        JMenu menu = new JMenu(Config.getString("editor." + key + LabelSuffix));

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
                }
            }
        }
        return menu;
    }

    /**
     * Choose a key to use in the menu from all defined keys.
     */
    private KeyStroke chooseKey(KeyStroke[] keys)
    {
        if (keys.length == 1) {
            return keys[0];
        }
        else {
            KeyStroke key = keys[0];
            // give preference to shortcuts using letter keys (CTRL-V, rather
            // than F2)
            for (int i = 1; i < keys.length; i++) {
                if (keys[i].getKeyCode() >= 'A' && keys[i].getKeyCode() <= 'Z') {
                    key = keys[i];
                }
            }
            return key;
        }
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
        //toolbar.setBackground(frameBgColor);

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
     * @param key  The internal key identifyting the action and label
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
        Action tbAction = new ToolbarAction(action, label);
        
        
        button = new JButton(tbAction);
        button.setName(actionName);
        
        if (action == null) {
            button.setEnabled(false);
            Debug.message("Moe: action not found for button " + label);
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

        /**
         * Construct the PrintHandler.
         */
        public PrintHandler(PrinterJob pj, PageFormat format)
        {
            super();
            printJob = pj;
            pageFormat = format;
        }

        /**
         * Implementation of Runnable interface
         */
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
            if (printer.printDocument(printJob, sourceDocument, windowTitle, printFont, pageFormat)) {
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
        public Component getComponentAfter(Container focusCycleRoot,  Component aComponent) {
            return currentTextPane;
        }

        public Component getComponentBefore(Container focusCycleRoot,  Component aComponent) {
            return currentTextPane;
        }

        public Component getDefaultComponent(Container focusCycleRoot) {
            return currentTextPane;
        }

        public Component getFirstComponent(Container focusCycleRoot) {
            return currentTextPane;
        }

        public Component getInitialComponent(Window window) {
            return currentTextPane;
        }

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
        
        public void actionPerformed(ActionEvent e)
        {
            subAction.actionPerformed(e);
        }
        
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
}
