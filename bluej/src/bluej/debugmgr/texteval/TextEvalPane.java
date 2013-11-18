/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013  Michael Kolling and John Rosenberg 
 
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

package bluej.debugmgr.texteval;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;

import bluej.BlueJEvent;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.IndexHistory;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ValueCollection;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.MoeSyntaxEditorKit;
import bluej.parser.TextAnalyzer;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 * A modified editor pane for the text evaluation area.
 * The standard JEditorPane is adjusted to take the tag line to the left into
 * account in size computations.
 * 
 * @author Michael Kolling
 */
public class TextEvalPane extends JEditorPane 
    implements Accessible, ValueCollection, ResultWatcher, MouseMotionListener
{
    // The cursor to use while hovering over object icon
    private static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor objectCursor = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor textCursor = new Cursor(Cursor.TEXT_CURSOR);
    
    private static final String nullLabel = "null";
    
    private static final String uninitializedWarning = Config.getString("pkgmgr.codepad.uninitialized");
    
    private PkgMgrFrame frame;
    private MoeSyntaxDocument doc;  // the text document behind the editor pane
    private String currentCommand = "";
    private IndexHistory history;
    private Invoker invoker = null;
    private TextAnalyzer textParser = null;
    
    // Keeping track of invocation
    private boolean firstTry;
    private boolean wrappedResult;
    private String errorMessage;
    
    private boolean mouseInTag = false;
    private boolean mouseOverObject = false;
    private boolean busy = false;
    private Action softReturnAction;
    
    private List<CodepadVar> localVars = new ArrayList<CodepadVar>();
    private List<CodepadVar> newlyDeclareds;
    private List<String> autoInitializedVars;

    public TextEvalPane(PkgMgrFrame frame)
    {
        super();
        getAccessibleContext().setAccessibleName(Config.getString("pkgmgr.codepad.title"));
        this.frame = frame;
        setEditorKit(new MoeSyntaxEditorKit(true, null));
        doc = (MoeSyntaxDocument) getDocument();
        doc.enableParser(true);
        defineKeymap();
        clear();
        history = new IndexHistory(20);
        addMouseMotionListener(this);
        setCaret(new TextEvalCaret());
        setAutoscrolls(false);          // important - dragging objects from this component
                                        // does not work correctly otherwise
    }
    
    public Dimension getPreferredSize() 
    {
        Dimension d = super.getPreferredSize();
        d.width += TextEvalSyntaxView.TAG_WIDTH + 8;  // bit of empty space looks nice
        return d;
    }
    
    /**
     * Make sure, when we are scrolling to follow the caret,
     * that we can see the tag area as well.
     */
    public void scrollRectToVisible(Rectangle rect)
    {
        super.scrollRectToVisible(new Rectangle(rect.x - (TextEvalSyntaxView.TAG_WIDTH + 4), rect.y,
                rect.width + TextEvalSyntaxView.TAG_WIDTH + 4, rect.height));
    }
    
    /**
     * Clear all text in this text area.
     */
    public void clear()
    {
        setText("");
    }
    
    /**
     * Clear the local variables.
     */
    public void clearVars()
    {
        localVars.clear();
        if (textParser != null) {
            textParser.newClassLoader(frame.getProject().getClassLoader());
        }
    }

    /**
     * Paste the contents of the clipboard.
     */
    public void paste()
    {
        ensureLegalCaretPosition();
        super.paste();
    }

    /**
     * This is called when we get a 'paste' action (since we are handling 
     * ordinary key input differently with the InsertCharacterAction.
     * So: here we assume that we have a potential multi-line paste, and we
     * want to treat it accordingly (as multi-line input).
     */
    public void replaceSelection(String content)
    {
        ensureLegalCaretPosition();

        String[] lines = content.split("\n");
        super.replaceSelection(lines[0]);
        for(int i=1; i< lines.length;i++) {
            softReturnAction.actionPerformed(null);
            super.replaceSelection(lines[i]);
        }
    }
    
    //   --- ValueCollection interface ---
    
    /*
     * @see bluej.debugmgr.ValueCollection#getValueIterator()
     */
    public Iterator<CodepadVar> getValueIterator()
    {
        return localVars.iterator();
    }
    
    /*
     * @see bluej.debugmgr.ValueCollection#getNamedValue(java.lang.String)
     */
    public NamedValue getNamedValue(String name)
    {
        NamedValue nv = getLocalVar(name);
        if (nv != null) {
            return nv;
        }
        else {
            return frame.getObjectBench().getNamedValue(name);
        }
    }
    
    /**
     * Search for a named local variable, but do not fall back to the object
     * bench if it cannot be found (return null in this case).
     * 
     * @param name  The name of the variable to search for
     * @return    The named variable, or null
     */
    private NamedValue getLocalVar(String name)
    {
        Iterator<CodepadVar> i = localVars.iterator();
        while (i.hasNext()) {
            NamedValue nv = (NamedValue) i.next();
            if (nv.getName().equals(name))
                return nv;
        }
        
        // not found
        return null;
    }
    
    //   --- ResultWatcher interface ---

    /*
     * @see bluej.debugmgr.ResultWatcher#beginExecution()
     */
    @Override
    public void beginCompile() { }
    
    /*
     * @see bluej.debugmgr.ResultWatcher#beginExecution()
     */
    @Override
    public void beginExecution(InvokerRecord ir)
    { 
        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
    }
    
    /*
     * @see bluej.debugmgr.ResultWatcher#putResult(bluej.debugger.DebuggerObject, java.lang.String, bluej.testmgr.record.InvokerRecord)
     */
    @Override
    public void putResult(final DebuggerObject result, final String name, final InvokerRecord ir)
    {
        frame.getObjectBench().addInteraction(ir);
        frame.getPackage().getProject().updateInspectors();
        
        // Newly declared variables are now initialized
        if (newlyDeclareds != null) {
            Iterator<CodepadVar> i = newlyDeclareds.iterator();
            while (i.hasNext()) {
                CodepadVar cpv = (CodepadVar) i.next();
                cpv.setInitialized();
            }
            newlyDeclareds = null;
        }
        
        boolean giveUninitializedWarning = autoInitializedVars != null && autoInitializedVars.size() != 0; 
        
        if (giveUninitializedWarning && Utility.firstTimeThisRun("TextEvalPane.uninitializedWarning")) {
            // Some variables were automatically initialized - warn the user that
            // this won't happen in "real" code.
            
            String warning = uninitializedWarning;
            
            int findex = 0;
            while (findex < warning.length()) {
                int nindex = warning.indexOf('\n', findex);
                if (nindex == -1)
                    nindex = warning.length();
                
                String warnLine = warning.substring(findex, nindex);
                append(warnLine);
                markAs(TextEvalSyntaxView.ERROR, Boolean.TRUE);
                findex = nindex + 1; // skip the newline character
            }
            
            autoInitializedVars.clear();
        }
        
        if (!result.isNullObject()) {
            DebuggerField resultField = result.getField(0);
            String resultString = resultField.getValueString();
            
            if(resultString.equals(nullLabel)) {
                DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultString);
                output(resultString);
            }
            else {
                boolean isObject = resultField.isReferenceType();
                
                if(isObject) {
                    DebuggerObject resultObject = resultField.getValueObject(null);
                    String resultType = resultObject.getGenType().toString(true);
                    String resultOutputString = resultString + "   (" + resultType + ")";
                    DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultOutputString);
                    objectOutput(resultOutputString,  new ObjectInfo(resultObject, ir));
                }
                else {
                    String resultType = resultField.getType().toString(true);
                    String resultOutputString = resultString + "   (" + resultType + ")";
                    DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultOutputString);
                    output(resultOutputString);
                }
            }            
        } 
        else {
            markCurrentAs(TextEvalSyntaxView.OUTPUT, false);
        }
        
        ExecutionEvent executionEvent = new ExecutionEvent(frame.getPackage());
        executionEvent.setCommand(currentCommand);
        executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
        executionEvent.setResultObject(result);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
        
        currentCommand = "";
        textParser.confirmCommand();
        setEditable(true);    // allow next input
        busy = false;
    }
    
    /**
     * An invocation has failed - here is the error message
     */
    public void putError(String message, InvokerRecord ir)
    {
        if(firstTry) {
            if (wrappedResult) {
                // We thought we knew what the result type should be, but there
                // was a compile time error. So try again, assuming that we
                // got it wrong, and we'll use the dynamic result type (meaning
                // we won't get type arguments).
                wrappedResult = false;
                errorMessage = null; // use the error message from this second attempt
                invoker = new Invoker(frame, this, currentCommand, TextEvalPane.this);
                invoker.setImports(textParser.getImportStatements());
                invoker.doFreeFormInvocation("");
            }
            else {
                // We thought there was going to be a result, but compilation failed.
                // Try again, but assume we have a statement this time.
                firstTry = false;
                invoker = new Invoker(frame, this, currentCommand, TextEvalPane.this);
                invoker.setImports(textParser.getImportStatements());
                invoker.doFreeFormInvocation(null);
                if (errorMessage == null) {
                    errorMessage = message;
                }
            }
        }
        else {
            if (errorMessage == null) {
                errorMessage = message;
            }
            
            // An error. Remove declared variables.
            if (autoInitializedVars != null) {
                autoInitializedVars.clear();
            }
            
            removeNewlyDeclareds();
            DataCollector.codePadError(frame.getPackage(), ir.getOriginalCommand(), errorMessage);
            showErrorMsg(errorMessage);
            errorMessage = null;
        }
    }
    
    /**
     * A runtime exception occurred.
     */
    public void putException(ExceptionDescription exception, InvokerRecord ir)
    {
        ExecutionEvent executionEvent = new ExecutionEvent(frame.getPackage());
        executionEvent.setCommand(currentCommand);
        executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
        executionEvent.setException(exception);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
        frame.getPackage().getProject().updateInspectors();
        
        if (autoInitializedVars != null) {
            autoInitializedVars.clear();
        }
        
        removeNewlyDeclareds();
        String message = exception.getClassName() + " (" + exception.getText() + ")";
        DataCollector.codePadException(frame.getPackage(), ir.getOriginalCommand(), message);
        showExceptionMsg(message);
    }
    
    /**
     * The remote VM terminated before execution completed (or as a result of
     * execution).
     */
    public void putVMTerminated(InvokerRecord ir)
    {
        if (autoInitializedVars != null)
            autoInitializedVars.clear();
        
        removeNewlyDeclareds();
        
        
        String message = Config.getString("pkgmgr.codepad.vmTerminated");
        DataCollector.codePadError(frame.getPackage(), ir.getOriginalCommand(), message);
        append(message);
        markAs(TextEvalSyntaxView.ERROR, Boolean.TRUE);
        
        completeExecution();
    }
    
    /**
     * Remove the newly declared variables from the value collection.
     * (This is needed if compilation fails, or execution bombs with an exception).
     */
    private void removeNewlyDeclareds()
    {
        if (newlyDeclareds != null) {
            Iterator<CodepadVar> i = newlyDeclareds.iterator();
            while (i.hasNext()) {
                localVars.remove(i.next());
            }
            newlyDeclareds = null;
        }
    }
    
    //   --- end of ResultWatcher interface ---
    
    /**
     * Show an error message, and allow further command input.
     */
    private void showErrorMsg(final String message)
    {
        error("Error: " + message);
        completeExecution();
    }
    
    /**
     * Show an exception message, and allow further command input.
     */
    private void showExceptionMsg(final String message)
    {
        error("Exception: " + message);
        completeExecution();
    }
    
    /**
     * Execution of the current command has finished (one way or another).
     * Allow further command input.
     */
    private void completeExecution()
    {
        currentCommand = "";
        setEditable(true);
        busy = false;
    }

    /**
     * We had a click in the tag area. Handle it appropriately.
     * Specifically: If the click (or double click) is on an object, then
     * start an object drag (or inspect).
     * @param pos   The text position where we got the click.
     * @param clickCount  Number of consecutive clicks
     */
    public void tagAreaClick(int pos, int clickCount)
    {
        ObjectInfo objInfo = objectAtPosition(pos);
        if(objInfo != null) {
            if(clickCount == 1) {
                DragAndDropHelper dnd = DragAndDropHelper.getInstance();
                dnd.startDrag(this, frame, objInfo.obj, objInfo.ir);
            }
            else if(clickCount == 2) {   // double click
                inspectObject(objInfo);
            }
        }
    }
    
    /**
     * Inspect the given object.
     * This is done with a delay, because we are in the middle of a mouse click,
     * and focus gets weird otherwise.
     */
    private void inspectObject(TextEvalPane.ObjectInfo objInfo)
    {
        final TextEvalPane.ObjectInfo oi = objInfo;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.getProject().getInspectorInstance(oi.obj, null, frame.getPackage(), oi.ir, frame);
            }
        });
    }

    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void output(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(TextEvalSyntaxView.OUTPUT, Boolean.TRUE);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void objectOutput(String s, ObjectInfo objInfo)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(TextEvalSyntaxView.OBJECT, objInfo);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Write an error message to the text area.
     * @param s The message
     */
    private void error(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(TextEvalSyntaxView.ERROR, Boolean.TRUE);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Append some text to this area.
     * @param s The text to append.
     */
    private void append(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            setCaretPosition(doc.getLength());
        
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }

    /**
     * Ensure that the caret position (including the whole
     * selection, if any) is within the editale area (the last 
     * line of text). If it isn't, adjust it so that it is.
     */
    private void ensureLegalCaretPosition()
    {
        Caret caret = getCaret();
        boolean dotOK = isLastLine(caret.getDot());
        boolean markOK = isLastLine(caret.getMark());
        
        if(dotOK && markOK)     // both in last line - no problem
            return;
        
        if(!dotOK && !markOK) { // both not in last line - append at end
            setCaretPosition(getDocument().getLength());
        }
        else {                  // selection reaches into last line
            caret.setDot(Math.max(caret.getDot(), caret.getMark()));
            caret.moveDot(startOfLastLine());
        }
    }
    
    /**
     * Check whether the given text position is within the area
     * intended for editing (the last line).
     * 
     * @param pos  The position to be checked
     * @return  True if this position is within the last text line.
     */
    private boolean isLastLine(int pos)
    {
        return pos >= startOfLastLine();
    }
    
    /**
     * Return the text position of the start of the last text line
     * (the start of the area editable by the user).
     * 
     * @return  The position of the start of the last text line.
     */
    private int startOfLastLine()
    {
        AbstractDocument doc = (AbstractDocument) getDocument();
        Element line = doc.getParagraphElement(doc.getLength());
        return line.getStartOffset();
    }
    
    /**
     * Get the text of the current line (the last line) of this area.
     * @return The text of the last line.
     */
    private String getCurrentLine()
    {
        Element line = doc.getParagraphElement(doc.getLength());
        int lineStart = line.getStartOffset();
        int lineEnd = line.getEndOffset() - 1;      // ignore newline char
        
        try {
            return doc.getText(lineStart, lineEnd-lineStart);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in text eval operation");
            return "";
        }
    }
    
    /**
     * Return the current column number.
     */
    private int getCurrentColumn()
    {
        Caret caret = getCaret();
        int pos = Math.min(caret.getMark(), caret.getDot());
        return getColumnFromPosition(pos);
    }

    /**
     * Return the column for a given position.
     */
    private int getColumnFromPosition(int pos)
    {
        int lineStart = doc.getParagraphElement(pos).getStartOffset();
        return (pos - lineStart);       
    }
    
    /**
     * Mark the last line of the text area as output. and start a new 
     * line after that one.
     */
    private void markAs(String flag, Object value)
    {
        append("\n");
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(flag, value);
        doc.setParagraphAttributes(doc.getLength()-2, a);
        repaint();
    }
    
    /**
     * Mark the current line of the text area as output.
     */
    private void markCurrentAs(String flag, Object value)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(flag, value);
        doc.setParagraphAttributes(doc.getLength(), a);
    }
    
     /**
     * Replace the text of the current line with some new text.
     * @param s The new text for the line.
     */
    private void replaceLine(String s)
    {
        Element line = doc.getParagraphElement(doc.getLength());
        int lineStart = line.getStartOffset();
        int lineEnd = line.getEndOffset() - 1;      // ignore newline char
        
        try {
                doc.replace(lineStart, lineEnd-lineStart, s, null);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in text eval operation");
        }
    }
    
    /**
     * Return the object stored with the line at position 'pos'.
     * If that line does not have an object, return null.
     */
    private ObjectInfo objectAtPosition(int pos)
    {
        Element line = getLineAt(pos);
        return (ObjectInfo) line.getAttributes().getAttribute(TextEvalSyntaxView.OBJECT);
    }

    /**
     *  Find and return a line by text position
     */
    private Element getLineAt(int pos)
    {
        return doc.getParagraphElement(pos);
    }

    /**
     * Check whether a given point on screen is over an object icon.
     */
    private boolean pointOverObjectIcon(int x, int y)
    {
        int pos = getUI().viewToModel(this, new Point(x, y));
        ObjectInfo objInfo = objectAtPosition(pos);
        return objInfo != null;        
    }
    
    // ---- MouseMotionListener interface: ----
    
    public void mouseDragged(MouseEvent evt) {}

    /**
     * When the mouse is moved, check whether we should change the 
     * mouse cursor.
     */
    public void mouseMoved(MouseEvent evt) 
    {
        int x = evt.getX();
        int y = evt.getY();
        
        if(mouseInTag) {
            if(x > TextEvalSyntaxView.TAG_WIDTH) {    // moved out of tag area
                setCursor(textCursor);
                mouseInTag = false;
            }
            else 
                setTagAreaCursor(x, y);
        }
        else {
            if(x <= TextEvalSyntaxView.TAG_WIDTH) {   // moved into tag area
                setCursor(defaultCursor);
                mouseOverObject = false;
                setTagAreaCursor(x, y);
                mouseInTag = true;
            }
        }
    }

    /**
     * Set the mouse cursor for the tag area. 
     */
    private void setTagAreaCursor(int x, int y)
    {
        if(pointOverObjectIcon(x, y) != mouseOverObject) {  // entered or left object
            mouseOverObject = !mouseOverObject;
            if(mouseOverObject)
                setCursor(objectCursor);
            else
                setCursor(defaultCursor);
        }        
    }

    // ---- end of MouseMotionListener interface ----

    /**
     * Set the keymap for this text area. Especially: take care that cursor 
     * movement is restricted so that the cursor remains in the last line,
     * and interpret Return keys to evaluate commands.
     */
    private void defineKeymap()
    {
        Keymap newmap = JTextComponent.addKeymap("texteval", getKeymap());

        // Note that we rely on behavior of the current DefaultEditorKit default key typed
        // handler to actually insert characters (it calls replaceSelection to do so,
        // which we've overridden).

        Action action = new ExecuteCommandAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), action);

        softReturnAction = new ContinueCommandAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK), softReturnAction);

        action = new BackSpaceAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), action);
        
        action = new CursorLeftAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), action);

        if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT) == false)
        {
            action = new HistoryBackAction();
            newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), action);
            newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), action);

            action = new HistoryForwardAction();
            newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), action);
            newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), action);
        }
        
        action = new TransferFocusAction(true);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), action);

        action = new TransferFocusAction(false);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), action);
        
        action = new CursorHomeAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), action);

        setKeymap(newmap);
    }

    final class ExecuteCommandAction extends AbstractAction {

        /**
         * Create a new action object. This action executes the current command.
         */
        public ExecuteCommandAction()
        {
            super("ExecuteCommand");
        }
        
        /**
         * Execute the text of the current line in the text area as a Java command.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy) {
                return;
            }
            
            String line = getCurrentLine();
            currentCommand = (currentCommand + line).trim();
            if(currentCommand.length() != 0) {
                       
                history.add(line);
                append("\n");
                firstTry = true;
                setEditable(false);    // don't allow input while we're thinking
                busy = true;
                if (textParser == null) {
                    textParser = new TextAnalyzer(frame.getProject().getEntityResolver(),
                            frame.getPackage().getQualifiedName(), TextEvalPane.this);
                }
                String retType;
                retType = textParser.parseCommand(currentCommand);
                wrappedResult = (retType != null && retType.length() != 0);
                
                // see if any variables were declared
                if (retType == null) {
                    firstTry = false; // Only try once.
                    currentCommand = textParser.getAmendedCommand();
                    List<DeclaredVar> declaredVars = textParser.getDeclaredVars();
                    if (declaredVars != null) {
                        Iterator<DeclaredVar> i = declaredVars.iterator();
                        while (i.hasNext()) {
                            if (newlyDeclareds == null) {
                                newlyDeclareds = new ArrayList<CodepadVar>();
                            }
                            if (autoInitializedVars == null) {
                                autoInitializedVars = new ArrayList<String>();
                            }
                            
                            DeclaredVar dv = i.next();
                            String declaredName = dv.getName();
                            
                            if (getLocalVar(declaredName) != null) {
                                // The variable has already been declared
                                String errMsg = Config.getString("pkgmgr.codepad.redefinedVar");
                                errMsg = Utility.mergeStrings(errMsg, declaredName);
                                showErrorMsg(errMsg);
                                removeNewlyDeclareds();
                                return;
                            }
                            
                            CodepadVar cpv = new CodepadVar(dv.getName(), dv.getDeclaredType(), dv.isFinal());
                            newlyDeclareds.add(cpv);
                            localVars.add(cpv);

                            // If the variable was declared but not initialized, the codepad
                            // auto-initializes it. We add to a list so that we can display
                            // a warning to that effect, once the command has completed.
                            if (! dv.isInitialized()) {
                                autoInitializedVars.add(dv.getName());
                            }
                        }
                    }
                }
                
                invoker = new Invoker(frame, TextEvalPane.this, currentCommand, TextEvalPane.this);
                invoker.setImports(textParser.getImportStatements());
                if (!invoker.doFreeFormInvocation(retType)) {
                    // Invocation failed
                    firstTry = false;
                    putError("Invocation failed.", null);
                }
            }
            else {
                markAs(TextEvalSyntaxView.OUTPUT, Boolean.TRUE);
            }
        }
    }

    final class ContinueCommandAction extends AbstractAction {
        
        /**
         * Create a new action object. This action reads the current
         * line as a start for a new command and continues reading the 
         * command in the next line.
         */
        public ContinueCommandAction()
        {
            super("ContinueCommand");
        }
        
        /**
         * Read the text of the current line in the text area as the
         * start of a Java command and continue reading in the next line.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy)
                return;
            
            String line = getCurrentLine();
            currentCommand += line + " ";
            history.add(line);
            markAs(TextEvalSyntaxView.CONTINUE, Boolean.TRUE);
        }
    }

    final class BackSpaceAction extends AbstractAction {

        /**
         * Create a new action object.
         */
        public BackSpaceAction()
        {
            super("BackSpace");
        }
        
        /**
         * Perform a backspace action.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy) {
                return;
            }
            
            try {
                if(getSelectionEnd() == getSelectionStart()) { // no selection
                    if (getCurrentColumn() > 0) {
                        doc.remove(getCaretPosition()-1, 1);
                    }
                }
                else {
                    replaceSelection("");
                }
            }
            catch(BadLocationException exc) {
                Debug.reportError("bad location in text eval operation");
            }
        }
    }

    final class CursorLeftAction extends AbstractAction {

        /**
         * Create a new action object.
         */
        public CursorLeftAction()
        {
            super("CursorLeft");
        }

        /**
         * Move the cursor left (if allowed).
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy) {
                return;
            }
            
            if(getCurrentColumn() > 0) {
                Caret caret = getCaret();
                caret.setDot(caret.getDot() - 1);
            }
        }
    }
    
    final class CursorHomeAction extends AbstractAction {
        
        /**
         * Create a new action object.
         */
        public CursorHomeAction()
        {
            super("CursorHome");
        }
        
        /**
         * Home the cursor. This overrides the default behaviour (move to
         * column 0) to "move to column 1".
         */
        public void actionPerformed(ActionEvent event)
        {
            if (busy) {
                return;
            }
            
            Caret caret = getCaret();
            int curCol = getColumnFromPosition(caret.getDot());
            if (curCol != 1) {
                caret.setDot(caret.getDot() - curCol);
            }
        }
    }

    final class HistoryBackAction extends AbstractAction {

        /**
         * Create a new action object.
         */
        public HistoryBackAction()
        {
            super("HistoryBack");
        }
        
        /**
         * Set the current line to the previous input history entry.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy)
                return;
            
            String line = history.getPrevious();
            if(line != null) {
                replaceLine(line);
            }
        }

    }

    final class HistoryForwardAction extends AbstractAction {

        /**
         * Create a new action object.
         */
        public HistoryForwardAction()
        {
            super("HistoryForward");
        }
        
        /**
         * Set the current line to the next input history entry.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if (busy)
                return;
            
            String line = history.getNext();
            if(line != null) {
                replaceLine(line);
            }
        }

    }

    final class TransferFocusAction extends AbstractAction {
        private boolean forward;
        /**
         * Create a new action object.
         */
        public TransferFocusAction(boolean forward)
        {
            super("TransferFocus");
            this.forward = forward;
        }
        
        /**
         * Transfer the keyboard focus to another component.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if(forward)
                transferFocus();
            else
                transferFocusBackward();
        }

    }    

    final class ObjectInfo {
        DebuggerObject obj;
        InvokerRecord ir;
        
        /**
         * Create an object holding information about an invocation.
         */
        public ObjectInfo(DebuggerObject obj, InvokerRecord ir) {
            this.obj = obj;
            this.ir = ir;
        }
    }
    
    final class CodepadVar implements NamedValue {
        
        String name;
        boolean finalVar;
        boolean initialized = false;
        JavaType type;
        
        public CodepadVar(String name, JavaType type, boolean finalVar)
        {
            this.name = name;
            this.finalVar = finalVar;
            this.type = type;
        }
        
        public String getName()
        {
            return name;
        }
        
        public JavaType getGenType()
        {
            return type;
        }
        
        public boolean isFinal()
        {
            return finalVar;
        }
        
        public boolean isInitialized()
        {
            return initialized;
        }
        
        public void setInitialized()
        {
            initialized = true;
        }
    }
    
}
