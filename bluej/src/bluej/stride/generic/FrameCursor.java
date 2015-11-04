/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.NumberExpressionBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import bluej.editor.stride.FrameEditorTab;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.GreenfootFrameCategory;
import bluej.stride.framedjava.frames.GreenfootFrameDictionary;
import bluej.stride.framedjava.frames.NormalMethodFrame;
import bluej.stride.framedjava.frames.VarFrame;
import bluej.stride.generic.FrameDictionary.Entry;
import bluej.stride.operations.PasteFrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * Between-block horizontal cursor placeholder/button
 * @author Fraser McKay
 */
public class FrameCursor implements RecallableFocus
{    
    public static final int FULL_HEIGHT = 5;
    public static final int HIDE_HEIGHT = 0; // To leave a click target there
    final int ERROR_COUNT_TRIGGER = 2;
    int consecutiveErrors = 0;
    private ContextMenu menu;
    private final FrameCanvas parentCanvas;
    
    private final Button node = new Button();

    public boolean keyTyped(final FrameEditorTab editor, final FrameCanvas parentCanvas, char key, boolean viaRedirect)
    {
        if (!editor.isEditable() || !canInsert())
            return false;

        if (Character.isLetter(key) || Arrays.asList('/', '\\', '*', '=', '+', '-', '\n', ' ').contains(key))
        {
            if (parentCanvas.getParent().tryRedirectCursor(parentCanvas, key))
                return true;

            List<Entry<?>> available = editor.getDictionary().getFramesForShortcutKey(key).stream()
                    .filter(t -> parentCanvas.getParent().acceptsType(parentCanvas, t.getBlockClass()))
                    .collect(Collectors.toList());

            final boolean selection = !editor.getSelection().getSelected().isEmpty();
            // Is it a request to expand/collapse?
            if (key == '+' || key == '-') {
                List<Frame> targets = selection ? editor.getSelection().getSelected() : Collections.singletonList(getFrameAfter());
                targets.stream().filter(Frame::isCollapsible)
                    .forEach(t -> t.setCollapsed(key == '-')); // otherwise it's plus
                return true;
            }
            if (selection) {
                List<Frame> targets = editor.getSelection().getSelected();

                // If there is only on selected frame and it accept the key typed as an extension
                if (targets.size() == 1) {
                    for (ExtensionDescription extension : targets.get(0).getAvailableExtensions()) {
                        if (extension.getShortcutKey() == key) {
                            extension.activate();
                            return true;
                        }
                    }
                }

                // To disable
                if (key == '\\') {
                    // If all disabled, enabled all. Otherwise, disable all.
                    boolean allDisabled = targets.stream().filter(f -> f.canHaveEnabledState(false)).allMatch(f -> !f.isFrameEnabled());

                    // TODO Refactor the Enable/Disable FrameOperations to make them more consistent and use them here instead of next lines
                    editor.beginRecordingState(this);
                    targets.stream().filter(f -> f.canHaveEnabledState(allDisabled ? true : false))
                            .forEach(t -> t.setFrameEnabled(allDisabled ? true : false));
                    editor.endRecordingState(this);

                    return true;
                }
                // Toggle variable final
                if (key == 'n') {
                    // If all final, remove final. Otherwise, make all final.
                    List<Frame> nonIgnoredFrames = targets.stream().filter(f -> f.isEffectiveFrame()).collect(Collectors.toList());
                    if (nonIgnoredFrames.stream().allMatch(f -> f instanceof VarFrame)) {
                        new VarFrame.ToggleFinalVar(editor).activate(nonIgnoredFrames);
                        return true;
                    }
                    if (nonIgnoredFrames.stream().allMatch(f -> f instanceof NormalMethodFrame)) {
                        new NormalMethodFrame.ToggleFinalMethod(editor).activate(nonIgnoredFrames);
                        return true;
                    }
                }
                // Toggle variable static
                if (key == 's') {
                    // If all static, remove static. Otherwise, make all static.
                    List<Frame> nonIgnoredFrames = targets.stream().filter(f -> f.isEffectiveFrame()).collect(Collectors.toList());
                    if (nonIgnoredFrames.stream().allMatch(f -> f instanceof VarFrame && ((VarFrame)f).isField(getParentCanvas()))) {
                        new VarFrame.ToggleStaticVar(editor).activate(nonIgnoredFrames);
                        return true;
                    }
                    if (nonIgnoredFrames.stream().allMatch(f -> f instanceof NormalMethodFrame)) {
                        new NormalMethodFrame.ToggleStaticMethod(editor).activate(nonIgnoredFrames);
                        return true;
                    }
                }
            }

            if (!selection) {
                Frame before = getFrameBefore();
                // First, check if the block we are in supports this key:
                if (CanvasParent.processInnerExtensionKey(getParentCanvas().getParent(), getParentCanvas(), this, key, FrameCursor.this, before == null))
                {
                    // Done
                    return true;
                }
                // Otherwise check the frame before us:
                else if ( before != null && before.notifyExtensionKey(key, FrameCursor.this) ) {
                    // Done
                    return true;
                }
                else if ( getFrameAfter() != null && getFrameAfter().notifyPrefixKey(key, FrameCursor.this) ) {
                    // Done
                    return true;
                }

            }

            // Third, check if the canvas we are in supports this block generally:
            if (available.size() > 1) {
                throw new IllegalStateException("Ambigious keypress: " + key + " in frame: " + parentCanvas.getParent());
            }
            else if (available.size() == 1) {
                Entry<?> frameType = available.get(0);

                if (!selection || frameType.validOnSelection()) {
                    editor.beginRecordingState(FrameCursor.this);

                    //Don't animate our removal when adding blocks,
                    //just disappear:
                    disappear();

                    Frame newFrame;
                    if (selection) {
                        List<Frame> selected = editor.getSelection().getSelected();
                        // We must add the new frame before removing the old ones because removing the old
                        // ones may remove us as a cursor!
                        List<Frame> selectedCopy = Utility.mapList(selected, f -> Loader.loadElement(((CodeFrame<CodeElement>) f).getCode().toXML()).createFrame(editor));
                        newFrame = frameType.getFactory().createBlock(editor, selectedCopy);
                        insertBlockBefore(newFrame);
                        selected.forEach(f -> f.getParentCanvas().removeBlock(f));
                        editor.getSelection().clear();
                    }
                    else {
                        newFrame = frameType.getFactory().createBlock(editor);
                        insertBlockBefore(newFrame);
                    }
                    editor.modifiedFrame(newFrame);
                    newFrame.markFresh();

                    // Must ask for scroll before focusing (which will also attempt scroll):
                    if (viaRedirect) {
                        editor.scrollTo(getNode(), -20, Duration.millis(2000));
                    }

                    if (!newFrame.focusWhenJustAdded()) {
                        appear(); // Reverse the disappear above; we are still focused, since the frame had nothing to focus on
                        editor.updateCatalog(FrameCursor.this);
                    }

                    editor.endRecordingState(null);

                    //Not an error
                    consecutiveErrors = 0;

                    return true;
                }
            }
            // Otherwise (fourth), no match!
            else {
                //There's no block matching this
                consecutiveErrors++;
                if (consecutiveErrors >= ERROR_COUNT_TRIGGER) {
                    editor.showCatalogue();
                }
                //Ignore one-off mis-typing, just to stop every slip-up triggering a dialog
                return true;
            }
        }
        editor.getSelection().clear();
        return false;
    }

