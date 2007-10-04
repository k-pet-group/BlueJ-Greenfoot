package greenfoot.gui;

import bluej.Config;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

/**
 * An image preview box accessory for a file chooser
 * 
 * @author Davin McCall
 * @version $Id: ImageFilePreview.java 5287 2007-10-04 04:32:24Z bquig $
 */
public class ImageFilePreview extends JLabel
    implements PropertyChangeListener
{
    private ImageIcon blankPreview;
    
    public ImageFilePreview(JFileChooser chooser)
    {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        int width = dpi * 2;
        int height = dpi * 2;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        FontMetrics fontMetrics = graphics.getFontMetrics();
        Rectangle2D stringBounds = fontMetrics.getStringBounds(Config.getString("imagelib.file.noPreview"), graphics);
        int ypos = (int)(height - stringBounds.getHeight()) / 2 + fontMetrics.getAscent();
        int xpos = (int)(width - stringBounds.getWidth()) / 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(Config.getString("imagelib.file.noPreview"), xpos, ypos);
        blankPreview = new ImageIcon(image);
        setIcon(blankPreview);
        
        chooser.addPropertyChangeListener(this);
        chooser.setAccessory(this);
    }
    
    public void propertyChange(PropertyChangeEvent evt)
    {
        boolean update = false;
        String prop = evt.getPropertyName();
        File file = null;

        //If the directory changed, don't show an image.
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            file = null;
            update = true;

        //If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            file = (File) evt.getNewValue();
            update = true;
        }

        //Update the preview accordingly.
        if (update) {
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            int width = dpi * 2;
            int height = dpi * 2;
            
            if (file != null) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        Image scaledImage = GreenfootUtil.getScaledImage(image, width, height);
                        Icon icon = new ImageIcon(scaledImage);
                        setIcon(icon);
                    }
                    else {
                        file = null;
                    }
                }
                catch (IOException ioe) {
                    file = null;
                }
            }
            
            // No file is selected, or a non-image file selected.
            if (file == null) {
                setIcon(blankPreview);
            }
        }
    }
}
