/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018 Michael Kölling and John Rosenberg
 
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.generic;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.BlankFrame;

import bluej.utility.javafx.ScalableHeightLabel;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.slots.HeaderItem;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A canvas: an area that contain several frames in a vertical array.
 * 
 * Each canvas actually contains:
 *  - N amounts of these triples:
 *     - cursor
 *     - "special"
 *     - frame
 *  - One final cursor
 *
 * Thus, even when there are no subframes, there is always one cursor in the canvas.
 * 
 * The "special" is a VBox to which you can add extra content between frames,
 * such as error messages or popup meta-information about a frame.
 * 
 * @author Fraser McKay
 */
public class FrameCanvas implements FrameContentItem
{
    private final CanvasParent parentBlock;
    //protected SelectionScope selectionScope;

    protected final InteractionManager editorFrm;

    private final ObservableList<Frame> blockContents = FXCollections.observableArrayList();
    private final List<VBox> specials = new ArrayList<VBox>();
    private final List<FrameCursor> cursors = new ArrayList<FrameCursor>();
    // When editorFrm != null, cursors should always be one larger than blockContents, and thus never empty

    private final CanvasVBox canvas = new CanvasVBox(200.0, blockContents);

    private final SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(true);
    private boolean animateLeftMarginScale;

    private int childBlockIndex(int blockIndex)
    {
        // block 0 is child 2
        // block 1 is child 5
        // etc
        return blockIndex * 3 + 2; 
    }
    
    private int childCursorIndex(int cursorIndex)
    {
        // cursor 0 is child 0
        // cursor 1 is child 3
        // etc
        return (cursorIndex * 3);
    }
    
    private void validate(FrameCursor cursor, int cursorIndex)
    {
        validate();
        if (cursors.get(cursorIndex) != cursor)
        {
            throw new IllegalStateException("Stable cursor was moved, from: " + cursorIndex + " to: " + cursors.indexOf(cursor));
        }
    }

    /**
     * Checks that the contents are valid
     */
    private void validate()
    {
        if (cursors.size() > 0 && cursors.size() != blockContents.size() + 1)
            throw new IllegalStateException("Canvas: cursors and blocks out of length sync");
        if (canvas.getChildren().size() != cursors.size() + blockContents.size() + specials.size())
            throw new IllegalStateException("Canvas: children out of length sync");
        for (int i = 0; i < cursors.size(); i++)
        {
            if (cursors.get(i) == null)
                throw new IllegalStateException("Canvas: cursor is null");
            
            if (cursors.indexOf(cursors.get(i)) != i || cursors.lastIndexOf(cursors.get(i)) != i)
                throw new IllegalStateException("Canvas: cursor is duplicated");
            
            if (canvas.getChildren().get(childCursorIndex(i)) != cursors.get(i).getNode())
                throw new IllegalStateException("Canvas: cursors out of sync");
            
            if (cursors.get(i).getParentCanvas() != this)
                throw new IllegalStateException("Canvas: cursor parent out of sync");
        }
        for (int i = 0; i < blockContents.size(); i++)
        {
            if (blockContents.get(i) == null)
                throw new IllegalStateException("Canvas: block is null");
            
            if (blockContents.indexOf(blockContents.get(i)) != i || blockContents.lastIndexOf(blockContents.get(i)) != i)
                throw new IllegalStateException("Canvas: block is duplicated");
            
            if (canvas.getChildren().get(childBlockIndex(i)) != blockContents.get(i).getNode())
                throw new IllegalStateException("Canvas: blocks out of sync");
            
            if (blockContents.get(i).getParentCanvas() != this)
                throw new IllegalStateException("Canvas: block parent out of sync");
        }
        for (int i = 0; i < blockContents.size(); i++)
        {
            if (specials.get(i) == null)
                throw new IllegalStateException("Canvas: special is null");
            
            if (specials.indexOf(specials.get(i)) != i || specials.lastIndexOf(specials.get(i)) != i)
                throw new IllegalStateException("Canvas: special is duplicated");
            
            if (canvas.getChildren().get(childBlockIndex(i) - 1) != specials.get(i))
                throw new IllegalStateException("Canvas: specials out of sync");
        }
    }

    /**
     * Find if this is embedded in, for example, a "container" construct block like a loop or an if.
     */
    public CanvasParent getParent()
    {
        return parentBlock;
    }

