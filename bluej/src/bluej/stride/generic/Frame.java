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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.generic;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.elements.CodeElement;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.frames.BlankFrame;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.FrameHelper;
import bluej.stride.operations.DeleteFrameOperation;
import bluej.stride.operations.DisableFrameOperation;
import bluej.stride.operations.EnableFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.BetterVBox;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * The base frame control from which specific frames are built.
 * 
 * The Frame class should be completely language-agnostic, and should focus on layout and GUI properties.
 * For example, if we swapped the editor to work on XML, Frame should be unchanged.
 * 
 * @author Fraser McKay
 */
public abstract class Frame implements CursorFinder, FocusParent<FrameContentItem>, ErrorShower
{
    // 0 means no preview, 1 means preview as enabled, 2 means preview as disabled:
    private static int FRAME_ENABLE_PREVIEW_NO_PREVIEW = 0;
    private static int FRAME_ENABLE_PREVIEW_ENABLED = 1;
    private static int FRAME_ENABLE_PREVIEW_DISABLED = 2;
    protected final ObservableList<FrameContentItem> contents = FXCollections.observableArrayList();
    protected final FrameContentRow header;
    protected final SlotLabel headerCaptionLabel;
    protected final BooleanProperty frameEnabledProperty = new SimpleBooleanProperty(true);
    /**
     * Area that contains the frame content
     */
    private final BetterVBox frameContents;
    // When the frame is disabled, we keep track of whether this is the root (the highest level disabled frame),
    // because only the root should display the blur effect.  Without this, all child frames get
    // double-, triple-, etc- blurred.
    private final BooleanProperty disabledRoot = new SimpleBooleanProperty(true);
    private final IntegerProperty framePreviewEnableProperty = new SimpleIntegerProperty(0);
    private final BooleanProperty frameDragSourceProperty = new SimpleBooleanProperty(false);
    private final InteractionManager editor;
    private final BooleanProperty fresh = new SimpleBooleanProperty(false);
    private final ObservableList<CodeError> allFrameErrors = FXCollections.observableArrayList();
    private final ObjectProperty<CodeError> shownError = new SimpleObjectProperty<>(null);
    private FrameCanvas parentCanvas;
    private boolean alwaysBeenBlank = true;
    /**
     * Creates a new block with a default caption for the statement (e.g. "break", "continue", "for").
     * @param caption The default caption string.
     */
    public Frame(final InteractionManager editor, String caption, String stylePrefix)
    {
        frameContents = new BetterVBox(200.0) {
            @Override
            public double getBottomMarginFor(Node n)
            {
                return Frame.this.getBottomMarginFor(n);
            }

            @Override
            public double getLeftMarginFor(Node n)
            {
                return Frame.this.getLeftMarginFor(n);
            }

            @Override
            public double getRightMarginFor(Node n)
            {
                return Frame.this.getRightMarginFor(n);
            }
        };
        //Debug.time("&&&&&& Constructing frame");
        frameContents.getStyleClass().addAll("frame", stylePrefix + "frame");
        
        frameEnabledProperty.addListener((a, b, enabled) -> {
            getEditableSlotsDirect().forEach(e -> e.setEditable(enabled));
            editor.modifiedFrame(this);
        });
        
        //Debug.time("&&&&&&   Binding effect");

        //  Here's the state diagram for when we are enabled/disabled, and the preview state, as to what
        //  effect we should show:
        //  -------------------------------------------------
        //  | Actual   | _NO_PREVIEW | _DISABLED | _ENABLED |
        //  -------------------------------------------------
        //  | Enabled  | Enabled     | Disabled  | Enabled  |
        //  | Disabled | Disabled    | Disabled  | Enabled  |
        //  -------------------------------------------------
        // This our test for showing disabled effect is either FRAME_ENABLE_PREVIEW_DISABLED, or
        // not enabled && FRAME_ENABLE_PREVIEW_NO_PREVIEW

        // I suspect this could be done better:
        frameContents.effectProperty().bind(
            new When(disabledRoot.and(frameEnabledProperty.not().and(framePreviewEnableProperty.isEqualTo(FRAME_ENABLE_PREVIEW_NO_PREVIEW)).or(framePreviewEnableProperty.isEqualTo(FRAME_ENABLE_PREVIEW_DISABLED))))
               .then(new When(frameDragSourceProperty )
                         .then(FrameEffects.getDragSourceAndDisabledEffect())
                         .otherwise(FrameEffects.getDisabledEffect()))
               .otherwise(new When(frameDragSourceProperty)
                             .then(FrameEffects.getDragSourceEffect())
                             .otherwise((Effect)null)));
        // Drag and drop
        if (editor != null) {
            editor.setupFrame(this);
        }
        
        this.editor = editor;
        //Debug.time("&&&&&&   Making frame internals");

        if (caption == null)
        {
            headerCaptionLabel = null;
        }
        else
        {
            headerCaptionLabel = new SlotLabel(caption, "caption", stylePrefix + "caption");
        }


        header = makeHeader(stylePrefix);
        // Add listener before setAll call:
        contents.addListener((ListChangeListener<? super FrameContentItem>) c -> frameContents.getChildren().setAll(calculateContents(Utility.mapList(contents, FrameContentItem::getNode))));
        contents.setAll(header);
        
        // By default, add caption (done automatically by method):
        setHeaderRow();


        getNode().focusedProperty().addListener( (observable, oldValue, newValue) -> {
            if (newValue) {
                getCursorAfter().requestFocus();
            }
        });
        
        allFrameErrors.addListener((ListChangeListener<CodeError>)c -> {
            shownError.set(allFrameErrors.stream().min(CodeError::compareErrors).orElse(null));
            FXRunnable update = () -> JavaFXUtil.setPseudoclass("bj-frame-error", shownError.get() != null, frameContents);
            if (isFresh())
                // This may queue up a few of them, but it doesn't really matter:
                onNonFresh(update);
            else
                update.run();
        });

        
        //Debug.time("&&&&&& Constructed frame");
    }
    
