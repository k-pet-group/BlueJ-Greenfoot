package bluej.debugmgr.texteval;

import java.awt.*;
import java.awt.event.*;

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
 * @version $Id: TextEvalArea.java 2879 2004-08-17 07:31:45Z mik $
 */
public final class TextEvalArea extends JScrollPane
    implements ResultWatcher, KeyListener, FocusListener, MouseMotionListener
{
    // The cursor to use while hovering over object icon
    private static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor objectCursor = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor textCursor = new Cursor(Cursor.TEXT_CURSOR);
    
    private static final Color selectionColour = Config.getSelectionColour();

    //    private JTextArea text;
    private TextEvalPane text;
    private MoeSyntaxDocument doc;  // the text document behind the editor pane
    private String currentCommand = "";
    private PkgMgrFrame frame;
    private Invoker invoker = null;
    private boolean firstTry;
    private IndexHistory history;
    private boolean mouseInTag = false;
    private boolean mouseOverObject = false;
    
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
     * Sets whether or not this component is enabled.  
     */
    public void setEnabled(boolean enabled)
    {
        text.setEnabled(enabled);
    }

    /**
     * Clear all text in this text area.
     */
    public void clear()
    {
        text.setText(" ");
        caretToEnd();
    }

    //   --- ResultWatcher interface ---

    /**
     * An invocation has completed - here is the result.
     * If the invocation has a void result (note that is a void type), result == null.
     */
    public void putResult(DebuggerObject result, String name, InvokerRecord ir)
    {
        frame.getObjectBench().addInteraction(ir);

        append(" ");
        if (result != null) {
            //Debug.message("type:"+result.getFieldValueTypeString(0));

            String resultString = result.getFieldValueString(0);
            String resultType = JavaNames.stripPrefix(result.getFieldValueTypeString(0));
            boolean isObject = result.instanceFieldIsObject(0);
            
            if(isObject)
                objectOutput(resultString + "   (" + resultType + ")", 
                             new ObjectInfo(result.getFieldObject(0), ir));
            else
                output(resultString + "   (" + resultType + ")");
            
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
            append(" ");
            error(message);
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
    }

    //   --- end of ResultWatcher interface ---

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
            if(x > BlueJSyntaxView.TAG_WIDTH) {    // moved out of tag area
                text.setCursor(textCursor);
                mouseInTag = false;
            }
            else 
                setTagAreaCursor(x, y);
        }
        else {
            if(x <= BlueJSyntaxView.TAG_WIDTH) {   // moved into tag area
                text.setCursor(defaultCursor);
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
                text.setCursor(objectCursor);
            else
                text.setCursor(defaultCursor);
        }        
    }

    /**
     * Check whether a given point on screen is over an object icon.
     */
    private boolean pointOverObjectIcon(int x, int y)
    {
        int pos = text.getUI().viewToModel(text, new Point(x, y));
        ObjectInfo objInfo = objectAtPosition(pos);
        return objInfo != null;        
    }
    
    // ---- end of MouseMotionListener interface ----

    /**
     * Inspect the given object.
     * This is done with a delay, because we are in the middle of a mouse click,
     * and focus gets weird otherwise.
     */
    public void inspectObject(ObjectInfo objInfo)
    {
        final ObjectInfo oi = objInfo;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ObjectInspector viewer = ObjectInspector.getInstance(oi.obj, 
                        null, frame.getPackage(), oi.ir, frame);
            }
        });
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
                dnd.startDrag(text, frame, objInfo.obj, objInfo.ir);
            }
            else if(clickCount == 2) {   // double click
                inspectObject(objInfo);
            }
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

    // --- FocusListener interface ---
    
    /**
     * Note that the object bench got keyboard focus.
     */
    public void focusGained(FocusEvent e) 
    {
        if (!e.isTemporary()) {
            setBorder(Config.focusBorder);        
            repaint();
        }
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
    public void objectOutput(String s, ObjectInfo objInfo)
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
    public void error(String s)
    {
        try {
            doc.insertString(doc.getLength(), "Error: " + s, null);
            markAs(TextEvalSyntaxView.ERROR, Boolean.TRUE);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Mark the last line of the text area as output.
     */
    private void markAs(String flag, Object value)
    {
        append("\n ");          // ensure space at the beginning of every line
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(flag, value);
        doc.setParagraphAttributes(doc.getLength()-2, a);
        text.repaint();
    }
    
    /**
     * Mark the last line of the text area as output.
     */
    private void markCurrentAs(String flag, Object value)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(flag, value);
        doc.setParagraphAttributes(doc.getLength(), a);
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
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Insert some text to this area.
     * @param s The text to insert.
     */
    private void insert(String s)
    {
        try {
            doc.insertString(text.getCaretPosition(), s, null);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    /**
     * Replace the text of the current line with some new text.
     * @param s The new text for the line.
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
        return getColumnFromPosition(pos);
    }

    /**
     * Return tha column for a given position.
     */
    public int getColumnFromPosition(int pos)
    {
        int lineStart = doc.getParagraphElement(pos).getStartOffset();
        return (pos - lineStart);        
    }
    
    /**
     * Create the Swing component representing the text area.
     */
    private void createComponent(Font font)
    {
        text = new TextEvalPane();
        text.setMargin(new Insets(2,2,2,2));

        text.setEditorKit(new MoeSyntaxEditorKit(true));
        text.setCaret(new TextEvalCaret(this));
        text.addKeyListener(this);
        text.addFocusListener(this);
        text.addMouseMotionListener(this);
        text.setFont(font);
        text.setSelectionColor(selectionColour);
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

        Action action = new InsertCharacterAction();
        newmap.setDefaultAction(action);

        action = new ExecuteCommandAction();
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
    
    // ======= Actions =======
    
    final class InsertCharacterAction extends AbstractAction {

        /**
         * Create a new action object. This action executes the current command.
         */
        public InsertCharacterAction()
        {
            super("InsertCharacter");
        }
        
        /**
         * Insert a character into the text.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if(!text.isEditable())
                return;
            
            String s = event.getActionCommand();  // will always be length 1
            if(s.charAt(0) != '\n') {             // bug workaround: enter goes through default
                                                  //  action as well as set action
                if(text.isLegalCaretPos())
                    insert(s);
                else
                    append(s);
            }
        }
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
            String line = getCurrentLine();
            currentCommand = (currentCommand + line).trim();
            if(currentCommand.length() != 0) {
                       
                history.add(line);
                append("\n");      // ensure space at the beginning of every line, because
                                    // line properties do not work otherwise
                markCurrentAs(TextEvalSyntaxView.OUTPUT, Boolean.TRUE);
                firstTry = true;
                text.setEditable(false);    // don't allow input while we're thinking
                invoker = new Invoker(frame, currentCommand, TextEvalArea.this);
            }
            else {
                markAs(TextEvalSyntaxView.OUTPUT, Boolean.TRUE);
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

        /**
         * Move the cursor left (if allowed).
         */
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
        
        /**
         * Set the current line to the previous input history entry.
         */
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
        
        /**
         * Set the current line to the next input history entry.
         */
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
        
        /**
         * Transfer the keyboard focus to another component.
         */
        final public void actionPerformed(ActionEvent event)
        {
            if(forward)
                text.transferFocus();
            else
                text.transferFocusBackward();
        }

    }

}
