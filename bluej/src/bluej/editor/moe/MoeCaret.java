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

public class MoeCaret extends DefaultCaret  {
   
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
	if (e.getX() > MoeEditor.TAG_WIDTH)
	    super.positionCaret(e);
	else {
	    Point pt = new Point(e.getX(), e.getY());
	    Position.Bias[] biasRet = new Position.Bias[1];
	    int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);
	    editor.toggleBreakpoint(pos);
	}
    }

    protected void fireStateChanged()
    {
	editor.clearMessage();
	super.fireStateChanged();
    }

}