    private static void blitImage(WritableImage dest, int xOffset, int yOffset, Image src)
    {
        dest.getPixelWriter().setPixels(xOffset, yOffset, (int)(Math.ceil(src.getWidth())), (int)(Math.ceil(src.getHeight())), src.getPixelReader(), 0, 0);
    }

    public static Image takeShot(List<Frame> frames, Color border /* none if null */)
    {
        int totalHeight = 0;
        int maxWidth = 0;
        for (Frame f : frames) {
            Bounds b = f.getNode().getBoundsInParent();
            totalHeight += (int)Math.ceil(b.getHeight()) + FrameCursor.HIDE_HEIGHT;
            maxWidth = Math.max(maxWidth, (int)Math.ceil(b.getWidth()));
        }
        totalHeight -= FrameCursor.HIDE_HEIGHT; // Don't need trailing space
    
        WritableImage collated;
        int xOffset, yOffset;
        if (border != null) {
            collated = new WritableImage(maxWidth + 2, totalHeight + 2);
            for (int x = 0; x < maxWidth + 2; x++) {
                collated.getPixelWriter().setColor(x, 0, border);
                collated.getPixelWriter().setColor(x, totalHeight + 1, border);
            }
            for (int y = 0; y < totalHeight + 2; y++) {
                collated.getPixelWriter().setColor(0, y, border);
                collated.getPixelWriter().setColor(maxWidth + 1, y, border);
            }
            xOffset = yOffset = 1;
        }
        else {
            if (maxWidth * totalHeight < 1) {
                return null;
            }
            collated = new WritableImage(maxWidth, totalHeight);
            xOffset = yOffset = 0;
        }
        
        int y = 0;
        for (Frame f : frames) {
            Bounds b = f.getNode().getBoundsInParent();
            WritableImage image = new WritableImage((int)Math.ceil(b.getWidth()), (int)Math.ceil(b.getHeight()));
            SnapshotParameters p = new SnapshotParameters();
            p.setFill(Color.TRANSPARENT);
            // Quick way to make sure we show blur if we are disabled:
            boolean dr = f.disabledRoot.get();
            f.disabledRoot.set(true);
            JavaFXUtil.setPseudoclass("bj-hide-caret", true, f.getNode());
            f.getNode().snapshot(p, image);
            f.disabledRoot.set(dr);
            JavaFXUtil.setPseudoclass("bj-hide-caret", false, f.getNode());
            Frame.blitImage(collated, xOffset, yOffset + y, image);
            y += (int)Math.ceil(b.getHeight()) + FrameCursor.HIDE_HEIGHT;
        }
        
        return collated;
    }

    protected static Stream<RecallableFocus> getFocusablesInclContained(Frame f)
    {
        return Utility.concat(f.getEditableSlotsDirect(), f.getPersistentCanvases().flatMap(c -> c.getFocusableCursors().stream()),
                 f.getPersistentCanvases().flatMap(c -> c.getBlockContents().stream()).flatMap(Frame::getFocusablesInclContained));
    }

    // TODO should probably be in CSS
    protected double getRightMarginFor(Node n)
    {
        return n == header.getNode() ? 1.0 : 0.0;
    }

