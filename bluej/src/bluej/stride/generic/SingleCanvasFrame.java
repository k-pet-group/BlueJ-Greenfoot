/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.generic;


import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A statement block with a canvas underneath for embedding additional blocks - used for if statements, loops, etc.
 * @author Fraser McKay
 */
public abstract class SingleCanvasFrame extends Frame implements CanvasParent
{
    protected FrameCanvas canvas;
    
    private Sidebar sidebar;
    
    private boolean collapsible = false; 
    private boolean collapsed = false;

    private PlusMinus collapsedIndicator;
        
    /**
     * @param caption
     * @param stylePrefix
     */
    protected SingleCanvasFrame(final InteractionManager editor, String caption, String stylePrefix)
    {
        super(editor, caption, stylePrefix);
        
        canvas = createCanvas(editor, stylePrefix);

        sidebar = Sidebar.addSidebar(editor, getSidebarContainer(), getNode().layoutBoundsProperty(), stylePrefix);

        contents.setAll(getHeaderRow(), canvas);

        //setBlockContainerSpacing(3);
        //vBox.setMargin(canvas, new Insets(0,0,0,35));

        //canvas.getChildren().add(0, new CursorBlock());
        //header.getChildren().add(new ParameterSlot(prevRedirect, canvas.getChildren().get(0), b));

    }

    private static class PlusMinus extends Canvas
    {
        private final static double DIAMETER = 15;
        private final static double BAR_WIDTH = 2;
        
        public PlusMinus()
        {
            setWidth(DIAMETER + 5); // To get some RHS spacing
            setHeight(DIAMETER);
            drawPlus(false);
        }
        
        public void drawPlus(boolean light)
        {
            drawMinus(light);
            // Minus with one more bar removed:
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(DIAMETER / 2 - BAR_WIDTH / 2, 0, BAR_WIDTH, DIAMETER);
        }
        public void drawMinus(boolean light)
        {
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
            
            gc.setFill(Color.rgb(0, 0, 0, light ? 0.1 : 0.2));
            gc.fillOval(0, 0, DIAMETER, DIAMETER);
            
            
            // Now take out the minus part:
            gc.clearRect(0, DIAMETER / 2 - BAR_WIDTH / 2, DIAMETER, BAR_WIDTH);
            
            //gc.setStroke(Color.rgb(0, 0, 0, 0.1));
            //gc.strokeOval(0, 0, getWidth(), getHeight());
        }
    }
    
    protected void setCollapsible(boolean collapsible)
    {
        if (collapsible != this.collapsible)
        {
            if (collapsible)
            {
                collapsedIndicator = new PlusMinus();
                collapsedIndicator.setCursor(Cursor.HAND);
                collapsedIndicator.setOnMouseClicked(e ->
                    {
                        setCollapsed(!collapsed);
                        e.consume();
                    });
                collapsedIndicator.setOnMouseEntered(e ->
                {
                   if (collapsed)
                       collapsedIndicator.drawPlus(true);
                   else
                       collapsedIndicator.drawMinus(true);
                });
                collapsedIndicator.setOnMouseExited(e ->
                {
                   if (collapsed)
                       collapsedIndicator.drawPlus(false);
                   else
                       collapsedIndicator.drawMinus(false);
                });
                addTopRight(collapsedIndicator);
            }
            else
            {
                // TODO remove +/-
                // TODO expand if collapsed
            }
            
            this.collapsible = collapsible;
        }
    }
    
    @Override
    public boolean isCollapsible()
    {
        return collapsible;
    }
    
    @Override
    public void setCollapsed(boolean collapse)
    {
        if (collapse && !collapsible)
            return;
        
        if (collapse != this.collapsed)
        {
            // TODO we also need to hide the overlays.  Use cleanup?  Need reverse for when we re-add
            if (collapse) {
                collapsedIndicator.drawPlus(false);
            }
            else {
                collapsedIndicator.drawMinus(false);
            }
            this.collapsed = collapse;
        }
    }

