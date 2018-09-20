import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
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
    // Takes seven args: original image path, string to draw, font file, size, X position, Y position, output image path
    public static void main(String[] args)
    {
        if (args.length != 7)
        {
            System.err.println("Wrong number of args (" + args.length + "), should be 6: original-image-path, string, size, X, Y, output-image-path");
            System.exit(-1);
        }
        try
        {
            File originalImage = new File(args[0]);
            String stringToDraw = args[1];
            File fontFile = new File(args[2]);
            int fontSize = Integer.parseInt(args[3]);            
            int xPosition = Integer.parseInt(args[4]);
            int yPosition = Integer.parseInt(args[5]);
            File destFile = new File(args[6]);
            
            BufferedImage image = ImageIO.read(originalImage);
            Graphics g = image.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.setColor(new Color(255,255,255));
            g.setFont(Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont((float)fontSize));
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D)g;
                RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.addRenderingHints(hints);
            }
            g.drawString(stringToDraw, xPosition, yPosition);
            ImageIO.write(image, "png", destFile);
        }
        catch (IOException | NumberFormatException | FontFormatException e)
        {
            System.err.println("Exception: " + e.getLocalizedMessage());
            System.exit(-2);
        }
    }
}
