/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;

import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * Panel that lets you manipulate an image by zooming (with slider or
 * mouse wheel) and moving (by dragging with the mouse).
 * 
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
public class ImageEditPane extends HBox
{
    /** Canvas for the image we are controlling. */
    private ImageEditCanvas imageCanvas;
    /** Last position where mouse was dragged. */
    private double lastX;
    /** Last position where mouse was dragged. */
    private double lastY;

    /** Slider for zooming*/
    private Slider zoomSlider;

    /** Width of the image view */
    private int width;
    /** Height of the image view */
    private int height;
    
    /** Label used for the slider. */
    private ImageView bigLabel;
    /** Label used for the slider. */
    private ImageView smallLabel;
    
    /** Whether to enable dragging / zooming, when we have an image */
    private boolean enableImageControls = true;
    /** Whether we actually have an image in the edit canvas */
    private boolean haveImage;
    
    /**
     * Construct a new image edit panel for an image with the specified height and width.
     *
     * @param width The width of the image pane.
     * @param height The height of the image pane.
     */
    public ImageEditPane(int width, int height)
    {
        this.width = width;
        this.height = height;
        setPrefSize(width + 2, height + 2);
        getStyleClass().add("image-edit-pane");
        buildUI();
    }
    
    /**
     * Set the image to be manipulated.
     *
     * @param snapShot The image to be edited. It could be null.
     */
    public void setImage(Image snapShot)
    {
        double oldMinScale = imageCanvas.getMinimumScale();
        imageCanvas.setImage(snapShot); 
        double newMinScale = imageCanvas.getMinimumScale();            
        if (!haveImage || Math.abs(newMinScale - oldMinScale) > .0000001)
        {
            // Only re-fit scaling if there was a change in size.
            imageCanvas.fit();
            zoomSlider.setValue(imageCanvas.getScale());
        }
        if (!haveImage)
        {
            haveImage = true;
            enableImageEditPanel(enableImageControls);
        }
    }
    
    /**
     * Compose the user interface components.
     */
    private void buildUI()
    {
        imageCanvas = new ImageEditCanvas(width, height, null);
        imageCanvas.setCursor(Cursor.HAND);
        imageCanvas.setOnMouseDragged(this::mouseDragged);
        imageCanvas.setOnMousePressed(this::mousePressed);
        imageCanvas.setOnMouseReleased(this::mouseReleased);
        imageCanvas.setOnScroll(this::mouseScroll);

        Pane sliderPane = createSliderPane();

        // A Pane with a border to contain the image canvas only.
        // It has been added so that the borders are not drawn on
        // the canvas, but just outside it. The BorderPane is used
        // instead of Pane as the Pane causes the imageCanvas to
        // be shifted one pixel left and up for some reason.
        BorderPane border = new BorderPane(imageCanvas);
        border.getStyleClass().add("image-canvas");

        getChildren().addAll(border, sliderPane);
    }

    /**
     * Create a slider attaching two icons to it, a big one and a small one.
     * It also sets it min/max values and link it to the image canvas scale.
     *
     * @return A pane containing the slider and its icons.
     */
    private Pane createSliderPane()
    {
        zoomSlider = new Slider();
        zoomSlider.setOrientation(Orientation.VERTICAL);
        zoomSlider.setPrefHeight(imageCanvas.getHeight());
        zoomSlider.setMin(imageCanvas.getMinimumScale());
        zoomSlider.setMax(1);
        JavaFXUtil.addChangeListener(zoomSlider.valueProperty(),
                scale -> imageCanvas.setScale(scale.doubleValue()));

        // Create labels for slider using the Greenfoot logo.
        Image image = JavaFXUtil.loadImage(Config.getGreenfootLibDir().getAbsolutePath()
                + "/imagelib/other/greenfoot.png");
        bigLabel = new ImageView(image);
        bigLabel.setScaleX(1.2);
        bigLabel.setScaleY(1.2);
        smallLabel = new ImageView(image);
        smallLabel.setScaleX(.8);
        smallLabel.setScaleY(.8);

        AnchorPane anchorPane  = new AnchorPane(zoomSlider, bigLabel, smallLabel);
        AnchorPane.setLeftAnchor(zoomSlider, 0.0);
        AnchorPane.setTopAnchor(bigLabel, 0.0);
        AnchorPane.setLeftAnchor(bigLabel, 20.0);
        AnchorPane.setBottomAnchor(smallLabel, 0.0);
        AnchorPane.setLeftAnchor(smallLabel, 20.0);

        return anchorPane;
    }

    /**
     * The event to move the image in response to mouse drag.
     *
     * @param e The mouse event information
     */
    public void mouseDragged(MouseEvent e)
    {
        if (imageCanvas.isDisabled())
        {
            return;
        }
        if (e.getButton().equals(MouseButton.PRIMARY))
        {
            imageCanvas.setCursor(Cursor.MOVE);
            double dx = e.getX() - lastX;
            double dy = e.getY() - lastY;
            imageCanvas.move(dx, dy);
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    /**
     * The event to store the coordinates of a mouse press.
     * It may be a start of a drag operation.
     *
     * @param e The mouse event information
     */
    public void mousePressed(MouseEvent e)
    {
        if (imageCanvas.isDisabled())
        {
            return;
        }
        if (e.getButton().equals(MouseButton.PRIMARY))
        {
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    /**
     * The event in response to mouse release.
     * It may be an end to a drag operation.
     *
     * @param e The mouse event information
     */
    public void mouseReleased(MouseEvent e)
    {
        if (imageCanvas.isDisabled())
        {
            return;
        }
        if (e.getButton().equals(MouseButton.PRIMARY))
        {
            imageCanvas.setCursor(Cursor.HAND);
        }
    }

    /**
     * The event to zoom the image in response to a mouse scroll.
     *
     * @param e The scroll event information
     */
    public void mouseScroll(ScrollEvent e)
    {
        if (imageCanvas.isDisabled())
        {
            return;
        }
        double scroll = e.getDeltaY() / 100;
        zoomSlider.setValue(zoomSlider.getValue() - scroll);
    }

    /**
     * Get the image created by this image panel or null if none exists.
     */
    public Image getImage()
    {
        if (!haveImage)
        {
            return null;
        }
        return JavaFXUtil.createImage(width, height, graphicsContext -> imageCanvas.paintImage(graphicsContext));
    }
    
    /**
     * Sets the slider and the image canvas to be enabled/disabled
     *
     * @param enabled If true, the controls, the zoom slider and image's canvas
     *                will be enabled. Otherwise, they will be disabled.
     */
    public void enableImageEditPanel(boolean enabled)
    {
        enableImageControls = enabled;
        if (!enabled || haveImage)
        {
            zoomSlider.setDisable(!enabled);
            imageCanvas.setDisable(!enabled);
        }
    }
}
