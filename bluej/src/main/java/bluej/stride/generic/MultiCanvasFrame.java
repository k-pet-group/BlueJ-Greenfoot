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

import bluej.stride.operations.FrameOperation;
import bluej.stride.operations.PullUpContentsOperation;
import javafx.geometry.Bounds;
import javafx.scene.Node;

import bluej.utility.javafx.FXConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

public abstract class MultiCanvasFrame extends Frame implements CanvasParent
{
    protected final List<FrameCanvas> canvases = new ArrayList<FrameCanvas>();
    // Each canvas is potentially preceded by a divider.
    // Should always be same size as canvases, but may have null entries:
    protected final List<FrameContentItem> dividers = new ArrayList<>();
    
    // TODO do we want several sidebars, one per canvas?
    protected final Sidebar sidebar;
        
    /**
     * @param caption
     * @param stylePrefix
     */
    protected MultiCanvasFrame(final InteractionManager editor, String caption, String stylePrefix)
    {
        super(editor, caption, stylePrefix);

        sidebar = Sidebar.addSidebar(editor, getSidebarContainer(), getNode().layoutBoundsProperty(), stylePrefix);

        //setBlockContainerSpacing(3);
        //vBox.setMargin(canvas, new Insets(0,0,0,35));

        //canvas.getChildren().add(0, new CursorBlock());
        //header.getChildren().add(new ParameterSlot(prevRedirect, canvas.getChildren().get(0), b));
    }
    
    /**
     * 
     * @param divider Divider to go above the canvas.  Can be null.
     * @param canvas The canvas to add.
     */
    protected void addCanvas(FrameContentItem divider, FrameCanvas canvas, int at)
    {
        canvases.add(at, canvas);
        dividers.add(at, divider);
        
        updateDisplay();
    }
    
    protected void addCanvas(FrameContentItem divider, FrameCanvas canvas)
    {
        addCanvas(divider, canvas, canvases.size());
    }
    
    protected void removeCanvas(FrameCanvas canvas)
    {
        int index = canvases.indexOf(canvas);
        // Remove preceding divider (it may be null, but I think that will be fine:
        dividers.remove(index);
        canvases.remove(index);
        updateDisplay();
    }

    private void updateDisplay()
    {
        List<FrameContentItem> updatedChildren = new ArrayList<>();
        updatedChildren.add(header);
        for (int i = 0; i < canvases.size(); i++) {
            if (dividers.get(i) != null) {
                updatedChildren.add(dividers.get(i));
            }
            updatedChildren.add(canvases.get(i));

            canvases.get(i).setLastInMulti(i == canvases.size() - 1);
        }
        modifyChildren(updatedChildren);
        contents.setAll(updatedChildren);
    }

    protected void modifyChildren(List<FrameContentItem> updatedChildren)
    {
        // Nothing to do by default; can be overridden in subclasses
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = super.getContextOperations();
        r.add(new PullUpContentsOperation(getEditor()));
        return r;
    }

    protected final FrameCanvas getLastCanvas()
    {
        return canvases.get(canvases.size() - 1);
    }
    
    @Override
    public double lowestCursorY()
    {
        // Our local bounds are unreliable because of the way we transform
        // the side label -- therefore use canvas plus margin:
        Bounds canvasBounds = getLastCanvas().getSceneBounds();
        return canvasBounds.getMaxY();
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

    @Override
    protected List<? extends Node> calculateContents(List<Node> normalContent)
    {
        ArrayList<Node> content = new ArrayList<>(super.calculateContents(normalContent));
        if (sidebar != null)
            content.add(0, sidebar.getNode());
        return content;
    }

    @Override
    public Frame getFrame()
    {
        return this;
    }
}
