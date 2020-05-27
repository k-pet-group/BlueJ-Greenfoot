/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020 Michael Kölling and John Rosenberg
 
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.Config;
import bluej.stride.framedjava.slots.StructuredSlot;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.utility.javafx.*;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

import bluej.collect.StrideEditReason;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.frames.BlankFrame;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.FrameHelper;
import bluej.stride.operations.CopyFrameAsImageOperation;
import bluej.stride.operations.CopyFrameAsJavaOperation;
import bluej.stride.operations.CopyFrameAsStrideOperation;
import bluej.stride.operations.CutFrameOperation;
import bluej.stride.operations.DeleteFrameOperation;
import bluej.stride.operations.DisableFrameOperation;
import bluej.stride.operations.EnableFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;

import bluej.utility.Debug;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The base frame from which specific frames are derived.
 * 
 * Frame relates primarily to the GUI representation of a frame.  For the semantic side, see
 * the CodeElement class instead.
 * 
 * Frames often implement the CodeFrame interface, from which you can generate a CodeElement
 * (e.g. for saving and compiling).  Most CodeElements are also frame factories (e.g. for loading).
 * 
 * @author Fraser McKay
 */
public abstract class Frame implements CursorFinder, FocusParent<FrameContentItem>, ErrorShower, AbstractOperation.ContextualItem<Frame>
{
    /**
     * The list of contents of a frame.  The primary dimension of a frame is vertical:
     * the FrameContentItem objects in this list are laid out vertically within the frame,
     * with the first in the list shown at the top.  FrameContentItem is typically either
     * a FrameContentRow (a flow pane, e.g. for frame headers or single frame contents), or
     * a FrameCanvas (e.g. body of a while frame), or documentation (e.g. top of method frame)
     * 
     * FrameContentItem is a logical container, not a GUI item directly.
     */
    protected final ObservableList<FrameContentItem> contents = FXCollections.observableArrayList();
    
    /**
     * All frames have a header row, so we include it in the base class.  The header will
     * also always (I *think*) be in the contents list above, but a reference is kept separately for convenience).
     * header is never null.
     */
    protected final FrameContentRow header;

    /**
     * The headerCaptionLabel is often, but not always, present in the header.  In an IfFrame
     * this is the label showing "if".  Method frames don't have one.  Again, it's kept as a
     * separate reference for convenience.  It may be null.
     */
    protected final SlotLabel headerCaptionLabel;

    /**
     * Property tracking where this frame is enabled or disabled (akin to commented out)
     */
    protected final BooleanProperty frameEnabledProperty = new SimpleBooleanProperty(true);
    /**
     * The actual GUI control that contains the frame content (based on this.contents).  Never null.
     */
    private final BetterVBox frameContents;
    /**
     * When the frame is disabled, we keep track of whether this is the root (the highest level disabled frame),
     * because only the root should display the blur effect.  Without this tracking, all child frames get
     * double-, triple-, etc- blurred.
     */
    private final BooleanProperty disabledRoot = new SimpleBooleanProperty(true);
    private final String stylePrefix;

    public final String getStylePrefix()
    {
        return stylePrefix;
    }

    // Called when frame has lost direct focus (i.e. is not in its own slots; it could
    // be in the slots or canvas of a child frame).  Overridden in subclasses.
    @OnThread(Tag.FXPlatform)
    public void lostFocus()
    {
        
    }

    /**
     * Informs us Ctrl was held down when we were inserted via the keyboard.
     * Just here so it can be overridden in child classes.
     */
    @OnThread(Tag.FXPlatform)
    public void insertedWithCtrl()
    {
        
    }

    /** enum for keeping track of frame preview state */
    public static enum FramePreviewEnabled
    {
        PREVIEW_NONE, PREVIEW_ENABLED, PREVIEW_DISABLED;
    }

