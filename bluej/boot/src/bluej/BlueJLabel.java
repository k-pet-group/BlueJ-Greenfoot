package bluej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Label used for the SplashWindow for BlueJ.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
class BlueJLabel extends SplashLabel
{
    public BlueJLabel()
    {
        super("splash.jpg");
    }

    public void paintComponent(Graphics g)
    {
        BufferedImage image = getImage();
        g.drawImage(image, 0, 0, null);
        g.setColor(new Color(255,255,255));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Version " + Boot.BLUEJ_VERSION, 36, image.getHeight()-28);
  }
}