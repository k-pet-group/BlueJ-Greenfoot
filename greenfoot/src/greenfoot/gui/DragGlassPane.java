package greenfoot.gui;

import greenfoot.GreenfootObject;
import greenfoot.ImageVisitor;
import greenfoot.core.LocationTracker;
import greenfoot.util.Location;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.*;

/**
 * Component that can be used for dragging. It should be used as a glasspane on
 * a JFrame. A drag is started with the startDrag() method. The drag will end
 * when the mouse is released and the component on that location get the
 * MouseEvent (mouseReleased)
 * 
 * Some of this is taken from:
 * http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/GlassPaneDemo.java
 * 
 * after startDrag():
 * - drag() sent to drop target when object is dragged over it
 * - dragEnded() sent to drop target when object is dragged off it
 * 
 * If the object is dropped on a drop target:
 * - drop() is sent to the drop target (dragEnded() is not sent)
 * - dragFinished() is sent to the drag listener
 * 
 * If the drag is cancelled:
 * - dragEnded() is sent to the drop target
 * - dragFinished() is sent to the drag listener
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: DragGlassPane.java 3653 2005-10-06 13:21:09Z polle $
 *  
 */
public class DragGlassPane extends JComponent
    implements MouseMotionListener, MouseListener
{

    private transient final static Logger logger = Logger.getLogger("greenfoot");

    /** Singleton */
    private static DragGlassPane instance;

    /** The image displayed when dragging where no DropTarget is below */
    private greenfoot.GreenfootImage image;
    private Icon noParkingIcon;

    /** Should the dragGlassPane display the no drop image? */
    private boolean paintNoDropImage;

    /** Rotation of the image */
    private double rotation;

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

    private boolean forcedDrag;

    public static DragGlassPane getInstance()
    {
        if (instance == null) {
            instance = new DragGlassPane();
        }
        return instance;
    }

    private DragGlassPane()
    {
        //HACK this is a mac hack that is necessay because I can't get the
        // glasspane to grab the focus.
        //Toolkit.getDefaultToolkit().addAWTEventListener(eventListener,
        //        (AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK));
        setVisible(false);

        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        URL noParkingIconFile = this.getClass().getClassLoader().getResource("noParking.png");
        if (noParkingIconFile != null) {
            noParkingIcon = new ImageIcon(noParkingIconFile);
        }
    }

    public void paintComponent(Graphics g)
    {
        if (image != null && paintNoDropImage) {
            Graphics2D g2 = (Graphics2D) g;

            int width = rect.width;
            int height = rect.height;

            double halfWidth = width / 2.;
            double halfHeight = height / 2.;

            double rotateX = halfWidth + rect.getX();
            double rotateY = halfHeight + rect.getY();
            g2.rotate(Math.toRadians(rotation), rotateX, rotateY);
            ImageVisitor.drawImage(image, g2, rect.x, rect.y, this);

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
     * is up. In the case of a genuine drag, the DragGlassPane should bet set
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
    public void startDrag(GreenfootObject object, int xOffset, int yOffset, DragListener dl, DropTarget initialDropTarget, boolean forcedDrag)
    {
        if (object == null || object.getImage() == null) {
            return;
        }
        this.forcedDrag = forcedDrag;
        setDragImage(object.getImage(), object.getRotation());
        setDragObject(object);
        paintNoDropImage = false;
        dragOffsetX = xOffset;
        dragOffsetY = yOffset;
        dragListener = dl;
        setVisible(true);
        if(initialDropTarget != null) {
            lastDropTarget = initialDropTarget;
            //force painting of drag object
            Location l = LocationTracker.instance().getLocation();
            Point p = new Point(l.getX(), l.getY());
            lastDropTarget.drag(object, p);
        }
        //Toolkit.getDefaultToolkit().addAWTEventListener(eventListener,
        //        (AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK));
        logger.info("DragGlassPane.startDrag begin: " + this);
    }

    /**
     * Call this method to cancel a drag/drop operation. dragEnded() will be
     * called for the dropTarget over which the object is currently being
     * dragged, and then dragFinished() will be called on the DragListener.
     */
    public void cancelDrag()
    {
        if (lastDropTarget != null)
            lastDropTarget.dragEnded(data);
        
        endDrag();
    }
    
    /**
     * The drag is finished.
     */
    private void endDrag()
    {
        logger.info("DragGlassPane.endDrag: " + this);
        if (lastDropTarget != null) {
            lastDropTarget.dragEnded(data);
        }

        // Save the old values of dragListener and data for the "dragFinished"
        // call below
        DragListener dl = dragListener;
        Object od = data;
        
        setVisible(false);
        data = null;
        image = null;
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
    public void setDragImage(greenfoot.GreenfootImage image, double rotation)
    {
        this.image = image;
        int width = image.getWidth();
        int height = image.getHeight();
        rect.width = width;
        rect.height = height;
        this.rotation = rotation;
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
        if(image == null) {
            //No valid drag object available.
            return;
        }
        //logger.info("DragGlassPane.move" + e.paramString());
        storePosition(e);
        paintNoDropImage = true;
        boolean doRepaint = true;
        Component destination = getComponentBeneath(e);
        DropTarget dropTarget = null;
        if (destination != null && destination instanceof DropTarget) {
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
        JFrame frame = (JFrame) SwingUtilities.getRoot(this);
        if (frame == null) {
            return null;
        }
        Container contentPane = frame.getContentPane();

        Component glassPane;
        if (e.getSource() instanceof Component)
            glassPane = (Component) e.getSource();
        else
            glassPane = null;
            
        JMenuBar menuBar = frame.getJMenuBar();

        Point glassPanePoint = e.getPoint();
        Container container = contentPane;
        Point containerPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, contentPane);
        if (containerPoint.y < 0) { //we're not in the content pane
            if (containerPoint.y + menuBar.getHeight() >= 0) {
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

    private void storePosition(MouseEvent e)
    {
        MouseEvent eThis = SwingUtilities.convertMouseEvent((Component) e.getSource(), e, this);
        rect.x = eThis.getX() + dragOffsetX - image.getWidth()/2;
        rect.y = eThis.getY() + dragOffsetY - image.getHeight()/2;
    }
}