    /**
     * A frame can be enabled or disabled, which is tracked via frameEnabledProperty.
     * But when the user hovers over enable or disable in the context menu, we also
     * show a preview of the opposite state.  This property keeps track of whether
     * we are showing no preview (most common option), and thus display according to
     * frameEnabledProperty, or whether we are showing a preview of the enabled or disabled
     * state.
     */
    private final ObjectProperty<FramePreviewEnabled> framePreviewEnableProperty =
            new SimpleObjectProperty<>(FramePreviewEnabled.PREVIEW_NONE);

    /**
     * Tracks whether this frame is the source of a current drag operation.  If so,
     * it is displayed with a blur effect.
     */
    private final BooleanProperty frameDragSourceProperty = new SimpleBooleanProperty(false);

    /**
     * A reference to the editor in which this frame lives.  A frame object may never move editor;
     * any operation that appears to do this actually creates a new copy of the frame.
     */
    private final InteractionManager editor;

    /**
     * Keeps track of whether the frame is fresh.  A fresh frame is one which has been inserted
     * but the user has not yet left the frame since creation.  Fresh frames are not checked
     * for errors until they lose focus, to avoid red error underlines popping up as you
     * enter new (and thus partially complete) code.
     */
    private final BooleanProperty fresh = new SimpleBooleanProperty(false);

    /**
     * A list of all the *frame errors* which exist right now for this frame.  A frame error
     * is one that belongs to the frame, typically because there is no suitable slot to display
     * it in instead.  An example is if you put two return frames without a value.  The second
     * return frame should show an unreachable code error, but there's no sensible slot to show it
     * on, so we attach it to the return frame.  That goes in allFrameErrors.  In contrast,
     * a syntax error in an if condition is not a frame error; that gets attached to the 
     * expression slot for the condition.
     * 
     * Only one frame error is shown at any given time; this is tracked in shownError.
     */
    private final ObservableList<CodeError> allFrameErrors = FXCollections.observableArrayList();

    /**
     * shownError is usually null, but if there are any *frame errors* (see allFrameErrors)
     * then one of them is picked and shown as a red border around the frame.  This property
     * keeps track of which error was picked.  shownError should be null if and only if allFrameErrors is empty.
     */
    private final ObjectProperty<CodeError> shownError = new SimpleObjectProperty<>(null);

    /**
     * The parent canvas of this frame.  It can be null, if the frame is not in a parent canvas.
     * This canvas will change at most twice during the frame's lifetime:
     *   - Once, from null to a parent canvas when the frame gets inserted somewhere
     *   - Optionally, once more back to null when the frame is discarded.
     * It should thus never move canvas in its lifetime (this is usually done by making a new
     * copy), only be added and removed from one.
     */
    private FrameCanvas parentCanvas = null;

    /**
     * Keep track of whether a frame has always been blank (or near-blank).  Frames which have
     * had no content inserted yet can be removed by pressing the escape key.
     */
    private boolean alwaysBeenBlank = true;