    public VBox getSpecialBefore(FrameCursor cursor)
    {
        int index = 0;
        if (cursor == null)
        {
            index = 0;
        }
        else
        {
            index = cursors.indexOf(cursor);
        }
        if (index < 0)
            throw new IllegalArgumentException("insertSpecialBefore: canvas does not contain specified cursor");
        return specials.get(index);
    }
    
    public VBox getSpecialBefore(Frame f)
    {
        return getSpecialAfter(getCursorBefore(f));
    }
    
    public VBox getSpecialAfter(FrameCursor cursor)
    {
        int index;
        if (cursor == null)
        {
            index = cursors.size() - 1;
        }
        else
        {
            index = cursors.indexOf(cursor);
        }
        if (index < 0)
            throw new IllegalArgumentException("insertSpecialBefore: canvas does not contain specified cursor");
        return specials.get(index);
    }
    
    /**
     * Inserts block before the given cursor (which is guaranteed to end up after the inserted block)
     */
    public void insertBlockBefore(Frame toAdd, FrameCursor cursor)
    {
        if (toAdd == null)
            throw new IllegalArgumentException("Cannot add null block");
        if (!acceptsType(toAdd))
            throw new IllegalArgumentException("Block " + getClass() + " does not accept " + toAdd.getClass());
        if (toAdd.getParentCanvas() != null)
            throw new IllegalArgumentException("Block already has parent");
        
        int index = 0;
        if (cursor == null)
        {
            index = 0;
            cursor = cursors.get(index);
        }
        else
        {
            index = cursors.indexOf(cursor);
        }
        if (index < 0)
            throw new IllegalArgumentException("insertBlockBefore: canvas does not contain specified cursor");
        int childIndex = childCursorIndex(index);
        FrameCursor newCursor = editorFrm.createCursor(this);
        VBox special = new VBox();
        canvas.getChildren().add(childIndex, toAdd.getNode()); // Add block...
        canvas.getChildren().add(childIndex, special); // Then special before that..
        canvas.getChildren().add(childIndex, newCursor.getNode()); // Then add new cursor before it
        cursors.add(index, newCursor);
        blockContents.add(index, toAdd);
        specials.add(index, special);
        toAdd.setParentCanvas(this);
        validate(cursor, index + 1);
    }
    
    /**
     * Inserts block after the given cursor (which is guaranteed to remain in the same place)
     */
    public void insertBlockAfter(Frame toAdd, FrameCursor cursor)
    {
        if (toAdd == null)
            throw new IllegalArgumentException("Cannot add null block");
        if (!acceptsType(toAdd))
            throw new IllegalArgumentException("Block " + getClass() + " does not accept " + toAdd.getClass());
        if (toAdd.getParentCanvas() != null)
            throw new IllegalArgumentException("Block already has parent");
        
        int index;
        if (cursor == null)
        {
            index = cursors.size() - 1;
            cursor = cursors.get(index);
        }
        else
        {
            index = cursors.indexOf(cursor);
        }
        if (index < 0)
            throw new IllegalArgumentException("insertBlockAfter: canvas does not contain specified cursor");
        int childIndex = childCursorIndex(index);
        FrameCursor newCursor = editorFrm.createCursor(this);
        VBox special = new VBox();
        canvas.getChildren().add(childIndex + 1, newCursor.getNode()); //Add cursor...
        canvas.getChildren().add(childIndex + 1, toAdd.getNode()); // Then add new block before it...
        canvas.getChildren().add(childIndex + 1, special); // Then special before that..
        cursors.add(index + 1, newCursor);
        blockContents.add(index, toAdd); //Not +1 -- block with index "index" is after cursor with same index
        specials.add(index, special);
        toAdd.setParentCanvas(this);
        validate(cursor, index);
    }

    /**
     * Remove a given block object from the list.
     * @param b block to remove
     */
    public void removeBlock(Frame b)
    {
        int index = blockContents.indexOf(b);
        if (index < 0)
            throw new IllegalArgumentException("removeBlock: canvas does not contain specified block");
        //Remove the special before, the block, and the cursor after:
        canvas.getChildren().remove(blockContents.get(index).getNode());
        canvas.getChildren().remove(specials.get(index));
        canvas.getChildren().remove(cursors.get(index + 1).getNode());
        blockContents.remove(index);
        cursors.remove(index + 1);
        specials.remove(index);
        b.cleanup();
        b.setParentCanvas(null);
        validate();
    }
    
