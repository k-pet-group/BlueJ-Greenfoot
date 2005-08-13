package bluej;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;


/**
 * Super class for splash images.
 *
 * @author Poul Henriksen
 * @version $Id$
 */
public abstract class SplashLabel extends JComponent
{
    private BufferedImage image;
    
    public SplashLabel(String imageName)
    {
        loadImage(imageName);
        setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }
    
    public BufferedImage getImage() {
        return image;
    }
   
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        Dimension prefSize = new Dimension();
        if(image != null) {
            prefSize.setSize(image.getWidth(), image.getHeight());
        }
        return prefSize;
    }
    
    private void loadImage(String imageName) {
        URL splashURL = getClass().getResource(imageName); 
      
        if (splashURL == null) {
            System.out.println("cannot find splash image: " + imageName);
            return;
        }
        try {
            image = ImageIO.read(splashURL);
        }
        catch (IOException exc) { // ignore
        }
    }

}
