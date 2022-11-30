/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2014,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.standalone;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.WorldHandler;
import greenfoot.export.GreenfootScenarioViewer;
import greenfoot.gui.WorldRenderer;
import greenfoot.platforms.WorldHandlerDelegate;
import javafx.animation.AnimationTimer;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Implementation for running scenarios in a standalone application or applet.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public class WorldHandlerDelegateStandAlone implements WorldHandlerDelegate
{
    @OnThread(Tag.Any)
    private final GreenfootScenarioViewer viewer;
    private boolean lockScenario;
    private final WorldRenderer worldRenderer = new WorldRenderer();
    // Time last frame was painted, from System.nanoTime
    private long lastFramePaint;
    
    // The two threads want to share images, but they both need time to draw/read the
    // image once they have hold of one.  We have one reference with the
    // latest image ready to draw (which the simulation thread promises not to touch
    // afterwards), and one with old images which FX promises it has finished reading from,
    // and which can be redrawn into by the simulation thread.
    @OnThread(Tag.Any)
    private final AtomicReference<BufferedImage> pendingImage = new AtomicReference<>(null);
    @OnThread(Tag.Any)
    private final ConcurrentLinkedQueue<BufferedImage> oldImages = new ConcurrentLinkedQueue<>();

    @OnThread(Tag.FXPlatform)
    public WorldHandlerDelegateStandAlone (GreenfootScenarioViewer viewer, boolean lockScenario) 
    {
        this.viewer = viewer;
        this.lockScenario = lockScenario;
        new AnimationTimer() {
            @Override
            @OnThread(Tag.FXPlatform)
            public void handle(long now)
            {
                BufferedImage worldImage = pendingImage.getAndSet(null);
                // If there was an image ready, draw it as the world:
                if (worldImage != null)
                {
                    viewer.setWorldImage(worldImage);
                    // Afterwards, put the image back on the queue for re-use:
                    oldImages.add(worldImage);
                }
            }
        }.start();
    }
    
    public boolean maybeShowPopup(MouseEvent e)
    {
        // Not used in standalone
        return false;
    }

    public void mouseClicked(MouseEvent e)
    {
        // Not used in standalone
    }
    
    public void mouseMoved(MouseEvent e)
    {
        // Not used in standalone
    }

    @Override
    @OnThread(Tag.Any)
    public void setWorld(final World oldWorld, final World newWorld)
    {
    }
    
    @Override
    @OnThread(Tag.Any)
    public void instantiateNewWorld(String className, Runnable runIfError)
    {
        WorldHandler.getInstance().clearWorldSet();
        World newWorld = viewer.instantiateNewWorld();
        if (! WorldHandler.getInstance().checkWorldSet()) {
            WorldHandler.getInstance().setWorld(newWorld, false);
        }
    }

    @OnThread(Tag.Any)
    public void discardWorld(World world)
    {
        // Remove the current world image:
        BufferedImage image = pendingImage.getAndSet(null);
        if (image != null)
        {
            oldImages.add(image);
        }
    }

    public void addActor(Actor actor, int x, int y)
    {
        // Nothing to be done
    }

    @Override
    public void objectAddedToWorld(Actor actor)
    {
    }

    @Override
    public String ask(String prompt)
    {
        String r = viewer.ask(prompt);
        return r;
    }

    @Override
    public void paint(World world, boolean forcePaint)
    {
        if (world == null)
            return;
        
        long now = System.nanoTime();
        // Don't try to go above 100 FPS:
        if (now - lastFramePaint < 10_000_000L)
            return;
        lastFramePaint = now;
        
        int imageWidth = WorldVisitor.getWidthInPixels(world);
        int imageHeight = WorldVisitor.getHeightInPixels(world);

        BufferedImage worldImage = oldImages.poll();
        // Re-use the image if it's available and the right size,
        // otherwise discard it and make a new one of right size:
        if (worldImage == null || worldImage.getHeight() != imageHeight
                || worldImage.getWidth() != imageWidth)
        {
            worldImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        }

        worldRenderer.renderWorld(world, worldImage);
        // Set the latest world image as pending, and get the old one to
        // keep for re-use:
        BufferedImage oldImage = pendingImage.getAndSet(worldImage);
        if (oldImage != null)
        {
            oldImages.add(oldImage);
        }
    }

    @Override
    public void notifyStoppedWithError()
    {
        // Nothing to be done, really.
    }
}
