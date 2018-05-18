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
import greenfoot.util.GreenfootUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

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
@OnThread(Tag.Simulation)
public class WorldRenderer
{
    private static final Color BACKGROUND = Color.WHITE;
    
    /** The actor being dragged. Null if no dragging. */
    private Actor dragActor;
    /** The current location where the object is dragged - in pixel coordinates relative to this canvas. */
    private Point dragLocation;
    /** Image used when dragging new actors on the world. Includes the drop shadow.*/
    private BufferedImage dragImage;

    @OnThread(Tag.Any)
    public WorldRenderer()
    {
    }
    
    /**
     * Render the currently held world into the given image.  It is assumed
     * that the image size matches the current world size.
     * 
     * @param drawWorld The world to draw (may be null, in which case a blank image is drawn)
     * @param worldImage The image to draw onto, which is assumed to be
     *                   of the right size for the world (or desired blank size
     *                   if drawWorld is null)
     */
    public void renderWorld(World drawWorld, BufferedImage worldImage)
    {
        Graphics2D g2 = (Graphics2D)worldImage.getGraphics();
        
        if (drawWorld == null)
        {
            g2.setColor(BACKGROUND);
            g2.fillRect(0, 0, worldImage.getWidth(), worldImage.getHeight());
        }
        else
        {
            paintBackground(g2, drawWorld, worldImage.getWidth(), worldImage.getHeight());
            paintObjects(g2, drawWorld);
            paintDraggedObject(g2, drawWorld);
            WorldVisitor.paintDebug(drawWorld, g2);
            paintWorldText(g2, drawWorld);
        }
    }

    /**
     * Paints all the objects.
     *
     * Must be synchronized on the World.lock.
     */
    private void paintObjects(Graphics2D g, World drawWorld)
    {
        // This can happen if we try to grab a screenshot while the world is being replaced:
        if (drawWorld == null)
            return;

        Set<Actor> objects = WorldVisitor.getObjectsListInPaintOrder(drawWorld);
        int paintSeq = 0;
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor thing = iter.next();
            int cellSize = WorldVisitor.getCellSize(drawWorld);

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
    private void paintBackground(Graphics2D g, World drawWorld, int width, int height)
    {
        if (drawWorld != null) {
            GreenfootImage backgroundImage = WorldVisitor.getBackgroundImage(drawWorld);
            if (backgroundImage != null) {
                ImageVisitor.drawImage(backgroundImage, g, 0, 0, null, true);
            }
            else {
                Color oldColor = g.getColor();
                g.setColor(BACKGROUND);
                g.fillRect(0, 0, width, height);
                g.setColor(oldColor);
            }
        }
    }

    /**
     * Paint text labels that have been placed on the world using World.showText(...).
     * @param g   The graphics context to draw on
     * @param drawWorld   The world
     */
    private void paintWorldText(Graphics2D g, World drawWorld)
    {
        List<TextLabel> labels = WorldVisitor.getTextLabels(drawWorld);

        if (labels.isEmpty()) {
            return;
        }

        // Set up rendering context:
        Font origFont = g.getFont();
        Color orig = g.getColor();
        Object origAntiAliasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        int cellsize = WorldVisitor.getCellSize(drawWorld);
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
    private void paintDraggedObject(Graphics g, World drawWorld)
    {
        if(dragImage != null) {
            int x = (int) dragLocation.getX();
            int y = (int) dragLocation.getY();
            int xCell =  WorldVisitor.toCellFloor(drawWorld, x);
            int yCell =  WorldVisitor.toCellFloor(drawWorld, y);
            int cellSize = WorldVisitor.getCellSize(drawWorld);
            x = (int) ((xCell + 0.5) * cellSize - dragImage.getWidth()/2);
            y = (int) ((yCell + 0.5) * cellSize - dragImage.getHeight()/2);

            g.drawImage(dragImage, x, y, null);
        }
    }
}
