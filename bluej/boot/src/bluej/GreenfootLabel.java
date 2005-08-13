package bluej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Label used for the SplashWindow for greenfoot.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class GreenfootLabel extends SplashLabel
{
    public GreenfootLabel()
    {
        super("greenfootsplash.jpg");
    }

    public void paintComponent(Graphics g)
    {
        BufferedImage image = getImage();
        g.drawImage(image, 0, 0, null);
        g.setColor(new Color(50, 92, 16));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Version " + Boot.GREENFOOT_VERSION, 168, image.getHeight() - 91);
    }
}
