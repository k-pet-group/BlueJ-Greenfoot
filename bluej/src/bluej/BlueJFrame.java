package bluej;

import javax.swing.JFrame;
import java.awt.*;

/**
 ** Generic frame for the various BlueJ windows
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **/
public abstract class BlueJFrame extends JFrame
{
    /**
     * Close it 
     **/
    public void doClose()
    {
	setVisible(false);
    }

    /**
     * setVisible - Make this frame visible. At the same time, notify the 
     *  Main objectof its existence (so that it may be closed when
     *  necessary).
     */
    public void setVisible(boolean visible)
    {
	super.setVisible(visible);
		
	if(visible) {
	    Main.addFrame(this);
	    // Do a resize to get around a bug in JDK 1.1.5v5 for Linux
	    // Dimension d = getSize();
	    // setSize(d.width + 1, d.height + 1);
	}
	else
	    Main.removeFrame(this);
    }

    /**
     * setStatus - display some text in the status bar. The default
     *  implementation does nothing. If a subclass actually has a status
     *  bar, it might want to implement this.
     */
    public void setStatus(String status) {}

}