    protected Map<String, BooleanProperty> modifiers = new HashMap<>();
    /**
     * Creates a new frame.
     * 
     * @param editor The editor that this frame belongs to
     * @param caption The caption to use in the header label.  If null, no header label is
     *                added to the frame
     * @param stylePrefix The prefix to be added on to the CSS style class.  If you pass, e.g.
     *                    "if-", the frame will get the style classes "frame" and "if-frame".
     *                    May not be null.
     */
    @OnThread(Tag.FX)
    public Frame(final InteractionManager editor, String caption, String stylePrefix)
    {
        frameContents = new BetterVBox(200.0) {
            @OnThread(Tag.FX)
            @Override
            public double getBottomMarginFor(Node n)
            {
                return Frame.this.getBottomMarginFor(n);
            }

            @OnThread(Tag.FX)
            @Override
            public double getLeftMarginFor(Node n)
            {
                return Frame.this.getLeftMarginFor(n);
            }

            @OnThread(Tag.FX)
            @Override
            public double getRightMarginFor(Node n)
            {
                return Frame.this.getRightMarginFor(n);
            }
        };
        //Debug.time("&&&&&& Constructing frame");
        if (stylePrefix == null)
            throw new NullPointerException();
        JavaFXUtil.addStyleClass(frameContents, "frame", stylePrefix + "frame");
        this.stylePrefix = stylePrefix;
        
        // When we are enabled/disabled, update our child slot states and trigger a compilation.
        frameEnabledProperty.addListener((a, b, enabled) -> {
            getEditableSlotsDirect().forEach(e -> e.setEditable(enabled));
            editor.modifiedFrame(this, false);
        });
        
        //Debug.time("&&&&&&   Binding effect");

        //  Here's the state diagram for when we are enabled/disabled, and the preview state, as to what
        //  effect we should show:
        //  ---------------------------------------------------
        //  | Actual   | _PREVIEW_NONE | _DISABLED | _ENABLED |
        //  ---------------------------------------------------
        //  | Enabled  | Enabled       | Disabled  | Enabled  |
        //  | Disabled | Disabled      | Disabled  | Enabled  |
        //  ---------------------------------------------------
        // Thus, our test for showing disabled effect is either PREVIEW_DISABLED, or
        // not enabled && FRAME_ENABLE_PREVIEW_NO_PREVIEW

        // I suspect this could be done better:
        frameContents.effectProperty().bind(
            new When(disabledRoot.and(frameEnabledProperty.not().and(framePreviewEnableProperty.isEqualTo(FramePreviewEnabled.PREVIEW_NONE)).or(framePreviewEnableProperty.isEqualTo(FramePreviewEnabled.PREVIEW_DISABLED))))
               .then(new When(frameDragSourceProperty )
                         .then(FrameEffects.getDragSourceAndDisabledEffect())
                         .otherwise(FrameEffects.getDisabledEffect()))
               .otherwise(new When(frameDragSourceProperty)
                             .then(FrameEffects.getDragSourceEffect())
                             .otherwise((Effect)null)));
        
        // We put some setup into the editor class because the setup requires a lot of access
        // to editor internals.  Easier to pass the editor the frame than it is to expose all
        // the editor internals to the frame.
        this.editor = editor;
        if (editor != null) {
            editor.setupFrame(this);
        }
        
        
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
        
        // Whenever the logical containers in contents change, we update the
        // GUI elements in frameContents:
        
        // Add listener before setAll call:
        contents.addListener((ListChangeListener<? super FrameContentItem>) c -> frameContents.getChildren().setAll(calculateContents(Utility.mapList(contents, FrameContentItem::getNode))));
        contents.setAll(header);
        
        // By default, the header row contains only the caption, if present:
        setHeaderRow();

        // In the case that someone tries to focus the frame directly, we pass the focus
        // on to the cursor after us.  (Not sure if we need this any more?)
        getNode().focusedProperty().addListener( (observable, oldValue, newValue) -> {
            if (newValue) {
                getCursorAfter().requestFocus();
            }
        });
        
        // Whenever a *frame error* (not slot error) is added, we recalculate whether,
        // and which error to show.
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

    /**
     * Initialise things which must be done on the actual FX thread, not just a loader thread.
     * Will be overridden by subclasses.
     */
    @OnThread(Tag.FXPlatform)
    protected void initialiseFX()
    {
    }

    /**
     * Helper method to take an image (screenshot) of the given list of frames,
     * optionally with the given colour of border around the image.
     */
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

            // alternative attempt - also not good:
            //BufferedImage bimage = new Robot().createScreenCapture(new Rectangle((int)Math.ceil(b.getWidth()), (int)Math.ceil(b.getHeight())));
            //WritableImage image = SwingFXUtils.toFXImage(bimage, null);

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
            JavaFXUtil.blitImage(collated, xOffset, yOffset + y, image);
            y += (int)Math.ceil(b.getHeight()) + FrameCursor.HIDE_HEIGHT;
        }
        
