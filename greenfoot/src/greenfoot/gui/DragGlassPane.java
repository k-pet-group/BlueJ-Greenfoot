package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.core.LocationTracker;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
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
 * <p>
 * All instance methods in this class should be called on the Swing event thread.
 * <p>
 * This component is used for dragging initiated either by invoking a
 * constructor through the menus or by using the SHIFT-add feature.
 * <p>
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
 * @author Poul Henriksen
 * @version $Id: DragGlassPane.java 5708 2008-04-22 21:26:08Z polle $
 * 
 */
public class DragGlassPane extends JComponent
    implements TriggeredMouseMotionListener, TriggeredMouseListener
{
    /** Singleton */
    private static DragGlassPane instance;

    /** The image displayed when dragging where no DropTarget is below */
    private Icon noParkingIcon;

    /** Should the dragGlassPane display the no drop image? */
    private boolean paintNoDropImage;

    /** The object that is dragged */
    private Object data;

    /** Rectangle defining the current bounds of the object being dragged */
    private Rectangle dragRect = new Rectangle();

    /** Rectangle defining the last bounds of where a drag object was painted */
    private Rectangle lastPaintRect = new Rectangle();

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
     * Image used when dragging. If this is null, no dragging is happening at
     * the moment.
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
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        URL noParkingIconFile = this.getClass().getClassLoader().getResource("noParking.png");
        if (noParkingIconFile != null) {
            noParkingIcon = new ImageIcon(noParkingIconFile);
        }
    }

    public void paintComponent(Graphics g)
    {
        // We only handle painting here if no drop-target could handle the
        // painting, and if we have a dragImage
        if (dragImage != null && paintNoDropImage) {
            Graphics2D g2 = (Graphics2D) g;
            // Set the clip to be the union between the image we are going to
            // paint now, and the previously painted image.
            Rectangle currentClip = new Rectangle(dragRect.x, dragRect.y, dragImage.getWidth(), dragImage.getHeight());
            Rectangle temp = new Rectangle(currentClip);
            currentClip.add(lastPaintRect);
            lastPaintRect = temp;
            g2.clip(currentClip);
            g2.drawImage(dragImage, dragRect.x, dragRect.y, null);
        }
        else {
            // we do nothing - a DropTarget should have handled this
        }
    }

    /**
     * Initiates a drag.
     * <p>
     * 
     * There are two types of drag: a "genuine" drag where an object is being
     * dragged with the mouse button down, and a "forced" drag where the button
     * is up. In the case of a genuine drag, the DragGlassPane should be added
     * as a MouseListener and MouseMotionListener to the component receiving the
     * drag events. Otherwise, this is not necessary.
     * 
     * @param object
     *            The object to drag.
     * @param dl
     *            The listener to be notified when the operation finishes
     * @param initialDropTarget
     *            An initial drop target. It can be null. Used when we want to
     *            immediately paint a dragImage unto the drop target.
     * @param forcedDrag
     *            indicates whether the drag is done without any buttons
     *            pressed. This allows the drag to continue even if no keyboard
     *            or mouse buttons are pressed.
     * 
     */
    public void startDrag(Actor object, DragListener dl, DropTarget initialDropTarget, boolean forcedDrag)
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

        // get last mouseevent to get first location
        MouseEvent e = LocationTracker.instance().getMouseMotionEvent();
        if (e == null) {
            // This startDrag was probably initiated by a mouse event that was
            // handled before the LocationTracker got a chance to handle it.
            endDrag();
            return;
        }

        setDragImage(objectImage);
        setDragObject(object);
        paintNoDropImage = true;

        storePosition(e);
        lastDropTarget = initialDropTarget;

        setVisible(true);
        if (initialDropTarget != null) {
            // force painting of drag object
            Point p = e.getPoint();
            paintNoDropImage = !initialDropTarget.drag(object, p);
            if (paintNoDropImage) {
                repaint();
            }
        }
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
     */
    public void setDragImage(final greenfoot.GreenfootImage image)
    {
        BufferedImage awtImage = image.getAwtImage();

        // TODO: run on event thread since it is used in paintComponent?
        dragImage = GreenfootUtil.createDragShadow(awtImage);

        dragRect.width = image.getWidth();
        dragRect.height = image.getHeight();

        Graphics2D g = dragImage.createGraphics();

        // We use original image proportions, to get the icon in the middle when
        // not considering the shadow.
        int x = (int) ((image.getWidth() - noParkingIcon.getIconWidth()) / 2);
        int y = (int) ((image.getHeight() - noParkingIcon.getIconHeight()) / 2);
        g.setColor(Color.RED);
        noParkingIcon.paintIcon(this, g, x, y);
        g.dispose();
    }

    /**
     * Sets the object to be dragged.
     * 
     * @param object
     */
    public void setDragObject(final Object object)
    {
        data = object;
    }

    private void move(MouseEvent e)
    {
        if (dragImage == null) {
            // No valid drag object available.
            return;
        }
        storePosition(e);
        boolean doRepaint = true;
        Component destination = getComponentBeneath(e);
        DropTarget dropTarget = null;
        if (destination instanceof DropTarget) {
            dropTarget = (DropTarget) destination;

            Point tp = e.getPoint().getLocation(); // copy the point
            Point p = SwingUtilities.convertPoint(e.getComponent(), tp, destination);
            if (dropTarget.drag(data, p)) {
                if (paintNoDropImage) {
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
            // We need to repaint because the drag was not processed by another
            // component.
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
    {}

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
        if (frame instanceof JFrame) {
            JMenuBar menuBar = ((JFrame) frame).getJMenuBar();
            if (menuBar != null) {
                menuBarHeight = menuBar.getHeight();
            }
        }
        Point glassPanePoint = e.getPoint();
        Container container = contentPane;
        Point containerPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, contentPane);
        if (containerPoint.y < 0) { // we're not in the content pane
            if (containerPoint.y + menuBarHeight >= 0) {
                // The mouse event is over the menu bar.
                // Could handle specially.
            }
            else {
                // The mouse event is over non-system window
                // decorations, such as the ones provided by
                // the Java look and feel.
                // Could handle specially.
            }
        }
        else {
            // The mouse event is probably over the content pane.
            // Find out exactly which component it's over.
            Component destination = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            return destination;
        }
        return null;
    }

    /**
     * Returns the RootPaneContainer from this components parent hierarchy.
     * 
     * @param pane
     * @return
     */
    private RootPaneContainer getRootPaneContainer(Component pane)
    {
        Component c = pane;
        while (c.getParent() != null && !(c instanceof RootPaneContainer)) {
            c = c.getParent();
        }

        return (RootPaneContainer) c;
    }

    private void storePosition(MouseEvent e)
    {
        e = SwingUtilities.convertMouseEvent((Component) e.getSource(), e, this);
        dragRect.x = (int) (e.getX() - dragRect.getWidth() / 2);
        dragRect.y = (int) (e.getY() - dragRect.getHeight() / 2);
    }

    public void listeningEnded()
    {
        cancelDrag();
    }

    public void listeningStarted()
    {
    }
}