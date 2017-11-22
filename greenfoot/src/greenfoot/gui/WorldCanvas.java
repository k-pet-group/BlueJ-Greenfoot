/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014,2015,2016  Poul Henriksen and Michael Kolling 
 
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

import bluej.utility.Debug;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.ImageVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.Simulation;
import greenfoot.core.TextLabel;
import greenfoot.core.WorldHandler;
import greenfoot.gui.input.KeyboardManager;
import greenfoot.gui.input.mouse.MousePollingManager;
import greenfoot.guifx.GreenfootStage;
import greenfoot.util.GreenfootUtil;
import javafx.scene.input.KeyCode;
import rmiextension.ProjectManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * The visual representation of the world.
 * 
 * @author Poul Henriksen
 */
public class WorldCanvas extends JPanel
    implements  DropTarget, Scrollable
{
    private World world;
    private DropTarget dropTargetListener;
    /** The actor being dragged. Null if no dragging. */ 
    private Actor dragActor;
    /** The current location where the object is dragged - in pixel coordinates relative to this canvas. */
    private Point dragLocation;
    /** Image used when dragging new actors on the world. Includes the drop shadow.*/
    private BufferedImage dragImage;
    /** Preferred size (not counting insets) */
    private Dimension size;
    private Image overrideImage;

    private boolean sending = false;
    /**
     * Shared memory documentation (this comment may get moved to somewhere more appropriate later).
     *
     * The shared memory is a single lump of memory.  Its format is as follows, where
     * each position is an integer position (i.e. bytes times four):
     *
     * Pos 0: Reserved, in case we want to switch back to using an atomic integer as lock.
     * Pos 1: When the number is positive, it is a strictly increasing counter set by the
     *        debug VM to indicate a frame index.  That way the server VM can see the counter
     *        and see if it increased to determine if there's a new frame to paint.
     *        (Even at 1000FPS, the scenario could run for 20+ solid days before counter
     *        wraps so not too fussed by that possibility.)
     *
     *        When the number is negative, it indicates that the server VM has sent back
     *        information to the debug VM to read.  This includes keyboard and mouse events,
     *        as shown below.
     *
     *
     * When positive frame counter in position 1, interpret rest as follows:
     * Pos 2: Width of world image in pixels (W)
     * Pos 3: Height of world image in pixels (H)
     * Pos 4 incl to 3+(W*H) excl, if W and H are both greater than zero:
     *        W * H pixels one row at a time with no gaps, each pixel is one
     *        integer, in BGRA form, i.e. blue is highest 8 bits, alpha is lowest.
     * Pos 3+(W*H): Sequence ID of most recently processed command, or -1 if N/A.
     *
     * When negative frame counter in position 1, interpret rest as follows:
     * Pos 2: Count of commands (C), can be zero
     * Pos 3 onwards:
     *        Commands.  Each command begins with an integer sequence ID, then has
     *        an integer length (L), followed by L integers (L >= 1).
     *        The first integer of the L integers is always the
     *        command type, and the amount of other integers depend on the command.  For example,
     *        GreenfootStage.COMMAND_RUN just has the command type integer and no more, whereas
     *        mouse events have four integers.
     */
    private final IntBuffer sharedMemory;
    private int seq = 1;
    private final FileChannel shmFileChannel;
    private long lastPaintNanos = System.nanoTime();
    private int lastAckCommand = -1;

    /**
     * @param world The world which we are the canvas for.
     * @param shmFilePath The path to the shared-memory file to be mmap-ed for communication
     */
    public WorldCanvas(World world, File projectDir, String shmFilePath)
    {
        setWorld(world);
        setBackground(Color.WHITE);
        setOpaque(true);
        try
        {
            shmFileChannel = new RandomAccessFile(shmFilePath, "rw").getChannel();
            MappedByteBuffer mbb = shmFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 10_000_000L);
            sharedMemory = mbb.asIntBuffer();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Sets the world that should be visualised by this canvas.
     * Call only from the Swing event thread.
     */
    public void setWorld(World world)
    {
        this.world = world;
        if (world != null) {
            setOverrideImage(null);
            this.setSize(getPreferredSize());
            revalidate();
            repaint();
        }
        else {
            // this.setSize(0, 0);
        }
    }

    /**
     * Set the last known world size. Affects the preferred size of the
     * WorldCanvas.
     */
    public void setWorldSize(int xsize, int ysize)
    {
        if (world == null) {
            size = new Dimension(xsize, ysize);
        }
    }
    
    /**
     * Paints all the objects.
     * 
     * Must be synchronized on the World.lock.
     */
    public void paintObjects(Graphics2D g)
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

                    ImageVisitor.drawImage(image, g, paintX, paintY, this, true);
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

    @Override
    public void paintComponent(Graphics g)
    {
        if (overrideImage != null)
        {
            g.drawImage(overrideImage, 1, 1, null); // Draw at 1, 1 to account for border
            return;
        }
        
        if (world == null) {
            Color c = g.getColor();
            g.setColor(getParent().getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(c);
            WorldHandler.getInstance().repainted();
            return;
        }
        
        // We need to sync, so that objects are not added and removed when we
        // traverse the list.
        // But, we only try to get the lock for a brief period to avoid
        // deadlocks. A deadlock could otherwise happen if a modal dialog is
        // created from the user code in one of the act() methods.
        // We could do the sync only on the paintObjects, but that would mean
        // that the background will be reset and no objects painted, resulting
        // in a slightly broken look, if the user code is sleeping (with
        // Thread.sleep).
        try {
            ReentrantReadWriteLock lock = WorldHandler.getInstance().getWorldLock();
            int timeout = WorldHandler.READ_LOCK_TIMEOUT;
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                try {
                    Insets insets = getInsets();
                    g.translate(-insets.left, -insets.top);
                    //paintRemote();
                }
                finally {
                    lock.readLock().unlock();
                    WorldHandler.getInstance().repainted();
                }
            }
            else {
                WorldHandler.getInstance().repainted(); // we failed, but notify waiters anyway
                // (otherwise they keep waiting indefinitely...)
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Paints the current world into the shared memory buffer so that the server VM can
     * display it in the window there.
     *
     * @param forcePaint Always paint.  If false, painting may be skipped if it's close to a recent paint.
     */
    public void paintRemote(boolean forcePaint)
    {
        long now = System.nanoTime();
        if (!forcePaint && now - lastPaintNanos <= 8_333_333L)
        {
            return; // No need to draw frame if less than 1/120th of sec between them
        }
        lastPaintNanos = now;

        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
        Graphics2D g2 = (Graphics2D)img.getGraphics();
        paintBackground(g2);
        paintObjects(g2);
        paintDraggedObject(g2);
        WorldVisitor.paintDebug(world, g2);
        paintWorldText(g2, world);

        if (!sending)
        {
            sending = true;
            int [] raw = ((DataBufferInt) img.getData().getDataBuffer()).getData();
            try (FileLock fileLock = shmFileChannel.lock())
            {
                sharedMemory.position(1);
                int recvSeq = sharedMemory.get();
                if (recvSeq < 0)
                {
                    int latest = readCommands();
                    if (latest != -1)
                    {
                        lastAckCommand = latest;
                    }
                }
                sharedMemory.position(1);
                sharedMemory.put(this.seq++);
                sharedMemory.put(getWidth());
                sharedMemory.put(getHeight());
                for (int i = 0; i < raw.length; i++)
                {
                    sharedMemory.put(raw[i] << 8 | 0xFF);
                }
                sharedMemory.put(lastAckCommand);
            }
            catch (IOException e)
            {
                Debug.reportError(e);
            }
            sending = false;
        }
    }

    /**
     * Read commands from the server VM.  Eventually, at the end of the Greenfoot
     * rewrite, this should live elsewhere (probably in WorldHandler or similar).
     *
     * @return The command acknowledge to write back to the buffer
     */
    private int readCommands()
    {
        int lastSeqID = -1;
        int commandCount = sharedMemory.get();
        for (int i = 0; i < commandCount; i++)
        {
            lastSeqID = sharedMemory.get();
            int commandLength = sharedMemory.get();
            int data[] = new int[commandLength];
            sharedMemory.get(data);
            if (GreenfootStage.isKeyEvent(data[0]))
            {
                int awtCode = JavaFXUtil.fxKeyCodeToAWT(KeyCode.values()[data[1]]);
                if (awtCode != -1)
                {
                    KeyboardManager keyboardManager = WorldHandler.getInstance().getKeyboardManager();
                    switch(data[0])
                    {
                        case GreenfootStage.KEY_DOWN:
                            keyboardManager.pressKey(awtCode);
                            break;
                        case GreenfootStage.KEY_UP:
                            keyboardManager.releaseKey(awtCode);
                            break;
                        // KEY_TYPED is not processed
                    }
                }
            }
            else if (GreenfootStage.isMouseEvent(data[0]))
            {
                MouseEvent fakeEvent = new MouseEvent(new JPanel(), 1, 0, 0, data[1], data[2], data[3], false, data[4]);
                MousePollingManager mouseManager = WorldHandler.getInstance().getMouseManager();
                switch (data[0])
                {
                    case GreenfootStage.MOUSE_CLICKED:
                        mouseManager.mouseClicked(fakeEvent);
                        break;
                    case GreenfootStage.MOUSE_PRESSED:
                        mouseManager.mousePressed(fakeEvent);
                        break;
                    case GreenfootStage.MOUSE_RELEASED:
                        mouseManager.mouseReleased(fakeEvent);
                        break;
                    case GreenfootStage.MOUSE_DRAGGED:
                        mouseManager.mouseDragged(fakeEvent);
                        break;
                    case GreenfootStage.MOUSE_MOVED:
                        mouseManager.mouseMoved(fakeEvent);
                        break;
                }
            }
            else
            {
                // Commands which are not keyboard or mouse events:
                switch (data[0])
                {
                    case GreenfootStage.COMMAND_RUN:
                        Simulation.getInstance().setPaused(false);
                        break;
                }
            }
        }
        return lastSeqID;
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
     * Paint the world background. This takes tiling into account: the
     * world image is painted either once or tiled onto this component.
     */
    public void paintBackground(Graphics2D g)
    {
        if (world != null) {
            GreenfootImage backgroundImage = WorldVisitor.getBackgroundImage(world);
            if (backgroundImage != null) {
                ImageVisitor.drawImage(backgroundImage, g, 0, 0, this, true);
            }
            else {
                Color oldColor = g.getColor();
                g.setColor(getBackground());
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                g.setColor(oldColor);
            }
        }
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize()
    {
        if (world != null) {
            size = new Dimension();
            size.width = WorldVisitor.getWidthInPixels(world) ;
            size.height = WorldVisitor.getHeightInPixels(world) ;
            Insets insets = getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }
        else if (size != null) {
            return size;
        }
        else {
            return super.getPreferredSize();
        }
    }
    
    public void setDropTargetListener(DropTarget dropTargetListener)
    {
        this.dropTargetListener = dropTargetListener;
    }

    @Override
    public boolean drop(Object o, Point p)
    {
        Insets insets = getInsets();
        Point p2 = new Point(p.x - insets.left, p.y - insets.top);
        clearDragInfo();
        if (dropTargetListener != null) {
            return dropTargetListener.drop(o, p2);
        }
        else {
            return false;
        }
    }

    /**
     * If it is a new actor, that has not been added to the world yet, the
     * dragging is handled here.
     */
    public boolean drag(Object o, Point p)
    {
        Insets insets = getInsets();
        Point p2 = new Point(p.x - insets.left, p.y - insets.top);
        if(o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {   
            if(!getVisibleRect().contains(p)) {
                return false;
            }
            if(o != dragActor) {
                // It is the first time we are dragging this actor. Create the drag image.
                dragActor = (Actor) o;          
                dragImage = GreenfootUtil.createDragShadow(ActorVisitor.getDragImage(dragActor).getAwtImage());
            }
            dragLocation = p2;
            repaint();
            return true;            
        }        
        else if (dropTargetListener != null) {
            return dropTargetListener.drag(o, p2);
        }
        else {        
            return false;
        }
    }
    
    public void dragEnded(Object o)
    {
        clearDragInfo();
        if (dropTargetListener != null) {
            dropTargetListener.dragEnded(o);
        }
        
    }

    /** 
     * End the drag by setting all the drag information to null. And request repaint to update the graphics.
     */
    private void clearDragInfo()
    {
        dragLocation = null;
        dragActor = null;
        dragImage = null;
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        int cellSize = world.getCellSize();
        double scrollPos = 0;
        if(orientation == SwingConstants.HORIZONTAL) {
            //scrolling left
            if(direction < 0) {
                scrollPos = visibleRect.getMinX();
               
            }
            //scrolling right
            else if (direction > 0) {
                scrollPos = visibleRect.getMaxX();
            }
        } else {
            //scrolling up
            if(direction < 0) {
                scrollPos = visibleRect.getMinY();
            }
            //scrolling down
            else if (direction > 0) {
                scrollPos = visibleRect.getMaxY();
            }
        }
        int increment = Math.abs((int) Math.IEEEremainder(scrollPos, cellSize));
        if(increment == 0) {
            increment = cellSize;
        }
      
        return  increment;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
         return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }

    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    public void setOverrideImage(Image snapshot)
    {
        this.overrideImage = snapshot;
        repaint();
    }
}
