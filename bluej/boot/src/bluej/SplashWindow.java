package bluej;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.imageio.ImageIO;
import java.io.IOException;

import java.net.URL;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 3235 2004-12-14 15:42:21Z mik $
 */

public class SplashWindow extends Frame
{
    public SplashWindow()
    {
        setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    		setUndecorated(true);
    		URL splashURL = getClass().getResource("splash.jpg");
    		BlueJLabel image = null;

        if(splashURL == null) {
            System.out.println("cannot find splash image");
            return;
        }
        try {
            BufferedImage splashImage = ImageIO.read(splashURL);
            image = new BlueJLabel(splashImage);
        }
        catch(IOException exc) { // ignore
        }
        add(image);
        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width)/2,
                    (screenDim.height - getSize().height)/2);
        setVisible(true);
        //try { Thread.sleep(7000);} catch(Exception e) {}  // for testing: show longer
    }

    /**
     * Remove this splash screen from screen. Since we never need it again,
     * throw it away completely.
     */
    public void remove()
    {
        dispose();
    }

    private class BlueJLabel extends JComponent {
        
        private BufferedImage image;
        
        BlueJLabel(BufferedImage splashImage) {
            image = splashImage;
            setBorder(BorderFactory.createLineBorder(Color.black, 1));
        }
        
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);
            g.setColor(new Color(31,70,110));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("Version " + Boot.BLUEJ_VERSION, 24, image.getHeight()-14);
        }
        
        public Dimension getMinimumSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        
        public Dimension getMaximumSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }
}
