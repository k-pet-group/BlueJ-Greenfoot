package bluej;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 910 2001-05-24 07:24:41Z mik $
 */

public class SplashWindow extends JWindow
{
    public SplashWindow(File bluejLibDir)
    {
        ImageIcon icon = new ImageIcon(bluejLibDir.getPath() + 
                                       "/images/splash.jpg");
        // Note: it is intentional that the forward slash is used here
        // for all systems. See the documentation of 
        // public ImageIcon(String filename)

        JLabel image = new JLabel(icon);
        getContentPane().add(image);

        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width)/2,
                    (screenDim.height - getSize().height)/2);

        setVisible(true);

        // for testing - if you want to look at it a bit longer...
        //try { Thread.sleep(10000); } catch (Exception e) {}
    }


    /**
     * Remove this splash screen from screen. Since we never need it again,
     * throw it away completely.
     */
    public void remove()
    {
        dispose();
    }


    /*
    public void componentShown(ComponentEvent e) {
        System.out.println("view...");
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setColor(new Color(71,0,0));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Version " + Main.BLUEJ_VERSION, 
                     20, 20);
    }
    */
}

