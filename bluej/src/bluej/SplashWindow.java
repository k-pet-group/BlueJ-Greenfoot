package bluej;

import java.awt.*;
import javax.swing.*;
import java.io.File;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 853 2001-04-19 04:24:26Z ajp $
 */

public class SplashWindow extends JWindow
{
    public SplashWindow(File bluejLibDir)
    {
        ImageIcon icon = new ImageIcon(new File(
                                        new File(bluejLibDir, "images"),
                                        "splash.jpg").getPath());
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