    /**
     * Replaces the block without altering any cursors
     */
    public void replaceBlock(Frame old, Frame replacement)
    {
        if (replacement == null)
            throw new IllegalArgumentException("Cannot add null block");
        if (!acceptsType(replacement))
            throw new IllegalArgumentException("Block " + getClass() + " does not accept " + replacement.getClass());
        if (replacement.getParentCanvas() != null)
            throw new IllegalArgumentException("Block already has parent");
        
        int index = blockContents.indexOf(old);
        if (index < 0)
            throw new IllegalArgumentException("replaceBlock: canvas does not contain specified block");
        blockContents.set(index, replacement);
        canvas.getChildren().set(childBlockIndex(index), replacement.getNode());
        replacement.setParentCanvas(this);
        old.cleanup();
        old.setParentCanvas(null);
        validate();
        replacement.focusWhenJustAdded();
    }
    
    /**
     * Gets the block before the cursor.  Returns null if it's the first cursor.
     */
    public Frame getFrameBefore(FrameCursor cursor)
    {
        int index = cursors.indexOf(cursor);
        if (index < 0)
            throw new IllegalArgumentException("getBlockBefore: canvas does not contain specified cursor");
        else if (index == 0)
            return null;
        else
            return blockContents.get(index - 1);
    }
    

    public Stream<Frame> getFramesBefore(Frame f)
    {
        if (f == null)
            return blockContents.stream();
        int index = blockContents.indexOf(f);
        if (index == -1)
            throw new IllegalArgumentException("getFramesBefore: canvas does not contain specified frame");
        
        return blockContents.stream().limit(index);
    }
    
    /**
     * Gets the block after the cursor.  Returns null if it's the last cursor.
     */
    public Frame getFrameAfter(FrameCursor cursor)
    {
        int index = cursors.indexOf(cursor);
        if (index < 0)
            throw new IllegalArgumentException("getBlockAfter: canvas does not contain specified cursor");
        else if (index == blockContents.size())
            return null;
        else
            return blockContents.get(index); //Not +1 -- each cursor index is before its corresponding block index
    }
    
    /**
     * Gets the blocks after the cursor.  Null gets all blocks.
     */
    public Stream<Frame> getFramesAfter(Frame frame)
    {
        if (frame == null)
            return blockContents.stream();
        int index = blockContents.indexOf(frame);
        if (index < 0)
            throw new IllegalArgumentException("getFramesAfter: canvas does not contain specified frame");
        else
            return blockContents.stream().skip(index + 1);
    }
    
    /**
     * Gets the cursor before the block.  Never returns null.
     */
    public FrameCursor getCursorBefore(Frame block)
    {
        int index = blockContents.indexOf(block);
        if (index < 0)
            throw new IllegalArgumentException("getCursorBefore: canvas does not contain specified block");
        else
            return cursors.get(index); //Not -1 -- each cursor index is before its corresponding block index
    }

    /**
     * Gets the cursor after the block.  Never returns null.
     */
    public FrameCursor getCursorAfter(Frame block)
    {
        int index = blockContents.indexOf(block);
        if (index < 0)
            throw new IllegalArgumentException("getCursorAfter: canvas does not contain specified block");
        else
            return cursors.get(index + 1);
    }
    
    public boolean acceptsType(Frame blockOfType)
    {
        if (blockOfType != null) {
            return parentBlock.check(this).canPlace(blockOfType.getClass());
        }
        return false;
    }
    
    // To be used for formatting, not querying children, etc:
    public Node getNode()
    {
        return canvas;
    }

    public <T> List<T> getBlocksSubtype(Class<T> clazz)
    {
        List<T> r = new ArrayList<T>();
        for (Frame b : blockContents)
        {
            if (clazz.isAssignableFrom(b.getClass()))
            {
                r.add(clazz.cast(b));
            }
        }
        return r;
    }
    
    public FrameCursor getPrevCursor(FrameCursor orig, boolean canChangeLevel)
    {
        int index = cursors.indexOf(orig);
        if (index < 0)
        {
            throw new IllegalArgumentException("getPrevCursor: cursor not in this canvas");
        }
        else if (index == 0)
        {
            //Already at first cursor in this canvas:
            if (canChangeLevel)
                return parentBlock.getCursorBefore(this);
            else
                return null;
        }
        else
        {
            if (canChangeLevel)
            {
                FrameCursor c = blockContents.get(index - 1).getLastInternalCursor();
                if (c != null)
                {
                    return c;
                }
            }
            // If we can't change level, or there is no internal cursor, go past:
            return cursors.get(index - 1);
        }
    }
    
