package bluej.debugmgr.texteval;

import bluej.editor.moe.BlueJSyntaxView;

import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;

/**
 * A customised caret for Moe. It gets most of its bahaviour from
 * Swing's "DefaultCaret" and adds some functionality.
 *
 * @author  Michael Kolling
 */

public class TextEvalCaret extends DefaultCaret  
{
   
    private TextEvalArea textEval;

    /**
     * Constructs a Moe Caret
     */
    public TextEvalCaret(TextEvalArea textEval) 
    {
        super();
        this.textEval = textEval;
        setBlinkRate(0);
    }

    /**
     * Redefinition of caret positioning (after mouse click). Here, we
     * first check whether the click was in the tag line. If it was, we
     * toggle the breakpoint, if not we just position the caret as usual.
     */
    protected void positionCaret(MouseEvent e) 
    {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);

        if (e.getX() > BlueJSyntaxView.TAG_WIDTH) {
            if (pos >= 0) {
                setDot(pos);
            }
        }
        else {
            textEval.tagAreaClick(pos, e.getClickCount());
        }
    }

    /**
     * Tries to move the position of the caret from
     * the coordinates of a mouse event, using viewToModel(). 
     * This will cause a selection if the dot and mark
     * are different.
     *
     * @param e the mouse event
     */
    protected void moveCaret(MouseEvent e) 
    {
        if (e.getX() > BlueJSyntaxView.TAG_WIDTH) {
            Point pt = new Point(e.getX(), e.getY());
            Position.Bias[] biasRet = new Position.Bias[1];
            int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);
            if (pos >= 0) {
                moveDot(pos);
            }
        }
    }
}