    /**
     * As the cursor moves up/down, we want to make it look like the blocks are sliding out of the
     * way for the cursor.  To achieve this, we actually shrink old cursor positions while
     * growing new position.
     * 
     * To link the size of the growing new position to the old shrinking positions we maintain
     * a list of current shrinkers in this class, and bind our property to their size.
     * Then we bind the height of the growing portion to be the target, minus this total height
     * of remaining shrinkers.
     */
    private static class TotalHeightBinding extends IntegerBinding
    {
        private static final Duration DEFAULT_DURATION = Duration.millis(100);
        private Set<NumberExpressionBase> shrinkingSpace = new HashSet<>();
        private FrameCursor growing;
        
        public synchronized void shrink(FrameCursor c, double target, boolean animate)
        {
            // If we were previously growing, untag ourselves:
            if (growing == c) {
                growing = null;
            }
            
            // Animate using integer property to avoid floating point half-height problems:
            // We definitely want this -- otherwise you get odd effects
            SimpleIntegerProperty heightProp = new SimpleIntegerProperty((int)c.node.getMaxHeight());
            c.node.maxHeightProperty().bind(heightProp);
            if (animate)
            {
                c.animation = new Timeline(new KeyFrame(DEFAULT_DURATION, new KeyValue(heightProp, target)));
                c.animation.play();
            }
            else
            {
                heightProp.set((int)target);
            }
            
            shrinkingSpace.add(c.node.maxHeightProperty()); //Must match remove() in remove()
            bind(c.node.maxHeightProperty()); // Must match unbind() in remove()
        }
        