    public FrameCursor getNextCursor(FrameCursor orig, boolean canChangeLevel)
    {
        int index = cursors.indexOf(orig);
        if (index < 0) {
            throw new IllegalArgumentException("getPrevCursor: cursor not in this canvas");
        }
        else if (index == cursors.size() - 1) {
            //Already at last cursor in this canvas:
            if (canChangeLevel) {
                return parentBlock.getCursorAfter(this);
            }
            return null;
        }
            
        if (canChangeLevel) {
            FrameCursor c = blockContents.get(index).getFirstInternalCursor();
            if (c != null) {
                return c;
            }
        }
        // If we can't change level, or there is no internal cursor, go past:
        return cursors.get(index + 1);
    }
    
    /**
     * Finds closest cursor to given point, even if out of bounds of this block
     */
    public FrameCursor findClosestCursor(double sceneX, double sceneY, List<Frame> exclude, boolean isDrag, boolean canDescend)
    {
        CursorLoop: for (int i = 0; i < cursors.size(); i++)
        {
            //First check cursor:
            {
                FrameCursor c = cursors.get(i);
                Bounds sceneBounds = c.getSceneBounds();
                if (sceneY < sceneBounds.getMaxY())
                {
                    return c;
                }
            }
             
            // Then check block (if not on last cursor):
            if (i < blockContents.size())
            {
                Frame b = blockContents.get(i);
                
                //Debug.message("Looking for " + sceneY + )
                
                if (!canDescend || (exclude != null && exclude.contains(b)))
                {
                    // Find first non-excluded cursors above and below, pick closest:
                    int validCursorAbove = i;
                    while (validCursorAbove >= 1 && (exclude != null && exclude.contains(blockContents.get(validCursorAbove - 1))))
                    {
                        validCursorAbove -= 1;
                    }
                    
                    int validCursorBelow = i + 1;
                    while (validCursorBelow < blockContents.size() && (exclude != null && exclude.contains(blockContents.get(validCursorBelow))))
                    {
                        validCursorBelow += 1;
                    }
                    
                    double distToAbove = sceneY - cursors.get(validCursorAbove).getSceneBounds().getMaxY();
                    double distToBelow = sceneY - cursors.get(validCursorBelow).getSceneBounds().getMinY();
                    
                    if (distToBelow < 0) // If we're above the next valid cursor:
                    {
                        if (distToAbove <= -distToBelow)
                        {
                            // Closest to top:
                            return cursors.get(validCursorAbove);
                        }
                        else
                        {
                            return cursors.get(validCursorBelow);
                        }
                    }
                    else
                    {
                        // Jump to just before the next valid cursor, ready to move on to it:
                        i = validCursorBelow - 1;
                        continue CursorLoop;
                    }
                }
                else
                {
                    FrameCursor c = b.findCursor(sceneX, sceneY, cursors.get(i), cursors.get(i + 1), exclude, isDrag, true);
                    if (sceneY < b.lowestCursorY())
                    {
                        return c;
                    }
                }
            }
        }
        //TODO does this allow you to drag off screen?
        return getLastCursor();
    }
    
    
    public FrameCursor getFirstCursor()
    {
        return cursors.get(0);
    }
    
    public FrameCursor getLastCursor()
    {
        return cursors.get(cursors.size() - 1);
    }
    
    public Bounds getSceneBounds()
    {
        return canvas.localToScene(canvas.getBoundsInLocal());
    }

    public double getHeight()
    {
        return canvas.getHeight();
    }

    public int blockCount()
    {
        return blockContents.size();
    }
    
    /**
     * Do not modify the returned list! 
     * 
     * (I did try wrapping this with FXCollections.unmodifiableObservableList, but it
     * seemed to destroy the observable aspect for any listeners.
     */
    public ObservableList<Frame> getBlockContents()
    {
        return blockContents; // FXCollections.unmodifiableObservableList(blockContents);
    }
    
    public List<? extends RecallableFocus> getFocusableCursors()
    {
        return cursors;
    }
    
