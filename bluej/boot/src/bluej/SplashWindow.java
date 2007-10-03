package bluej;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Toolkit;

/**
 * This class implements a splash window that can be displayed while BlueJ is
 * starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 5253 2007-10-03 06:04:25Z davmac $
 */

public class SplashWindow extends Frame
{
    private boolean painted = false;
    
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
    
    public synchronized void paint(Graphics g)
    {
        painted = true;
        super.paint(g);
        notify();
    }
    
    public synchronized void waitUntilPainted()
    {
        while (!painted) {
            try {
                wait();
            }
            catch (InterruptedException ie) {
                painted = true;
            }
        }
    }
}

