package bluej.guibuilder.graphics;

import java.awt.*;
import java.awt.event.*;

/**
 * An ImageButtonListener that reacts to 
 * mousePressed/mouseClicked events exactly as a 
 * java.awt.Button does.<p>
 *
 * @version 1.0, Apr 1 1996
 * @version 1.1, Nov 8 1996
 *
 *    Took out check for right-mouse ib; mouse events are
 *    handled no matter which mouse ib initiated the event.
 *
 * @version 1.2, Dec 20, 1996
 *
 *    Refactored for the new event handling model.
 *
 * @author  David Geary
 * @see     ImageButton
 * @see     ImageButtonListener
 * @see     StickyImageButtonListener
 */
public class SpringyImageButtonListener extends 
                                        ImageButtonListener {

	public void activate(ImageButton ib) {
		ib.processActionEvent();
		if( ! ib.isRaised()) ib.paintRaised();
		ib.setArmed(false);
	}
	public void arm(ImageButton ib) {
		if( ! ib.isInset()) ib.paintInset();
		ib.setArmed(true);
	}
	public void disarm(ImageButton ib) {
		if( ! ib.isRaised()) ib.paintRaised();
		ib.setArmed(false);
	}
	public void mousePressed(MouseEvent me) {
		ImageButton ib = (ImageButton)me.getComponent();
		if(! ib.isDisabled())
			arm(ib);
	}
	public void mouseClicked(MouseEvent me) {
		ImageButton ib = (ImageButton)me.getComponent();
		if(! ib.isDisabled() && ib.isArmed()) {
			activate(ib);
		}
	}
	public void mouseReleased(MouseEvent me) {
		ImageButton ib = (ImageButton)me.getComponent();
		if(ib.contains(me.getPoint().x, me.getPoint().y))
			mouseClicked(me);
	}
	public void mouseDragged(MouseEvent me) {
		ImageButton ib = (ImageButton)me.getComponent();
		if(! ib.isDisabled()) {
			int x = me.getPoint().x; 
			int y = me.getPoint().y;

        	if(ib.contains(x,y)) arm   (ib);
        	else                 disarm(ib);
		}
	}
}
