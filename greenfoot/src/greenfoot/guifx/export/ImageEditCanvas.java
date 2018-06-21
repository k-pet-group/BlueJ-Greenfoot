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

            x = ensureSuitableCoordinate(x, canvasWidth, imageWidth);
            y = ensureSuitableCoordinate(y, canvasHeight, imageHeight);

            // Snap if size fits
            double xSnapped = x;
            double ySnapped = y; 
            if (Math.abs(scaleFactor - minScaleFactor) < .0000001)
            {
                double xs = (imageWidth / 2 + xSnapped) * scaleFactor;
                double ys = (imageHeight / 2 + ySnapped) * scaleFactor;

                // Threshold for which to snap to initial position (only if scale
                // factor is the initial size).
                final int snapThreshold = 7;
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
     * Study the value of a passed coordinate (i.e. x or y) to guarantee it is
     * within acceptable values to keep the image reasonably within the canvas.
     * The method returns the same coordinate value if it is acceptable,
     * otherwise the nearest calculated value will be returned.
     * The following conditions are to be met:
     *  - The image centers the canvas on a dimension where its edge on that
     *    dimension is smaller than the canvas's.
     *  - The image shouldn't be moved beyond the canvas's edge leaving an
     *    apparent background on a dimension where it is bigger than the canvas's.
     *
     * @param coordinate  The value of a coordinate (x or y) that need to be tested.
     * @param canvasEdge  The length of the canvas's edge.
     * @param imageEdge   The length of the image's edge.
     * @return The coordinate value suitable to show the image reasonably in the canvas.
     */
    private double ensureSuitableCoordinate(double coordinate, double canvasEdge, double imageEdge)
    {
        double maxCoordinate = -(canvasEdge / scaleFactor) / 2;
        double minCoordinate = -(imageEdge + maxCoordinate);

        // If the image's edge is smaller than the canvas's, make
        // sure the image centers the canvas on that dimension.
        if (imageEdge * scaleFactor <= canvasEdge)
        {
            return -imageEdge / 2;
        }

        // If the coordinate value is to the right/below of the
        // canvas's left/top edge, return the that edge.
        if (coordinate > maxCoordinate)
        {
            return maxCoordinate;
        }

        // If the coordinate value will cause the image's to be more to
        // the left/up than the canvas's right/bottom edge, return the
        // minimum coordinate value to guarantee it will stay at that edge.
        if (coordinate < minCoordinate)
        {
            return minCoordinate;
        }

        // If all conditions are not met, the initial value is acceptable.
        return coordinate;
    }

    /**
     * Scale and move the image so that it fits within the size of the canvas.
     */
    public void fit()
    {
        x = -image.getWidth() / 2;
        y = -image.getHeight() / 2;

        setScale(minScaleFactor);
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
