package bluej.editor.moe;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * This class manages the view of ...
 *
 * @author  Michael Kolling
 */

public class MoeBoxView extends BoxView  {
   

    /**
     * Constructs a MoeBoxView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public MoeBoxView(Element elem, int axis) 
    {
	super(elem, axis);
    }

    /**
     * 
     */
    public void paint(Graphics g, Shape allocation) 
    {
	// paine the tag line (possibly grey, always a separator line)

	Rectangle bounds = allocation.getBounds();

	if(Boolean.FALSE.equals(
		getDocument().getProperty(MoeEditor.COMPILED))) {
	    g.setColor(Color.lightGray);
	    g.fillRect(0, 0, bounds.x + MoeEditor.TAG_WIDTH, 
		       bounds.y + bounds.height);
	}
	g.setColor(Color.black);
	g.drawLine(bounds.x + MoeEditor.TAG_WIDTH, 0, 
		   bounds.x + MoeEditor.TAG_WIDTH, bounds.y + bounds.height);

	// paint the lines
        super.paint(g, allocation);
    }

}

