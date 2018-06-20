/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx.export;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.transform.Affine;

/**
 * Component that shows an image and supplies methods for scaling and cropping the view.
 * 
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
public class ImageEditCanvas extends Canvas
{
    /** Original image */
    private Image image;

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
    private int snapThreshold = 7;

    /**
     * Create a new image canvas.
     * 
     * @param width   Width of this component.
     * @param height  Height of this component.
     * @param image   The image to manipulate.
     */
    public ImageEditCanvas(int width, int height, Image image)
    {
        super(width, height);
        setImage(image);
    }

    /**
     * Set the distance form which snapping should start. Only used when the
     * image is scaled to the same size as the desired size.
     *
     * @param threshold The Snap threshold value.
     */
    public void setSnapThreshold(int threshold)
    {
        this.snapThreshold = threshold;
    }

    /**
     * Set the image that should be shown on this canvas.
     *
     * @param image The JavaFX Image object to be assigned to the Canvas.
     *              It could be null.
     */
    public void setImage(Image image)
    {
        this.image = image;
        if (image != null)
        {
            double minScaleFactorX = getWidth() / image.getWidth();
            double minScaleFactorY = getHeight() / image.getHeight();
            minScaleFactor = minScaleFactorX < minScaleFactorY ? minScaleFactorX : minScaleFactorY;
            if (minScaleFactor > 1)
            {
                minScaleFactor = 1;
            }
        }
    }

    /**
     * Paints the image with the current scale and offset.
     *
     * @param graphics The graphics context used to paint the image.
     */
    public void paintImage(GraphicsContext graphics)
    {
        if (image != null)
        {
            Affine oldTx = graphics.getTransform();

            final double canvasWidth = getWidth();
            final double canvasHeight = getHeight();
            final double imageWidth = image.getWidth();
            final double imageHeight = image.getHeight();

            // Make sure the image centers the canvas horizontally when it is narrower.
            if (imageWidth * scaleFactor <= canvasWidth)
            {
                fitX();
            }
            // Make sure the image centers the canvas vertically when it is shorter.
            if (imageHeight * scaleFactor <= canvasHeight)
            {
                fitY();
            }

            // Snap if size fits
            double xSnapped = x;
            double ySnapped = y; 
            if (Math.abs(scaleFactor - minScaleFactor) < .0000001)
            {
                double xs = (imageWidth / 2 + xSnapped) * scaleFactor;
                double ys = (imageHeight / 2 + ySnapped) * scaleFactor;
                if (Math.abs(xs) < snapThreshold && Math.abs(ys) < snapThreshold)
                {
                    xSnapped = -imageWidth / 2;
                    ySnapped = -imageHeight / 2;
                }
            }

            // Clear the rectangle before redrawing again to clear
            // any left over artifacts.
            graphics.clearRect(0, 0, canvasWidth, canvasHeight);

            // Scale around center of canvas
            graphics.translate(canvasWidth / 2, canvasHeight / 2);
            graphics.scale(scaleFactor, scaleFactor);
            graphics.translate(xSnapped, ySnapped);

            graphics.drawImage(image, 0, 0);
            graphics.setTransform(oldTx);
        }
    }

    /**
     * Scale and move the image so that it fits within the size of the canvas.
     */
    public void fit()
    {
        fitX();
        fitY();

        setScale(minScaleFactor);
    }

    /**
     * Move the image on X coordinates so that it centers the canvas horizontally.
     */
    private void fitX()
    {
        x = -image.getWidth() / 2;
    }

    /**
     * Move the image on Y coordinates so that it centers the canvas vertically.
     */
    private void fitY()
    {
        y = -image.getHeight() / 2;
    }

    /**
     * Move the image.
     *
     * @param dx The shift value on the x-axis
     * @param dy The shift value on the y-axis
     */
    public void move(double dx, double dy)
    {
        // Divide by scaleFactor since we want the location in the original image.
        x += dx / scaleFactor;
        y += dy / scaleFactor;
        paintImage(getGraphicsContext2D());
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
     * @param scaleFactor The scale factor. 1 is real size of the image.
     */
    public void setScale(double scaleFactor)
    {
        this.scaleFactor = scaleFactor;
        if (scaleFactor < minScaleFactor)
        {
            this.scaleFactor = minScaleFactor;
        }
        paintImage(getGraphicsContext2D());
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
}
