package bluej.guibuilder.graphics;

import java.awt.event.*;

/**
 * A listener for a StateButton, that cycles through a 
 * series of images which reside in the StateButton class.  
 * Each action event advances the image and invokes the
 * superclass (SpringyImageButtonListener) actionEvent().
 * 
 * @version 1.0, Apr 1 1996
 * @version 1.1, Dec 1996
 *
 *    Modified for new event handling model.
 *
 * @author  David Geary
 * @see     StateButton
 * @see     SpringyImageButtonListener
 * @see     gjt.test.StateButtonTest
 */
class StateButtonListener extends SpringyImageButtonListener {
    public void activate(ImageButton ib) {
		((StateButton)ib).advanceImage();
		super.activate(ib);
    }
}
