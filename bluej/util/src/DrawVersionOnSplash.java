import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by neil on 14/02/2017.
 */
public class DrawVersionOnSplash
{
    // Takes five args: original image path, string to draw, size, X position, Y position, output image path
    public static void main(String[] args)
    {
        if (args.length != 6)
        {
            System.err.println("Wrong number of args (" + args.length + "), should be 6: original-image-path, string, size, X, Y, output-image-path");
            System.exit(-1);
        }
        try
        {
            BufferedImage image = ImageIO.read(new File(args[0]));
            Graphics g = image.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.setColor(new Color(255,255,255));
            g.setFont(new Font("SansSerif", Font.BOLD, Integer.parseInt(args[2])));
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D)g;
                RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.addRenderingHints(hints);
            }
            g.drawString(args[1], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            ImageIO.write(image, "png", new File(args[5]));
        }
        catch (IOException | NumberFormatException e)
        {
            System.err.println("Exception: " + e.getLocalizedMessage());
            System.exit(-1);
        }
    }
}