    /**
     * Gets an ordered list of frames that lie between the two given cursors.
     * 
     * Both cursors must be non-null and must be in this canvas.  However, they can be passed
     * in either order; calling framesBetween(f, g) will give the same resuly as framesBetween(g, f)
     * and both are valid.  If f == g, you will get an empty list back.
     */
    public List<Frame> framesBetween(FrameCursor a, FrameCursor b)
    {
        int early, late;
        
        int ai = cursors.indexOf(a);
        int bi = cursors.indexOf(b);
        
        if (ai == -1 || bi == -1)
            throw new IllegalArgumentException("framesBetween called for a cursor not present in canvas");
        
        if (ai < bi)
        {
            early = ai;
            late = bi;
        }
        else if (bi < ai)
        {
            early = bi;
            late = ai;
        } 
        else
        {
            // Otherwise they are same cursor
            return Collections.emptyList();
        }
        // If early is 0 and late is 2, there are two frames between those cursors, at positions
        // 0 and 1.  So we just take a sublist from early (incl) to late (excl). 
        return Collections.unmodifiableList(blockContents.subList(early, late));
    }
    
    // Must be later followed by call to growUsing
    public void shrinkUsing(DoubleExpression animate)
    {
        // If too many children, can't sensibly animate nicely, and if we do then we get an exception
        // in JavaFX because of the canvas size needed to animate a VBox that high (e.g. > 16K pixels high)
        // So for large canvases, just jump to the end result:
        if (canvas.getChildren().size() >= 100) {
            Rectangle clipRect = new Rectangle(0.0, 0.0);
            canvas.setClip(clipRect);
            canvas.maxHeightProperty().set(0);
            canvas.prefHeightProperty().bind(canvas.maxHeightProperty());
        }
        else
        {
            // After lots of experimentation, here's how we shrink.
            // - We must set minimum height to 0
            // - We must animate maximum height down to 0
            // - We must set preferred height to maximum height
            // - We must have set a clip rectangle that is bound to maximum size (we do this in constructor)
            // If you do not have all three steps, it will not animate!

            // Force layout:
            canvas.snapshot(null, null);

            // To support shrinking and growing, we need a clip rectangle bound to max height and width
            Rectangle clipRect = new Rectangle();
            clipRect.widthProperty().bind(canvas.widthProperty());
            clipRect.heightProperty().bind(canvas.maxHeightProperty());
            canvas.setClip(clipRect);

            canvas.maxHeightProperty().bind(animate.multiply(getHeight()));
            canvas.prefHeightProperty().bind(canvas.maxHeightProperty());

            // Make the contents look like it is shrinking:
            PerspectiveTransform pt = new PerspectiveTransform();
            pt.setLlx(0.0);
            pt.setUlx(0.0);
            pt.setLrx(canvas.getWidth());
            pt.setUrx(canvas.getWidth());
            pt.setUly(0.0);
            pt.setUry(0.0);
            pt.llyProperty().bind(canvas.maxHeightProperty());
            pt.lryProperty().bind(canvas.maxHeightProperty());
            canvas.setEffect(pt);
        }
    }
    
    // Must have been preceded by call to shrinkUsing
    public void growUsing(DoubleExpression animate)
    {
        // If too many children, can't sensibly animate nicely, and if we do then we get an exception
        // in JavaFX because of the canvas size needed to animate a VBox that high (e.g. > 16K pixels high)
        // So only animate for small enough canvases.  For large, we will just jump to end result:
        if (canvas.getChildren().size() < 100)
        {
            // Reverse changes in shrinkUsing
            double calcHeight = blockContents.stream().mapToDouble(f -> f.getRegion().getHeight()).sum();
            calcHeight += cursors.stream().mapToDouble(f -> f.getNode().getHeight()).sum();
            calcHeight += Math.max(0, blockContents.size() - 1) * canvas.spacingProperty().get();

            canvas.maxHeightProperty().bind(animate.multiply(calcHeight));
        }
        // We keep on the previous effect and clip until we have reached full height
        
        animate.addListener((a, b, newVal) -> {
            if (newVal.doubleValue() >= 0.99)
            {
                canvas.maxHeightProperty().unbind();
                canvas.prefHeightProperty().unbind();
                canvas.setPrefHeight(Region.USE_COMPUTED_SIZE);
                canvas.setMaxHeight(Double.MAX_VALUE);
                canvas.setClip(null);
                canvas.setEffect(null);
            }
        });
    }
    

    public DoubleExpression widthProperty()
    {
        return canvas.widthProperty();
    }
    
//    //Top level canvas
//    private Block embeddedInBlock = null;
//    //Top-level selection scope
//    protected SelectionScope selectionScope;
//    private BlockCursor topCursor;
    
