// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@monash.edu.au

package bluej.editor.moe;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.editor.EditorWatcher;
import bluej.pkgmgr.DocuGenerator;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.DateFormat;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model
import javax.swing.*;		// all the GUI components
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import javax.swing.text.html.*;
import java.net.URL;

import java.awt.print.*;
import java.awt.geom.*;

import org.gjt.sp.jedit.syntax.*; // Syntax highlighting package

/**
 * @author Michael Kolling 
 *
 */

// PENDING: add "finalize" method that does:
//        MoeEditorManager.editorManager.removeEditor(this);
// cuurently, editors never get removed from editor manager!

public final class MoeEditor extends JFrame
    implements bluej.editor.Editor, BlueJEventListener, HyperlinkListener
{
    // -------- CONSTANTS --------

    // version number
    static final int version = 110;
    static final String versionString = "1.1";

    // colours
    static final Color textColor = new Color(0,0,0);		// normal text
    static final Color textBgColor = new Color(255,255,255);	// background
    static final Color selectionColor = Color.pink;		// selection
    static final Color cursorColor = new Color(255,0,100);	// cursor

    static final Color frameBgColor = new Color(196, 196, 196);
    static final Color infoColor = new Color(240, 240, 240);
    static final Color lightGrey = new Color(224, 224, 224);
    static final Color selectionColour = new Color(204, 204, 204);
    static final Color titleCol = Config.getItemColour("colour.text.fg");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    // Icons
    static final Image iconImage = 
        new ImageIcon(Config.getImageFilename("image.icon.editor")).getImage();

    // Fonts
    public static Font printFont = new Font("Monospaced", Font.PLAIN,
                                            10);
    // Strings
    String implementationString = Config.getString("editor.implementationLabel");
    String interfaceString = Config.getString("editor.interfaceLabel");


    // suffixes for resources
    static final String LabelSuffix = "Label";
    static final String ActionSuffix = "Action";
    static final String TooltipSuffix = "Tooltip";
    static final String AcceleratorSuffix = "Accelerator";

    //  width of tag area for setting breakpoints
    static final short TAG_WIDTH = 14;

    static final String spaces = "    ";

    // Attributes for lines and document
    public static final String BREAKPOINT = "break";
    public static final String STEPMARK = "step";
    static final String COMPILED = "compiled";

    // PageFormat object for printing page format
    private static PageFormat pageFormat = new PageFormat();


    // -------- INSTANCE VARIABLES --------

    private EditorWatcher watcher;
    private Properties resources;

    private AbstractDocument document;
    private MoeSyntaxDocument sourceDocument;
    private HTMLDocument htmlDocument;

    private MoeActions actions;

    private JEditorPane currentTextPane;  // text component currently dislayed
    private JEditorPane sourcePane;	// the component holding the source text
    private JEditorPane htmlPane;	// the component holding the javadoc html

    private Info info;	            // the info number label
    private JPanel statusArea;	    // the status area
    private LineNumberLabel lineCounter;	// the line number label
    private StatusLabel saveState;  // the status label
    private JComboBox interfaceToggle; 

    private JScrollPane scrollPane;
    private JComponent toolbar;	    // The toolbar

    private String filename;        // name of file or null
    private String windowTitle;	    // title of editor window
    private boolean firstSave;      // true if never been saved
    private boolean isCompiled;	    // true when source has been compiled
    private String docpath;         // path to javadoc html file

    private String newline;	        // the line break character used
    private boolean sourceIsCode;   // true if current buffer is code
    private boolean viewingHTML;

    private int currentStepPos;         // position of step mark (or -1)
    private boolean mayHaveBreakpoints;	// true if there were BP here
    private boolean ignoreChanges = false;

    private MoePrinter printer;

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
                     boolean showToolbar, boolean showLineNum,
                     Properties resources)
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

        initWindow(showToolbar, showLineNum);
    }

    // --------------------------------------------------------------------
    /**
     *  Load the file "fname" and show the editor window.
     */

    public boolean showFile(String filename, boolean compiled,
                            Vector breakpoints)
                            // inherited from Editor, redefined
    {
        this.filename = filename;

        boolean loaded = false;
        boolean readError = false;

        if(breakpoints != null)
            Debug.reportError("breakpoints in showfile not supported.");

        if (filename != null) {

            try {
                FileReader reader = new FileReader(filename);
                sourcePane.read(reader, null);
                reader.close();

                sourceDocument = (MoeSyntaxDocument)sourcePane.getDocument();

                // set TokenMarker for syntax highlighting if desired
                checkSyntaxStatus();

                sourceDocument.addDocumentListener(saveState);
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
            info.message (Config.getString("editor.info.version") + 
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
     *  @param style         the style in which the text is to be displayed
     */

    public void insertText(String text, boolean bold, boolean italic)
    // inherited from Editor, redefined
    {
        sourcePane.replaceSelection(text);
    }

    // --------------------------------------------------------------------
    /**
     *  Show the editor window. This includes whatever is necessary of the
     *  following: make visible, de-iconify, bring to front of window stack.
     */
    public void setVisible(boolean vis)  // inherited from Editor, redefined
    {
        if(vis) {
            currentTextPane.setFont(PrefMgr.getStandardEditorFont());
            checkSyntaxStatus();
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

            Debug.assert(filename != null);

            try {
                FileWriter writer = new FileWriter(filename);
                sourcePane.write(writer);
                writer.close();
                setSaved();
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
        currentTextPane.moveCaretPosition(line.getEndOffset());

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

    // --------------------------------------------------------------------
    /**
     *  Clear the message in the info area.
     */
    public void clearMessage()
    {
        info.clear();
    }

    /**
     *  Write a message into the info area.
     */
    public void writeMessage(String msg)
    {
        info.message(msg);
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
     * Implementation of the "print" user function.
     * Printing is delegated to a PrintThread inner class to run in a low
     * priority thread.
     */
    public void print()
    {
        PrintThread pt = new PrintThread();
        pt.setPriority((Thread.currentThread().getPriority() - 1));
        pt.start();
    }


    // --------------------------------------------------------------------
    /**
     * Implementation of the "page setup" user function.
     * This provides a dialog for print page setup.  The resulting format
     * is only persistent for session at present.
     */
    public void pageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = job.pageDialog(pageFormat);
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
     *  Implementation of "find" user function.
     */

    public void find()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        DialogManager.centreWindow(finder, this);
        String s = finder.getNewSearchString(this, Finder.FORWARD);
        if(s != null)
            findString(finder, s, finder.getDirection() == Finder.BACKWARD);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-backward" user function.
     */

    public void findBackward()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        String s = finder.getNewSearchString(this, Finder.BACKWARD);
        if(s != null)
            findString(finder, s, finder.getDirection() == Finder.BACKWARD);
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
            s = finder.getLastSearchString();
            if (s == null) {
                info.warning(DialogManager.getMessage("no-search-string"));
                return;
            }
        }
        findString(finder, s, finder.getDirection() == Finder.BACKWARD);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-next-reverse" user function.
     */

    public void findNextReverse()
    {
        Finder finder = MoeEditorManager.editorManager.getFinder();
        String s = currentTextPane.getSelectedText();
        if (s == null) {
            s = finder.getLastSearchString();
            if (s == null) {
                info.warning(DialogManager.getMessage("no-search-string"));
                return;
            }
        }
        findString(finder, s, finder.getDirection() == Finder.FORWARD);
    }


    // --------------------------------------------------------------------
    /**
     *   Do a find with info in the info area.
     */

    private void findString(Finder finder, String s, boolean backward)
    {
        if (s.length()==0) {
            info.warning(Config.getString("editor.info.emptySearchString"));
            return;
        }
        String msg;
        boolean wrap = ! finder.lastSearchFound();
        msg = "Find " + (backward ? "backward" : "forward") +
            (wrap ? " (wrap around): " : ": ") + s;
        info.message(msg);
        boolean found;
        if(backward)
            found = doFindBackward(s, wrap);
        else
            found = doFind(s, wrap);
        finder.setSearchString(s);
        finder.setSearchFound(found);
        if (! found)
            info.warning(msg, Config.getString("editor.info.notFound"));
    }

    // --------------------------------------------------------------------
    /**
     *  doFind - do a find without visible feedback. Returns false if not found.
     */

    private boolean doFind(String s, boolean wrap)
    {
        int docLength = document.getLength();
        int startPosition = currentTextPane.getCaretPosition();
        int endPos = docLength;

        boolean found = false;
        boolean finished = false;

        int start = startPosition;
        Element line = getLineAt(start);
        int lineEnd = Math.min(line.getEndOffset(), endPos);

        try {
            while (!found && !finished) {
                String lineText = document.getText(start, lineEnd - start);
                if(lineText != null && lineText.length() > 0) {
                    int foundPos = lineText.indexOf(s);
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

    private boolean doFindBackward(String s, boolean wrap)
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
                    int foundPos = lineText.lastIndexOf(s);
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
            Debug.message("error in editor find operation");
        }
        return found;
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

    // --------------------------------------------------------------------
    /**
     *  Switch on the source view (it it isn't showing already).
     */
    private void switchToSourceView()
    {
        if(!viewingHTML)
            return;

        document = sourceDocument;
        currentTextPane = sourcePane;
        viewingHTML = false;
        scrollPane.setViewportView(currentTextPane);
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

        save();
        if(docUpToDate()) {
            displayInterface(false);
        }
        else {  // interface needs to be re-generated
            info.message(Config.getString("editor.info.generatingDoc"));
            BlueJEvent.addListener(this);
            DocuGenerator.generateClassDocu(filename);
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
            File doc = new File(getDocPath());

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
     *  Try to set or remove a breakpoint (depending on the parameter) at
     *  the given position. Informs the watcher.
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
     *  Return the number of lines in the documant.
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
        Debug.assert (filename != null);

        try {
            FileReader reader = new FileReader(filename);
            sourcePane.read(reader, null);
            reader.close();

            sourceDocument = (MoeSyntaxDocument)sourcePane.getDocument();

            // flag document type as a java file by associating a JavaTokenMarker
            // for syntax colouring if specified
            checkSyntaxStatus();
            sourceDocument.addDocumentListener(saveState);
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
            if(viewingCode() && PrefMgr.useSyntaxHilighting()) {
                if(sourceDocument.getTokenMarker() == null)
                    sourceDocument.setTokenMarker(new JavaTokenMarker());
            }
            else
                sourceDocument.setTokenMarker(null);
        }
        // else ??
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
    void setChanged()
    {
        if(ignoreChanges)
            return;
        setCompileStatus (false);
        if(watcher != null)
            watcher.modificationEvent(this);
    }

    // --------------------------------------------------------------------

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
        if(docpath == null)
            docpath = DocuGenerator.getDocuPath(filename);
        return docpath;
    }

    // --------------------------------------------------------------------
    /**
     *  Show or hide the line number display (depending on the parameter
     *  'show').
     */

    private void showLineCounter(boolean show)
    {
        if (show)
            statusArea.add(lineCounter);
        else
            statusArea.remove(lineCounter);
        validate();
    }

    // --------------------------------------------------------------------

    private String getResource(String name)
    {
        return resources.getProperty(name);
    }

    // --------------------------------------------------------------------

    private String[] tokenize(String input)
    {
        Vector v = new Vector();
        StringTokenizer t = new StringTokenizer(input);
        String tokens[];

        while (t.hasMoreTokens())
        v.addElement(t.nextToken());
        tokens = new String[v.size()];
        for (int i=0; i<tokens.length; i++)
        tokens[i] = (String) v.elementAt(i);
        return tokens;
    }


    // ======================= WINDOW INITIALISATION =======================

    // --------------------------------------------------------------------
    /**
     *  Create all the Window components
     */

    private void initWindow(boolean showTool, boolean showLine)
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

        lineCounter = new LineNumberLabel(1);
        saveState = new StatusLabel(StatusLabel.SAVED, this);
        if (showLine)				// if the line number display
            statusArea.add(lineCounter);
        statusArea.add(saveState);
        bottomArea.add(statusArea, BorderLayout.EAST);

        contentPane.add(bottomArea, BorderLayout.SOUTH);

        // create the text document

        sourceDocument = new MoeSyntaxDocument();
        sourceDocument.addDocumentListener(saveState);
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
        sourcePane.setCaret(new MoeCaret(this));
        sourcePane.getCaret().setBlinkRate(0);
        sourcePane.setSelectionColor(selectionColor);
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

        actions.getActionByName("replace").setEnabled(false);
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
        JMenu menu = new JMenu(Config.getString("editor."+key + LabelSuffix));

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
                        item.setAccelerator(keys[0]);
                }
            }
        }
        return menu;
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
        button.setMargin(new Insets(2,2,2,2));

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
     * inner class for printing thread to allow printing to occur
     * as a background operation.
     * @author Bruce Quig
     */
    class PrintThread extends Thread
    {
        public void run()
        {
            if(printer == null)
                printer = new MoePrinter();

            //printer.printDocument(document, windowTitle, printFont, pageFormat);
            // print document, using new pageformat object at present
            info.message (Config.getString("editor.info.printing"));
            if(printer.printDocument(sourceDocument, windowTitle, printFont, 
                                     pageFormat))
                info.message (Config.getString("editor.info.printed"));
            else
                info.message (Config.getString("editor.info.cancelled"));
        }

    }

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
                        htmlPane.setPage("");
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


} // end class MoeEditor
