/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2020,2021 Michael Kölling and John Rosenberg
 
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
import java.util.Collections;;
import java.util.List;
import java.util.stream.Collectors;

import bluej.Config;
import bluej.collect.StrideEditReason;
import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.frames.StrideCategory;
import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.utility.javafx.AbstractOperation;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
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

import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.FrameDictionary.Entry;
import bluej.stride.operations.PasteFrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    
    private final Button node = new Button(){
        // Babis
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public Object queryAccessibleAttribute(AccessibleAttribute accessibleAttribute, Object... objects) {
            switch (accessibleAttribute) {
                case TEXT:
                    return normalText;
                case HELP:
                    return helpText;
                case ROLE:
                    return AccessibleRole.BUTTON;
                case ROLE_DESCRIPTION:
                    return ""; // If we leave the default it always says it is a "Button"
                default:
                    return super.queryAccessibleAttribute(accessibleAttribute, objects);
            }
        }
    };

    private String normalText;
    private String helpText;

    @OnThread(Tag.FXPlatform)
    public boolean keyTyped(final InteractionManager editor, final FrameCanvas parentCanvas, char key, boolean ctrlDown)
    {
        if (!editor.isEditable() || !canInsert())
            return false;

        // Ignore accidental caps lock:
        key = Character.toLowerCase(key);

        if (key == '?')
        {
            boolean show = !editor.cheatSheetShowingProperty().get();
            editor.cheatSheetShowingProperty().set(show);
            editor.recordShowHideFrameCatalogue(show, FrameCatalogue.ShowReason.MENU_OR_SHORTCUT);
            return true;
        }

        if (Character.isLetter(key) || Arrays.asList('/', '\\', '*', '=', '+', '-', '\n', ' ').contains(key))
        {
            List<Entry<?>> available = editor.getDictionary().getFramesForShortcutKey(key).stream()
                    .filter(t -> parentCanvas.getParent().check(parentCanvas).canInsert(t.getCategory()))
                    .collect(Collectors.toList());

            final boolean selection = !editor.getSelection().getSelected().isEmpty();
            // Is it a request to expand/collapse?
            if (false && (key == '+' || key == '-')) {
                //List<Frame> targets = selection ? editor.getSelection().getSelected() : Collections.singletonList(getFrameAfter());
                //targets.stream().filter(Frame::isCollapsible)
                //    .forEach(t -> t.setCollapsed(key == '-')); // otherwise it's plus
                return true;
            }

            if (selection && editor.getSelection().executeKey(this, key))
                return true;

            if (!selection) {
                Frame before = getFrameBefore();
                // First, check if the block we are in supports this key:
                if (CanvasParent.processInnerExtensionKey(getParentCanvas().getParent(), getParentCanvas(), this, key, FrameCursor.this, before == null))
                {
                    // Done
                    return true;
                }
                // Otherwise check the frame after us:
                else if ( getFrameAfter() != null && getFrameAfter().notifyKeyBefore(key, FrameCursor.this) ) {
                    // Done
                    return true;
                }
                // Otherwise check the frame before us:
                else if ( before != null && before.notifyKeyAfter(key, FrameCursor.this) ) {
                    // Done
                    return true;
                }
            }

            // Third, check if the canvas we are in supports this block generally:
            if (available.size() > 1) {
                throw new IllegalStateException("Ambigious keypress: " + key + " in frame: " + parentCanvas.getParent() + " [" + available.stream().map(e -> e.getName()).collect(Collectors.joining(", ")) + "]");
            }
            else if (available.size() == 1) {
                Entry<?> frameType = available.get(0);

                if (!selection || frameType.isValidOnSelection()) {
                    editor.beginRecordingState(FrameCursor.this);
                    editor.recordEdits(StrideEditReason.FLUSH);

                    // Hide the cursor:
                    showHide(false);

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
                    editor.recordEdits(selection ? StrideEditReason.SELECTION_WRAP_KEY : StrideEditReason.SINGLE_FRAME_INSERTION_KEY);
                    editor.modifiedFrame(newFrame, false);
                    newFrame.markFresh();


                    if (!newFrame.focusWhenJustAdded()) {
                        showHide(true); // Reverse the hiding above; we are still focused, since the frame had nothing to focus on
                        editor.updateCatalog(FrameCursor.this);
                    }
                    if (ctrlDown)
                        newFrame.insertedWithCtrl();

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
                    BooleanProperty cheatSheetShowingProperty = editor.cheatSheetShowingProperty();
                    if ( ! cheatSheetShowingProperty.get() ) {
                        cheatSheetShowingProperty.set(true);
                        editor.recordShowHideFrameCatalogue(true, FrameCatalogue.ShowReason.UNKNOWN_FRAME_COMMAND);
                    }
                }
                editor.recordUnknownCommandKey(getEnclosingFrame(), getCursorIndex(), key);
                //Ignore one-off mis-typing, just to stop every slip-up triggering a dialog
                return true;
            }
        }
        else
        {
            editor.recordUnknownCommandKey(getEnclosingFrame(), getCursorIndex(), key);
        }
        editor.getSelection().clear();
        return false;
    }

    /**
     * Gets the index of this cursor object, relative to the start of cursors' positions within the inclusive frame.
     *
     * @return Our relative position in the enclosing frame.
     */
    public int getCursorIndex()
    {
        return parentCanvas.getCursors().indexOf(this);
    }

    private final InteractionManager editor;
    private Canvas redCross;
    private Canvas copyingPlus;
    private ImageView dragTargetOverlayFake;
    
    /**
     * Constructor
     */
    public FrameCursor(final InteractionManager editor, final FrameCanvas parentCanvas)
    {
        node.getStyleClass().add("frame-cursor");
        node.setMaxWidth(100);
        node.setMaxHeight(HIDE_HEIGHT);
        node.setOpacity(0);
        this.parentCanvas = parentCanvas;
        if (parentCanvas == null) {
            throw new IllegalArgumentException("BlockCursor: parentCanvas cannot be null");
        }
        
        this.editor = editor;
        
        // Bind min and pref size to max size:
        node.minWidthProperty().bind(node.maxWidthProperty());
        node.prefWidthProperty().bind(node.maxWidthProperty());
        node.minHeightProperty().bind(node.maxHeightProperty());
        node.prefHeightProperty().bind(node.maxHeightProperty());

        JavaFXUtil.addChangeListener(node.focusedProperty(), nowFocused -> {
            // Oddly, we can get told we are focused even after we have left the Scene,
            // so we add a check here to guard against that:
            if (node.getScene() != null)
            {
                showHide(nowFocused);
            }

            //cherry
            if (nowFocused)
            {
//                node.setAccessibleRole(AccessibleRole.NODE); // This causes a weird bug where the screenreader reads out the same help text for all cursors, rather than the different help text of each cursor
                this.normalText = "frame cursor normal text";
                this.helpText = "frame cursor help text";
                if (getFrameAfter() != null)
                {
                    this.normalText = getFrameAfter().getScreenReaderText();
                    this.helpText = "you are before a" + getFrameAfter().getFrameName() + "," + parentCanvas.getParentLocationDescription();
                }
                else
                {
                    this.normalText = "no frame selected";
                    CanvasParent.CanvasKind area = parentCanvas.getParent().getChildKind(parentCanvas);
                    switch(area)
                    {
                        case STATEMENTS:
                            this.helpText = "you are " + parentCanvas.getParentLocationDescription();
                            break;
                        default:
                            this.helpText = "you are in the " + area + " area, " + parentCanvas.getParentLocationDescription();
                            break;
                    }
                }
            }
            //end of cherry

        });
        JavaFXUtil.addChangeListener(node.localToSceneTransformProperty(), t -> JavaFXUtil.runNowOrLater(() -> adjustDragTargetPosition()));
        
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
        getNode().focusedProperty().addListener((observable, oldValue, nowFocused) -> {
        {
            if (!nowFocused)
            {
                //Resets error count for this cursor point, so it doesn't remember errors from previous edits
                consecutiveErrors = 0;
            }
        }
    });



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
                    boolean ignore = Config.isMacOS() && event.isShortcutDown();
                    if (!ignore && keyTyped(editor, parentCanvas, key, event.isControlDown())) {
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
                editor.beginRecordingState(FrameCursor.this);
                // Check they are from our canvas:
                if (toDelete.stream().allMatch(f -> f.getParentCanvas() == getParentCanvas()))
                {
                    editor.recordEdits(StrideEditReason.FLUSH);
                    int effort = toDelete.stream().mapToInt(Frame::calculateEffort).sum();
                    editor.showUndoDeleteBanner(effort);
                    // We might get deleted during this code, so cache value of getParentCanvas:
                    FrameCanvas c = getParentCanvas();
                    toDelete.forEach(f -> c.removeBlock(f));
                    editor.recordEdits(event.getCode() == KeyCode.BACK_SPACE ? StrideEditReason.DELETE_FRAMES_KEY_BKSP : StrideEditReason.DELETE_FRAMES_KEY_DELETE);
                }
                else
                {
                    Debug.message("Warning: trying to delete selection from remote cursor");
                }
                editor.getSelection().clear();
                focusAfter.requestFocus();
                editor.endRecordingState(focusAfter);
                editor.updateCatalog(focusAfter);
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
                    editor.recordEdits(StrideEditReason.FLUSH);
                    editor.showUndoDeleteBanner(target.calculateEffort());
                    parentCanvas.removeBlock(target);
                    editor.recordEdits(StrideEditReason.DELETE_FRAMES_KEY_BKSP);
                    editor.modifiedFrame(target, false);
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
                    editor.recordEdits(StrideEditReason.FLUSH);
                    editor.showUndoDeleteBanner(target.calculateEffort());
                    parentCanvas.removeBlock(target);
                    editor.recordEdits(StrideEditReason.DELETE_FRAMES_KEY_DELETE);
                    editor.modifiedFrame(target, false);
                    editor.endRecordingState(FrameCursor.this);
                }
            }
            // Mac doesn't produce the right key typed events for Enter or Ctrl-Space
            // so we have special cases here in key pressed:
            else if (event.getCode() == KeyCode.ENTER)
            {
                keyTyped(editor, parentCanvas, '\n', false);
            }
            else if (event.getCode() == KeyCode.SPACE && event.isControlDown())
            {
                keyTyped(editor, parentCanvas, ' ', event.isControlDown());
            }
        });
    }

    private void showHide(boolean show)
    {
        node.setOpacity(show ? 1.0 : 0.0);
        node.setMaxHeight(show ? FULL_HEIGHT : HIDE_HEIGHT);
    }

    @OnThread(Tag.FXPlatform)
    public void showAsDropTarget(boolean showAsSource, boolean dragPossible, boolean copying)
    {
        int chosen;
        
        if (dragPossible) {
            chosen = showAsSource ? 1 : 0;
        }
        else {
            chosen = 2;
        }
        
        // Must resize before setting drag class:
        showHide(true);
        setDragClass(chosen);
        updateDragCopyState(copying);
    }

    /**
     * 
     * @param classIndex -1 for no drag target, 0 for possible, 1 for source, 2 for imposibble
     */
    @OnThread(Tag.FXPlatform)
    private void setDragClass(int classIndex)
    {
        JavaFXUtil.selectPseudoClass(node, classIndex,
                "bj-drag-possible", "bj-drag-source", "bj-drag-impossible");
        setDragTargetOverlayVisible(classIndex != -1, classIndex == 2);
    }

    @OnThread(Tag.FXPlatform)
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

    @OnThread(Tag.FXPlatform)
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

    @OnThread(Tag.FXPlatform)
    public void stopShowAsDropTarget()
    {
        showHide(false);
        setDragClass(-1);
    }
    
    public void insertBlockAfter(Frame b)
    {
        getParentCanvas().insertBlockAfter(b, this);
    }
    
    public void insertBlockBefore(Frame b)
    {
        getParentCanvas().insertBlockBefore(b, this);
    }
    
    public FrameTypeCheck check()
    {
        return getParentCanvas().getParent().check(getParentCanvas());
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
        final FrameCanvas parentCanvas = getParentCanvas();
        if (parentCanvas == null) return null;
        final CanvasParent canvasParent = parentCanvas.getParent();
        if (canvasParent == null) return null;
        return canvasParent.getFrame();

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

    public InteractionManager getEditor()
    {
        return this.editor;
    }

    @OnThread(Tag.FXPlatform)
    private boolean showContextMenu(double screenX, double screenY)
    {
        if (JavaFXUtil.hasPseudoclass(node, "bj-java-preview"))
            return false;

        if (menu != null) {
            menu.hide();
        }
        menu = AbstractOperation.MenuItems.makeContextMenu(Collections.singletonMap(EditableSlot.TopLevelMenu.EDIT, getMenuItems(true)));
        if (menu.getItems().size() > 0) {
            menu.show(node, screenX, screenY);
            return true;
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public AbstractOperation.MenuItems getMenuItems(boolean contextMenu)
    {
        boolean selection = !editor.getSelection().isEmpty();
        AbstractOperation.MenuItems menuItems = new AbstractOperation.MenuItems(FXCollections.observableArrayList(new PasteFrameOperation(editor).getMenuItem(contextMenu, () -> editor.getSelection().getSelected())));
        if (!editor.getSelection().isEmpty())
        {
            menuItems = AbstractOperation.MenuItems.concat( AbstractOperation.getMenuItems(editor.getSelection().getSelected(), contextMenu), menuItems);
        }

        if (canInsert() && contextMenu && !selection)
        {
            Menu insertMenu = new Menu("Insert");
            insertMenu.getItems().addAll(getAcceptedFramesMenuItems());
            menuItems = AbstractOperation.MenuItems.concat( new AbstractOperation.MenuItems(FXCollections.observableArrayList(AbstractOperation.MenuItemOrder.INSERT_FRAME.item(insertMenu))), menuItems);
        }

        return menuItems;
    }

    private List<MenuItem> getAcceptedFramesMenuItems()
    {
        List<MenuItem> items = new ArrayList<>();
        List<Entry<StrideCategory>> entries = StrideDictionary.getDictionary().getAllBlocks();
        for (Entry<StrideCategory> entry : entries) {
            if ( check().canInsert(entry.getCategory()) ) {
                 items.add(createMenuItem(entry, this));
            }
        }
        return items;
    }

    private MenuItem createMenuItem(Entry<StrideCategory> entry, FrameCursor cursor)
    {
        Label d = new Label();
        d.setText(entry.getShortcuts() + "\t  " + entry.getName());
        d.setPrefWidth(250);
        MenuItem item = new MenuItem(entry.getName());

        // Delete (with hover preview)
        item.setOnAction(e -> {
            editor.beginRecordingState(FrameCursor.this);
            editor.recordEdits(StrideEditReason.FLUSH);
            Frame newFrame = entry.getFactory().createBlock(editor);
            cursor.insertBlockAfter(newFrame);
            editor.recordEdits(StrideEditReason.SINGLE_FRAME_INSERTION_CONTEXT_MENU);
            newFrame.markFresh();
            newFrame.focusWhenJustAdded();
            editor.endRecordingState(null);
            editor.getSelection().clear();
            e.consume();
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

    @OnThread(Tag.FXPlatform)
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
