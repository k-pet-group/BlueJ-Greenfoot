// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.editor.EditorWatcher;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.editor.moe.autocomplete.*;


import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model


import javax.swing.*;		// all the GUI components
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.net.URL;
import java.awt.print.*;
import org.gjt.sp.jedit.syntax.*; // Syntax highlighting package




/**
 * Moe is the editor of the BlueJ environment. This class is the main class of this 
 * editor and implements the top-level functionality.
 *
 * MoeEditor implements the Editor interface, which defines the interface to the
 * rest of the BlueJ system.
 *
 * @author Michael Kolling
 * @author Bruce Quig
 */

public final class MoeEditor extends JFrame
    implements bluej.editor.Editor, BlueJEventListener, HyperlinkListener,
               DocumentListener
{
    // -------- CONSTANTS --------

    // version number
    static final int version = 140;
    static final String versionString = "1.4";

    // colours
    static final Color textColor = new Color(0,0,0);		// normal text
    static final Color textBgColor = Config.getItemColour("colour.text.bg"); // background
    static final Color cursorColor = new Color(255,0,100);	// cursor

    static final Color frameBgColor = new Color(196, 196, 196);
    static final Color infoColor = new Color(240, 240, 240);
    static final Color lightGrey = new Color(224, 224, 224);
    static final Color selectionColour = new Color(204, 204, 204);
    static final Color titleCol = Config.getItemColour("colour.text.fg");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    // Icons
    static final Image iconImage =
        Config.getImageAsIcon("image.icon.editor").getImage();

    // Fonts
    public static int printFontSize = Config.getDefaultPropInteger("bluej.fontsize.printText", 10);
    public static Font printFont = new Font("Monospaced", Font.PLAIN, printFontSize);

    // Strings
    String implementationString = Config.getString("editor.implementationLabel");
    String interfaceString = Config.getString("editor.interfaceLabel");

    // suffixes for resources
    static final String LabelSuffix = "Label";
    static final String ActionSuffix = "Action";
    static final String TooltipSuffix = "Tooltip";
    static final String AcceleratorSuffix = "Accelerator";

    // file suffixes
    static final String CRASHFILE_SUFFIX = "#";
    static final String BACKUP_SUFFIX = "~";

    //  width of tag area for setting breakpoints
    static final short TAG_WIDTH = 14;

    static final String spaces = "    ";

    // Attributes for lines and document
    public static final String BREAKPOINT = "break";
    public static final String STEPMARK = "step";
    static final String COMPILED = "compiled";

    // PageFormat object for printing page format
    private static PageFormat pageFormat = PkgMgrFrame.getPageFormat();
    
    private static boolean matchBrackets = false; 


    // -------- INSTANCE VARIABLES --------

    private EditorWatcher watcher;
    private Properties resources;

    private AbstractDocument document;
    private MoeSyntaxDocument sourceDocument;
    private HTMLDocument htmlDocument;

    private MoeActions actions;

    
    JEditorPane currentTextPane;  // text component currently dislayed
    private JEditorPane sourcePane;	// the component holding the source text
    
    private JEditorPane htmlPane;	// the component holding the javadoc html
    private MoeCaret moeCaret;

    private Info info;	            // the info number label
    private JPanel statusArea;	    // the status area
    private StatusLabel saveState;  // the status label
    private JComboBox interfaceToggle;
    private GoToLineDialog goToLineDialog;

    private JScrollPane scrollPane;
    private JComponent toolbar;	    // The toolbar

    private String filename;        // name of file or null
    private String windowTitle;	    // title of editor window
    private boolean firstSave;      // true if never been saved
    private boolean isCompiled;	    // true when source has been compiled
    private String docFilename;     // path to javadoc html file

    private String newline;	        // the line break character used
    private boolean sourceIsCode;   // true if current buffer is code
    private boolean viewingHTML;

    private int currentStepPos;         // position of step mark (or -1)
    private boolean mayHaveBreakpoints;	// true if there were BP here
    private boolean ignoreChanges = false;
    private boolean tabsAreExpanded = false;
   
    private MoePrinter printer;

    private TextInsertNotifier doTextInsert = new TextInsertNotifier();

    private MoeAutocompleteManager moeAutocompleteManager = null;
    private ClassLoader projectClassLoader;
    
    // =========================== NESTED CLASSES ===========================

    // inner class for listening for undoable edits in text

    private class MoeUndoableEditListener implements UndoableEditListener {

        public void undoableEditHappened(UndoableEditEvent e)
        {
            actions.undoManager.addEdit(e.getEdit());
            actions.undoAction.update();
            actions.redoAction.update();
        }
    }


    /**
     *  Inner class listening for disabling actions - if an action is
     *  disabled (enabled), the connected button is disabled (enabled)
     *  as well.
     */
    private class ActionChangeListener implements PropertyChangeListener {
        JButton button;

        ActionChangeListener(JButton b) {
            super();
            button = b;
        }

        public void propertyChange(PropertyChangeEvent e)
        {
            if (e.getPropertyName().equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                button.setEnabled(enabledState.booleanValue());
            }
        }
    }

    // =========================== PUBLIC METHODS ===========================

    /**
     *  Constructor. Title may be null
     */

    public MoeEditor(String title, boolean isCode, EditorWatcher watcher,
                     boolean showToolbar, boolean showLineNum, Properties resources,
                     ClassLoader aProjectClassLoader)
    {
        super("Moe");
        this.watcher = watcher;
        this.resources = resources;
        filename = null;
        windowTitle = title;
        firstSave = true;
        isCompiled = false;
        newline = System.getProperty("line.separator");
        sourceIsCode = isCode;
        viewingHTML = false;
        currentStepPos = -1;
        mayHaveBreakpoints = false;
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);
        projectClassLoader = aProjectClassLoader;

        initWindow(showToolbar);
    }

    // --------------------------------------------------------------------
    /**
     * Returns the projectClassLoader.
     * Can return null if no classloader is available.
     */
    public ClassLoader getProjectClassLoader ()
    {
        return projectClassLoader;
    }
    
    /**
     *  Load the file "filename" and show the editor window.
     */
    public boolean showFile(String filename, boolean compiled, String docFilename)
                            // inherited from Editor, redefined
    {
        this.filename = filename;
        this.docFilename = docFilename;

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
                    crashFile.renameTo(backupFile);
                    DialogManager.showMessage(this, "editor-crashed");
                }

                FileReader reader = new FileReader(filename);
                sourcePane.read(reader, null);
                reader.close();

                sourceDocument = (MoeSyntaxDocument)sourcePane.getDocument();

                // set TokenMarker for syntax highlighting if desired
                checkSyntaxStatus();

                sourceDocument.addDocumentListener(this);
                sourceDocument.addUndoableEditListener(new MoeUndoableEditListener());
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

        if (! loaded)  // should exist, but didn't
            return false;

        //     if (loaded) ## NYI
        //       if (newFile.canWrite()) {		// have write permission
        // 	  save_state = Saved;
        // 	  statusLabel.setText("saved");
        //       }
        //       else {
        // 	  save_state = ReadOnly;
        // 	  statusLabel.setText("read only");
        //       }
        //     else
        //       save_state = Saved;

        if (loaded)
            info.message (Config.getString("editor.info.version") + " " +
                          versionString);
        else if (readError)
            info.warning (Config.getString("editor.info.readingProblem"),
                          Config.getString("editor.info.regularFile"));
        else
            info.message (Config.getString("editor.info.version" +
                                           versionString),
                          Config.getString("editor.info.newFile"));

        setWindowTitle();
        sourcePane.setFont(PrefMgr.getStandardEditorFont());
        sourcePane.setSelectionColor(selectionColour);

        setCompileStatus(compiled);

        return true;
    } // showFile

    // --------------------------------------------------------------------

    public void reloadFile() // inherited from Editor, redefined
    {
        doReload();
    }

    // --------------------------------------------------------------------
    /**
     *  Wipe out contents of the editor.
     */

    public void clear()	// inherited from Editor, redefined
    {
        ignoreChanges = true;
        sourcePane.setText("");
        ignoreChanges = false;
    }

    // --------------------------------------------------------------------
    /**
     *  Insert a string into the buffer. The editor is not immediately
     *  redisplayed. This function is typically used in a sequence
     *  "clear; [insertText]*; setVisible(true)". If the selection is on,
     *  it is replaced by the new text.
     *
     *  @param text         the text to be inserted
     *  @param caretBack    move the caret to the beginning of the inserted text
     */

    public void insertText(String text, boolean caretBack)
    // inherited from Editor, redefined
    {
        sourcePane.replaceSelection(text);
        if(caretBack)
            sourcePane.setCaretPosition(sourcePane.getCaretPosition()-text.length());
    }

    // --------------------------------------------------------------------
    /**
     *  Show the editor window. This includes whatever is necessary of the
     *  following: make visible, de-iconify, bring to front of window stack.
     */
    public void setVisible(boolean vis)  // inherited from Editor, redefined
    {
        if(vis) {

            //Use of the singleton pattern so that each MoeEditor
            //only has one MoeAutoCompleteManager. Damiano
            if(moeAutocompleteManager==null){
                moeAutocompleteManager
                    = new MoeAutocompleteManager(new File(filename),
                                                 this,
                                                 sourcePane);    
            }



        
            currentTextPane.setFont(PrefMgr.getStandardEditorFont());
            checkSyntaxStatus();
            checkBracketStatus();
            setState(Frame.NORMAL);  // de-iconify
            toFront();               // window to front
        }
        super.setVisible(vis);		// show the window
    }

    // --------------------------------------------------------------------
    /**
     *  Refresh the editor window.
     */
    public void refresh()	// inherited from Editor, redefined
    {
        currentTextPane.setFont(PrefMgr.getStandardEditorFont());
        checkBracketStatus();
        checkSyntaxStatus();
        currentTextPane.repaint();
    }

    // --------------------------------------------------------------------
    /**
     *  True is the editor is on screen.
     */
    public boolean isShowing()	// inherited from Editor, redefined
    {
        if(isVisible() != super.isShowing())
            Debug.message("isVisible is not isShowing!");
        return super.isShowing();
    }


    // --------------------------------------------------------------------
    /**
     *  Save the buffer to disk under current filename.  This is often called
     *  from the outside - just in case.  Save only if really necessary,
     *  otherwise we save much too often.
     *  PRE: filename != null
     */
    public void save() // inherited from Editor
    {
        if (saveState.isChanged()) {

            try {
                // The crash file is used during writing and will remain in
                // case of a crash during the write operation. The backup
                // file always contains the last version.
                String crashFilename = filename + CRASHFILE_SUFFIX;
                String backupFilename = filename + BACKUP_SUFFIX;

                // make a backup to the crash file
                FileUtility.copyFile(filename, crashFilename);

                BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                sourcePane.write(writer);
                writer.close();
                setSaved();

                if(PrefMgr.getFlag(PrefMgr.MAKE_BACKUP)) {
                    // if all went well, rename the crash file as a normal backup
                    File crashFile = new File(crashFilename);
                    File backupFile = new File(backupFilename);
                    crashFile.renameTo(backupFile);
                }
                else {
                    File crashFile = new File(crashFilename);
                    crashFile.delete();
                }
            }
            catch (IOException ex) {
                info.warning (Config.getString("editor.info.errorSaving"));
            }
        }
    }

    // --------------------------------------------------------------------
    /**
     *  The editor wants to close. Do this through the EditorManager so that
     *  we can be removed from the list of open editors.
     */
    public void close()	// inherited from Editor
    {
        save(); // temporary - should really be done by watcher from outside
        doClose();
    }

    // --------------------------------------------------------------------
    /**
     *  Display a message (used for compile/runtime errors). An editor
     *  must support at least two lines of message text, so the message
     *  can contain a newline character.
     *
     *  @param message	the message to be displayed
     *  @param line		the line to move the cursor to (the line is
     *			also highlighted)
     *  @param column		the column to move the cursor to
     *  @param beep		if true, do a system beep
     *  @param setStepMark	if true, set step mark (for single stepping)
     *  @param help		name of help group (may be null)
     */
    public void displayMessage(String message, int lineNumber, int column,
                               boolean beep, boolean setStepMark,
                               String help)
                               // inherited from Editor
    {
        switchToSourceView();

        Element line = getLine(lineNumber);
        int pos = line.getStartOffset();

        if(setStepMark)
            setStepMark(pos);

        // highlight the line

        currentTextPane.setCaretPosition(pos);
        currentTextPane.moveCaretPosition(line.getEndOffset()-1); // w/o line break

        // display the message

        if (beep)
            info.warning (message);
        else
            info.message (message);

        if (help != null)
            info.setHelp(help);
    }

    /**
     * Set the selection of the editor to be a len characters on the
     * line lineNumber, starting with column columnNumber
     *
     * @param lineNumber the line to select characters on
     * @param columnNumber the column to start selection at (1st column is 1 - not 0)
     * @param len the number of characters to select
     */
    public void setSelection(int lineNumber, int columnNumber, int len)
    {
        Element line = getLine (lineNumber);

        currentTextPane.select(line.getStartOffset()+columnNumber-1,
                               line.getStartOffset()+columnNumber+len-1);
    }

    public void setSelection(int lineNumber1, int columnNumber1,
                                 int lineNumber2, int columnNumber2)
    {
/*        if (lineNumber2 < lineNumber1)
            return;
        if (lineNumber2 == lineNumber1 && (columnNumber2 < columnNumber1))
            return;*/

        Element line1 = getLine (lineNumber1);
        Element line2 = getLine (lineNumber2);

        currentTextPane.select(line1.getStartOffset()+columnNumber1-1,
                               line2.getStartOffset()+columnNumber2-1);
    }


    /**
     * Get the text currently selected. (currently not needed)
     */
     public String getSelectedText()
     {
         return currentTextPane.getSelectedText();
     }

    // --------------------------------------------------------------------
    /**
     *  Remove the step mark (the mark that shows the current line when
     *  single-stepping through code). If it is not currently displayed,
     *  do nothing.
     */
    public void removeStepMark()		// inherited from Editor
    {
        if(currentStepPos != -1) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttribute(STEPMARK, Boolean.FALSE);
            sourceDocument.setParagraphAttributes(currentStepPos, 0, a, false);
            currentStepPos = -1;
            // force an update of UI
            repaint();
        }
        // remove highlight as well
        sourcePane.setCaretPosition(sourcePane.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     *  Change class name.
     *
     *  @param title	new window title
     *  @param filename	new file name
     */
    public void changeName(String title, String filename)
    // inherited from Editor
    {
        this.filename = filename;		// error ## - need to add full path
        windowTitle = title;
        setWindowTitle();
    }

    // --------------------------------------------------------------------
    /**
     *  Set the "compiled" status
     *
     *  @param compiled	true if the class has been compiled
     */
    public void setCompiled(boolean compiled)
    {
        setCompileStatus(compiled);
        if (compiled)
            info.message(Config.getString("editor.info.compiled"));
    }

    // --------------------------------------------------------------------
    /**
     *  Remove all breakpoints in this editor.
     */
    public void removeBreakpoints()
    {
        clearAllBreakpoints();
    }

    // --------------------------------------------------------------------
    /**
     *  Determine whether this buffer has been modified.
     *  @returns	a boolean indicating whether the file is modified
     */
    public boolean isModified()	// inherited from Editor, redefined
    {
        return (saveState.isChanged());
    }

    // --------------------------------------------------------------------
    /**
     *  Set this editor to read-only.
     */
    public void setReadOnly(boolean readOnlyStatus) {
        if (readOnlyStatus) {
            saveState.setState(StatusLabel.READONLY);
            actions.undoManager.discardAllEdits();
            actions.undoAction.update();
            actions.redoAction.update();
        }
        currentTextPane.setEditable(!readOnlyStatus);
    }

    // --------------------------------------------------------------------
    // ------------ end of interface inherited from Editor ----------------
    // --------------------------------------------------------------------

    // ---- BlueJEventListener interface ----

    /**
     *  A BlueJEvent was raised. Check whether it is one that we're interested
     *  in.
     */
    public void blueJEvent(int eventId, Object arg)
    {
        switch(eventId) {
        case BlueJEvent.DOCU_GENERATED:
            BlueJEvent.removeListener(this);
            displayInterface(true);
            break;
        case BlueJEvent.DOCU_ABORTED:
            BlueJEvent.removeListener(this);
            info.warning (Config.getString("editor.info.docAborted"));
            break;
        }
    }

    // -------- three methods from DocumentListener: --------

    // insert into document
    public void insertUpdate(DocumentEvent e)
    {
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
        doTextInsert.setEvent(e);
        SwingUtilities.invokeLater(doTextInsert);
    }

    // remove from document
    public void removeUpdate(DocumentEvent e)
    {
        if (!saveState.isChanged()) {
            saveState.setState(StatusLabel.CHANGED);
            setChanged();
        }
        actions.userAction();
    }

    // document (properties?) changed - ignore
    public void changedUpdate(DocumentEvent e) {}

    // --------------------------------------------------------------------
    /**
     *  Clear the message in the info area.
     */
    public void clearMessage()
    {
        info.clear();
    }

    // --------------------------------------------------------------------
    /**
     *  Write a message into the info area.
     */
    public void writeMessage(String msg)
    {
        info.message(msg);
    }

    /**
     *  Write a warning message into the info area.
     * Typically some form of unexpected behaviour has occurred.
     */
    public void writeWarningMessage(String msg)
    {
        info.warning(msg);
    }


    // ==================== USER ACTION IMPLEMENTATIONS ===================

    // --------------------------------------------------------------------
    /**
     *
     */
    public void userSave()
    {
        if (saveState.isSaved())
            info.message (Config.getString("editor.info.noChanges"));
        else
            save();
    }

    // --------------------------------------------------------------------
    /**
     *
     */
    public void reload()
    {
        if (filename == null) {
            info.warning (Config.getString("editor.info.cannotReload"),
                          Config.getString("editor.info.reload"));
        }
        else if (saveState.isChanged()) {
            int answer = DialogManager.askQuestion(this, "really-reload");
            if (answer == 0)
                doReload();
        }
        else
            doReload();
    }

    // --------------------------------------------------------------------
   
    /**
     * Prints source code from Editor
     * 
     * @param printerJob A PrinterJob to print to.
     */
    public void print(PrinterJob printerJob)
    {
        PrintHandler pt = new PrintHandler(printerJob);
        pt.print();
    }
   
    /**
     * Generalised version of print function.  This is what is typically 
     * called when print is initiated from within the source code editor
     * menu.  This sets up and runs the print process as a separate lower 
     * priority thread.
     */
    public void print()
    {
        // create a printjob
        PrinterJob job = PrinterJob.getPrinterJob(); 

        // make sure the pageformat is ok
        pageFormat = job.validatePage(pageFormat);
        if(job.printDialog()) {
            PrintHandler pt = new PrintHandler(job);
            Thread printJobThread = new Thread(pt);
            printJobThread.setPriority((Thread.currentThread().getPriority() - 1));
            printJobThread.start();
        }
    }
    
        

    // --------------------------------------------------------------------
    /**
     * Implementation of the "page setup" user function.
     * This provides a dialog for print page setup.  
     */
    public void pageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = job.pageDialog(PkgMgrFrame.getPageFormat());
        PkgMgrFrame.setPageFormat(pageFormat);
    }


    // --------------------------------------------------------------------
    /**
     *  The editor has been closed. Hide the editor window now.
     */
    public void doClose()
    {
        setVisible(false);
        if (watcher != null)
            watcher.closeEvent(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Check whether TABs need expanding in this editor. If they
     *  do, return true. At the same time, set this flag to false.
     */
    public boolean checkExpandTabs()
    {
        if(tabsAreExpanded)
            return false;
        else {
            tabsAreExpanded = true;
            return true;
        }
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find" user function.
     */
    public void find()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        finder.show(this, currentTextPane.getSelectedText(), false);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "replace" user function.
     *  Replace adds extra functionality to that of a find dialog,
     *  as well as altered behaviour.  It can remain open for multiple
     *  functions.
     */
    public void replace()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        finder.show(this, currentTextPane.getSelectedText(), true);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-next" user function.
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
     *  Implementation of "find-next-reverse" user function.
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
     *   Do a find with info in the info area.
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
     *   Do a find with info in the info area.
     */
    boolean findString(String s, boolean backward, boolean ignoreCase,
                       boolean wholeWord, boolean wrap)
    {
        if (s.length()==0) {
            info.warning(Config.getString("editor.info.emptySearchString"));
            return false;
        }
        boolean found;
        if(backward)
            found = doFindBackward(s, ignoreCase, wholeWord, wrap);
        else
            found = doFind(s, ignoreCase, wholeWord, wrap);
        StringBuffer msg = new StringBuffer("Find ");
        msg.append((backward ? "backward" : "forward"));
        if(ignoreCase || wholeWord || wrap)
            msg.append(" (");
        if(ignoreCase)
            msg.append("ignore case, ");
        if(wholeWord)
            msg.append("whole word, ");
        if(wrap)
            msg.append("wrap around, ");
        if(ignoreCase || wholeWord || wrap)
            msg.replace(msg.length()-2, msg.length(), "): ");
        else
            msg.append(": ");
        msg.append(s);

        if(found)
            info.message(msg.toString());
        else
            info.warning(msg.toString(), Config.getString("editor.info.notFound"));
        return found;
    }

    // --------------------------------------------------------------------
    /**
     *  doFind - do a find without visible feedback. Returns false if not found.
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
                if(lineText != null && lineText.length() > 0) {
                    int foundPos = findSubstring(lineText, s, ignoreCase, wholeWord, false);
                    if (foundPos != -1) {
                        currentTextPane.select(start+foundPos, start+foundPos+s.length());
                        found = true;
                    }
                }
                if (lineEnd >= endPos) {
                    if (wrap) {   // do the wrapping
                        endPos = startPosition;
                        line = document.getParagraphElement(0);
                        start = line.getStartOffset();
                        lineEnd = Math.min(line.getEndOffset(), endPos);
                        wrap = false;  // don't wrap again
                    }
                    else
                        finished = true;
                }
                else {	// go to next line
                    line = document.getParagraphElement(lineEnd+1);
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
     *  doFindBackward - do a find backwards without visible feedback.
     *   Returns false if not found.
     */
    boolean doFindBackward(String s, boolean ignoreCase, boolean wholeWord,
                           boolean wrap)
    {
        int docLength = document.getLength();
        int startPosition = currentTextPane.getCaretPosition() - 1;
        if(startPosition < 0)
            startPosition = docLength;
        int endPos = 0;		// where the search ends

        boolean found = false;
        boolean finished = false;

        int start = startPosition;	// start of next partial search
        Element line = getLineAt(start);
        int lineStart = Math.max(line.getStartOffset(), endPos);

        try {
            while (!found && !finished) {
                String lineText = document.getText(lineStart, start-lineStart);
                if(lineText != null && lineText.length() > 0) {
                    int foundPos = findSubstring(lineText, s, ignoreCase, wholeWord, true);
                    if (foundPos != -1) {
                        currentTextPane.select(lineStart+foundPos,
                                               lineStart+foundPos+s.length());
                        found = true;
                    }
                }
                if (lineStart <= endPos) {	// reached end of search
                    if (wrap) {   // do the wrapping around
                        endPos = startPosition;
                        line = document.getParagraphElement(docLength);
                        start = line.getEndOffset();
                        lineStart = Math.max(line.getStartOffset(), endPos);
                        wrap = false;  // don't wrap again
                    }
                    else
                        finished = true;
                }
                else {	// go to next line
                    line = document.getParagraphElement(lineStart-1);
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
        if(goToLineDialog == null)
           goToLineDialog = new GoToLineDialog(this); 
           
        DialogManager.centreDialog(goToLineDialog);
        goToLineDialog.showDialog(numberOfLines());
        int newPosition = goToLineDialog.getLineNumber();
        if(newPosition > 0)
            setSelection(newPosition, 1, 0);
    }

    /**
     * Find the position of a substring in a given string, ignoring case or searching for
     * whole words if desired. Return the position of the substring or -1.
     *
     * @param text  the full string to be searched
     * @param sub   the substring that we're looking for
     * @param ignoreCase  if true, case is ignored
     * @param wholeWord   if true, and the search string resembles something like a word,
     *                    find only whole-word ocurrences
     * @returns     the index of the substring, or -1 if not found
     */
    private int findSubstring(String text, String sub, boolean ignoreCase,
                              boolean wholeWord, boolean backwards)
    {
        int strlen = text.length();
        int sublen = sub.length();

        if(sublen == 0)
            return -1;

        // 'wholeWord' search does not make much sense when the search string is not a word
        // (ar at least the first and last character is a letter). Check that.
        if(!Character.isJavaIdentifierPart(sub.charAt(0))
               || !Character.isJavaIdentifierPart(sub.charAt(sublen-1)))
            wholeWord = false;

        boolean found = false;
        int pos = (backwards ? strlen-sublen : 0);
        boolean itsOver = (backwards ? (pos < 0) : (pos+sublen > strlen));

        while(!found && !itsOver) {
            found = text.regionMatches(ignoreCase, pos, sub, 0, sublen);
            if(found && wholeWord) {
                found = ((pos == 0) || !Character.isJavaIdentifierPart(text.charAt(pos-1))) &&
                        ((pos+sublen >= strlen) ||
                           !Character.isJavaIdentifierPart(text.charAt(pos+sublen)));
            }
            if(!found) {
                pos = (backwards ? pos-1 : pos+1 );
                itsOver = (backwards ? (pos < 0) : (pos+sublen > strlen));
            }
        }
        if(found)
            return pos;
        else
            return -1;
    }

    // --------------------------------------------------------------------
    /**
     *
     */
    public void setFontSize(int size)
    {
        MutableAttributeSet attr = new SimpleAttributeSet();
        //bq StyleConstants.setFontSize(attr, size);
        int start = document.getStartPosition().getOffset();
        int length = document.getEndPosition().getOffset() - start;
        //bq document.setCharacterAttributes(start, length, attr, false);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "compile" user function.
     */
    public void compile()
    {
        if (watcher == null)
            return;
        if (!viewingCode()) {
            info.warning(" ");
            return;
        }

        info.message (Config.getString("editor.info.compiling"));
        watcher.compile(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Toggle the interface popup menu. This is used when using keys
     *  to toggle the interface view. Toggling the menu will result in
     *  invoking the action.
     */
    public void toggleInterfaceMenu()
    {
        if(!sourceIsCode)
            return;
        if(interfaceToggle.getSelectedIndex() == 0)
            interfaceToggle.setSelectedIndex(1);
        else
            interfaceToggle.setSelectedIndex(0);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "toggle-interface-view" user function. The menu
     *  has already been changed - now see what it is and do it.
     */
    public void toggleInterface()
    {
        if(!sourceIsCode)
            return;
        boolean wantHTML = (interfaceToggle.getSelectedItem() == interfaceString);
        if(wantHTML && !viewingHTML)
            switchToInterfaceView();
        else if(!wantHTML && viewingHTML)
            switchToSourceView();
    }
    
    
	/**
	 * Allow the enabling/disabling of print menu option.
     * Added to disable the printing og javadoc html for the time 
     * being until until implemented.  (This is reliant on the use 
     * of j2sdk1.4 and Java Unified Print Service implementation JSR 6)
	 * @param flag true to enable printing from menu.
	 */
    public void enablePrinting(boolean flag)
    {
        Action printAction = actions.getActionByName("print");
        if(printAction != null)
            printAction.setEnabled(flag);
         Action pageSetupAction = actions.getActionByName("page-setup");
        if(pageSetupAction != null)
            pageSetupAction.setEnabled(flag);        
        
    }

    // --------------------------------------------------------------------
    /**
     *  Switch on the source view (it it isn't showing already).
     */
    private void switchToSourceView()
    {
        if(!viewingHTML)
            return;

        // enable print option
        enablePrinting(true);
        document = sourceDocument;
        currentTextPane = sourcePane;
        viewingHTML = false;
        scrollPane.setViewportView(currentTextPane);
        checkSyntaxStatus();
        currentTextPane.requestFocus();
    }

    // --------------------------------------------------------------------
    /**
     *  Switch on the javadoc interface view (it it isn't showing already).
     *  If necessary, generate it first.
     */
    private void switchToInterfaceView()
    {
        if(viewingHTML)
            return;
            
        // disable print menu option until implemented
        enablePrinting(false);
        save();
        if(docUpToDate()) {
            displayInterface(false);
        }
        else {  // interface needs to be re-generated
            info.message(Config.getString("editor.info.generatingDoc"));
            BlueJEvent.addListener(this);
            //DocuGenerator.generateClassDocu(filename);
            watcher.generateDoc();
        }
    }

    // --------------------------------------------------------------------
    /**
     * Check whether javadoc file is up to date.
     */
    private boolean docUpToDate()
    {
        try {
            File src = new File(filename);
            File doc = new File(docFilename);

            if(!doc.exists()
               || (src.exists() && (src.lastModified() > doc.lastModified())))
                return false;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------
    /**
     * We want to display the interface view. We have checked (or waited)
     * that the html file is available. It is there now, ready to be displayed.
     * Display it.
     *
     * Don't call this directly to switch to the interface view. Call
     * switchToInterfaceView() instead.
     */
    private void displayInterface(boolean reload)
    {
        info.message(Config.getString("editor.info.loadingDoc"));

        // start the call in a separate thread to allow fast return to GUI.
        Thread loadThread = new HTMLDisplayThread(reload);
        //loadThread.setPriority(Thread.MIN_PRIORITY);
        loadThread.start();
    }

    // --------------------------------------------------------------------
    /**
     *
     */
    public void createHTMLPane()
    {
        htmlPane = new JEditorPane();
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(this);
    }

    // --------------------------------------------------------------------
    /**
     *  A hyperlink was activated in the document. Do something appropriate.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        info.clear();
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
                HTMLDocument doc = (HTMLDocument)pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else {
                try {
                    pane.setPage(e.getURL());
                } catch (Throwable t) {
                    info.warning("cannot display hyperlink: " + e.getURL());
                    Debug.reportError("hyperlink failed: " + t);
                }
            }
        }
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "toggle-breakpoint" user function.
     */

    public void toggleBreakpoint()
    {
        if (!viewingCode()) {
            info.warning (" ");  // cause a beep
            return;
        }
        toggleBreakpoint(sourcePane.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     *  Toggle a breakpoint at a given position.
     */
    public void toggleBreakpoint(int pos)
    {
        if (positionHasBreakpoint(pos))
            setUnsetBreakpoint(pos, false);  // remove
        else
            setUnsetBreakpoint(pos, true);  // set

    }

    // --------------------------------------------------------------------
    /**
     *   Clear all known breakpoints.
     */
    private void clearAllBreakpoints()
    {
        if(mayHaveBreakpoints) {

            for(int i = 1; i <= numberOfLines(); i++) {
                if(lineHasBreakpoint(i))
                    doRemoveBreakpoint(getPositionInLine(i));
            }
            mayHaveBreakpoints = false;
        }
    }

    // --------------------------------------------------------------------
    /**
     *  Check weather a position has a breakpoint set
     */
    private boolean positionHasBreakpoint(int pos)
    {
        Element line = getLineAt(pos);
        boolean temp = (Boolean.TRUE.equals(line.getAttributes().getAttribute(BREAKPOINT)));
        //Debug.message("positionHasBreakpoint: " + temp);
        return temp;
    }

    // --------------------------------------------------------------------
    /**
     *  Check weather a line has a breakpoint set
     */
    private boolean lineHasBreakpoint(int lineNo)
    {
        Element line = getLine(lineNo);
        return (Boolean.TRUE.equals(
                                    line.getAttributes().getAttribute(BREAKPOINT)));
    }

    // --------------------------------------------------------------------
    /**
     *  Try to set or remove a breakpoint (depending on the parameter) at
     *  the given position. Informs the watcher.
     */
    private void setUnsetBreakpoint(int pos, boolean set)
    {
        if (watcher != null) {
            int line = getLineNumberAt(pos);
            String result = watcher.breakpointToggleEvent(this, line, set);

            if(result == null) {
                // no problem, go ahead
                SimpleAttributeSet a = new SimpleAttributeSet();
                if(set) {
                    a.addAttribute(BREAKPOINT, Boolean.TRUE);
                    mayHaveBreakpoints = true;
                }
                else
                    a.addAttribute(BREAKPOINT, Boolean.FALSE);

                sourceDocument.setParagraphAttributes(pos, 0, a, false);
            }
            else
                info.warning(result);

            // force an update of UI
            repaint();
        }
        else
            info.warning(Config.getString("editor.info.cannotSetBreak"));

    }

    // --------------------------------------------------------------------
    /**
     *  Remove a breakpoint without question.
     */
    private void doRemoveBreakpoint(int pos)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(BREAKPOINT, Boolean.FALSE);
        sourceDocument.setParagraphAttributes(pos, 0, a, false);
    }

    // --------------------------------------------------------------------
    /**
     *  Try to set or remove a step mark (depending on the parameter) at
     *  the given position. 
     */
    private void setStepMark(int pos)
    {
        removeStepMark();
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(STEPMARK, Boolean.TRUE);
        sourceDocument.setParagraphAttributes(pos, 0, a, false);
        currentStepPos = pos;
        // force an update of UI
        repaint();
    }

    // ========================= SUPPORT ROUTINES ==========================

    // --------------------------------------------------------------------
    /**
     *  return a boolean representing whether in source editing view
     */
    private boolean viewingCode()
    {
        return sourceIsCode && (!viewingHTML);
    }

    // --------------------------------------------------------------------
    /**
     *  Return the number of lines in the documant.
     */
    private int numberOfLines()
    {
        return document.getDefaultRootElement().getElementCount();
    }

    // --------------------------------------------------------------------
    /**
     *  Return the current line.
     */
    private Element getCurrentLine()
    {
        return document.getParagraphElement(currentTextPane.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a line by line number
     */
    private Element getLine(int lineNo)
    {
        return document.getDefaultRootElement().getElement(lineNo-1);
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a line by text position
     */
    private Element getLineAt(int pos)
    {
        return document.getParagraphElement(pos);
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a position in a line.
     */
    private int getPositionInLine(int lineNo)
    {
        return getLine(lineNo).getStartOffset();
    }

    // --------------------------------------------------------------------
    /**
     *  Return the number of the current line.
     */
    private int getCurrentLineNo()
    {
        return document.getDefaultRootElement().getElementIndex(
                                   currentTextPane.getCaretPosition()) + 1;
    }

    // --------------------------------------------------------------------
    /**
     *  Return the number of the line containing position 'pos'.
     */
    private int getLineNumberAt(int pos)
    {
        return document.getDefaultRootElement().getElementIndex(pos) + 1;
    }

    // --------------------------------------------------------------------
    /**
     *  Revert the buffer contents to the last saved version.  Do
     *  not ask any question - just do it. Must have a file name.
     */
    public void doReload()
    {
//        Debug.assert (filename != null);

        try {
            FileReader reader = new FileReader(filename);
            sourcePane.read(reader, null);
            reader.close();

            sourceDocument = (MoeSyntaxDocument)sourcePane.getDocument();

            // flag document type as a java file by associating a JavaTokenMarker
            // for syntax colouring if specified
            checkSyntaxStatus();
            sourceDocument.addDocumentListener(this);
            sourceDocument.addUndoableEditListener(new MoeUndoableEditListener());
        }
        catch (FileNotFoundException ex) {
            info.warning (Config.getString("editor.info.fileDisappeared"));
        }
        catch (IOException ex) {
            info.warning (Config.getString("editor.info.fileReadError"));
        }
        setSaved();
    }


    // --------------------------------------------------------------------
    /**
     * Checks that current status of syntax highlighting option is
     * consistent with desired option eg off/on.
     */
    private void checkSyntaxStatus()
    {
        if(sourceDocument != null) {

            // flag document type as a java file by associating a
            // JavaTokenMarker for syntax colouring if specified
            if(viewingCode() && PrefMgr.getFlag(PrefMgr.HILIGHTING)) {
                if(sourceDocument.getTokenMarker() == null)
                    sourceDocument.setTokenMarker(new JavaTokenMarker());
            }
            else
                sourceDocument.setTokenMarker(null);
        }
        // else ??
    }
    
    /**
     * Checks that current status of syntax highlighting option is
     * consistent with desired option eg off/on.  Called when refreshing or 
     * making visible to pick up any Preference Manager changes to 
     * this functionality
     */
    private void checkBracketStatus()
    {
        matchBrackets = PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS);
        // tidies up leftover highlight if matching is switched off
        // while highlighting a valid bracket or refreshes bracket in open editor
        if(matchBrackets) 
            doBracketMatch();
        else 
            moeCaret.removeBracket();
    }
    
    /**
     * returns status of flag for bracket matching in editor
     */
    public boolean matchBrackets()
    {
        return matchBrackets;        
    }


    // --------------------------------------------------------------------
    /**
     * Toggle the editor's 'compiled' status. If compiled, enable the
     * breakpoint function.
     */
    private void setCompileStatus(boolean compiled)
    {
        actions.getActionByName("toggle-breakpoint").setEnabled(
                                                                compiled && viewingCode());
        isCompiled = compiled;

        if(compiled)
            document.putProperty(COMPILED, Boolean.TRUE);
        else
            document.putProperty(COMPILED, Boolean.FALSE);
        currentTextPane.repaint();
    }

    // --------------------------------------------------------------------
    /**
     *  Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
        info.message (Config.getString("editor.info.saved"));
        saveState.setState (StatusLabel.SAVED);
        if(watcher != null)
            watcher.saveEvent(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Buffer just went from saved to changed state (called by StatusLabel)
     */
    private void setChanged()
    {
        if(ignoreChanges)
            return;
        setCompileStatus (false);
        if(watcher != null)
            watcher.modificationEvent(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Clear the message in the info area.
     */
    void caretMoved()
    {
        clearMessage();
        if(matchBrackets)
            doBracketMatch();
        actions.userAction();
    }
    
    
    /**
     * returns the position of the matching bracket for the source pane's
     * current caret position.  Returns -1 if not found or not valid/appropriate
     * @return the int representing bracket position
     */
    public int getBracketMatch()
    {
        int pos = -1;
        try {
            int caretPos = sourcePane.getCaretPosition();
            if(caretPos != 0)
                caretPos--;
            pos = TextUtilities.findMatchingBracket(sourceDocument, caretPos);
        }
        catch (BadLocationException ble) {
            Debug.reportError("Bad document location reached while trying to match brackets");
        }
        return pos;
    }
    
    /**
     * delegates bracket matching to the source pane's caret
     *
     */
    public void doBracketMatch()
    {
        Caret caret = sourcePane.getCaret();
        if(caret instanceof MoeCaret)
            ((MoeCaret)caret).paintMatchingBracket();
    }
    

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
     *  Return the path to the class documentation.
     */
    private String getDocPath()
    {
        return docFilename;
    }

    // --------------------------------------------------------------------

    private String getResource(String name)
    {
        return resources.getProperty(name);
    }

    // --------------------------------------------------------------------

    private String[] tokenize(String input)
    {
        List list = new ArrayList();
        StringTokenizer t = new StringTokenizer(input);
        String tokens[];

        while (t.hasMoreTokens())
            list.add(t.nextToken());

        tokens = new String[list.size()];
        list.toArray(tokens);
        return tokens;
    }


    // ======================= WINDOW INITIALISATION =======================

    // --------------------------------------------------------------------
    /**
     *  Create all the Window components
     */

    private void initWindow(boolean showTool)
    {
        setIconImage(iconImage);
        setBackground(frameBgColor);

        // prepare the content pane

        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        setContentPane(contentPane);

        // create and add info and status areas

        JPanel bottomArea = new JPanel();		// create panel for info/status
        bottomArea.setLayout(new BorderLayout(5, 5));

        info = new Info();
        bottomArea.add (info, BorderLayout.CENTER);

        statusArea = new JPanel();
        statusArea.setLayout(new GridLayout(0, 1));	// one column, many rows
        statusArea.setBackground(infoColor);
        statusArea.setBorder(BorderFactory.createLineBorder(Color.black));

        saveState = new StatusLabel(StatusLabel.SAVED);
        statusArea.add(saveState);
        bottomArea.add(statusArea, BorderLayout.EAST);

        contentPane.add(bottomArea, BorderLayout.SOUTH);

        // create the text document

        sourceDocument = new MoeSyntaxDocument();
        sourceDocument.addDocumentListener(this);
        sourceDocument.addUndoableEditListener(new MoeUndoableEditListener());

        // create the text pane

        MoeSyntaxEditorKit kit = new MoeSyntaxEditorKit();
        sourcePane = new MoeEditorPane();

        sourcePane.setDocument(sourceDocument);
        //sourcePane.setLineWrap(false);
        sourcePane.setCaretPosition(0);
        sourcePane.setMargin(new Insets(2,2,2,2));
        sourcePane.setOpaque(true);
        sourcePane.setEditorKit(kit);
        moeCaret = new MoeCaret(this);
        sourcePane.setCaret(moeCaret);
        sourcePane.getCaret().setBlinkRate(0);
        sourcePane.setBackground(textBgColor);
        sourcePane.setSelectionColor(selectionColour);
        sourcePane.setCaretColor(cursorColor);

        // default showing:
        document = sourceDocument;
        currentTextPane = sourcePane;

        scrollPane = new JScrollPane(currentTextPane);
        scrollPane.setPreferredSize(new Dimension(598,400));

        contentPane.add(scrollPane, BorderLayout.CENTER);

        // get table of edit actions

        actions = MoeActions.getActions(sourcePane);

        // **** temporary: disable all unimplemented actions ****

        //actions.getActionByName("replace").setEnabled(false);
        actions.getActionByName("show-manual").setEnabled(false);

        // ****

        // create menubar and menus

        JMenuBar menubar = createMenuBar();
        setJMenuBar(menubar);

        // create toolbar

        toolbar = createToolbar();
        contentPane.add(toolbar, BorderLayout.NORTH);

        // add event listener to handle the window close requests

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    close();
                }
                public void windowActivated(WindowEvent e) {
                    currentTextPane.requestFocus();
                }
            });

        //     if (showTool)
        //        show_toolbar(true);

        setWindowTitle();
        pack();

    } // init_window


    // --------------------------------------------------------------------

    /**
     *  Create the editor's menu bar.
     */
    private JMenuBar createMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = null;

        String[] menuKeys = tokenize(getResource("menubar"));
        for (int i=0; i<menuKeys.length; i++) {
            menu = createMenu(menuKeys[i]);
            if (menu != null) {
                // Hack while "setHelpMenu" does not work...
                if(menuKeys[i].equals("help"))
                    menubar.add(Box.createHorizontalGlue());
                menubar.add(menu);
            }
        }
        if (menu != null) {
            // Always put help menu last
            //menubar.setHelpMenu(menu);  // not implemented in Swing 1.1
        }
        return menubar;
    }

    // --------------------------------------------------------------------

    /**
     *  Create a single menu for the editor's menu bar. The key for the menu
     *  (as defined in moe.properties) is supplied.
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
            Debug.message ("Moe: cannot find menu definition for " + key);
            return null;
        }

        // cut menu definition into separate items
        String[] itemKeys = tokenize(itemString);

        // create menu item for each item
        for (int i=0; i<itemKeys.length; i++) {
            if (itemKeys[i].equals("-"))
                menu.addSeparator();
            else {
                Action action = actions.getActionByName(itemKeys[i]);
                if (action == null)
                    Debug.message ("Moe: cannot find action " + itemKeys[i]);
                else {
                    item = menu.add(action);
                    label = Config.getString("editor."+itemKeys[i] + LabelSuffix);
                    if (label != null)
                        item.setText(label);
                    KeyStroke[] keys = actions.getKeyStrokesForAction(action);
                    if (keys != null)
                        item.setAccelerator(chooseKey(keys));
                }
            }
        }
        return menu;
    }

    /*
     * Choose a key to use in the menu from all defined keys.
     */
    private KeyStroke chooseKey(KeyStroke[] keys)
    {
        if(keys.length == 1)
            return keys[0];
        else {
            KeyStroke key = keys[0];
            // give preference to shortcuts using letter keys (CTRL-V, rather than F2)
            for(int i=1; i < keys.length; i++) {
                if(keys[i].getKeyCode() >= 'A' && keys[i].getKeyCode() <= 'Z')
                    key = keys[i];
            }
            return key;
        }
    }

    // --------------------------------------------------------------------

    private JComponent createToolbar()
    {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        //((FlowLayout)toolbar.getLayout()).setAlignment(FlowLayout.LEFT);

        String[] toolKeys = tokenize(getResource("toolbar"));
        for (int i=0; i<toolKeys.length; i++) {
            toolbar.add(createToolbarButton(toolKeys[i], false));
            toolbar.add(Box.createHorizontalStrut(4));
        }

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(createInterfaceSelector());

        return toolbar;
    }

    // --------------------------------------------------------------------

    /**
     * Create a button on the toolbar.
     */
    private AbstractButton createToolbarButton(String key, boolean isToggle)
    {
        String label = Config.getString("editor." + key + LabelSuffix);
        AbstractButton button;

        if(isToggle)
            button = new JToggleButton(label);
        else
            button = new JButton(label);

        button.setRequestFocusEnabled(false);   // never get keyboard focus
        //button.setMargin(new Insets(2,2,2,2));
        button.setFont(PrefMgr.getStandardFont());

        String actionName = getResource(key + ActionSuffix);
        if (actionName == null)
            actionName = key;
        Action action = actions.getActionByName(actionName);
        if (action != null) {	// should never be null...
            button.addActionListener(action);
            button.setActionCommand(actionName);
        }
        else {
            button.setEnabled(false);
            Debug.message("Moe: action not found for button " + label);
        }

        // mac os property to change button shape
        button.putClientProperty("JButton.buttonType", "toolbar");  //"icon"

        return button;
    }

    // --------------------------------------------------------------------

    /**
     * Create a combo box for the toolbar
     */
    private JComboBox createInterfaceSelector()
    {
        String[] choiceStrings = { implementationString, interfaceString };
        interfaceToggle = new JComboBox(choiceStrings);

        interfaceToggle.setRequestFocusEnabled(false);
        interfaceToggle.setFont(PrefMgr.getStandardFont());
        interfaceToggle.setBorder(new EmptyBorder(2,2,2,2));
        interfaceToggle.setForeground(envOpColour);

        String actionName = "toggle-interface-view";
        Action action = actions.getActionByName(actionName);
        if (action != null) {	// should never be null...
            // for jdk 1.3 and newer only:
            //  interfaceToggle.setAction(action);

            // code for 1.2 - remove once 1.2 is out of fashion
            interfaceToggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toggleInterface();
                }
            });
            // end of 1.2 code

        }
        else {
            interfaceToggle.setEnabled(false);
            Debug.message("Moe: action not found: " + actionName);
        }
        if(!sourceIsCode)
            interfaceToggle.setEnabled(false);
        return interfaceToggle;
    }

    // --------------------------------------------------------------------

    /**
     * Inner class for printing thread to allow printing to occur
     * as a background operation.
     * @author Bruce Quig
     */
    class PrintHandler implements Runnable
    {
        PrinterJob printJob;
        
		/**
		 * Constructor for PrintHandler.
		 */
		public PrintHandler(PrinterJob pj) {
			super();
            printJob = pj;
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
            if(printer == null)
                printer = new MoePrinter();

            
            // print document, using new pageformat object at present
            info.message (Config.getString("editor.info.printing"));
            if(printer.printDocument(printJob, sourceDocument, windowTitle, printFont,
                                     pageFormat))
                info.message (Config.getString("editor.info.printed"));
            else
                info.message (Config.getString("editor.info.cancelled"));
            
        }

    }

    // --------------------------------------------------------------------

    /**
     * inner class for loading HTML documentation
     */
    class HTMLDisplayThread extends Thread
    {
        private boolean reload;

        HTMLDisplayThread(boolean load)
        {
            reload = load;
        }

        public void run()
        {
            if(htmlDocument == null) {
                createHTMLPane();
                reload = true;
            }
            
            if(reload) {
                try {
                    try {
                        // this statement fails, but it is needed to avoid
                        // caching of html page
                        htmlPane.setPage("file:/dummy");
                    }
                    catch(Exception e) {}

                    File urlFile = new File(getDocPath());
                    URL myURL = urlFile.toURL();
                    htmlPane.setPage(myURL);
                    htmlDocument = (HTMLDocument)htmlPane.getDocument();
                    htmlDocument.setBase(myURL);
                    info.message(Config.getString("editor.info.docLoaded"));
                }
                catch (Exception exc) {
                    info.warning(
                                 Config.getString("editor.info.docDisappeared"),
                                 getDocPath());
                    Debug.reportError("loading class interface failed: "+exc);
                }
            }
            document = htmlDocument;
            currentTextPane = htmlPane;
            viewingHTML = true;
            scrollPane.setViewportView(currentTextPane);
            currentTextPane.requestFocus();
        }

    }

    // --------------------------------------------------------------------

    class TextInsertNotifier implements Runnable {
        private DocumentEvent evt;
        public void setEvent(DocumentEvent e) {
            evt = e;
        }
        public void run() {
            actions.textInsertAction(evt);
        }
    }

} // end class MoeEditor
