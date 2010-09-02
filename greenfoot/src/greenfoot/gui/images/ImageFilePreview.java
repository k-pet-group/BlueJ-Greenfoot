/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui.images;

import bluej.Config;
import greenfoot.util.GraphicsUtilities;
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
 * @version $Id: ImageFilePreview.java 8238 2010-09-02 11:04:59Z nccb $
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

        BufferedImage image = GraphicsUtilities.createCompatibleTranslucentImage(width, height);
        Graphics2D graphics = image.createGraphics();
        FontMetrics fontMetrics = graphics.getFontMetrics();
        Rectangle2D stringBounds = fontMetrics.getStringBounds(Config.getString("imagelib.file.noPreview"), graphics);
        int ypos = (int)(height - stringBounds.getHeight()) / 2 + fontMetrics.getAscent();
        int xpos = (int)(width - stringBounds.getWidth()) / 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(Config.getString("imagelib.file.noPreview"), xpos, ypos);
        blankPreview = new ImageIcon(image);
        setIcon(blankPreview);
        
        graphics.dispose();
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