    /**
     * Dissolves/deletes the "edges" of the container, so that contents remain intact, on the container's own parent
     * @see expandContents()
     */
    public void dissolve()
    {
        this.expandContents();
        getParentCanvas().removeBlock(this);
    }
    
    /**
     * Remove all children and dump them in this container's parent, expanding contents out of container
     */
    public void expandContents()
    {
//        try
//        {
            FrameCanvas p = this.getParentCanvas();
            canvas.emptyTo(p, this);
//        }
//        catch(Exception ex){
//            //
//        }
    }

    /**
     * Get this block's internal canvas that contains its inner body (for a loop, etc.).
     * @return This container's body canvas
     */
    public FrameCanvas getCanvas()
    {
        return canvas;
    }

    /**
     * Gets an update function for the sidebar.
     * (Curried refers to functional programming term, meaning a function that
     * takes one argument and returns a function which consumes the next)
     */
    protected FXConsumer<String> updateSidebarCurried(String prefix)
    {
        return content -> sidebar.textProperty().set(prefix + "(" + content + ")");
    }

    protected void setSidebar(String content)
    {
        sidebar.setText(content);
    }

    @Override
    public double lowestCursorY()
    {
        // Our local bounds are unreliable because of the way we transform
        // the side label -- therefore use canvas which includes margin:
        return canvas.getSceneBounds().getMaxY();
    }

    /*
    @Override
    protected void setPreviewMode(PreviewMode view)
    {
        if (view == PreviewMode.EXPAND_OUT)
        {
            canvas.getNode().setEffect(new DropShadow(10.0, Color.BLACK));
            
            Timeline t = new Timeline();
            KeyValue kv1 = new KeyValue(canvas.getNode().translateXProperty(), -40);
            KeyValue kv2 = new KeyValue(canvas.getNode().translateYProperty(), 20);
            KeyValue kv3 = new KeyValue(getRegion().maxHeightProperty(), 0);
            BorderPane.setMargin(getRegion(), new Insets(0, 0, canvas.getHeight() - 20, 0));
            KeyFrame kf1 = new KeyFrame(Duration.millis(500), kv1);
            KeyFrame kf2 = new KeyFrame(Duration.millis(500), kv2);
            KeyFrame kf3 = new KeyFrame(Duration.millis(500), kv3);
            t.getKeyFrames().add(kf1);
            t.getKeyFrames().add(kf2);
            t.getKeyFrames().add(kf3);            
            t.play();
        }
        else
        {
            if (view == PreviewMode.NONE)
            {
                canvas.getNode().setTranslateX(0);
                canvas.getNode().setTranslateY(0);
                canvas.getNode().setEffect(null);
                getRegion().setMaxHeight(Region.USE_COMPUTED_SIZE);
                BorderPane.setMargin(getRegion(), new Insets(0));
            }
            super.setPreviewMode(view);
        }
    }
    */

    protected FrameCanvas createCanvas(InteractionManager editor, String stylePrefix)
    {
        return new FrameCanvas(editor, this, stylePrefix);
    }

    @Override
    public Frame getFrame()
    {
        return this;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, sidebar.getStyleable());
        canvas.getCursors().forEach(c -> c.setView(newView, animateProgress));
        canvas.setView(oldView, newView, animateProgress);
    }

    protected DoubleExpression tweakOpeningCurlyY()
    {
        return null;
    }

    protected double tweakCurlyX()
    {
        return 0;
    }

    @Override
    protected List<? extends Node> calculateContents(List<Node> normalContent)
    {
        ArrayList<Node> content = new ArrayList<>(super.calculateContents(normalContent));
        if (sidebar != null)
            content.add(0, sidebar.getNode());
        return content;
    }
    
    @Override
    public FrameTypeCheck check(FrameCanvas child)
    {
        if (child == this.canvas)
        {
            return StrideDictionary.checkStatement();
        }
        else
        {
            throw new IllegalStateException("Asking about unknown child of SingleCanvasFrame");
        }
    }
    
}