        return collated;
    }

    /**
     * Gets all RecallableFocus items within the frame, to unlimited depth.
     * Used for undo; see RecallableFocus.
     * @return A stream of all contained RecallableFocus items
     */
    protected Stream<RecallableFocus> getFocusablesInclContained()
    {
        return Utility.concat(getEditableSlotsDirect(), getPersistentCanvases().flatMap(c -> c.getFocusableCursors().stream()),
            getPersistentCanvases().flatMap(c -> c.getBlockContents().stream()).flatMap(Frame::getFocusablesInclContained));
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

    // TODO should probably be in CSS
    protected double getBottomMarginFor(Node n)
    {
        return 0.0;
    }

    /**
     * Makes the header row, using the given style prefix.
     * Drawn out into its own method so that it can be overridden and customised in subclasses.
     */
    protected FrameContentRow makeHeader(String stylePrefix)
    {
        return new FrameContentRow(this, stylePrefix);
    }

    /**
     * Given the normal content for this frame, gives back a list of altered content.
     * This is overridden by subclasses, for example, to add sidebar displays. 
     * 
     * Can be overridden in subclasses to add more content.  By default just returns its
     * list of nodes.
     */
    protected List<? extends Node> calculateContents(List<Node> normalContent)
    {
        return normalContent;
    }

    /**
     * Gets a list of available context operations for this frame.
     *
     * This is called when the context menu is about to be shown, so you can dynamically decide
     * which operations to include based on the current state at the time of this call, rather
     * than trying to do complex bindings to update the operations in future.
     * 
     * By default this is cut, copy, disable/enable, and delete operations.  Override this method in sub-classes
     * if you want different behaviour (e.g. if the frame doesn't support cut or delete).
     *
     * Overridden by subclasses.
     */
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> ops = new ArrayList<FrameOperation>();
        ops.add(new CutFrameOperation(editor));
        ops.add(new CopyFrameAsStrideOperation(editor));
        ops.add(new CopyFrameAsImageOperation(editor));
        ops.add(new CopyFrameAsJavaOperation(editor));
        ops.add(isFrameEnabled() ? new DisableFrameOperation(editor) : new EnableFrameOperation(editor));
        ops.add(new DeleteFrameOperation(editor));
        return ops;
    }

    /**
     * Gets the first cursor inside this frame (or null if none)
     */
    public final FrameCursor getFirstInternalCursor()
    {
        return getCanvases().map(FrameCanvas::getFirstCursor).findFirst().orElse(null);
    }
    
    /**
     * Gets the last cursor inside this frame (or null if none)
     */
    public final FrameCursor getLastInternalCursor()
    {
        return Utility.findLast(getCanvases().map(FrameCanvas::getLastCursor)).orElse(null);
    }

    /**
     * Gets the parent canvas of this frame (may be null)
     */
    public FrameCanvas getParentCanvas()
    {
        return parentCanvas;
    }

    /**
     * Sets the parent canvas of this frame.  Should only ever change
     * the parent once from null to non-null, and optionally once
     * back to null.
     */
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

    /**
     * Gets the cursor after this frame in the parent canvas.  Returns null if not in a parent canvas.
     */
    public final FrameCursor getCursorAfter() { return parentCanvas == null ? null : parentCanvas.getCursorAfter(this);}

    /**
     * Gets the cursor before this frame in the parent canvas.  Returns null if not in a parent canvas.
     */
    public final FrameCursor getCursorBefore() { return parentCanvas == null ? null : parentCanvas.getCursorBefore(this);}

    /**
     * Adds the given CSS style class to this frame
     */
    protected final void addStyleClass(String styleClass)
    {
        JavaFXUtil.addStyleClass(frameContents, styleClass);
    }

    /**
     * Remove the given CSS style class to this frame. (Eventually we should remove this in favour of using pseudo-classes).  
     */
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

    /**
     * Sets the drag source effect (i.e. notifies us if we are the source of a drag)
     */
    public void setDragSourceEffect(boolean on)
    {
        frameDragSourceProperty.set(on);
    }
    
    /**
     * Returns whether the frame is currently enabled.
     */
    public boolean isFrameEnabled()
    {
        return frameEnabledProperty.get();
    }

    /**
     * Sets the frame enabled state, if possible.  For example, you cannot enable
     * the child of a disabled frame.
     */
    public void setFrameEnabled(boolean enabled)
    {
        if (!canHaveEnabledState(enabled))
            return;

        frameEnabledProperty.set(enabled);

        // Bit ugly, but the only way to use setElementEnabled's default implementation
        // without making Frame (rather than subclasses) implement CodeFrame, which wouldn't
        // work because we'd lose the useful type parameter on CodeFrame
        if (this instanceof CodeFrame)
            ((CodeFrame<?>)this).setElementEnabled(enabled);
        
        if (!enabled)
        {
            // Get rid of all errors straight away:
            JavaFXUtil.runNowOrLater(() -> {
                flagErrorsAsOld();
                removeOldErrors();
            });
        }
        
        // When our status changes, copy that status to all children:
        getCanvases().forEach(canvas -> canvas.getBlocksSubtype(Frame.class).forEach(b -> b.setFrameEnabled(enabled)));

        // Updated the disabledRoot variable:
        updateAppearance(getParentCanvas());
    }

    /**
     * Checks if a frame can have the given enabled state.  This boils down to checking,
     * when given true, that all parents of this frame are enabled.
     */
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

    /**
     * Sets the frame enable state.
     */
    public void setFrameEnablePreview(FramePreviewEnabled state)
    {
        framePreviewEnableProperty.set(state);
    }

    /**
     * Flags all errors as old: frame errors and slot errors, to unlimited depth
     */
    @OnThread(Tag.FXPlatform)
    public final void flagErrorsAsOld()
    {
        allFrameErrors.forEach(CodeError::flagAsOld);
        getEditableSlotsDirect().forEach(EditableSlot::flagErrorsAsOld);
        getPossiblyHiddenSlotsDirect().forEach(EditableSlot::flagErrorsAsOld);
        getCanvases().forEach(FrameHelper::flagErrorsAsOld);
    }

    /**
     * Removes all errors flagged as old: frame errors and slot errors, to unlimited depth
     */
    @OnThread(Tag.FXPlatform)
    public final void removeOldErrors()
    {
        allFrameErrors.removeIf(CodeError::isFlaggedAsOld);
        getEditableSlotsDirect().forEach(EditableSlot::removeOldErrors);
        getPossiblyHiddenSlotsDirect().forEach(EditableSlot::removeOldErrors);
        getCanvases().forEach(FrameHelper::removeOldErrors);
    }
    
    /**
     * Public method called while frame is being removed.  Should remove any overlays, listeners, etc.
     * 
     * This version is final, to make sure all sub-items are cleaned up.  To provide cleanup specific
     * to this kind of frame, override the cleanupFrame method.
     */
    public final void cleanup()
    {
        getEditableSlots().forEach(EditableSlot::cleanup);
        getCanvases().forEach(FrameCanvas::cleanup);
        cleanupFrame();
    }

    /**
     * Override this method to provide cleanup specific to that type of frame
     */
    protected void cleanupFrame()
    {
        // Nothing to do by default
    }

    /**
     * Gets a stream of all directly contained canvases.
     */
    public final Stream<FrameCanvas> getCanvases()
    {
        return contents.stream().map(FrameContentItem::getCanvas).flatMap(Utility::streamOptional);
    }

    /**
     * Get only those canvases which are persistent (i.e. exclude generated items like inherited method canvas)
     */
    public Stream<FrameCanvas> getPersistentCanvases()
    {
        // By default, assume all canvases are persistent:
        return getCanvases();
    }

    // Currently unused frame folding:
    public boolean isCollapsible()
    {
        return false;
    }

    // Currently unused frame folding:
    public void setCollapsed(boolean collapse)
    {
        // Nothing to do unless we're collapsible
    }

    /**
     * When a frame loses focus, sometimes it checks for certain empty slots and removes them,
     * e.g. the throws declaration at the end of a method header.
     */
    public void checkForEmptySlot()
    {
        // Do nothing
    }

    /**
     * By default, no extensions: override to specify.
     *
     * @param innerCanvas The inner canvas which we are asking about extensions for.
     *                    If we are not asking for inner extensions (e.g. instead before, or after)
     *                    then will be null.
     * @param cursorInCanvas The cursor position in the inner canvas.  Will be non-null iff innerCanvas is non-null
     */
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas innerCanvas, FrameCursor cursorInCanvas)
    {
        if (innerCanvas != null)
            return Collections.emptyList();

        return Arrays.asList(new ExtensionDescription('\\', "Disable/Enable frames", () -> {
            if (canHaveEnabledState(isFrameEnabled()))
            {
                setFrameEnabled(!isFrameEnabled());
            }
        }, false, ExtensionSource.BEFORE, ExtensionSource.AFTER));
    }

    @OnThread(Tag.FXPlatform)
    public final boolean notifyKeyAfter(char c, RecallableFocus rc)
    {
        return notifyKey(c, rc, getAvailableExtensions(null, null), ExtensionSource.AFTER);
    }

    @OnThread(Tag.FXPlatform)
    public final boolean notifyKeyBefore(char c, RecallableFocus rc)
    {
        return notifyKey(c, rc, getAvailableExtensions(null, null), ExtensionSource.BEFORE);
    }

    @OnThread(Tag.FXPlatform)
    private final boolean notifyKey(char c, RecallableFocus rc, List<ExtensionDescription> extensions, ExtensionSource src)
    {
        List<ExtensionDescription> candidates = extensions.stream()
                .filter(e -> e.getShortcutKey() == c && e.validFor(src))
                .collect(Collectors.toList());
        
        if (candidates.size() == 0) {
            return false;
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Ambiguous " + src + " for: " + (int)c);
        }

        editor.beginRecordingState(rc);
        candidates.get(0).activate();
        editor.endRecordingState(rc);
        return true;
    }

    /**
     * Checks whether this frame can be dragged.  True by default.  Some frames cannot
     * be dragged, e.g. inherited frames or class frames.
     */
    public boolean canDrag()
    {
        return true; 
    }
    
    /**
     * Notifies about a left-click.  Returns true if we have consumed the click, false otherwise.
     */
    @OnThread(Tag.FXPlatform)
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

    // Part of unused code folding
    protected void addTopRight(Node n)
    {
        //TODO
        //headerRowComponents.add(FXCollections.observableArrayList(n));
    }

    /**
     * Gets the header row (for use in subclasses)
     * @return
     */
    protected final FrameContentRow getHeaderRow()
    {
        return header;
    }

    /** Automatically prepends the caption to the given items, if there is one */
    protected final void setHeaderRow(HeaderItem... headerItems)
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

    /**
     * Accessor for the editor that this frame lives in (never changes for a given frame)
     */
    public InteractionManager getEditor()
    {
        return editor;
    }
        
    /**
     * Called when you have clicked on the frame in a stack trace or want to jump to definition.
     */
    public void show(ShowReason reason)
    {
        switch (reason)  {
        case EXCEPTION:
            JavaFXUtil.setPseudoclass("bj-stack-highlight", true, getNode());
            getParentCanvas().getCursorBefore(this).requestFocus();
            editor.scrollTo(getNode(), -50.0);
            editor.registerStackHighlight(this);
            break;
        case LINK_TARGET:
            // Don't think we need runlater here, but better to be consistent:
            editor.scrollTo(getNode(), -50.0);
            focusName();
            break;
        }
    }

    /**
     * Focuses the name of the frame, or something close to that concept.
     */
    public void focusName()
    {
        // By default:
        focusWhenJustAdded();
    }

    /**
     * Removes the highlight which was added when showing this frame as a stack trace item
     */
    public void removeStackHighlight()
    {
        JavaFXUtil.setPseudoclass("bj-stack-highlight", false, getNode());
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

    /**
     * Gets only editable slots which are directly in this frame (not any in frames inside any canvases)
     * but which may be currently hidden (e.g. type name in class-extends when
     * focus is lost, or return value, etc)
     */
    public Stream<EditableSlot> getPossiblyHiddenSlotsDirect()
    {
        return Stream.empty();
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

    /**
     * Called when the user has pressed backspace at the start of a header item inside a frame content item.
     * @param srcRow The row in the frame where backspace was pressed
     * @param src The item in which backspace was pressed
     * @return True if we deleted ourselves in response, otherwise false
     */
    @OnThread(Tag.FXPlatform)
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

    /**
     * Called when the user has pressed delete at the end of a header item inside a frame content item.
     * @param srcRow The row in the frame where delete was pressed
     * @param src The item in which delete was pressed
     * @return True if we deleted ourselves in response, otherwise false
     */
    @OnThread(Tag.FXPlatform)
    public boolean deleteAtEnd(FrameContentItem srcRow, HeaderItem src)
    {
        return false;
    }

    /**
     * Pulls up a frames contents.  For example, where you delete an if but want to leave the content,
     * we call this method to pull up the content of each subcanvas to replace the if.
     * By default, we pull up each frame canvas's contents, with a blank frame between each.
     * 
     * This method does not delete ourselves, just does the pull-up.
     */
    public void pullUpContents()
    {
        editor.modifiedFrame(this, false); //notify the editor that a change has been occured. That will trigger a file save
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

    /**
     * Sets this frame to be fresh
     */
    @OnThread(Tag.FXPlatform)
    public void markFresh()
    {
        fresh.set(true);
    }

    /** Sets this frame to be non-fresh */
    @OnThread(Tag.FXPlatform)
    public void markNonFresh()
    {
        // Don't mark it non-fresh just because they've hit ctrl-space:
        if (!isShowingSuggestions())
            fresh.set(false);
    }

    @OnThread(Tag.FXPlatform)
    protected boolean isShowingSuggestions()
    {
        return getEditableSlots().anyMatch(s -> s instanceof StructuredSlot &&
                ((StructuredSlot<?,?,?>)s).isShowingSuggestions());
    }

    /** Checks wheter this frame is fresh */
    public boolean isFresh()
    {
        return fresh.get();
    }

    /**
     * Only valid when called on a fresh frame.  In the future, if/when this frame becomes
     * non-fresh, the given action will be executed.
     */
    public void onNonFresh(FXRunnable action)
    {
        if (!isFresh()) {
            throw new IllegalStateException("Calling onNonFresh when we are already non-fresh; state cannot go back to fresh");
        }
        JavaFXUtil.addSelfRemovingListener(fresh, b -> action.run());
    }

    /**
     * Read-only interface to the fresh property.
     */
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
     * @param oldView the view transferring from
     * @param newView the new view selected
     * @param animation A class to hook into to synchronise animations
     */
    @OnThread(Tag.FXPlatform)
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

    /**
     * Gets the frame error, if any, which is currently being shown.
     */
    public Stream<CodeError> getCurrentErrors()
    {
        return shownError.get() == null ? Stream.empty() : Stream.of(shownError.get());
    }

    /**
     * Adds the given frame error to this frame.  Does not necessarily show it (depends
     * on which other errors are present on this frame)
     */
    @OnThread(Tag.FXPlatform)
    public void addError(CodeError err)
    {
        allFrameErrors.add(err);
        err.bindFresh(fresh, editor);
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

    /**
     * Callback to be called once the frame is compiled.  Put here so subclasses can
     * override it.
     */
    @OnThread(Tag.FXPlatform)
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
        alwaysBeenBlank = alwaysBeenBlank && isAlmostBlank();
    }

    /**
     * Called when escape has been pressed within the frame.
     * @param srcRow The row in which escape was pressed
     * @param src The item in that row in which escape was pressed
     */
    @OnThread(Tag.FXPlatform)
    public void escape(FrameContentItem srcRow, HeaderItem src)
    {
        if (alwaysBeenBlank && isFresh())
        {
            // Delete ourselves
            FrameCanvas parentCanvas = getParentCanvas();
            FrameCursor cursorBefore = getCursorBefore();
            editor.recordEdits(StrideEditReason.FLUSH);
            parentCanvas.removeBlock(this);
            editor.recordEdits(StrideEditReason.ESCAPE_FRESH);
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
        double sceneMaxY = lastCanvas.getSceneBounds().getMaxY();

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
            Bounds canvasBounds = canvases.get(canvasIndex).getContentSceneBounds();

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
                final FrameCursor cursor = canvases.get(canvasIndex).findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend && !isClickOnMargin);
                if (cursor != null)
                    return cursor;
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
        // the side label -- therefore use canvas which includes margin:
        FrameCanvas lastCanvas = Utility.findLast(getCanvases()).orElse(null);
        return lastCanvas.getSceneBounds().getMaxY();
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
     * Returns true for all frames with effective code. I.e only false in frames such as BlankFrame and CommentFrame.
     *
    * @return True except for non-effective frames (codewise)
    */
    public boolean isEffectiveFrame()
    {
        return true;
    }

    public void setModifier(String name, boolean value)
    {
        if (modifiers.containsKey(name)) {
            modifiers.get(name).set(value);
        }
        else {
            Debug.reportError("No such modifier: " + name + " in Frame: " + this);
        }
    }

    public BooleanProperty getModifier(String name)
    {
        return modifiers.get(name);
    }

    public static enum ShowReason { EXCEPTION, LINK_TARGET }

    /**
     * Different view modes in Stride editor.
     */
    public enum View {
        // The normal Stride preview
        NORMAL("normal"),
        // Java preview in Stride editor
        JAVA_PREVIEW("java_preview"),
        // Birdseye view without documentation
        BIRDSEYE_NODOC("birdseye_nodoc"),
        // Birdseye view with documentation
        BIRDSEYE_DOC("birdseye_doc");

        private final String text;

        View(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }

        public boolean isBirdseye()
        {
            return this == BIRDSEYE_DOC || this == BIRDSEYE_NODOC;
        }
    }

    /**
     * Tracks the cause of a view mode change, i.e. what user interaction actually
     * caused the change.  A mouse click, a shortcut key, etc.
     */
    public enum ViewChangeReason
    {
        // View has returned to Normal because user clicked on the mouse button.
        MOUSE_CLICKED("mouse_clicked"),
        // View has returned to Normal because user pressed Enter.
        KEY_PRESSED_ENTER("key_pressed_enter"),
        // View has returned to Normal because user pressed Escape.
        KEY_PRESSED_ESCAPE("key_pressed_escape"),
        // The check menu or its accelerator is used to toggle between view modes.
        // We don't have a straightforward way to actually differentiate between the accelerator and
        // actual clicking on the menu. Also, there is no big need to differentiate.
        MENU_OR_SHORTCUT("menu_or_shortcut");

        private final String text;

        ViewChangeReason(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }
    }

    protected static Node getHeaderNodeOf(Frame f)
    {
        return f.header.getNode();
    }

    /**
     * When we are deciding whether to display a message about a frame deletion that the user may
     * want to undo, we only want to show the message for "large" frames.  So we don't show it
     * for say a single assignment frame, or perhaps even an if frame with a single method call,
     * but we do show it for a method or other container with several inner frames.  To measure
     * this, we use the idea of "effort": how many keystrokes (roughly) it would take to recreate
     * a particular piece of code.
     *
     * This method performs the calculation of effort.  See comments inside for how it works
     *
     * @return The effort required to (re-)create this frame and all inner frames.
     */
    public int calculateEffort()
    {
        // One to create the frame in the first place:
        int effort = 1;
        // Plus the effort to create the content of our direct slots:
        effort += getEditableSlotsDirect().mapToInt(EditableSlot::calculateEffort).sum();
        // Plus the effort to create our contained frames:
        effort += getAllFrames().filter(f -> f != this).mapToInt(Frame::calculateEffort).sum();
        // We omit how much effort it was to add extensions, etc: this only needs to be a rough calculation.
        return effort;
    }

    /**
     * Called when the editor font size has changed to redraw any overlays
     */
    @OnThread(Tag.FXPlatform)
    public void fontSizeChanged()
    {
        header.fontSizeChanged();
    }
}