    // TODO should probably be in CSS
    protected double getLeftMarginFor(Node n)
    {
        return n == header.getNode() ? 1.0 : 0.0;
    }

    protected double getBottomMarginFor(Node n)
    {
        return 0.0;
    }

    protected FrameContentRow makeHeader(String stylePrefix)
    {
        return new FrameContentRow(this, stylePrefix);
    }
    
    // Can be overridden in subclasses to add more content:
    protected List<? extends Node> calculateContents(List<Node> normalContent)
    {
        return normalContent;
    }

    public List<FrameOperation> getContextOperations(InteractionManager editor)
    {
        return getStandardOperations(editor);
    }
    
    /**
     * Insert the given block in place of this one (and remove this block when finished).
     * @param replacement Block to be put on the parent in this block's place
     */
    public void replaceWith(Frame replacement)
    {
        getParentCanvas().replaceBlock(this, replacement);
    }
    
    protected void addUnmanagedToBlockContainer(Node n)
    {
        if (n.isManaged()) {
            throw new IllegalArgumentException("Attempting to add managed node as unmanaged");
        }
        frameContents.getChildren().add(n);
    }

    /**
     * Gets the first cursor inside this block (or null if none)
     */
    public final FrameCursor getFirstInternalCursor()
    {
        return getCanvases().map(FrameCanvas::getFirstCursor).findFirst().orElse(null);
    }
    
    /**
     * Gets the last cursor inside this block (or null if none)
     */
    public final FrameCursor getLastInternalCursor()
    {
        return Utility.findLast(getCanvases().map(FrameCanvas::getLastCursor)).orElse(null);
    }

    public FrameCanvas getParentCanvas()
    {
        return parentCanvas;
    }

    public void setParentCanvas(FrameCanvas parentCanvas)
    {
        FrameCanvas oldCanvas = this.parentCanvas;
        this.parentCanvas = parentCanvas;
        // We update all frames in the old and new canvas
        // Only way to make var frames update and hide/show caption correctly
        if (oldCanvas != null)
            oldCanvas.getBlockContents().forEach(f -> f.updateAppearance(f.getParentCanvas()));
        if (parentCanvas != null)
        {
            parentCanvas.getBlockContents().forEach(f -> f.updateAppearance(f.getParentCanvas()));
            Utility.iterableStream(getAllFrames()).forEach(f -> f.updateAppearance(f.getParentCanvas()));
        }
    }

    public final FrameCursor getCursorAfter() { return parentCanvas == null ? null : parentCanvas.getCursorAfter(this);}

    public final FrameCursor getCursorBefore() { return parentCanvas == null ? null : parentCanvas.getCursorBefore(this);}
    
    protected final void addStyleClass(String styleClass)
    {
        JavaFXUtil.addStyleClass(frameContents, styleClass);
    }
    
    protected final void removeStyleClass(String styleClass)
    {
        JavaFXUtil.removeStyleClass(frameContents, styleClass);
    }
    
    /**
     * This is primarily intended for use in adding the item to its parent container
     * and adding handlers.  It should not be used to circumvent this interface to the display properties.
     */
    public final Node getNode()
    {
        return frameContents;
    }
    
    /**
     * This is primarily intended for use by sub-classes which need to access properties
     * of the graphics item (e.g. layout properties)
     */
    protected final Region getRegion()
    {
        return frameContents;
    }
    
