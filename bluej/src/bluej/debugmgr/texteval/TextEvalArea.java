package bluej.debugmgr.texteval;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.*;

import bluej.BlueJEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ExpressionInformation;
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
 * @version $Id: TextEvalArea.java 2673 2004-06-28 14:30:30Z mik $
 */
public final class TextEvalArea extends JScrollPane
    implements ResultWatcher
{
    private static final int BUFFER_LINES = 40;
    
    //    private JTextArea text;
    private JEditorPane text;
    private MoeSyntaxDocument doc;  // the text document behind the editor pane
    private String currentCommand = "";
    private PkgMgrFrame frame;
    private Invoker invoker = null;
    private boolean firstTry;
    
    /**
     * Create a new text area with given size.
     */
    public TextEvalArea(PkgMgrFrame frame, Font font)
    {
        this.frame = frame;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(5,0,5,0)));

        text = new JEditorPane();
        text.setMargin(new Insets(2,2,2,2));
        text.setEditorKit(new MoeSyntaxEditorKit(true));

        doc = (MoeSyntaxDocument) text.getDocument();
        doc.setTokenMarker(new JavaTokenMarker());

        setViewportView(text);
        text.setFont(font);
        text.setText(" ");      // ensure space at the beginning of every line

        defineKeymap();
        
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        setPreferredSize(new Dimension(200,100));
    }

    /**
     * Request to get the keyboard focus into the text evaluation area.
     */
    public void requestFocus()
    {
        text.requestFocus();
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
            
            output(resultString + "   (" + resultType + ")");
            
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, resultString);
        } 
        else {
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, null);
        }
    }
    
    /**
     * An invocation has failed - here is the error message
     */
    public void putError(String message)
    {
    		if(firstTry) {
    			// append("   --error: " + message + "\n");
    			firstTry = false;
    	        invoker.tryAgain();
    		}
    		else {
    			error(message);
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


    private void output(String s)
    {
        try {
            doc.insertString(doc.getLength(), s, null);
            markAs(MoeSyntaxView.OUTPUT);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in terminal operation");
        }
    }
    
    private void error(String s)
    {
        try {
            doc.insertString(doc.getLength(), "Error: " + s, null);
            markAs(MoeSyntaxView.ERROR);
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
        int lineStart = line.getStartOffset();
        int lineEnd = line.getEndOffset();
        
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

        action = new CursorLeftAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), action);

        action = new HistoryBackAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), action);

        action = new HistoryForwardAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), action);

        action = new NoAction();
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, Event.SHIFT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.ALT_MASK), action);
        newmap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, Event.ALT_MASK), action);


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
            currentCommand += getCurrentLine();
            append("\n ");      // ensure space at the beginning of every line, because
                                // line properties do not work otherwise
            firstTry = true;
            System.out.println("comm:"+currentCommand);
            invoker = new Invoker(frame, currentCommand, TextEvalArea.this);
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
            currentCommand += getCurrentLine() + " ";
            append("\n ");      // ensure space at the beginning of every line, because
                                // line properties do not work otherwise
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
        }

    }

    final class NoAction extends AbstractAction {

        /**
         * Create a new action object.
         */
        public NoAction()
        {
            super("DoNothing");
        }
        
        /**
         * Empty action - do nothing.
         */
        final public void actionPerformed(ActionEvent event)
        {
        }

    }

}
