package javablue.gui;

import java.awt.*;
import java.awt.event.*;

/**
 * An ImageButtonListener that causes its associated 
 * ImageButton to "stick" when activated.  If the ImageButton 
 * is raised it depresses<b>[1]</b> upon a mouse down and stays 
 * down upon a subsequent mouse up event.  The same "sticky" 
 * behaviour occurs when a depressed ImageButton encounters a 
 * mouse down followed by a subsequent mouse up.<p>
 *
 * Note that false is returned from mouse event handlers; 
 * therefore mouse events will be propagated to the 
 * ImageButton's container.  While this is not always 
 * desirable, it was deemed a better default than swallowing 
 * the event here.  Subclasses may, of course, modify this 
 * behavior.<p>
 * 
 * <b>[1]</b> No psychiatric consultation is necessary.<p>
 *
 * @version 1.0, Apr 1 1996
 *
 *    Took out check for right-mouse button; mouse events are
 *    handled no matter which mouse button initiated the event.
 *
 * @version 1.1, Dec 20 1996
 *
 *    Upgraded to 1.1 event handling
 *
 * @author  David Geary
 * @see     ImageButton
 * @see     ImageButtonListener
 * @see     SpringyImageButtonListener
 */
public class StickyImageButtonListener extends 
									   ImageButtonListener {
    private boolean buttonUpOnLastMouseDown = true;

	public void activate(ImageButton ib) { 
		ib.processActionEvent();
		ib.setArmed(false);
	}
	public void arm(ImageButton ib) { 
		ib.setArmed(true);
	}
	public void disarm(ImageButton ib) { 
		ib.setArmed(false);
	}
    public void mousePressed(MouseEvent event) {
		ImageButton ib = (ImageButton)event.getSource();
		if(! ib.isDisabled()) {
        	if(ib.isRaised()) ib.paintInset();
        	else              ib.paintRaised();
        	buttonUpOnLastMouseDown = ib.isRaised();
			arm(ib);
		}
    }
	public void mouseClicked(MouseEvent event) {
		ImageButton ib = (ImageButton)event.getSource();
		if(! ib.isDisabled() && ib.isArmed())
			activate(ib);
	}
	public void mouseReleased(MouseEvent event) {
		ImageButton ib = (ImageButton)event.getSource();
		Point       pt = event.getPoint();
		if(ib.contains(pt.x, pt.y))
			mouseClicked(event);
	}
    public void mouseDragged(MouseEvent event) {
		ImageButton ib = (ImageButton)event.getSource();
		if(! ib.isDisabled()) {
			Point loc = event.getPoint();

        	if(ib.contains(loc.x,loc.y)) {
        	    if(buttonUpOnLastMouseDown) {
        	        if( ! ib.isRaised()) 
        	            ib.paintRaised();
        	    }
        	    else 
        	        if(ib.isRaised())
        	            ib.paintInset();
        	}
        	else {
        	    if(buttonUpOnLastMouseDown) {
        	        if(ib.isRaised())
        	            ib.paintInset();
				}
        		else 
        			if( ! ib.isRaised())
        				ib.paintRaised();
        	}
    	}
	}
}
