package bluej.debugmgr.texteval;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.*;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.text.*;

import bluej.BlueJEvent;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.IndexHistory;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.editor.moe.*;

import org.gjt.sp.jedit.syntax.*;

/**
 * A customised text area for use in the BlueJ Java text evaluation.
 *
 * @author  Michael Kolling
 * @version $Id: TextEvalArea.java 2728 2004-07-04 17:57:55Z mik $
 */
public final class TextEvalArea extends JScrollPane
    implements ResultWatcher, KeyListener, FocusListener
{
    private static final int BUFFER_LINES = 40;

    //    private JTextArea text;
    private JEditorPane text;
    private MoeSyntaxDocument doc;  // the text document behind the editor pane
    private String currentCommand = "";
    private PkgMgrFrame frame;
    private Invoker invoker = null;
    private boolean firstTry;
    private IndexHistory history;
    
    // the last object that we handled (this can be operated on)
    private DebuggerObject lastObject;
    // the invoker record of the last successful call
    private InvokerRecord lastInvokerRecord;
    
    
    /**
     * Create a new text area with given size.
     */
    public TextEvalArea(PkgMgrFrame frame, Font font)
    {
        this.frame = frame;
        createComponent(font);
        
        history = new IndexHistory(20);
    }

    /**
     * Request to get the keyboard focus into the text evaluation area.
     */
    public void requestFocus()
    {
        text.requestFocus();
    }

    
    /**
     * Try to get the last object that was handled onto the object bench.
     * This is the implementation of the interactive 'get' command.
     */
    public void getObjectToBench()
    {
        if(lastObject != null) {
            frame.getPackage().getEditor().raisePutOnBenchEvent(this, lastObject, lastInvokerRecord);
        }
        else {
            error("'Get' can only be used for objects. The last result was not an object.");
        }
    }
    

    /**
     * Try to inspect the last object that was handled.
     * This is the implementation of the interactive 'inspect' command.
     */
    public void inspectObject()
    {
            if(lastObject != null) {
                ObjectInspector viewer =
                    ObjectInspector.getInstance(lastObject, null, frame.getPackage(), null, frame);
            }
            else {
                error("'inspect' can only be used for objects. The last result was not an object.");
            }
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
        if(positionHasObject(pos)) {
            if(clickCount == 1) {
                DragAndDropHelper dnd = DragAndDropHelper.getInstance();
                dnd.startDrag(text, frame, lastObject, lastInvokerRecord);
            }
            else if(clickCount == 2) {   // double click
                inspectObject();
            }
        }
    }
    
    /**
     *  Check weather a position has a breakpoint set
     */
    private boolean positionHasObject(int pos)
    {
        Element line = getLineAt(pos);
        return Boolean.TRUE.equals(line.getAttributes().getAttribute(TextEvalSyntaxView.OBJECT));
    }

    /**
     *  Find and return a line by text position
     */
    private Element getLineAt(int pos)
    {
        return doc.getParagraphElement(pos);
    }

    //   --- ResultWatcher interface ---

    /**
     * An invocation has completed - here is the result.
     * If the invocation has a void result (note that is a void type), result == null.
     */
    public void putResult(DebuggerObject result, String name, InvokerRecord ir)
    {
        frame.getObjectBench().addInteraction(ir);

        if (result != null) {
            //Debug.message("type:"+result.getFieldValueTypeString(0));

            String resultString = result.getFieldValueString(0);
            String resultType = JavaNames.stripPrefix(result.getFieldValueTypeString(0));
            boolean isObject = result.instanceFieldIsObject(0);
            
            if(isObject)
                objectOutput(resultString + "   (" + resultType + ")");
            else
                output(resultString + "   (" + resultType + ")");
            
            lastInvokerRecord = ir;
            if(result.instanceFieldIsObject(0)) {
                lastObject = result.getFieldObject(0);
            }
            else {
                lastObject = null;
            }
            
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, resultString);
        } 
        else {
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, null);
        }
        text.setEditable(true);    // allow next input
    }
    
    /**
     * An invocation has failed - here is the error message
     */
    public void putError(String message)
    {
    		if(firstTry) {
    			// append("   --error, first try: " + message + "\n");
    			firstTry = false;
    	        invoker.tryAgain();
    		}
    		else {
            error(message);
            lastObject = null;
            text.setEditable(true);    // allow next input
    		}
    }
    
    /**
     * A watcher shuold be able to return information about the result that it
     * is watching. This may be used to display extra information 
     * (about the expression that gave the result) when the result is shown.
     * Unused for text eval expressions.
     * 
     * @return An object with information on the expression
     */
    public ExpressionInformation getExpressionInformation()
    {
        return null;
//        return new ExpressionInformation(currentCommand);
    }

    //   --- end of ResultWatcher interface ---

    
    // --- FocusListener interface ---
    
    /**
     * Note that the object bench got keyboard focus.
     */
    public void focusGained(FocusEvent e) 
    {
        setBorder(Config.focusBorder);
        repaint();
    }

    
    /**
     * Note that the object bench lost keyboard focus.
     */
    public void focusLost(FocusEvent e) 
    {
        setBorder(Config.normalBorder);
        repaint();
    }

    // --- end of FocusListener interface ---


    //   --- KeyListener interface ---

    /**
     * Workaround for JDK 1.4 bug: backspace keys are still handled internally
     * even when replaced in the keymap. So we explicitly remove them here.
     * This method (and the whole keylistener interface) can be removed
     * when we don't support 1.4 anymore. (Fixed in JDK 5.0.)
     */
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar() == '\b') {
            e.consume();
        }
    }  

    public void keyPressed(KeyEvent e) {}  
    public void keyReleased(KeyEvent e) {}  

    //   --- end of KeyListener interface ---

    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    public void output(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(TextEvalSyntaxView.OUTPUT);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    public void objectOutput(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(TextEvalSyntaxView.OBJECT);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Write an error message to the text area.
     * @param s The message
     */
    public void error(String s)
    {
        try {
            doc.insertString(doc.getLength(), "Error: " + s, null);
            markAs(TextEvalSyntaxView.ERROR);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Mark the last line of the text area as output.
     */
    private void markAs(String flag)
    {
        append("\n ");          // ensure space at the beginning of every line
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(flag, Boolean.TRUE);
        doc.setParagraphAttributes(doc.getLength()-2, a);
        text.repaint();
    }
    
    /**
     * Append some text to this area.
     * @param s The text to append.
     */
    private void append(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            caretToEnd();
            
//            int lines = text.getLineCount();
//            if(lines > BUFFER_LINES) {
//                int linePos = text.getLineStartOffset(lines-BUFFER_LINES);
//                text.replaceRange("", 0, linePos);
//            }
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Append some text to this area.
     * @param s The text to append.
     */
    private void replaceLine(String s)
    {
        Element line = doc.getParagraphElement(doc.getLength());
        int lineStart = line.getStartOffset() + 1;  // ignore space at front
        int lineEnd = line.getEndOffset() - 1;      // ignore newline char
        
        try {
        	    doc.replace(lineStart, lineEnd-lineStart, s, null);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in text eval operation");
        }
    }
    
    /**
     * Move the caret to the end of the text area.
     */
    private void caretToEnd() {
        text.setCaretPosition(doc.getLength());
    }

    public boolean isLegalCaretPos(int pos)
    {
        Element line = doc.getParagraphElement(doc.getLength());
        int lineStart = line.getStartOffset() + 1;  // ignore space at front
        return pos >= lineStart;
    }
    
    /**
     * Get the text of the current line (the last line) of this area.
     * @return The text of the last line.
     */
    private String getCurrentLine()
    {
        Element line = doc.getParagraphElement(doc.getLength());
        int lineStart = line.getStartOffset() + 1;  // ignore space at front
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
        Caret caret = text.getCaret();
        int pos = Math.min(caret.getMark(), caret.getDot());
        int lineStart = doc.getParagraphElement(pos).getStartOffset();
        return (pos - lineStart);
    }

    /**
     * Create the Swing component representing the text area.
     */
    private void createComponent(Font font)
    {
        text = new JEditorPane();
        text.setMargin(new Insets(2,2,2,2));

        text.setEditorKit(new MoeSyntaxEditorKit(true));
        text.setCaret(new TextEvalCaret(this));
        text.addKeyListener(this);
        text.addFocusListener(this);
        text.setFont(font);
        text.setAutoscrolls(false);  // important - dragging objects from this component
                                     // does not work correctly otherwise
        text.setText(" ");      // ensure space at the beginning of every line

        doc = (MoeSyntaxDocument) text.getDocument();
        doc.setTokenMarker(new JavaTokenMarker());

        setViewportView(text);

        defineKeymap();

        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        setPreferredSize(new Dimension(300,100));
        caretToEnd();
    }

    /**
     * Set the keymap for this text area. Especially: take care that cursor 
     * movement is restricted so that the cursor remains in the last line,
     * and interpret Return keys to evaluate commands.
     */
    private void defineKeymap()
    {
        Keymap newmap = JTextComponent.addKeymap("texteval", text.getKeymap());

        Action action = new ExecuteCommandAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), action);

        action = new ContinueCommandAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK), action);

        action = new BackSpaceAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), action);
        
        action = new CursorLeftAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), action);

        action = new HistoryBackAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), action);

        action = new HistoryForwardAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), action);

        action = new TransferFocusAction(true);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), action);

        action = new TransferFocusAction(false);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), action);

        text.setKeymap(newmap);
   
    }
    
    // ======= Actions =======
    
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
            String line = getCurrentLine();
            currentCommand = (currentCommand + line).trim();
            if(currentCommand.length() != 0) {
                       
                history.add(line);
                append("\n ");      // ensure space at the beginning of every line, because
                                    // line properties do not work otherwise
                firstTry = true;
                text.setEditable(false);    // don't allow input while we're thinking
                invoker = new Invoker(frame, currentCommand, TextEvalArea.this);
            }
            else {
                markAs(TextEvalSyntaxView.OUTPUT);
            }
            currentCommand = "";
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
            String line = getCurrentLine();
            currentCommand += line + " ";
            history.add(line);
            markAs(TextEvalSyntaxView.CONTINUE);
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
        
        final public void actionPerformed(ActionEvent event)
        {
            if(getCurrentColumn() > 1) {
                try {
                    doc.remove(text.getCaretPosition()-1, 1);
                }
                catch(BadLocationException exc) {
                    Debug.reportError("bad location in text eval operation");
                }
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
        
        final public void actionPerformed(ActionEvent event)
        {
            if(getCurrentColumn() > 1) {
                Caret caret = text.getCaret();
                caret.setDot(caret.getDot() - 1);
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
        
        final public void actionPerformed(ActionEvent event)
        {
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
        
        final public void actionPerformed(ActionEvent event)
        {
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
        
        final public void actionPerformed(ActionEvent event)
        {
            if(forward)
                text.transferFocus();
            else
                text.transferFocusBackward();
        }

    }

}
