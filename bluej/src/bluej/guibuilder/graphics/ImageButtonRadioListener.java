package bluej.guibuilder.graphics;

import java.awt.event.*;

/**
 * A listener for a container that contains multiple image
 * buttons.  This listener enforces exclusive selection of
 * image buttons that reside in the container.
 *
 * @version 1.0, Apr 1 1996
 * @author  David Geary
 * @see     ImageButtonPanelListener
 * @see     ImageButton
 * @see     ImageButtonPanel
 * @see     gjt.test.ToolbarTest
 */
class ImageButtonRadioListener implements ActionListener {
    ImageButton down;

    public void actionPerformed(ActionEvent event) {
    	ImageButton button = (ImageButton) event.getSource();

        if(down != button) 
			down.paintRaised();

        down = button;
    }
	public void select(ImageButton button) {
		if(down != null && down != button) {
			if(button.isShowing()) down.paintInset();
			else                   down.setInset();
		}
		down = button;
	}
}
