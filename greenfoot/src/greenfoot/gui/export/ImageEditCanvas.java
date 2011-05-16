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
package greenfoot.gui.export;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Component that shows an image and supplies methods for scaling and cropping the view.
 * 
 * @author Poul Henriksen
 */
public class ImageEditCanvas extends JPanel
{
    /** Original image */
    private BufferedImage image;

    /** Size of this component as specified by client */
    private Dimension size;

    /** Location in the original image that should be in the center of the view */
    private double x;
    /** Location in the original image that should be in the center of the view */
    private double y;
    /** Current factor to scale image with */
    private double scaleFactor = 1;
    /** Minimum scale factor */
    private double minScaleFactor;
    /**
     * Threshold for which to snap to initial position (only if scale factor is
     * the initial size)
     */
    int snapThreshold = 7;

    /**
     * Create a new image canvas.
     * 
     * @param width
     *            Width of this component.
     * @param height
     *            Height of this component.
     * @param image
     *            The image to manipulate.
     */
    public ImageEditCanvas(int width, int height, java.awt.image.BufferedImage image)
    {
        this.size = new Dimension(width, height);
        setOpaque(false);
        setImage(image);
    }

    /**
     * Set the distance form which snapping should start. Only used when the
     * image is scaled to the same size as the desired size.
     * 
     */
    public void setSnapThreshold(int threshold)
    {
        this.snapThreshold = threshold;
    }

    /**
     * Set the image that should be shown on this canvas.
     */
    public void setImage(java.awt.image.BufferedImage image)
    {
        this.image = image;
        if (image != null) {
            double minScaleFactorX = size.getWidth() / (double) image.getWidth();
            double minScaleFactorY = size.getHeight() / (double) image.getHeight();
            minScaleFactor = minScaleFactorX < minScaleFactorY ? minScaleFactorX : minScaleFactorY;
            if (minScaleFactor > 1) {
                minScaleFactor = 1;
            }
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        paintImage(g);
    }

    /**
     * Paints the image with the current scale and offset.
     */
    public void paintImage(Graphics g)
    {
        if (image != null) {
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform oldTx = g2.getTransform();

            // Snap if size fits
            double xSnapped = x;
            double ySnapped = y; 
            if (Math.abs(scaleFactor - minScaleFactor) < .0000001) {
                double xs = (image.getWidth() / 2 + xSnapped) * scaleFactor;
                double ys = (image.getHeight() / 2 + ySnapped) * scaleFactor;
                if (Math.abs(xs) < snapThreshold && Math.abs(ys) < snapThreshold) {
                    xSnapped = -image.getWidth() / 2;
                    ySnapped = -image.getHeight() / 2;
                }
            }

            // Scale around center of canvas
            g2.translate(size.width / 2, size.height / 2);
            g2.scale(scaleFactor, scaleFactor);
            g2.translate(xSnapped, ySnapped);

            g2.drawImage(image, 0, 0, null);
            g2.setTransform(oldTx);
        }
    }

    /**
     * Scale and move the image so that it fits within the size of the canvas.
     */
    public void fit()
    {
        x = (int) (-image.getWidth() / 2.) ;
        y = (int) (-image.getHeight() / 2.) ;
      
        setScale(minScaleFactor);
    }

    /**
     * Move the image.
     */
    public void move(int dx, int dy)
    {
        // Divide by scaleFactor since we want the location in the original
        // image.
        x += dx / scaleFactor;
        y += dy / scaleFactor;
        repaint();
    }

    /**
     * Return the current scale factor.
     */
    public double getScale()
    {
        return scaleFactor;
    }

    /**
     * Set the current scale factor. Will not allow the image to become
     * smaller than the component.
     * 
     * @param scaleFactor
     *            1 is real size of the image
     */
    public void setScale(double scaleFactor)
    {
        this.scaleFactor = scaleFactor;
        if (scaleFactor < minScaleFactor) {
            this.scaleFactor = minScaleFactor;
        }
        repaint();
    }

    /**
     * Returns the minimum scaling that will be allowed. The minimum scaling is
     * usually set so that the scaled size of the image is not smaller than the
     * canvas size.
     * 
     */
    public double getMinimumScale()
    {
        return minScaleFactor;
    }

    public Dimension getMaximumSize()
    {
        return size;
    }

    public Dimension getPreferredSize()
    {
        return size;
    }
}