    /**
     * Constructor that specifies that this canvas is part of, for example, an "if" or "for" block's canvas area
     */
    public FrameCanvas(InteractionManager editor, CanvasParent parent, String stylePrefix)
    {
        this.parentBlock = parent;
        //Default setup
        canvas.getStyleClass().addAll("frame-canvas", stylePrefix + "frame-canvas");
        
        JavaFXUtil.setPseudoclass("bj-empty", true, canvas);
        blockContents.addListener((ListChangeListener<Frame>) c -> {
                boolean empty = blockContents.size() == 0;
                JavaFXUtil.setPseudoclass("bj-empty", empty, canvas);
                JavaFXUtil.setPseudoclass("bj-non-empty", !empty, canvas);

                //Notify parent:
                parent.modifiedCanvasContent();
        });
        
      //Drag
        canvas.setOnDragOver(new EventHandler <DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                /* data is dragged over the target */
                //System.out.println("onDragOver");
                
                /* accept it only if it is  not dragged from the same node 
                 * and if it has a string data */
                if (event.getGestureSource() != this &&
                        event.getDragboard().hasString()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                
                event.consume();
            }
        });
        
        // If they click on the blank canvas, focus cursor
        // and move it to bottom of panel (since that's where the blank
        // part will be)
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress())
            {
                editor.clickNearestCursor(e.getSceneX(), e.getSceneY(), e.isShiftDown());
                e.consume();
            }
        });
        
        
        FrameCursor topCursor = editor.createCursor(this);
        //c.setTranslateY(-3);
        //topCursor.setTranslateX(2);
        cursors.add(topCursor);
        canvas.getChildren().add(0, topCursor.getNode());
        
        VBox topSpecial = new VBox();
        specials.add(topSpecial);
        canvas.getChildren().add(1, topSpecial);
    
        editorFrm = editor;
    }
    
    /**
     * Empty the canvas of block contents, and move them all to the target canvas, starting at the specified index (in that canvas's children)
     * @param targetCanvas canvas to export contents to
     * @param index  starting index in other canvas to insert at
     */
    public void emptyTo(FrameCanvas targetCanvas, Frame after)
    {
        List<Frame> allBlocks = getBlocksSubtype(Frame.class);
        //For each block inside, insert at specified index, working backwards so as to maintain order when they reach the target site
        for (int i = allBlocks.size() - 1; i >= 0; i--)
        {
            targetCanvas.insertBlockAfter(allBlocks.get(i), targetCanvas.getCursorAfter(after));            
        }
    }
    
    public void moveContentsTo(FrameCanvas targetCanvas)
    {
        getBlocksSubtype(Frame.class).forEach(b -> {
            removeBlock(b);
            targetCanvas.insertBlockAfter(b, targetCanvas.getLastCursor());
        });
    }
    
    /**
     * Get the code inside this section, as text
     * @return code contained in this container
     */
    /*
    @Override
    public String toCode(String prefix)
    {
        String code = "";
        for (CodeFrame b : getBlocksSubtype(CodeFrame.class))
        {
            //If it's code
            code += b.toCode("    " + prefix) + "\n";
        }
        return code;
    }
    */
    /*
    public void setSelectionScope(SelectionScope scope)
    {
        selectionScope = scope;
        for (Block bl : getBlocksSubtype(Block.class))
        {
            bl.setSelectionScope(scope);
        }
    }
    
    public SelectionScope getSelectionScope()
    {
        return selectionScope;
    }
    */
    
    public void focusTopCursor()
    {
        FrameCursor firstCursor = getFirstCursor();
        if (firstCursor == null) {
            firstCursor = getParent().getCursorAfter(this);
        }
        firstCursor.requestFocus();
    }

    public boolean focusBottomCursor()
    {
        FrameCursor c = getLastCursor();
        if (c != null)
        {
            c.requestFocus();
            return true;
        }
        else
            return false;
    }

    /*
    @Override
    public boolean acceptsType(Class<? extends Block> blockClass) {
        //Don't accept blocks for the parameter-canvas
        if (blockClass.equals(MethodParameter.class))
        {
            return false;
        }
        //Don't put methods inside other things - unless it's a class or a commented-out section (you can't put a method in a loop)
        if (MethodBlock.class.isAssignableFrom(blockClass) && !allowsMethods())
        {
            return false;
        }
        //Only allow certain statements inside a class?
        if (isClassCanvas())
        {
            if (MethodBlock.class.isAssignableFrom(blockClass))
                return true;
            if (blockClass.equals(CommentBlock.class))
                return true;
            if (blockClass.equals(MultiCommentBlock.class))
                return true;
            if (blockClass.equals(VarBlock.class))
                return true;
            if (blockClass.equals(ObjectBlock.class))
                return true;
            return false;
        }
        //Otherwise
        return true;
    }
    */

    public void cleanup()
    {
        getBlockContents().forEach(f -> f.cleanup());        
    }

    public Stream<HeaderItem> getHeaderItems()
    {
        return getBlocksSubtype(Frame.class).stream().flatMap(Frame::getHeaderItems);
    }
    
    // Not static!  One pair per canvas
    private ScalableHeightLabel previewOpeningCurly;
    private ScalableHeightLabel previewClosingCurly;
    
    public void setAnimateLeftMarginScale(boolean animateLeftMarginScale)
    {
        this.animateLeftMarginScale = animateLeftMarginScale;
    }

    public void setTopOutsideBorderBackgroundPadding(Optional<Double> height)
    {
        if (height.isPresent())
            canvas.setStyle("-bj-border-insets: " + (-height.get()) + " 0 0 0;");
        else
            canvas.setStyle("");
    }

    @OnThread(Tag.FXPlatform)
    public void previewCurly(boolean on, boolean affectOpen, boolean affectClose, double sceneX, DoubleExpression openingYAdjust, SharedTransition animate)
    {
        if (on)
        {
            canvas.addSpace(animate);

            ReadOnlyDoubleWrapper xOffset = new ReadOnlyDoubleWrapper(sceneX - canvas.localToScene(canvas.getBoundsInLocal()).getMinX());

            if (affectOpen)
            {
                previewOpeningCurly = new ScalableHeightLabel("{", true);
                JavaFXUtil.addStyleClass(previewOpeningCurly, "preview-curly");
                editorFrm.getCodeOverlayPane().addOverlay(previewOpeningCurly, canvas, xOffset, openingYAdjust);
                previewOpeningCurly.growToFullHeightWith(animate, true);
            }
            if (affectClose)
            {
                previewClosingCurly = new ScalableHeightLabel("}", true);
                JavaFXUtil.addStyleClass(previewClosingCurly, "preview-curly");
                editorFrm.getCodeOverlayPane().addOverlay(previewClosingCurly, canvas, xOffset, canvas.heightProperty().subtract(18.0));
                previewClosingCurly.growToFullHeightWith(animate, true);
            }
        }
        else
        {
            canvas.removeSpace(animate);

            if (affectOpen)
            {
                previewOpeningCurly.shrinkToNothingWith(animate, true);
            }
            if (affectClose)
            {
                previewClosingCurly.shrinkToNothingWith(animate, true);
            }

            animate.addOnStopped(() -> {
                if (affectOpen)
                {
                    editorFrm.getCodeOverlayPane().removeOverlay(previewOpeningCurly);
                    previewOpeningCurly = null;
                }
                if (affectClose)
                {
                    editorFrm.getCodeOverlayPane().removeOverlay(previewClosingCurly);
                    previewClosingCurly = null;
                }
            });
        }
    }

    @OnThread(Tag.FXPlatform)
    public void previewCurly(boolean on, double sceneX, DoubleExpression openingYAdjust, SharedTransition animate)
    {
        previewCurly(on, true, true, sceneX, openingYAdjust, animate);
    }
    
    public List<FrameCursor> getCursors()
    {
        return cursors;
    }

    public void clear()
    {
        while (blockContents.size() > 0)
            removeBlock(blockContents.get(0));
    }

    public void setLastInMulti(boolean last)
    {
        JavaFXUtil.setPseudoclass("bj-last-canvas", last, canvas);
    }

    public SimpleBooleanProperty getShowingProperty()
    {
        return showingProperty;
    }

    public boolean isAlmostBlank()
    {
        // Will also return true if there are no frames:
        return blockContents.stream().allMatch(f -> f instanceof BlankFrame);
    }
    
    public double getBottomMargin()
    {
        return canvas.getBottomMargin();
    }

    public DoubleExpression leftMargin()
    {
        return canvas.leftMarginProperty();
    }

    public DoubleExpression rightMargin()
    {
        return canvas.rightMarginProperty();
    }

    public void setPseudoclass(String name, boolean on)
    {
        JavaFXUtil.setPseudoclass(name, on, canvas);
    }

    /**
     * Gets the bounds, in scene coordinates, of the contents of the canvas.
     *
     * The bounds of getNode() includes the margins of the canvas.  This utility method
     * excludes those margins and just gets the bounds of the actual visible canvas area
     * (which generally has the rounded rectangle around it)
     */
    public Bounds getContentSceneBounds()
    {
        return canvas.getContentSceneBounds();
    }

    // From FrameContentItem:

    @Override
    public Optional<FrameCanvas> getCanvas()
    {
        return Optional.of(this);
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDeep()
    {
        return getHeaderItems();
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDirect()
    {
        return Stream.empty();
    }

    @Override
    public boolean focusBottomEndFromNext()
    {
        return focusBottomCursor();
    }

    @Override
    public boolean focusLeftEndFromPrev()
    {
        focusTopCursor();
        return true;
    }

    @Override
    public boolean focusRightEndFromNext()
    {
        return focusBottomCursor();
    }

    @Override
    public boolean focusTopEndFromPrev()
    {
        focusTopCursor();
        return true;
    }

    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animation)
    {
        canvas.animateColorsToPseudoClass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, animation);
        if (animateLeftMarginScale)
        {
            if (newView == Frame.View.JAVA_PREVIEW)
            {
                // Animate from 1 to 0, removing margin:
                canvas.leftMarginScaleProperty().bind(animation.getOppositeProgress());
                animation.addOnStopped(canvas.leftMarginScaleProperty()::unbind);
            }
            else
            {
                // Animate from 0 to 1, adding margin:
                canvas.leftMarginScaleProperty().bind(animation.getProgress());
                animation.addOnStopped(canvas.leftMarginScaleProperty()::unbind);
            }
        }
    }
    
    public void restore(List<? extends CodeElement> elements, InteractionManager editor)
    {
        // First, make a mapping of all existing elements to their source
        Map<String, List<Frame>> existingLookup = new HashMap<>();
        List<String> existingList = new ArrayList<>();
        for (CodeFrame f : getBlocksSubtype(CodeFrame.class))
        {
            String xml = f.getCode().toXML().toXML();
            existingLookup.merge(xml, new ArrayList<>(Arrays.asList((Frame) f)), (a, b) -> {
                a.addAll(b);
                return a;
            });
            existingList.add(xml);
        }
        List<String> newContentXML = elements.stream().map(el -> el.toXML().toXML()).collect(Collectors.toList());

        // Check how many frames differ between the two, if they're of same length
        if (existingList.size() == newContentXML.size())
        {
            int numDiff = 0;
            int lastDiff = -1;
            for (int i = 0; i < existingList.size(); i++)
            {
                if (existingList.get(i).equals(newContentXML.get(i)) == false)
                {
                    numDiff += 1;
                    lastDiff = i;
                }
            }
            if (numDiff == 0)
            {
                // Perfect match; don't need to do anything:
                return;
            }
            else if (numDiff == 1)
            {
                // Just one frame was changed; can we re-purpose the frame in question?
                if (blockContents.get(lastDiff).tryRestoreTo(elements.get(lastDiff)))
                {
                    // Succesfully restored; we are done.
                    return;
                }
            }
        }

        // If that didn't pan out, make the new list from scratch, but
        // re-use old unchanged frames wherever possible:
        List<Frame> newContents = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++)
        {
            // We must remove, because if we have two identical frames,
            // e.g. two blanks, we don't want to re-use the same one twice for
            // our new list:
            List<Frame> fs = existingLookup.get(newContentXML.get(i));
            if (fs != null && fs.size() > 0)
            {
                newContents.add(fs.remove(0));
            }
            else
            {
                newContents.add(elements.get(i).createFrame(editor));
            }
        }

        // Copy across the old list.  Can't just manipulate blockContents directly
        // because we need to make sure all the cursors and specials are set right:
        while (blockContents.size() > 0)
        {
            removeBlock(blockContents.get(blockContents.size() - 1));
        }
        for (Frame f : newContents)
        {
            insertBlockAfter(f, getLastCursor());
        }
    }

    public double getCurlyBracketHeight()
    {
        return canvas.getCurlyBracketHeight();
    }


    public Parser.JavaContext getContext()
    {
        switch (getParent().getChildKind(this))
        {
            case IMPORTS:
                return Parser.JavaContext.TOP_LEVEL;
            case STATEMENTS:
                return Parser.JavaContext.STATEMENT;
            default:
                return Parser.JavaContext.CLASS_MEMBER;
        }
    }
}
