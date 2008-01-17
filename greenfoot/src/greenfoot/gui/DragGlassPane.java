package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.core.LocationTracker;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * Component that can be used for dragging. It should be used as a glasspane on
 * a JFrame. A drag is started with the startDrag() method. The drag will end
 * when the mouse is released and the component on that location get the
 * MouseEvent (mouseReleased)
 * 
 * This component is used for dragging initiated either by invoking a
 * constructor through the menus or by using the SHIFT-add feature.
 * 
 * Some of this is taken from:
 * http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/GlassPaneDemo.java
 * 
 * after startDrag(): - drag() sent to drop target when object is dragged over
 * it - dragEnded() sent to drop target when object is dragged off it
 * 
 * If the object is dropped on a drop target: - drop() is sent to the drop
 * target (dragEnded() is not sent) - dragFinished() is sent to the drag
 * listener
 * 
 * If the drag is cancelled: - dragEnded() is sent to the drop target -
 * dragFinished() is sent to the drag listener
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: DragGlassPane.java 5457 2008-01-17 12:22:42Z polle $
 * 
 */
public class DragGlassPane extends JComponent
    implements MouseMotionListener, MouseListener, KeyListener
{
    /** Singleton */
    private static DragGlassPane instance;

    /** The image displayed when dragging where no DropTarget is below */
    private Icon noParkingIcon;

    /** Should the dragGlassPane display the no drop image? */
    private boolean paintNoDropImage;

    /** The object that is dragged */
    private Object data;

    /** Rectangles used for graphics update */
    private Rectangle rect = new Rectangle();
    
    /** Offset from center of object being dragged to mouse cursor */
    private int dragOffsetX;
    private int dragOffsetY;

    /**
     * Keeps track of the last drop target, in order to send messages to old
     * drop targets when drag moves away from the component
     */
    private DropTarget lastDropTarget;
    
    /**
     * The listener to be notified when the drag operation finishes.
     */
    private DragListener dragListener;

    /**
     * Indicates whether the drag is done without any buttons pressed. This
     * allows the drag to continue even if no keyboard or mouse buttons are
     * pressed.
     */
    private boolean forcedDrag;

    
    /**
     * Image used when dragging. If this is null, no dragging is happening at the moment.
     */
    private BufferedImage dragImage;

    public static DragGlassPane getInstance()
    {
        if (instance == null) {
            instance = new DragGlassPane();
        }
        return instance;
    }

    private DragGlassPane()
    {
    	setVisible(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        URL noParkingIconFile = this.getClass().getClassLoader().getResource("noParking.png");
        if (noParkingIconFile != null) {
            noParkingIcon = new ImageIcon(noParkingIconFile);
        }
    }

    public void paintComponent(Graphics g)
    {
        // We only handle painting here if no drop-target could handle the painting
        if (dragImage != null && paintNoDropImage) {
            Graphics2D g2 = (Graphics2D) g;

            int width = rect.width;
            int height = rect.height;

            double halfWidth = width / 2.;
            double halfHeight = height / 2.;

            g2.drawImage(dragImage, rect.x, rect.y, null);
            
			g2.setColor(Color.RED);
            if (noParkingIcon != null) {
                int x = (int) (rect.getX() + halfWidth - noParkingIcon.getIconWidth() / 2);
                int y = (int) (rect.getY() + halfHeight - noParkingIcon.getIconHeight() / 2);
                noParkingIcon.paintIcon(this, g2, x, y);
            }
        }
        else {
            //we do nothing - a DropTarget should have handled this
        }
    }

   /* private Rectangle getClip()
    {
        int width = rect.width;
        int height = rect.height;
        int diag = (int) Math.ceil(Math.sqrt(width * width + height * height));
        int widthDiff = (diag - width) / 2;
        int heightDiff = (diag - height) / 2;
        Rectangle oldClip = new Rectangle(oldRect.x - widthDiff, oldRect.y - heightDiff, diag, diag);
        Rectangle newClip = new Rectangle(rect.x - widthDiff, rect.y - heightDiff, diag, diag);
        return oldClip.union(newClip);
    }*/

    /**
     * Initiates a drag. The xOffset and yOffset specify the offset in pixels
     * from the mouse cursor to the image top-left corner during the drag
     * operation (normally negative).
     * <p>
     * 
     * There are two types of drag: a "genuine" drag where an object is being
     * dragged with the mouse button down, and a "forced" drag where the button
     * is up. In the case of a genuine drag, the DragGlassPane should be
     * added as a MouseListener and MouseMotionListener to the component
     * receiving the drag events. Otherwise, this is not necessary.
     * 
     * @param object The object to drag.
     * @param xOffset The X offset from the icon's top-left to the mouse cursor
     * @param yOffset The Y offset from the icon's top-left to the mouse cursor
     * @param dl The listener to be notified when the operation finishes
     * @param initialDropTarget An initial drop target. It can be null. Used
     *            when we want to imediately paint a dragimage unto the drop
     *            target.
     * @param forcedDrag indicates whether the drag is done without any buttons
     *            pressed. This allows the drag to continue even if no keyboard
     *            or mouse buttons are pressed.
     * 
     */
    public void startDrag(Actor object, int xOffset, int yOffset, DragListener dl, DropTarget initialDropTarget, boolean forcedDrag)
    {
        // Save the listener first, so that calls to endDrag() work.
        dragListener = dl;
        
        if (object == null) {
            endDrag();
            return;
        }
        GreenfootImage objectImage = ActorVisitor.getDisplayImage(object);
        if (objectImage == null) {
            endDrag();
            return;
        }
        this.forcedDrag = forcedDrag;
        
        //get last mouseevent to get first location
        MouseEvent e =  LocationTracker.instance().getMouseMotionEvent();
        if(e == null) {
            // This startDrag was probably initiated by a mouse event that was
            // handled before the LocationTracker got a chance to handle it.
            endDrag();
            return;
        }
        
        setDragImage(objectImage);
        setDragObject(object);
        paintNoDropImage = true;
                
        storePosition(e);
        dragOffsetX = xOffset;
        dragOffsetY = yOffset;
        lastDropTarget = initialDropTarget;
        if(initialDropTarget != null) {
            //force painting of drag object
            Point p = e.getPoint();
            p.translate(xOffset, yOffset);
            paintNoDropImage =  ! lastDropTarget.drag(object, p);  
            if(paintNoDropImage) {
                repaint();
            }    
        }       
        setVisible(true); 
    }

    /**
     * Call this method to cancel a drag/drop operation. dragEnded() will be
     * called for the dropTarget over which the object is currently being
     * dragged, and then dragFinished() will be called on the DragListener.
     */
    public void cancelDrag()
    {
        endDrag();
    }
    
    /**
     * The drag is finished.
     */
    private void endDrag()
    {
        if (lastDropTarget != null) {
            lastDropTarget.dragEnded(data);
        }

        // Save the old values of dragListener and data for the "dragFinished"
        // call below
        DragListener dl = dragListener;
        Object od = data;
        
        setVisible(false);
        data = null;
        dragImage = null;
        dragListener = null;
        
        // Call dragFinished
        if (dl != null) {
            dl.dragFinished(od);
        }
    }

    /**
     * Sets the image to be dragged around
     * 
     * @param image
     *            The image
     * @param rotation
     *            The rotation of the image
     */
    public void setDragImage(greenfoot.GreenfootImage image)
    {        
        BufferedImage awtImage = image.getAwtImage();
        dragImage = GreenfootUtil.createDragShadow(awtImage);
        
        
        int width = image.getWidth();
        int height = image.getHeight();
        rect.width = width;
        rect.height = height;
    }

    public void setDragObject(Object object)
    {
        this.data = object;
    }

    public Object getDragObject()
    {
        return data;
    }

    private void move(MouseEvent e)
    {
        if(dragImage == null) {
            //No valid drag object available.
            return;
        }
        storePosition(e);
        boolean doRepaint = true;
        Component destination = getComponentBeneath(e);
        DropTarget dropTarget = null;
        if (destination instanceof DropTarget) {
            dropTarget = (DropTarget) destination;

            Point tp = e.getPoint().getLocation(); // copy the point
            tp.translate(dragOffsetX, dragOffsetY);
            Point p = SwingUtilities.convertPoint(e.getComponent(), tp, destination);
            if (dropTarget.drag(data, p)) {
                if(paintNoDropImage) {
                    paintNoDropImage = false;
                }
                else {
                    // The dropTarget has handled repaint, and since
                    // paintNoDropImage didn't changed state we do not need a
                    // repaint
                    doRepaint = false;
                }
            }
            else {
                paintNoDropImage = true;
            }
        }
        else {
            paintNoDropImage = true;
        }

        if (lastDropTarget != null && dropTarget != lastDropTarget) {
            lastDropTarget.dragEnded(data);
        }
        lastDropTarget = dropTarget;
        if (isVisible() && doRepaint) {
            //We need to repaint because the drag was not processed by another component.
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        move(e);
    }

    public void mouseDragged(MouseEvent e)
    {
        move(e);
    }

    public void mouseClicked(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {
        // Somehow during a drag the button was released without us noticing;
        // cancel the drag now then. (I think this can happen when some other
        // window steals focus during a drag).
        if(!forcedDrag && !e.isShiftDown() && ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK)==0) ) {
            cancelDrag();
        }
    }

    public void mouseExited(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {
        Component destination = getComponentBeneath(e);

        if (destination != null && destination instanceof DropTarget) {
            DropTarget dropTarget = (DropTarget) destination;
            Point tp = e.getPoint().getLocation();
            tp.translate(dragOffsetX, dragOffsetY);
            Point destinationPoint = SwingUtilities.convertPoint(e.getComponent(), tp, destination);
            Object tmpData = data;
            dropTarget.drop(tmpData, destinationPoint);
            lastDropTarget = null;
        }
        endDrag();
    }

    private Component getComponentBeneath(MouseEvent e)
    {
        RootPaneContainer frame = getRootPaneContainer(this);
        if (frame == null) {
            return null;
        }
        Container contentPane = frame.getContentPane();

        Component glassPane;
        if (e.getSource() instanceof Component)
            glassPane = (Component) e.getSource();
        else
            glassPane = null;
        
        int menuBarHeight = 0;
        if(frame instanceof JFrame) {
            JMenuBar menuBar = ((JFrame) frame).getJMenuBar();
            if(menuBar != null) {
                menuBarHeight = menuBar.getHeight();
            }
        }
        Point glassPanePoint = e.getPoint();
        Container container = contentPane;
        Point containerPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, contentPane);
        if (containerPoint.y < 0) { //we're not in the content pane
            if (containerPoint.y + menuBarHeight >= 0) {
                //The mouse event is over the menu bar.
                //Could handle specially.
            }
            else {
                //The mouse event is over non-system window
                //decorations, such as the ones provided by
                //the Java look and feel.
                //Could handle specially.
            }
        }
        else {
            //The mouse event is probably over the content pane.
            //Find out exactly which component it's over.
            Component destination = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            return destination;
        }
        return null;
    }

    /**
     * Returns the RootPaneContainer from this components parent hierarchy.
     * @param pane
     * @return
     */
    private RootPaneContainer getRootPaneContainer(Component pane)
    {
        Component c = pane;
        while(c.getParent() != null && !(c instanceof RootPaneContainer)) {
            c = c.getParent();
        }
        
        return (RootPaneContainer) c;
    }

    private void storePosition(MouseEvent e)
    {
        e = SwingUtilities.convertMouseEvent((Component) e.getSource(), e, this);
        rect.x = e.getX() + dragOffsetX - dragImage.getWidth()/2;
        rect.y = e.getY() + dragOffsetY - dragImage.getHeight()/2;
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
        cancelDrag(); // dragEnded/dragFinished
    }

    public void keyTyped(KeyEvent e)
    {
    }
}