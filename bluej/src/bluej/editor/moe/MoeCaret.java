package bluej.editor.moe;

import bluej.utility.Debug;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * A customised caret for Moe. It gets most of its bahaviour from
 * Swing's "DefaultCaret" and adds some functionality.
 *
 * @author  Michael Kolling
 */

public class MoeCaret extends DefaultCaret  
{
   
    MoeEditor editor;

    /**
     * Constructs a Moe Caret
     */
    public MoeCaret(MoeEditor editor) 
    {
        super();
        this.editor = editor;
    }

    /**
     * Redefinition of caret positioning (after mouse click). Here, we
     * first check whether the click was in the tag line. If it was, we
     * toggle the breakpoint, if not we just position the caret as usual.
     */
    protected void positionCaret(MouseEvent e) 
    {
        editor.clearMessage();
        Point pt = new Point(e.getX() - 15, e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);

        if (e.getX() > MoeEditor.TAG_WIDTH) {
            if(biasRet[0] == null)
                biasRet[0] = Position.Bias.Forward;
            if (pos >= 0) {
                setDot(pos); 
                
                // clear the preferred caret position
                // see: JCaret's UpAction/DownAction
                setMagicCaretPosition(null);
            
            }
        }
        else {
         
            editor.toggleBreakpoint(pos);

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
    protected void moveCaret(MouseEvent e) {
         Point pt = new Point(e.getX() - 15, e.getY());
         Position.Bias[] biasRet = new Position.Bias[1];
         int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);
         if(biasRet[0] == null)
             biasRet[0] = Position.Bias.Forward;
         if (pos >= 0) {
             //    moveDot(pos - 2);
             moveDot(pos);
         }
     }


    protected void fireStateChanged()
    {
        editor.clearMessage();
        super.fireStateChanged();
    }

    

}


