package bluej.debugmgr.texteval;

import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;

import bluej.BlueJEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.FreeFormCallDialog;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;

/**
 * A customised text area for use in the BlueJ Java text evaluation.
 *
 * @author  Michael Kolling
 * @version $Id: TextEvalArea.java 2612 2004-06-14 20:36:28Z mik $
 */
public final class TextEvalArea extends JScrollPane
    implements ResultWatcher
{
    private static final int BUFFER_LINES = 40;
    private static final String PROMPT = "> ";
    private static final int PROMPT_LENGTH = 2;
    
    private JTextArea text;
    private String currentCommand;
    private PkgMgrFrame frame;
    
    /**
     * Create a new text area with given size.
     */
    public TextEvalArea(PkgMgrFrame frame, Font font)
    {
        this.frame = frame;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(5,0,5,0)));

        text = new JTextArea(6,40);
        setViewportView(text);
        text.setFont(font);
        append(PROMPT);

        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        text.getKeymap().addActionForKeyStroke(enterKey, new ExecuteCommandAction(this));
    }

    public void requestFocus()
    {
        text.requestFocus();
    }

    /**
     * Execute the text of the current line in the text area as a Java command.
     */
    public void executeCommand()
    {
        currentCommand = getCurrentLine();
        append("\n");
        new Invoker(frame, currentCommand, this);
        append(PROMPT);
    }
    
    //   --- ResultWatcher interface ---

    /**
     * An invocation has completed - here is the result.
     * If the invocation has a void result (note that is a void type), name == null.
     * It should be possible for result to be null and name to not be,
     * though no code currently creates this situation.
     */
    public void putResult(DebuggerObject result, String name, InvokerRecord ir)
    {
        append("(result)\n");

//        getObjectBench().addInteraction(ir);
//        if (result != null) {
//            ResultInspector viewer = ResultInspector.getInstance(result, name,
//                    getPackage(), ir, getExpressionInformation(), PkgMgrFrame.this);
//            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
//        } else {
//            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, null);
//        }
}
    
    /**
     * An invocation has failed - here is the error message
     */
    public void putError(String message)
    {
        append("(error)\n");
    }
    
    /**
     * A watcher shuold be able to return information about the result that it
     * is watching. T is used to display extra information (about the expression
     * that gave the result) when the result is shown.
     * 
     * @return An object with information on the expression
     */
    public ExpressionInformation getExpressionInformation()
    {
        return new ExpressionInformation(currentCommand);
    }

    //   --- end of ResultWatcher interface ---


/**
     * Append some text to this area.
     * @param s The text to append.
     */
    public void append(String s)
    {
        text.append(s);

        int lines = text.getLineCount();
        if(lines > BUFFER_LINES) {
            try {
                int linePos = text.getLineStartOffset(lines-BUFFER_LINES);
                text.replaceRange("", 0, linePos);
            }
            catch(BadLocationException exc) {
                Debug.reportError("bad location in terminal operation");
            }
        }
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Get the text of the current line (the last line) of this area.
     * @return The text of the last line.
     */
    private String getCurrentLine()
    {
        int lastLine = text.getLineCount() - 1;
        try {
            int lineStart = text.getLineStartOffset(lastLine) + PROMPT_LENGTH;
            int lineEnd = text.getLineEndOffset(lastLine);
            return text.getText(lineStart, lineEnd-lineStart);
        }
        catch(BadLocationException exc) {
            Debug.reportError("bad location in text eval operation");
            return "";
        }
    }
}
