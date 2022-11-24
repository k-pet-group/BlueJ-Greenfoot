/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2013,2014,2015,2016,2020  Michael Kolling and John Rosenberg
 
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


import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A CodeOverlayPane is an overlay that sits on top of the entire code.  The
 * CodeOverlayPane is inside the ScrollPane, and moves up and down as you scroll.
 * 
 * It is useful for when you want an overlay that is fixed with respect to a particular
 * item in the code, for example you might want an autocomplete beneath a slot or a tooltip
 * above a slot.  If you want something that is fixed with respect to the window, see
 * WindowOverlayPane instead.
 *
 */
public class CodeOverlayPane
{
    /**
     * The actual Pane
     */
    private final Pane pane = new Pane();
    
    public CodeOverlayPane()
    {
        JavaFXUtil.addStyleClass(pane, "code-overlay-pane");
        // Capture all mouse events, by default:
        pane.pickOnBoundsProperty().set(false);
        pane.setMouseTransparent(false);
    }

    public static enum WidthLimit
    {
        NO_WIDTH_LIMIT,
        /***
        * Limits the width of the given graphical element
        * so that it doesn't extend beyond the right-hand side edge of the pane
        */
        LIMIT_WIDTH,
        /**
         * As above, but also move it left to make sure its minimum width can show
         */
        LIMIT_WIDTH_AND_SLIDE_LEFT
    }

    @OnThread(Tag.FXPlatform)
    public void addOverlay(final Node overlay, final Node relativeTo, final DoubleExpression xOffset, final DoubleExpression yOffset)
    {
        addOverlay(overlay, relativeTo, xOffset, yOffset, WidthLimit.NO_WIDTH_LIMIT);
    }

    
    /**
     * Adds the given overlay (first parameter) to the overlay pane so that its position always
     * remains at the top left of relativeTo (second parameter), plus xOffset (or 0 if null) and
     * yOffset (or 0 if null)
     */
    @OnThread(Tag.FXPlatform)
    public void addOverlay(final Node overlay, final Node relativeTo, final DoubleExpression xOffset, final DoubleExpression yOffset, WidthLimit widthLimit)
    {       
        final DoubleBinding xPosition = new DoubleBinding() {
            { super.bind(relativeTo.layoutBoundsProperty());
              super.bind(relativeTo.localToSceneTransformProperty()); 
              super.bind(pane.layoutBoundsProperty());
              super.bind(pane.localToSceneTransformProperty());
              if (xOffset != null) super.bind(xOffset);}
            
            @Override
            protected double computeValue()
            {
                double usualX = relativeTo.localToScene(relativeTo.getBoundsInLocal()).getMinX() + (xOffset == null ? 0 : xOffset.get());
                if (widthLimit == WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT)
                {
                    return Math.max(0, Math.min(usualX, pane.widthProperty().subtract(((Region) overlay).minWidthProperty()).get()));
                }
                else
                {
                    return usualX;
                }
            }
        };
        
        final DoubleBinding yPosition = new DoubleBinding() {
            { super.bind(relativeTo.layoutBoundsProperty());
              super.bind(relativeTo.localToSceneTransformProperty());
              super.bind(pane.layoutBoundsProperty());
              super.bind(pane.localToSceneTransformProperty());
              if (yOffset != null) super.bind(yOffset);}
            
            
            @Override
            protected double computeValue()
            {
                double endRef = relativeTo.localToScene(relativeTo.getBoundsInLocal()).getMinY();
                return sceneYToCodeOverlayY(endRef + (yOffset == null ? 0 : yOffset.get()));
            }
        };
       
        overlay.layoutXProperty().bind(xPosition);
        overlay.layoutYProperty().bind(yPosition);

        if (widthLimit != WidthLimit.NO_WIDTH_LIMIT)
            ((Region)overlay).maxWidthProperty().bind(pane.widthProperty().subtract(overlay.layoutXProperty()));

        pane.getChildren().add(overlay);
    }

    /**
     * Converts a scene Y coordinate into a Y coordinate in this pane
     */
    public double sceneYToCodeOverlayY(double sceneY)
    {
        return pane.sceneToLocal(0, sceneY).getY();
    }

    @OnThread(Tag.FXPlatform)
    public void removeOverlay(Node node)
    {
        pane.getChildren().remove(node);        
    }
    
    public Node getNode()
    {
        return pane;
    }

    /**
     * Adds a drop shadow to the given node
     */
    public static void setDropShadow(Node n)
    {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(8.0);
        dropShadow.setOffsetX(4.0);
        dropShadow.setOffsetY(4.0);
        dropShadow.setColor(Color.color(0.6, 0.6, 0.6));
        n.setEffect(dropShadow);
    }

    /**
     * Adds a canvas to the pane, which is the same size as the pane, and returns it.
     */
    public Canvas addFullSizeCanvas()
    {
        Canvas c = new Canvas(pane.getWidth(), pane.getHeight());
        c.setLayoutX(0.0);
        c.setLayoutY(0.0);
        c.setMouseTransparent(true);
        pane.getChildren().add(c);
        return c;
    }
}