    /**
     * Method which can be over-ridden if the block is styled differently depending on its parent
     */
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        // We're the root if we can be enabled:
        disabledRoot.set(canHaveEnabledState(true));
    }

    public void setDragSourceEffect(boolean on)
    {
        frameDragSourceProperty.set(on);
    }
    
    private List<FrameOperation> getStandardOperations(InteractionManager editor)
    {
        List<FrameOperation> ops = new ArrayList<FrameOperation>();
        ops.addAll(getCutCopyPasteOperations(editor));
        ops.add(isFrameEnabled() ? new DisableFrameOperation(editor) : new EnableFrameOperation(editor));
        ops.add(new DeleteFrameOperation(editor));
        return ops;
    }
    
    protected abstract List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor);
    
    public boolean isFrameEnabled()
    {
        return frameEnabledProperty.get();
    }
    
    public void setFrameEnabled(boolean enabled)
    {
        if (!canHaveEnabledState(enabled))
            return;

        frameEnabledProperty.set(enabled);

        // Bit ugly, but the only way to use setElementEnabled's default implementation
        // without making Frame (rather than subclasses) implement CodeFrame, which wouldn't
        // work because we'd lose the useful type parameter on CodeFrame
        if (this instanceof CodeFrame)
            ((CodeFrame)this).setElementEnabled(enabled);
        
        if (!enabled)
        {
            // Get rid of all errors straight away:
            flagErrorsAsOld();
            removeOldErrors();
        }
        
        // When our status changes, copy that status to all children:
        setChildrenEnabled(enabled);

        // Updated the disabledRoot variable:
        updateAppearance(getParentCanvas());
    }

    public boolean canHaveEnabledState(boolean enabled)
    {
        // The default behaviour is that a frame can only be enabled if all of its parents
        // are enabled, but it can always become disabled.
        if (enabled)
        {
            FrameCanvas canvas = getParentCanvas();
            if (canvas != null)
            {
                Frame parent = canvas.getParent().getFrame();
                if (parent != null)
                {
                    // If the parent is disabled, we definitely can't be enabled
                    // If the parent is enabled, we check it can be enabled for sanity:
                    return parent.isFrameEnabled() && parent.canHaveEnabledState(enabled);
                }
            }
            // If we don't have parents, assume we can have any state.
        }
        return true;
    }
    
    public void setFrameEnablePreview(Optional<Boolean> previewingEnable)
    {
        framePreviewEnableProperty.set(previewingEnable.isPresent() ? (previewingEnable.get() ? FRAME_ENABLE_PREVIEW_ENABLED : FRAME_ENABLE_PREVIEW_DISABLED) : FRAME_ENABLE_PREVIEW_NO_PREVIEW);
    }
    
    protected final void setChildrenEnabled(boolean enabled)
    {
        getCanvases().forEach(canvas -> canvas.getBlocksSubtype(Frame.class).forEach(b -> b.setFrameEnabled(enabled)));
    }
    
    public final void flagErrorsAsOld()
    {
        allFrameErrors.forEach(CodeError::flagAsOld);
        getEditableSlots().forEach(EditableSlot::flagErrorsAsOld);
        getCanvases().forEach(FrameHelper::flagErrorsAsOld);
    }

    public final void removeOldErrors()
    {
        allFrameErrors.removeIf(CodeError::isFlaggedAsOld);
        getEditableSlots().forEach(EditableSlot::removeOldErrors);
        getCanvases().forEach(FrameHelper::removeOldErrors);
    }
    
    /**
     * Method called while frame is being removed.  Should remove any overlays, listeners, etc
     */
    public final void cleanup()
    {
        getEditableSlots().forEach(EditableSlot::cleanup);
        getCanvases().forEach(FrameCanvas::cleanup);
        cleanupFrame();
    }

    protected void cleanupFrame()
    {
        // Nothing to do by default
    }

    public final Stream<FrameCanvas> getCanvases()
    {
        return contents.stream().map(FrameContentItem::getCanvas).flatMap(Utility::streamOptional);
    }

    public Stream<FrameCanvas> getPersistentCanvases()
    {
        // By default, assume all canvases are persistent:
        return getCanvases();
    }

    public boolean isCollapsible()
    {
        return false;
    }

    public void setCollapsed(boolean collapse)
    {
        // Nothing to do unless we're collapsible
    }

    public void checkForEmptySlot()
    {
        // Do nothing
    }

    /**
     * Allows blocks to respond to a keypress when the cursor
     * is just after the block, e.g. pressing a key to extend an if with an else.
     */
    public List<ExtensionDescription> getAvailableExtensions()
    {
        return Collections.emptyList();
    }

    public final boolean notifyExtensionKey(char c, RecallableFocus rc)
    {
        return notifyKey(c, rc, getAvailableExtensions(), "extension");
    }

    /**
     * Allows blocks to respond to a keypress when the cursor
     * is just before the block, e.g. pressing a '\\' key to disable a frame.
     */
    protected List<ExtensionDescription> getAvailablePrefixes()
    {
        return Arrays.asList(new ExtensionDescription('\\', "Disable/Enable frames", () -> {
            if (canHaveEnabledState(isFrameEnabled())) {
                setFrameEnabled(!isFrameEnabled());
            }
        }, false, false));
    }
    
    public final boolean notifyPrefixKey(char c, RecallableFocus rc)
    {
        return notifyKey(c, rc, getAvailablePrefixes(), "prefix");
    }

    private boolean notifyKey(char c, RecallableFocus rc, List<ExtensionDescription> extensions, String label)
    {
        List<ExtensionDescription> candidates = extensions.stream()
                .filter(e -> e.getShortcutKey() == c)
                .collect(Collectors.toList());
        
        if (candidates.size() == 0) {
            return false;
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Ambiguous " + label + " for: " + (int)c);
        }
        
        if (candidates.get(0).isAvailable()){
            editor.beginRecordingState(rc);
            candidates.get(0).activate();
            editor.endRecordingState(rc);
            return true;
        }
        return false;
    }

    // true by default:
    public boolean canDrag()
    {
        return true; 
    }
    
    /**
     * Notifies about a left-click.  Returns true if we have consumed the click, false otherwise.
     */
    public final boolean leftClicked(double sceneX, double sceneY, boolean shiftDown)
    {
        //Select block once it's clicked
        editor.clickNearestCursor(sceneX, sceneY, shiftDown);
        return true;
    }
    
    /**
     * Called automatically when first created and added to its parent; shows where to focus input after initial key-press
     *
     * Returns true if it successfully focused on something; false if there is nothing in the frame to focus on
     */
    public boolean focusWhenJustAdded()
    {
        EditableSlot s = getEditableSlotsDirect().findFirst().orElse(null);
        if (s != null) {
            s.requestFocus();
            return true;
        }

        return false;
    }

    protected void addTopRight(Node n)
    {
        //TODO
        //headerRowComponents.add(FXCollections.observableArrayList(n));
    }
    
    protected final FrameContentRow getHeaderRow()
    {
        return header;
    }

    /** Automatically prepends the caption, if there is one */
    protected void setHeaderRow(HeaderItem... headerItems)
    {
        if (headerCaptionLabel == null)
            header.setHeaderItems(Arrays.asList(headerItems));
        else
        {
            ArrayList<HeaderItem> items = new ArrayList<>();
            items.add(headerCaptionLabel);
            items.addAll(Arrays.asList(headerItems));
            header.setHeaderItems(items);
        }
    }

    /**
     * Called when this frame should become focused, e.g. because the user has navigated to it
     * using keys by pressing up from below this frame.
     * @param up true if used up to get here, false if used right
     * @return true if this frame can be focused and has been, false otherwise (e.g. frame is disabled, or cannot receive focus because it has no slots or content)
     */
    public final boolean focusFrameEnd(boolean up)
    {
        if (isFrameEnabled()) {
            FrameContentItem last = contents.get(contents.size() - 1);
            return up ? last.focusBottomEndFromNext() : last.focusRightEndFromNext();
        }
        return false;
    }

    /**
     * Called when this frame should become focused, e.g. because the user has navigated to it
     * using keys by pressing right from above this frame.
     */
    public final boolean focusFrameStart()
    {
        if (isFrameEnabled()) {
            FrameContentItem first = contents.get(0);
            return first.focusLeftEndFromPrev();
        }
        return false;
    }

    public InteractionManager getEditor()
    {
        return editor;
    }
        
    /**
     * Called when you have clicked on the frame in a stack trace or want to jump to definition.
     * 
     * Need Platform.runLater because we will be notified about exception by Swing at the moment.
     */
    public void show(ShowReason reason)
    {
        switch (reason)  {
        case EXCEPTION:
            Platform.runLater(() -> {
                JavaFXUtil.setPseudoclass("bj-stack-highlight", true, getNode());
                getParentCanvas().getCursorBefore(this).requestFocus();
                editor.scrollTo(getNode(), -50.0);
                editor.registerStackHighlight(this);
            });
            break;
        case LINK_TARGET:
            // Don't think we need runlater here, but better to be consistent:
            Platform.runLater(() -> {
                editor.scrollTo(getNode(), -50.0);
                focusName();
            });
            break;
        }
    }
    
    public void focusName()
    {
        // By default:
        focusWhenJustAdded();
    }

    public void removeStackHighlight()
    {
        Platform.runLater(() -> JavaFXUtil.setPseudoclass("bj-stack-highlight", false, getNode()));
    }
    
    /**
     * Gets all slots, from this frame and all frames contained in canvases, to unlimited depth
     */
    public final Stream<HeaderItem> getHeaderItems()
    {
        return contents.stream().flatMap(FrameContentItem::getHeaderItemsDeep);
    }

    /**
     * Gets all editable slots, from this frame and all frames contained in canvases, to unlimited depth
     */
    public final Stream<EditableSlot> getEditableSlots()
    {
        return getHeaderItems().map(HeaderItem::asEditable).filter(x -> x != null);
    }
    
    /**
     * Gets only those editable slots which are directly in this frame (not any in frames inside any canvases)
     * @return
     */
    public final Stream<EditableSlot> getEditableSlotsDirect()
    {
        return contents.stream().flatMap(FrameContentItem::getHeaderItemsDirect).map(HeaderItem::asEditable).filter(x -> x != null);
    }
    
    @Override
    public void focusUp(FrameContentItem src, boolean toEnd)
    {
        int index = contents.indexOf(src);
        if (index == -1)
            throw new IllegalStateException("Item not contained in frame");
        if (index == 0)
            focusPrevTarget();
        else
            if (!contents.get(index - 1).focusBottomEndFromNext())
                focusUp(contents.get(index - 1), toEnd);
    }
    // SlotParent:
    
    @Override
    public void focusDown(FrameContentItem src)
    {
        int index = contents.indexOf(src);
        if (index == -1)
            throw new IllegalStateException("Item not contained in frame");
        if (index == contents.size() - 1)
        {
            if (getCursorAfter() != null)
                getCursorAfter().requestFocus();
        }
        else
            if (!contents.get(index + 1).focusTopEndFromPrev())
                focusDown(contents.get(index + 1));
    }
    
    @Override
    public void focusEnter(FrameContentItem src)
    {
        focusDown(src);
    }

    @Override
    public void focusLeft(FrameContentItem src)
    {
        int index = contents.indexOf(src);
        if (index == -1)
            throw new IllegalStateException("Item not contained in frame");
        if (index == 0)
            focusPrevTarget();
        else
            if (!contents.get(index - 1).focusRightEndFromNext())
                focusLeft(contents.get(index - 1));
    }

    @Override
    public void focusRight(FrameContentItem src)
    {
        int index = contents.indexOf(src);
        if (index == -1)
            throw new IllegalStateException("Item not contained in frame");
        if (index == contents.size() - 1)
        {
            if (getCursorAfter() != null)
                getCursorAfter().requestFocus();
        }
        else
            if (!contents.get(index + 1).focusLeftEndFromPrev())
                focusRight(contents.get(index + 1));
    }

    protected void focusPrevTarget()
    {
        FrameCursor cursorBefore = getCursorBefore();
        if (cursorBefore != null) {
            cursorBefore.requestFocus();
        }
    }

    public boolean backspaceAtStart(FrameContentItem srcRow, HeaderItem src)
    {
        if (contents.size() > 0 && (src == contents.get(0) || srcRow == contents.get(0)))
        {
            if (isAlmostBlank())
            {
                // Delete ourselves
                FrameCanvas parentCanvas = getParentCanvas();
                FrameCursor cursorBefore = getCursorBefore();
                parentCanvas.removeBlock(this);
                cursorBefore.requestFocus();
                return true;
            }
        }
        return false;
    }

    public boolean deleteAtEnd(FrameContentItem srcRow, HeaderItem src)
    {
        return false;
    }

    /**
     * By default, we pull up each frame canvas's contents, with a blank frame between each
     */
    public void pullUpContents()
    {
        editor.modifiedFrame(this); //notify the editor that a change has been occured. That will trigger a file save
        getCursorBefore().insertFramesAfter(Utility.<Frame>concat(getCanvases().<List<Frame>>map(canvas -> {
            // Make copy because we're about to modify the contents:
            List<Frame> contents = new ArrayList<>(canvas.getBlockContents());
            contents.forEach(c -> canvas.removeBlock(c));
            return contents;
        }).collect(Utility.<List<Frame>>intersperse(() -> Arrays.<Frame>asList(new BlankFrame(editor)))).toArray(new List[0])));
    }

    /**
     * Gets all frames contained within, to unlimited depth
     */
    public final Stream<Frame> getAllFrames()
    {
        return Stream.concat(Stream.of(this), getCanvases().flatMap(c -> c.getBlocksSubtype(Frame.class).stream()).flatMap(Frame::getAllFrames));
    }

    public void markFresh()
    {
        fresh.set(true);
    }
    
    public void markNonFresh()
    {
        fresh.set(false);
    }
    
    public boolean isFresh()
    {
        return fresh.get();
    }
    
    public void onNonFresh(FXRunnable action)
    {
        if (!isFresh()) {
            throw new IllegalStateException("Calling onNonFresh when we are already non-fresh; state cannot go back to fresh");
        }
        JavaFXUtil.addSelfRemovingListener(fresh, b -> action.run());
    }
    
    public ObservableBooleanValue freshProperty()
    {
        return fresh;
    }

    /**
     * Gets a list of variables declared within this frame, that are in scope for
     * frames after this one in the same canvas
     */
    public List<String> getDeclaredVariablesAfter()
    {
        return Collections.emptyList();
    }

    /**
     * Gets a list of variables declared within this frame, that are in scope for
     * the given canvas
     */
    public List<String> getDeclaredVariablesWithin(FrameCanvas c)
    {
        return Collections.emptyList();
    }
    
    /**
     * Sets the given view.
     * @param view
     * @param animation A class to hook into to synchronise animations
     */
    public void setView(View oldView, View newView, SharedTransition animation)
    {
        setViewNoOverride(oldView, newView, animation);
    }
    
    // Used by ClassFrame to access this method while overriding other functionality
    protected final void setViewNoOverride(View oldView, View newView, SharedTransition animation)
    {
        final Background start = frameContents.getBackground();
        final Border startBorder = frameContents.getBorder();
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, frameContents);
        frameContents.applyCss();
        final Background end = frameContents.getBackground();
        final Border endBorder = frameContents.getBorder();
        boolean animatingBackground = false;
        if (start != null && end != null && start.getImages().isEmpty() && end.getImages().isEmpty() && start.getFills().size() == 1 && end.getFills().size() == 1)
        {
            final BackgroundFill startFill = start.getFills().get(0);
            final BackgroundFill endFill = end.getFills().get(0);
            if (startFill.getFill() instanceof Color && endFill.getFill() instanceof Color && !(startFill.getFill().equals(endFill.getFill())))
            {
                // We can animate from one colour to the other:
                final Color startColor = (Color) startFill.getFill();
                final Color endColor = (Color) endFill.getFill();

                animatingBackground = true;

                JavaFXUtil.addChangeListener(animation.getProgress(), t -> {
                    Color c = startColor.interpolate(endColor, t.doubleValue());
                    frameContents.setBackground(new Background(new BackgroundFill(c, endFill.getRadii(), endFill.getInsets())));
                });
            }
        }
        boolean animatingBorder = false;
        if (startBorder != null && endBorder != null && startBorder.getImages().isEmpty() && endBorder.getImages().isEmpty() && startBorder.getStrokes().size() == 1 && endBorder.getStrokes().size() == 1)
        {
            final BorderStroke startStroke = startBorder.getStrokes().get(0);
            final BorderStroke endStroke = endBorder.getStrokes().get(0);
            if (startStroke.isStrokeUniform() && endStroke.isStrokeUniform())
            {
                final Paint startStrokePaint = startStroke.getTopStroke();
                final Paint endStrokePaint = endStroke.getTopStroke();

                if (startStrokePaint instanceof Color && endStrokePaint instanceof Color && !startStrokePaint.equals(endStrokePaint))
                {
                    final Color startColor = (Color)startStrokePaint;
                    final Color endColor = (Color)endStrokePaint;

                    animatingBorder = true;

                    JavaFXUtil.addChangeListener(animation.getProgress(), t -> {
                        Color c = startColor.interpolate(endColor, t.doubleValue());
                        frameContents.setBorder(new Border(new BorderStroke(c, startStroke.getTopStyle(), startStroke.getRadii(), startStroke.getWidths())));
                    });
                }
            }
        }

        if (animatingBackground || animatingBorder)
            animation.addOnStopped(() -> frameContents.applyCss());

        if (!isFrameEnabled())
        {
            //Disabled frames should not be shown in Java preview mode:
            if (newView == View.JAVA_PREVIEW)
            {
                frameContents.opacityProperty().bind(animation.getOppositeProgress());
                animation.addOnStopped(() -> {
                    frameContents.opacityProperty().unbind();
                    frameContents.setVisible(false);
                });
            }
            else
            {
                frameContents.setVisible(true);
                frameContents.opacityProperty().bind(animation.getProgress());
                animation.addOnStopped(() -> frameContents.opacityProperty().unbind());
            }
        }

        contents.forEach(i -> i.setView(oldView, newView, animation));
    }

    public Stream<CodeError> getCurrentErrors()
    {
        return shownError.get() == null ? Stream.empty() : Stream.of(shownError.get());
    }

    public void addError(CodeError err)
    {
        allFrameErrors.add(err);
        err.bindFresh(fresh);
    }

    @Override
    public void focusAndPositionAtError(CodeError err)
    {
        // Whatever the error is, just focus the cursor before us:
        getCursorBefore().requestFocus();
    }

    @Override
    public Node getRelevantNodeForError(CodeError err)
    {
        // Whatever the error is, the relevant node is the cursor before us:
        if (getCursorBefore() == null) // May happen mid-removal
            return null;
        else
            return getCursorBefore().getNode();
    }

    public void compiled()
    {
        
    }

    // By default, we use the first slot if there are any, otherwise we return null
    // (meaning to add the error to ourselves):
    public EditableSlot getErrorShowRedirect()
    {
        return getEditableSlotsDirect().findFirst().orElse(null);
    }
    
    /**
     * Returns true if the frame is about as blank as when it was first created.  This typically means that
     * all the slots are near blank, and the canvases only have blank frames, if any
     */
    public boolean isAlmostBlank()
    {
        return getEditableSlotsDirect().allMatch(EditableSlot::isAlmostBlank) && getCanvases().allMatch(FrameCanvas::isAlmostBlank);
    }

    public void trackBlank()
    {
        alwaysBeenBlank &= isAlmostBlank();
    }

    public void escape(FrameContentItem srcRow, HeaderItem src)
    {
        if (alwaysBeenBlank && isFresh())
        {
            // Delete ourselves
            FrameCanvas parentCanvas = getParentCanvas();
            FrameCursor cursorBefore = getCursorBefore();
            parentCanvas.removeBlock(this);
            cursorBefore.requestFocus();
        }
    }

    // If you use this, be sure to also override calculateContents
    protected Pane getSidebarContainer()
    {
        return frameContents;
    }

    public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
    {
        List<FrameCanvas> canvases = getCanvases().filter(canvas -> canvas.getShowingProperty().get()).collect(Collectors.toList());

        // Our local bounds are unreliable because of the way we transform
        // the side label -- therefore use last canvas plus margin:
        final FrameCanvas lastCanvas = canvases.get(canvases.size() - 1);
        double sceneMaxY = lastCanvas.getSceneBounds().getMaxY() + lastCanvas.getBottomMargin();

        Bounds headerRowBounds = getHeaderRow().getSceneBounds();

        //The cursor can be in one of several regions:

        // 1. Above the half-way point of the header row (not header v-box).
        //    In this case, select before block
        // 2. Below half-way point of header row, but above canvas.
        //    In this case, select top of canvas
        // 3. In a canvas -- delegate to canvas
        // 3.5a. Above the half-way point of a divider; select end of previous canvas.
        // 3.5b. Below half-way point of a divider; select start of next canvas.
        // 4. Beneath canvas, above half-way in bottom border.
        //    Select last cursor in canvas
        // 5. Beneath canvas, below half-way in bottom border.
        //    Select cursor beneath this block.
        // 6. Beneath whole block. Select cursor beneath this block.



        if (prevCursor != null && sceneY <= (headerRowBounds.getMinY() + headerRowBounds.getMaxY()) / 2) {
            //Case 1:
            return prevCursor;
            //If previous cursor is null, we are a class block, so
            //case 1 is invalid.  Control goes to cases 2, 3 and 4 instead:
        }

        for (int canvasIndex = 0; canvasIndex < canvases.size(); canvasIndex++)
        {
            Bounds canvasBounds = canvases.get(canvasIndex).getSceneBounds();

            double nextY;
            if (canvasIndex == canvases.size() - 1) {
                nextY = sceneMaxY;
            }
            else {
                nextY = canvases.get(canvasIndex + 1).getSceneBounds().getMinY();
            }

            if (sceneY <= (canvasBounds.getMaxY() + nextY) / 2)
            {
                //Case 2, 3, 3.5a, 3.5b and 4:
                boolean isClickOnMargin = sceneX >= headerRowBounds.getMinX() && sceneX < canvasBounds.getMinX()
                    && !isDrag;
                return canvases.get(canvasIndex).findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend && !isClickOnMargin);
            }
        }

        //Case 5 or 6:
        if (nextCursor != null) {
            return nextCursor;
        }
        else {
            if (prevCursor != null) {
                return prevCursor;
            }
            return lastCanvas.findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend);
        }
    }

    /**
     * Gets the lowest scene Y coordinate inside this block, for the purposes of picking a cursor
     */
    public double lowestCursorY()
    {
        // Our local bounds are potentially unreliable because of the way we transform
        // the side label -- therefore use canvas plus margin:
        FrameCanvas lastCanvas = Utility.findLast(getCanvases()).orElse(null);
        Bounds canvasBounds = lastCanvas.getSceneBounds();
        return canvasBounds.getMaxY() + lastCanvas.getBottomMargin();
    }

    /**
     * Try to restore this frame's contents to match the given CodeElement.  In practice,
     * this will usually succeed if and only if CodeElement is the same type of frame as this one.
     * @return True if successfully restored, false if not
     */
    public boolean tryRestoreTo(CodeElement codeElement)
    {
        return false;
    }

    /**
     * Returns true for all frames with effictive code. I.e only flase in frames such as BlankFrame and CommentFrame.
     *
    * @return True except for non-effective frames (codewise)
    */
    public boolean isEffectiveFrame()
    {
        return true;
    }

    public static enum ShowReason { EXCEPTION, LINK_TARGET }

    public static enum View { NORMAL, JAVA_PREVIEW, BIRDSEYE }


}
