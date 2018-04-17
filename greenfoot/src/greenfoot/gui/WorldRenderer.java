/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.ImageVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.TextLabel;
import greenfoot.core.WorldHandler;
import greenfoot.util.GreenfootUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class which handles the rendering of a World into a BufferedImage, including
 * handling the currently-dragging actor (if any).
 */
public class WorldRenderer
{
    private static final Color BACKGROUND = Color.WHITE;
    private World world;

    /** Preferred size (not counting insets) */
    private Dimension size;

    private DropTarget dropTargetListener;
    
    /** The actor being dragged. Null if no dragging. */
    private Actor dragActor;
    /** The current location where the object is dragged - in pixel coordinates relative to this canvas. */
    private Point dragLocation;
    /** Image used when dragging new actors on the world. Includes the drop shadow.*/
    private BufferedImage dragImage;

    /**
     * Render the currently held world into the given image.  It is assumed
     * that the image size matches the current world size.
     * @param worldImage
     */
    public void renderWorld(BufferedImage worldImage)
    {
        Graphics2D g2 = (Graphics2D)worldImage.getGraphics();
        if (world == null)
        {
            Color c = g2.getColor();
            g2.setColor(BACKGROUND);
            g2.fillRect(0, 0, worldImage.getWidth(), worldImage.getHeight());
            WorldHandler.getInstance().repainted();
        }
        else
        {
            paintBackground(g2);
            paintObjects(g2);
            paintDraggedObject(g2);
            WorldVisitor.paintDebug(world, g2);
            paintWorldText(g2, world);
        }
    }

    /**
     * Paints all the objects.
     *
     * Must be synchronized on the World.lock.
     */
    private void paintObjects(Graphics2D g)
    {
        // This can happen if we try to grab a screenshot while the world is being replaced:
        if (world == null)
            return;

        Set<Actor> objects = WorldVisitor.getObjectsListInPaintOrder(world);
        int paintSeq = 0;
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor thing = iter.next();
            int cellSize = WorldVisitor.getCellSize(world);

            GreenfootImage image = ActorVisitor.getDisplayImage(thing);
            if (image != null) {
                ActorVisitor.setLastPaintSeqNum(thing, paintSeq++);

                double halfWidth = image.getWidth() / 2.;
                double halfHeight = image.getHeight() / 2.;

                AffineTransform oldTx = null;
                try {
                    int ax = ActorVisitor.getX(thing);
                    int ay = ActorVisitor.getY(thing);
                    double xCenter = ax * cellSize + cellSize / 2.;
                    int paintX = (int) Math.floor(xCenter - halfWidth);
                    double yCenter = ay * cellSize + cellSize / 2.;
                    int paintY = (int) Math.floor(yCenter - halfHeight);

                    int rotation = ActorVisitor.getRotation(thing);
                    if (rotation != 0) {
                        // don't bother transforming if it is not rotated at
                        // all.
                        oldTx = g.getTransform();
                        g.rotate(Math.toRadians(rotation), xCenter, yCenter);
                    }

                    ImageVisitor.drawImage(image, g, paintX, paintY, null, true);
                }
                catch (IllegalStateException e) {
                    // We get this if the object has been removed from the
                    // world. That can happen when interactively invoking a
                    // method that removes an object from the world, while the
                    // scenario is executing.
                }

                // Restore the old state of the graphics
                if (oldTx != null) {
                    g.setTransform(oldTx);
                }
            }
        }
    }

    /**
     * Paint the world background. This takes tiling into account: the
     * world image is painted either once or tiled onto this component.
     */
    private void paintBackground(Graphics2D g)
    {
        if (world != null) {
            GreenfootImage backgroundImage = WorldVisitor.getBackgroundImage(world);
            if (backgroundImage != null) {
                ImageVisitor.drawImage(backgroundImage, g, 0, 0, null, true);
            }
            else {
                Color oldColor = g.getColor();
                g.setColor(BACKGROUND);
                g.fillRect(0, 0, (int)size.getWidth(), (int)size.getHeight());
                g.setColor(oldColor);
            }
        }
    }


    /**
     * Paint text labels that have been placed on the world using World.showText(...).
     * @param g   The graphics context to draw on
     * @param world   The world
     */
    private void paintWorldText(Graphics2D g, World world)
    {
        List<TextLabel> labels = WorldVisitor.getTextLabels(world);

        if (labels.isEmpty()) {
            return;
        }

        // Set up rendering context:
        Font origFont = g.getFont();
        Color orig = g.getColor();
        Object origAntiAliasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        int cellsize = WorldVisitor.getCellSize(world);
        for (TextLabel label : labels) {
            label.draw(g, cellsize);
        }

        // Restore graphics context state:
        g.setFont(origFont);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasing);
        g.setColor(orig);
    }    
    
    /**
     * If an object is being dragged, paint it.
     */
    private void paintDraggedObject(Graphics g)
    {
        if(dragImage != null) {
            int x = (int) dragLocation.getX();
            int y = (int) dragLocation.getY();
            int xCell =  WorldVisitor.toCellFloor(world, x);
            int yCell =  WorldVisitor.toCellFloor(world, y);
            int cellSize = WorldVisitor.getCellSize(world);
            x = (int) ((xCell + 0.5) * cellSize - dragImage.getWidth()/2);
            y = (int) ((yCell + 0.5) * cellSize - dragImage.getHeight()/2);

            g.drawImage(dragImage, x, y, null);
        }
    }

    /**
     * If it is a new actor, that has not been added to the world yet, the
     * dragging is handled here.
     */
    public boolean drag(Object o, Point p)
    {
        if(o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {
            if(!new Rectangle(size).contains(p)) {
                return false;
            }
            if(o != dragActor) {
                // It is the first time we are dragging this actor. Create the drag image.
                dragActor = (Actor) o;
                dragImage = GreenfootUtil.createDragShadow(ActorVisitor.getDragImage(dragActor).getAwtImage());
            }
            dragLocation = p;
            return true;
        }
        else if (dropTargetListener != null) {
            return dropTargetListener.drag(o, p);
        }
        else {
            return false;
        }
    }

    /**
     * Set the current world.
     */
    public void setWorld(World world)
    {
        this.world = world;
        if (world != null)
        {
            size = getPreferredSize(world);
        }
    }

    /**
     * Set the size to render, if there is currently no world.
     */
    public void setWorldSize(int xsize, int ysize)
    {
        if (world == null) {
            size = new Dimension(xsize, ysize);
        }
    }



    /**
     * Get the preferred size for this component, assuming that it is housing the given world.
     */
    private Dimension getPreferredSize(World world)
    {
        if (world != null) {
            size = new Dimension();
            size.width = WorldVisitor.getWidthInPixels(world) ;
            size.height = WorldVisitor.getHeightInPixels(world) ;
            return size;
        }
        else if (size != null) {
            return size;
        }
        else {
            return new Dimension(100, 100);
        }
    }

    /**
     * Set the drag listener for when dragging moves the actor.
     */
    public void setDropTargetListener(DropTarget dropTargetListener)
    {
        this.dropTargetListener = dropTargetListener;
    }
}
