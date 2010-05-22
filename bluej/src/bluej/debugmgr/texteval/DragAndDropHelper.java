/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.texteval;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import bluej.Config;
import bluej.testmgr.record.InvokerRecord;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * This class manages the dragging of object icons, and the drop onto
 * drop targets.
 * 
 * @author Michael Kolling
 */
public class DragAndDropHelper 
    implements MouseListener, MouseMotionListener
{
    private static final Image plusDragImage =
        Config.getImageAsIcon("image.eval.dragobject-plus").getImage();
    private static final Image noPlusDragImage =
        Config.getImageAsIcon("image.eval.dragobject-noplus").getImage();
   // private static final int imageWidth = plusDragImage.getWidth(null);
   // private static final int imageHeight = plusDragImage.getHeight(null);

    private static DragAndDropHelper instance;
    
    /**
     * Lazy instantiation singleton factory method.
     * @return The singleton instance
     */
    public static DragAndDropHelper getInstance()
    {
        if(instance == null) {
            instance = new DragAndDropHelper();
        }
        return instance;
    }
    
    // ==================== instance ====================
    
    private PkgMgrFrame frame;
    private Component eventSource;
    private DragGlassPane glassPane;
    private ObjectBench target;

    // the last object that we handled (this can be operated on)
    private DebuggerObject object;
    // the invoker record of the last successful call
    private InvokerRecord invokerRecord;

    private boolean targetHiLightOn;
    private Image dragImage;
    
    private MouseMotionListener[] mml;  // other listeners
    
    /**
     * Construct the helper.
     */
    private DragAndDropHelper()
    {
    }
    
    public void startDrag(Component source, PkgMgrFrame frame, 
                          DebuggerObject object, InvokerRecord invokerRecord)
    {
        eventSource = source;
        this.frame = frame;
        target = frame.getObjectBench();
        glassPane = new DragGlassPane();
        frame.setGlassPane(glassPane);
        dragImage = noPlusDragImage;
        this.object = object;
        this.invokerRecord = invokerRecord;

        registerListeners();
        glassPane.setVisible(true);
    }
    
    /**
     * The drag operation has ended. Check where we are and decide what to do.
     * If we are over the target, drop the object.
     */
    private void stopDrag(int x, int y)
    {
        glassPane.setVisible(false);
        deregisterListeners();
        
        if(pointInTarget(x, y)) {
            // POLLE Create invoker record for this that allows "Get"?
            frame.getPackage().getEditor().raisePutOnBenchEvent(target, object, object.getGenType(), invokerRecord);
        }
        target.showFocusHiLight(false);
    }

    private void checkTargetHiLight(int x, int y)
    {
        if(pointInTarget(x, y)) {
            if(!targetHiLightOn) {
                target.showFocusHiLight(true);
                dragImage = plusDragImage;
            }
        }
        else {
            if(!targetHiLightOn) {
                target.showFocusHiLight(false);
                dragImage = noPlusDragImage;
            }
        }
    }
    
    /**
     * Check whether the specified point (in the event source's coordinate
     * system) is inside the target component.
     */
    private boolean pointInTarget(int x, int y)
    {
        Point pt = SwingUtilities.convertPoint(eventSource, x, y, target);
        return target.contains(pt);
    }
    
    /**
     * Register us as a mouse and motion listener, and disable
     * all other motion listeners (we handle this one).
     */
    private void registerListeners()
    {
        eventSource.addMouseListener(this);
        // temporarily remove other motion listeners
        mml = eventSource.getMouseMotionListeners();
        for(int i=0; i<mml.length; i++) {
            eventSource.removeMouseMotionListener(mml[i]);
        }
        eventSource.addMouseMotionListener(this);
    }
    
    /**
     * De-register our mouse and motion listener, and enable
     * the original motion listeners again.
     */
    private void deregisterListeners()
    {
        eventSource.removeMouseMotionListener(this);
        eventSource.removeMouseListener(this);
        // restore other motion listeners
        for(int i=0; i<mml.length; i++) {
            eventSource.addMouseMotionListener(mml[i]);
        }        
    }
    
    // ---- MouseMotionListener interface: ----
    
    public void mouseDragged(MouseEvent evt)
    {
        // make sure that we have left mouse button and not Mac ctrl-click
        if(!evt.isPopupTrigger() &&
           ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            int x = evt.getX();
            int y = evt.getY();

            checkTargetHiLight(x, y);

            // translate point to glassPane coords and add object offset
            Point pt = SwingUtilities.convertPoint(eventSource, x, y, glassPane);
            glassPane.setObjectLocation(pt);
            glassPane.repaint();
        }
    }
    

    public void mouseMoved(MouseEvent evt) {}

    // ---- end of MouseMotionListener interface ----

    // ---- MouseListener interface ----
    
    /**
     * A full mouse button click was completed.
     */
    public void mouseClicked(MouseEvent evt) {}
    

    /**
     * The mouse button was pressed.
     */
    public void mousePressed(MouseEvent evt) {}
    

    /**
     * The mouse button was released.
     */
    public void mouseReleased(MouseEvent evt)
    {
        stopDrag(evt.getX(), evt.getY());
    }


    /**
     * The mouse pointer entered this component.
     */
    public void mouseEntered(MouseEvent e) {}

    
    /**
     * The mouse pointer exited this component.
     */
    public void mouseExited(MouseEvent e) {}

    // ---- end of MouseListener interface ----

    /**
     * We have to provide our own glass pane so that it can paint the dragged object.
     */
    class DragGlassPane extends JComponent
    {
        private Point objectLocation;

        @Override
        protected void paintComponent(Graphics g) 
        {
            if (objectLocation != null) {
                g.drawImage(dragImage, objectLocation.x-30, objectLocation.y-30, null);

            }
        }

        public void setObjectLocation(Point p) 
        {
            objectLocation = p;
        }
    }
}
