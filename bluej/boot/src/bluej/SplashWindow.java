package bluej;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;

/**
 * This class implements a splash window that can be displayed while BlueJ is
 * starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 3513 2005-08-13 13:30:18Z polle $
 */

public class SplashWindow extends Frame
{
    public SplashWindow(SplashLabel image)
    {
        setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    	setUndecorated(true);

        add(image);
        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width) / 2, (screenDim.height - getSize().height) / 2);
        setVisible(true);
        //try { Thread.sleep(11000);} catch(Exception e) {}  // for testing: show longer
    }
}

