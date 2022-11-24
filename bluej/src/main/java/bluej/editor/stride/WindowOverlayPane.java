/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2013,2014,2015,2021,2022  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.stride;


import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * A WindowOverlayPane is as big as the editor window (well, the scrollpane part),
 * and sits on top of the window contents.  It is useful if you have an overlay
 * which should have its position fixed with respect to the window, for example
 * the pinned method header mechanism.  If you want an overlay fixed with respect to
 * an item inside the scroll pane (e.g. a tooltip or autocomplete beneath a slot),
 * use CodeOverlayPane instead.
 *
 */
public class WindowOverlayPane
{
    private final Pane pane = new Pane();
    // References saved for avoiding weak reference GCing in bindings
    private ObservableDoubleValue x, y;
    private NumberBinding xBinding, paneWidthBinding;
    private ReadOnlyDoubleProperty nodeWidthBinding;
    
    public WindowOverlayPane()
    {
        // Make overlay pane look at shapes not bounds for capturing mouse events:
        pane.pickOnBoundsProperty().set(false);
    }

    public void addOverlay(Node node, ObservableDoubleValue x, ObservableDoubleValue y)
    {
        addOverlay(node, x, y, false);
    }
    
    public void addOverlay(Node node, ObservableDoubleValue x, ObservableDoubleValue y, boolean moveLeftIfNeeded)
    {
        pane.getChildren().add(node);
        this.x = x;
        this.y = y;
        nodeWidthBinding = node instanceof Rectangle ? ((Rectangle)node).widthProperty() : ((Region)node).widthProperty();
        paneWidthBinding = pane.widthProperty().subtract(nodeWidthBinding);

        if (moveLeftIfNeeded)
        {
            xBinding = Bindings.min(x, paneWidthBinding);
            node.layoutXProperty().bind(xBinding);
        }
        else
        {
            node.layoutXProperty().bind(this.x);
        }
        node.layoutYProperty().bind(this.y);
    }

    public void removeOverlay(Node node)
    {
        pane.getChildren().remove(node);        
    }

    public double sceneXToWindowOverlayX(double sceneX)
    {
        return pane.sceneToLocal(sceneX, 0).getX();
    }

    public double sceneYToWindowOverlayY(double sceneY)
    {
        return pane.sceneToLocal(0, sceneY).getY();
    }
    
    public Node getNode()
    {
        return pane;
    }

    public boolean contains(Node node)
    {
        return pane.getChildren().contains(node);
    }
}
