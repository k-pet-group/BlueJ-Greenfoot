// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

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
    // matching bracket highlight holder
    private Object matchingBracketHighlight;

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
        if(e.getID() != MouseEvent.MOUSE_PRESSED)
            return;

        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);

        if (e.getX() > BlueJSyntaxView.TAG_WIDTH) {
            if (pos >= 0 && textEval.isLegalCaretPos(pos)) {
                setDot(pos);
                setMagicCaretPosition(null);
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
            if (pos >= 0 && textEval.isLegalCaretPos(pos)) {
                moveDot(pos);
            }
        }
    }
    
    /**
     * Set the dot (caret) to a new location.
     */
    public void setDot(int dot)
    {
        if(textEval.isLegalCaretPos(dot))
            super.setDot(dot);
    }
    
    /**
     * Move the dot (caret) to a new location, leaving behind the mark.
     */
    public void moveDot(int dot)
    {
        if(textEval.isLegalCaretPos(dot))
            super.moveDot(dot);
    }
}


