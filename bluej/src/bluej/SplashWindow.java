package bluej;

import bluej.utility.Debug;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.File;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 796 2001-03-13 04:37:47Z mik $
 */

public class SplashWindow extends JWindow
{
    public SplashWindow(String bluejHome)
    {
        //super(parent);
        ImageIcon icon = new ImageIcon(bluejHome + File.separator + 
                                       "images" + File.separator + 
                                       "splash.jpg");
        JLabel image = new JLabel(icon);
        getContentPane().add(image);
        pack();
        
        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width)/2,
                    (screenDim.height - getSize().height)/2);
        setVisible(true);
    }
    
    /**
     * Remove this splash screen from screen. Since we never need it again,
     * throw it away completely.
     */
    public void remove()
    {
        dispose();
    }

}