        private synchronized void remove(FrameCursor c)
        {
            shrinkingSpace.remove(c.node.maxHeightProperty()); //Must match add in shrink()
            unbind(c.node.maxHeightProperty()); // Must match bind() in shrink()
            if (shrinkingSpace.isEmpty() && growing != null)
            {
                // If no one is shrinking, all animations are done:
                growing.node.maxHeightProperty().unbind();
                growing = null;
            }
        }
        
        
        @Override
        protected int computeValue()
        {
            int height = 0;
            for (Iterator<NumberExpressionBase> it = shrinkingSpace.iterator(); it.hasNext();)
            {
                NumberExpressionBase val = it.next();
                int h = (int)Math.ceil(val.doubleValue());
                height += h - HIDE_HEIGHT;
                // If any have zero height, we can remove them to avoid clogging up our set:
                if (h == 0)
                {
                    it.remove();
                    unbind(val);
                }
            }
            return height;
        }

        public synchronized void grow(FrameCursor c, int target, boolean animate)
        {
            remove(c);
            growing = c;
            
            //Add dummy shrinking cursor, in case no-one is shrinking, e.g. if we came from a slot:
            if (shrinkingSpace.isEmpty())
            {
                IntegerProperty dummy = new SimpleIntegerProperty(target - (int)c.node.getMaxHeight() + HIDE_HEIGHT);
                bind(dummy);
                shrinkingSpace.add(dummy);
                if (animate)
                {
                    Animation anim = new Timeline(new KeyFrame(DEFAULT_DURATION, new KeyValue(dummy, HIDE_HEIGHT)));
                    anim.play();
                }
                else
                {
                    dummy.set(HIDE_HEIGHT);
                }
            }
            
            growing.node.maxHeightProperty().bind(Bindings.max(HIDE_HEIGHT, new ReadOnlyIntegerWrapper(target).subtract(this)));
        }
    }
    
    private static final Map<InteractionManager, TotalHeightBinding> shrinkingHeightBindings = new IdentityHashMap<>();
    private final InteractionManager editor;
    private Timeline animation;
    private Canvas redCross;
    private Canvas copyingPlus;
    private ImageView dragTargetOverlayFake;
    
