package bluej.guibuilder.graphics;

import java.awt.event.*;

/**
 * A listener for image buttons.<p>
 *
 * An ImageButtonListener is both a MouseListener (by way of
 * extending MouseAdapter) and a MouseMotionListener.  
 * MouseMotionListener only has two abstract methods:<pre>
 *
 *    void mouseMoved  (MouseEvent event)
 *    void mouseDragged(MouseEvent event)</pre>
 * 
 * In addition, ImageButtonListener adds some abstract method
 * definitions of its own:
 *
 *    void activate(ImageButton)
 *    void arm     (ImageButton)
 *    void disarm  (ImageButton)
 
 * @version 1.0, Dec 19 1996
 *
 * @author  David Geary
 * @see     java.awt.event.MouseListener
 * @see     java.awt.event.MouseAdapter
 * @see     java.awt.event.MouseMotionListener
 *
 * @see     ImageButton
 * @see     SpringyImageButtonListener
 * @see     StickyImageButtonListener
 * @see     gjt.test.ImageButtonTest
 */
abstract public class ImageButtonListener 
	extends MouseAdapter implements MouseMotionListener {

	public void mouseMoved(MouseEvent event) { }
	public void mouseDragged(MouseEvent event) { }

	abstract public void activate  (ImageButton button);
	abstract public void arm       (ImageButton button);
	abstract public void disarm    (ImageButton button);
}
