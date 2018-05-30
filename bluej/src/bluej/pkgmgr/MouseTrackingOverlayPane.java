/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2018  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An overlay pane which can display nodes at arbitrary positions, 
 * with some additional support for tracking the current mouse position.
 * 
 * It should be added to the front of a StackPane in front of whatever
 * you want the overlays to appear in front of.
 */
@OnThread(Tag.FXPlatform)
public class MouseTrackingOverlayPane extends Pane
{
    // Coordinates are local to the pane:
    private final DoubleProperty mouseX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty mouseY = new SimpleDoubleProperty(0.0);
    // Is the mouse currently inside the pane:
    private final BooleanProperty mouseInsideProperty = new SimpleBooleanProperty(false);
    private final List<MousePositionListener> listeners = new ArrayList<>();
    
    public MouseTrackingOverlayPane()
    {
        // Ideally, we'd track mouse movement on our own pane, but we need to
        // be mouse transparent so that even if the overlay overlaps the mouse cursor,
        // the user can still click on the item beneath.  So we track mouse movements
        // on our parent node:
        setMouseTransparent(true);
        JavaFXUtil.onceNotNull(parentProperty(), parent ->
        {
            parent.addEventFilter(MouseEvent.MOUSE_MOVED, e ->
            {
                Point2D p = MouseTrackingOverlayPane.this.sceneToLocal(e.getSceneX(), e.getSceneY());
                mouseX.set(p.getX());
                mouseY.set(p.getY());
                for (MousePositionListener l : listeners)
                    l.mouseMoved(e.getSceneX(), e.getSceneY());
            });
            parent.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> mouseInsideProperty.set(true));
            parent.addEventFilter(MouseEvent.MOUSE_EXITED, e -> mouseInsideProperty.set(false));
        });
    }

    /**
     * Adds a node to this pane.  The node's position will be set to be the current mouse
     * position plus the given offsets.
     * 
     * if showWhenMouseLeft is true, the node will be shown at the last mouse position when
     * the mouse was inside the (parent's; see constructor) node.  If it's false, it will
     * be hidden once the mouse exits.
     */
    public void addMouseTrackingOverlay(Node info, boolean showWhenMouseLeft, DoubleExpression xOffset, DoubleExpression yOffset)
    {
        getChildren().add(info);
        info.layoutXProperty().bind(mouseX.add(xOffset));
        info.layoutYProperty().bind(mouseY.add(yOffset));
        if (!showWhenMouseLeft)
            info.visibleProperty().bind(mouseInsideProperty);
    }
    
    public void addMouseListener(MousePositionListener mouseListener)
    {
        listeners.add(mouseListener);
    }

    /**
     * Removes the node from the pane.
     */
    public void remove(Node node)
    {
        getChildren().remove(node);
    }
    
    public void removeMouseListener(MousePositionListener mousePositionListener)
    {
        listeners.add(mousePositionListener);
    }
    
    
    @OnThread(Tag.FXPlatform)
    @FunctionalInterface
    public static interface MousePositionListener
    {
        // These will only be received while the mouse is in the pane,
        // not when it is outside the pane:
        public void mouseMoved(double sceneX, double sceneY);
    }
}