    /**
     * Constructor
     */
    public FrameCursor(final FrameEditorTab editor, final FrameCanvas parentCanvas)
    {
        node.getStyleClass().add("cursor-frame");
        node.setMaxWidth(100);
        node.setMaxHeight(HIDE_HEIGHT);
        node.setOpacity(0);
        this.parentCanvas = parentCanvas;
        if (parentCanvas == null) {
            throw new IllegalArgumentException("BlockCursor: parentCanvas cannot be null");
        }
        
        this.editor = editor;
        if (editor != null)
        {
            shrinkingHeightBindings.putIfAbsent(editor, new TotalHeightBinding()); 
        }
        
        // Bind min and pref size to max size:
        node.minWidthProperty().bind(node.maxWidthProperty());
        node.prefWidthProperty().bind(node.maxWidthProperty());
        node.minHeightProperty().bind(node.maxHeightProperty());
        node.prefHeightProperty().bind(node.maxHeightProperty());

        // We must start by insta-shrinking, so that the height bindings get set-up correctly
        // for the cursor:
        shrinkingHeightBindings.get(editor).shrink(this,HIDE_HEIGHT,false);
                                
        JavaFXUtil.addChangeListener(node.focusedProperty(), nowFocused -> animateShowHide(nowFocused, true));
        JavaFXUtil.addChangeListener(node.localToSceneTransformProperty(), t -> adjustDragTargetPosition());
        
        if (editor != null) {
            editor.setupFrameCursor(this);
        }
        
        // We must use a filter on press because potentially this event might set
        // a selection rather than focusing on us.  If we don't use a filter to consume
        // the press, the event will be handled by focusing on us by default
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            // Don't intercept the press if we are focused; might be start of drag event
            if (e.getButton() == MouseButton.PRIMARY && editor != null && !isFocused())
            {
                // We must go via the editor, because it might have extra work to do if
                // shift was pressed (i.e. the selection may need to be extended).
                // Otherwise it may just focus us anyway.
                editor.clickNearestCursor(e.getSceneX(), e.getSceneY(), e.isShiftDown());
                e.consume();
            }
        });
        
        JavaFXUtil.listenForContextMenu(node, this::showContextMenu);
        getNode().focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, final Boolean nowFocused) {
            {
                if (!nowFocused.booleanValue())
                {
                    //Resets error count for this cursor point, so it doesn't remember errors from previous edits
                    consecutiveErrors = 0;
                }
            }
        }});



        /**
         * To insert other blocks when text input is made from this cursor
         */
        getNode().setOnKeyTyped(event -> {
            try{
                String character = event.getCharacter();
                if (!character.isEmpty()) {
                    char key = character.toCharArray()[0];
                    //Insert a new block, depending on key-press
                    // TODO remove isShortcutDown when the JDK bug is solved (Maybe in JDK 9)
                    if (!event.isShortcutDown() && keyTyped(editor, parentCanvas, key, false)) {
                        event.consume();
                    }
                }
            }
            catch(Exception ex){
                Debug.reportError("CURSOR KEY PRESS: ", ex);
            }
        });

        getNode().setOnKeyPressed(event -> {

            if (!editor.isEditable())
                return;

            if ((event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE)
                    && !editor.getSelection().getSelected().isEmpty())
            {
                List<Frame> toDelete = editor.getSelection().getSelected();
                FrameCursor focusAfter = toDelete.get(0).getCursorBefore();
                // Check they are from our canvas:
                if (toDelete.stream().allMatch(f -> f.getParentCanvas() == getParentCanvas()))
                {
                    // We might get deleted during this code, so cache value of getParentCanvas:
                    FrameCanvas c = getParentCanvas();
                    toDelete.forEach(f -> c.removeBlock(f));
                }
                else
                {
                    Debug.message("Warning: trying to delete selection from remote cursor");
                }
                editor.getSelection().clear();
                focusAfter.requestFocus();
                event.consume();
                return;
            }

            //If 'delete'/backspace
            if (event.getCode() == KeyCode.BACK_SPACE)
            {
                Frame target = parentCanvas.getFrameBefore(FrameCursor.this);
                if (target != null)
                {
                    editor.beginRecordingState(FrameCursor.this);
                    FrameCursor cursorBeforeTarget = parentCanvas.getCursorBefore(target);
                    parentCanvas.removeBlock(target);
                    editor.modifiedFrame(target);
                    cursorBeforeTarget.requestFocus();
                    editor.endRecordingState(cursorBeforeTarget);
                }
                else
                {
                    // At top; inner extension may implement backspace:
                    editor.beginRecordingState(FrameCursor.this);
                    FrameCursor cursorBeforeTarget = parentCanvas.getParent().getCursorBefore(parentCanvas);
                    CanvasParent.processInnerExtensionKey(parentCanvas.getParent(), parentCanvas, this, '\b', FrameCursor.this, true);
                    editor.endRecordingState(cursorBeforeTarget);
                }
            }
            else if (event.getCode() == KeyCode.ESCAPE)
            {
                Frame target = parentCanvas.getFrameBefore(FrameCursor.this);
                if (target != null){
                    target.escape(null, null);
                }
                else
                {
                    //At the top. need to go to adjust to the target properly.
                    Frame BeforeTarget = parentCanvas.getParent().getCursorBefore(parentCanvas).getFrameAfter();
                    if (BeforeTarget != null)
                    {
                        BeforeTarget.escape(null, null);
                    }
                }
            }
            else if (event.getCode() == KeyCode.DELETE)
            {
                // Find next and do backspace:
                Frame target = parentCanvas.getFrameAfter(FrameCursor.this);
                if (target != null)
                {
                    editor.beginRecordingState(FrameCursor.this);
                    parentCanvas.removeBlock(target);
                    editor.modifiedFrame(target);
                    editor.endRecordingState(FrameCursor.this);
                }
            }
            else if (event.getCode() == KeyCode.ENTER)
            {
                keyTyped(editor, parentCanvas, '\n', false);
            }
        });
    }

    private void animateShowHide(boolean show, boolean animate)
    {
        // Stop any previous resizing animation:
        if (animation != null) {
            animation.stop();
        }
        
        node.setOpacity(show ? 1.0 : 0.0);
        
        //If we're the only item in a canvas, don't animate:
        if (getParentCanvas().blockCount() == 0) {
            node.maxHeightProperty().unbind();
            node.setMaxHeight(show ? FULL_HEIGHT : HIDE_HEIGHT);
        }
        else {
            if (show) {
                shrinkingHeightBindings.get(editor).grow(FrameCursor.this, FULL_HEIGHT, animate);
            }
            else {
                shrinkingHeightBindings.get(editor).shrink(FrameCursor.this, HIDE_HEIGHT, animate);
            }
        }
    }

    
    public void showAsDropTarget(boolean showAsSource, boolean dragPossible, boolean copying)
    {
        String chosen;
        
        if (dragPossible) {
            chosen = showAsSource ? "frame-cursor-drag-source" : "frame-cursor-drag";
        }
        else {
            chosen = "frame-cursor-drag-impossible";
        }
        
        // Must resize before setting drag class:
        animateShowHide(true, false);
        setDragClass(chosen);
        updateDragCopyState(copying);
    }

    private void setDragClass(String chosen)
    {
        JavaFXUtil.selectStyleClass(chosen, node,
                "frame-cursor-drag-impossible", "frame-cursor-drag-source", "frame-cursor-drag");
        setDragTargetOverlayVisible(chosen != null, "frame-cursor-drag-impossible".equals(chosen));
    }
    
    private void adjustDragTargetPosition()
    {
        if (dragTargetOverlayFake != null)
        {
            Pane dragTargetCursorPane = editor.getDragTargetCursorPane();
            Bounds scenePos = getNode().localToScene(getNode().getBoundsInLocal());
            Bounds panePos = dragTargetCursorPane.sceneToLocal(scenePos);
            dragTargetOverlayFake.setLayoutX(panePos.getMinX());
            dragTargetOverlayFake.setLayoutY(panePos.getMinY());
            if (redCross != null)
            {
                redCross.setLayoutX(dragTargetOverlayFake.getLayoutX() + node.widthProperty().divide(2.0).subtract(redCross.getWidth()/2.0).get());
                redCross.setLayoutY(dragTargetOverlayFake.getLayoutY() + node.heightProperty().divide(2.0).subtract(redCross.getHeight()/2.0).get());
            }
            if (copyingPlus != null)
            {
                copyingPlus.setLayoutX(dragTargetOverlayFake.getLayoutX() + node.getWidth());
                copyingPlus.setLayoutY(dragTargetOverlayFake.getLayoutY() - copyingPlus.getWidth());
            }
        }
    }
    
    private void setDragTargetOverlayVisible(boolean visible, boolean showCross)
    {
        Pane dragTargetCursorPane = editor.getDragTargetCursorPane();
        if (visible)
        {
            Image snapshot = getNode().snapshot(null, null);
            dragTargetOverlayFake = new ImageView(snapshot);
            
            dragTargetCursorPane.getChildren().add(dragTargetOverlayFake);
            
            if (showCross && redCross == null)
            {
                redCross = new Canvas(FULL_HEIGHT * 3, FULL_HEIGHT * 3);
                GraphicsContext gc = redCross.getGraphicsContext2D();
                gc.setStroke(Color.RED);
                gc.setLineWidth(2.0);
                gc.strokeLine(1.0, 1.0, redCross.getWidth() - 2.0, redCross.getHeight() - 2.0);
                gc.strokeLine(redCross.getWidth() - 2.0, 1.0, 1.0, redCross.getHeight() - 2.0);
                dragTargetCursorPane.getChildren().add(redCross);
            }
            
            adjustDragTargetPosition();
        }
        else 
        {
            if (dragTargetOverlayFake != null)
            {
                dragTargetCursorPane.getChildren().remove(dragTargetOverlayFake);
                dragTargetOverlayFake = null;
            }
            if (redCross != null)
            {
                dragTargetCursorPane.getChildren().remove(redCross);
                redCross = null;
            }
            if (copyingPlus != null)
            {
                dragTargetCursorPane.getChildren().remove(copyingPlus);
                copyingPlus = null;
            }
        }
    }

    public void stopShowAsDropTarget()
    {
        animateShowHide(false, false);
        setDragClass(null);
    }
    
    public void insertBlockAfter(Frame b)
    {
        getParentCanvas().insertBlockAfter(b, this);
    }
    
    public void insertBlockBefore(Frame b)
    {
        getParentCanvas().insertBlockBefore(b, this);
    }
    
    public boolean acceptsFrame(Class<? extends Frame> frameClass)
    {
        return getParentCanvas().getParent().acceptsType(getParentCanvas(), frameClass);
    }
    
    public void insertFramesAfter(List<Frame> frames)
    {
        List<Frame> rev = new ArrayList<>(frames);
        Collections.reverse(rev);
        rev.forEach(f -> getParentCanvas().insertBlockAfter(f, this));
    }
    
    /**
     * Gets the next block at the same level (in the same canvas) if possible,
     * or the next valid block cursor after that (by going up a level).
     */
    public FrameCursor getNextSkip()
    {
        return parentCanvas.getNextCursor(this, false);
    }
    
    /**
     * Gets the previous block at the same level (in the same canvas) if possible,
     * or the previous valid block cursor after that (by going up a level).
     */
    public FrameCursor getPrevSkip()
    {
        return parentCanvas.getPrevCursor(this, false);
    }
    
    /**
     * Gets the cursor just before the enclosing block for this cursor
     */
    public FrameCursor getUp()
    {
        Frame frameBefore = getFrameBefore();
        if (frameBefore != null) {
            return frameBefore.getCursorBefore();
        }
        return parentCanvas.getParent().getCursorBefore(parentCanvas);
    }
    
    /**
     * Gets the cursor just after the enclosing block for this cursor
     */
    public FrameCursor getDown()
    {
        Frame frameAfter = getFrameAfter();
        if (frameAfter != null) {
            return frameAfter.getCursorAfter();
        }
        return parentCanvas.getParent().getCursorAfter(parentCanvas);
    }

    public Frame getFrameBefore()
    {
        return parentCanvas.getFrameBefore(this);
    }
    
    public Frame getFrameAfter()
    {
        return parentCanvas.getFrameAfter(this);
    }

    public FrameCanvas getParentCanvas()
    {
        return parentCanvas;
    }
    
    public Frame getEnclosingFrame()
    {
        return getParentCanvas().getParent().getFrame();
    }
    
    public Region getNode()
    {
        return node;
    }
    
    public void requestFocus()
    {
        node.requestFocus();
    }

    public Bounds getSceneBounds()
    {
        return node.localToScene(node.getBoundsInLocal());
    }
    
    /** Vanish without animating */
    protected void disappear()
    {
        animateShowHide(false, false);
    }

    /** Appear without animating */
    protected void appear()
    {
        animateShowHide(true, false);
    }
    
    public InteractionManager getEditor()
    {
        return this.editor;
    }

    private boolean showContextMenu(double screenX, double screenY)
    {
        if (JavaFXUtil.hasPseudoclass(node, "bj-java-preview"))
            return false;

        if (menu != null) {
            menu.hide();
        }
        menu = EditableSlot.MenuItems.makeContextMenu(Collections.singletonMap(EditableSlot.TopLevelMenu.EDIT, getMenuItems(true)));
        if (menu.getItems().size() > 0) {
            menu.show(node, screenX, screenY);
            return true;
        }
        return false;
    }
    
    public EditableSlot.MenuItems getMenuItems(boolean contextMenu)
    {
        EditableSlot.MenuItems menuItems = new EditableSlot.MenuItems(FXCollections.observableArrayList(new PasteFrameOperation(editor).getMenuItem(contextMenu)));
        if (!editor.getSelection().isEmpty())
        {
            menuItems = EditableSlot.MenuItems.concat( editor.getSelection().getMenuItems(contextMenu), menuItems);
        }

        if (canInsert() && contextMenu)
        {
            Menu insertMenu = new Menu("Insert");
            insertMenu.getItems().addAll(getAcceptedFramesMenuItems());
            menuItems = EditableSlot.MenuItems.concat( new EditableSlot.MenuItems(FXCollections.observableArrayList(MenuItemOrder.INSERT_FRAME.item(insertMenu))), menuItems);
        }

        return menuItems;
    }

    private List<MenuItem> getAcceptedFramesMenuItems()
    {
        List<MenuItem> items = new ArrayList<MenuItem>();
        List<Entry<GreenfootFrameCategory>> entries = GreenfootFrameDictionary.getDictionary().getAllBlocks();
        for (Entry<GreenfootFrameCategory> entry : entries) {
            if ( acceptsFrame(entry.getBlockClass()) ) {
                 items.add(createMenuItem(entry, this));
            }
        }
        return items;
    }

    private MenuItem createMenuItem(Entry<GreenfootFrameCategory> entry, FrameCursor cursor)
    {
        Label d = new Label();
        d.textProperty().bind(new SimpleStringProperty(entry.getShortcuts() + "\t  " + entry.getName()));
        d.setPrefWidth(250);
        MenuItem item = new MenuItem(entry.getName());
        
        // Delete (with hover preview)
        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e)
            {
                editor.beginRecordingState(FrameCursor.this);
                Frame newFrame = entry.getFactory().createBlock(editor);
                cursor.insertBlockAfter(newFrame);
                newFrame.focusWhenJustAdded();
                editor.endRecordingState(null);
                editor.getSelection().clear();
                e.consume();
            }
        });
        return item;
    }

    
    public boolean isFocused()
    {
        return node.isFocused();
    }

    @Override
    public int getFocusInfo()
    {
        return -1; // No info of interest
    }

    @Override
    public Node recallFocus(int info)
    {
        requestFocus();
        return node;
    }

    public void updateDragCopyState(boolean copying)
    {
        if (dragTargetOverlayFake == null)
            return;
        
        Pane dragTargetCursorPane = editor.getDragTargetCursorPane();
        if (copying && copyingPlus == null)
        {
            copyingPlus = new Canvas(FULL_HEIGHT * 2 + 2, FULL_HEIGHT * 2 + 2);
            GraphicsContext gc = copyingPlus.getGraphicsContext2D();
            double middle = copyingPlus.getHeight() / 2.0;
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(4.0);
            gc.strokeLine(middle, 0, middle, copyingPlus.getHeight());
            gc.strokeLine(0, middle, copyingPlus.getWidth(), middle);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2.0);
            gc.strokeLine(middle, 2, middle, copyingPlus.getHeight() - 2);
            gc.strokeLine(2, middle, copyingPlus.getWidth() - 2, middle);
            dragTargetCursorPane.getChildren().add(copyingPlus);
        }
        else if (!copying && copyingPlus != null)
        {
            dragTargetCursorPane.getChildren().remove(copyingPlus);
            copyingPlus = null;
        }
        
        adjustDragTargetPosition();
    }


    public void setView(Frame.View view, SharedTransition animateProgress)
    {
        JavaFXUtil.setPseudoclass("bj-java-preview", view == Frame.View.JAVA_PREVIEW, node);
    }

    public boolean canInsert()
    {
        CanvasParent cp = getParentCanvas().getParent();
        if (cp != null)
        {
            Frame f = cp.getFrame();
            if (f != null)
                return f.isFrameEnabled();

        }
        // I don't think it matters what we return here, but as a default we'll go for true,
        // as that was effectively the default before adding this method:
        return true;
    }
}
