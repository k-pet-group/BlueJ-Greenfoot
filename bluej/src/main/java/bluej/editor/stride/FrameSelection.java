/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg

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

import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.AbstractOperation.MenuItems;
import bluej.stride.slots.EditableSlot.TopLevelMenu;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;

/**
 * A class for keeping track of the current frame selection.  A frame selection is
 * a contiguous group of frames in a single canvas, and is represented graphically
 * by a rectangle around the frames.
 */
@OnThread(Tag.FXPlatform)
public class FrameSelection
{
    @OnThread(Tag.FXPlatform)
    private final ObservableList<Frame> selection = FXCollections.observableList(new ArrayList<>());
    private final Canvas selectionHighlight = new Canvas();
    private final InteractionManager editor;
    private boolean deletePreview;
    private boolean pullUpPreview;

    @OnThread(Tag.FX)
    public FrameSelection(InteractionManager editor)
    {
        this.editor = editor;
        selectionHighlight.setMouseTransparent(true);
        deletePreview = false;
        pullUpPreview = false;

        // To update the position of the rectangle, we added a listener to the list of frames,
        // and the position of each frame within the list, by using a MultiListener:

        Function<Frame, MultiListener.RemoveAndUpdate> removeAndUpdate = f -> {
            FXRunnable removeA = JavaFXUtil.addChangeListener(f.getNode().localToSceneTransformProperty(), x -> JavaFXUtil.runNowOrLater(() -> redraw()));
            FXRunnable removeB = JavaFXUtil.addChangeListener(f.getNode().boundsInLocalProperty(), x -> JavaFXUtil.runNowOrLater(() -> redraw()));
            return JavaFXUtil.sequence(removeA, removeB)::run;
        };

        MultiListener<Frame> positionListener = new MultiListener<Frame>(removeAndUpdate);

        addChangeListener(() -> {
            redraw();
            positionListener.listenOnlyTo(selection.stream());
        });
    }

    /**
     * Recalculates position of selection rectangle (or removes it if selection has become empty)
     */
    @OnThread(Tag.FXPlatform)
    private void redraw()
    {
        editor.getCodeOverlayPane().removeOverlay(selectionHighlight);
        if (!selection.isEmpty())
        {
            // Re-add each time as position may have changed:
            editor.getCodeOverlayPane().addOverlay(selectionHighlight, selection.get(0).getNode(), null, null);
            Node topNode = selection.get(0).getNode();
            Node bottomNode = selection.get(selection.size() - 1).getNode();
            selectionHighlight.setWidth(topNode.getBoundsInParent().getWidth());
            selectionHighlight.setHeight(bottomNode.getBoundsInParent().getMinY() + bottomNode.getLayoutBounds().getHeight() - topNode.getBoundsInParent().getMinY());

            GraphicsContext gc = selectionHighlight.getGraphicsContext2D();
            gc.clearRect(0, 0, selectionHighlight.getWidth(), selectionHighlight.getHeight());

            if (deletePreview || pullUpPreview)
            {
                gc.setFill(new Color(1, 0.4, 0.4, 0.7));

                if (pullUpPreview)
                {
                    // This fill rule means that we can have one path for the outer bounds of the frame,
                    // then several bounds for the inner canvases, and then the frame will be filled, except for
                    // the inner canvases.
                    gc.setFillRule(FillRule.EVEN_ODD);
                    for (Frame f : selection)
                    {
                        gc.beginPath();
                        // We first draw a path for the outer frame:
                        roundedRectPath(gc, 0.5, 0.5, selectionHighlight.getWidth() - 1, selectionHighlight.getHeight() - 1, 7);
                        for (FrameCanvas c : Utility.iterableStream(f.getCanvases()))
                        {
                            Bounds sceneBounds = c.getContentSceneBounds();
                            Bounds b = selectionHighlight.sceneToLocal(sceneBounds);
                            // We then draw paths for each inner canvas:
                            roundedRectPath(gc, b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(), 5);
                        }
                        // Then fill and clear the path:
                        gc.fill();
                        gc.closePath();
                    }
                }
                else
                {
                    gc.fillRoundRect(0.5, 0.5, selectionHighlight.getWidth() - 1, selectionHighlight.getHeight() - 1, 7, 7);
                }
            }

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeRoundRect(0.5, 0.5, selectionHighlight.getWidth() - 1, selectionHighlight.getHeight() - 1, 7, 7);
        }
    }

    /**
     * Draws a rounded rectangle path with the given extents and corner radius:
     */
    private void roundedRectPath(GraphicsContext gc, double minX, double minY, double width, double height, double arc)
    {
        gc.moveTo(minX + arc, minY);
        // top and top-right:
        gc.lineTo(minX + width - arc, minY);
        gc.arcTo(minX + width, minY, minX + width, minY + arc, arc);
        // right and bottom-right:
        gc.lineTo(minX + width, minY + height - arc);
        gc.arcTo(minX + width, minY + height, minX + width - arc, minY + height, arc);
        // bottom and bottom-left:
        gc.lineTo(minX + arc, minY + height);
        gc.arcTo(minX, minY + height, minX, minY + height - arc, arc);
        // left and top-left:
        gc.lineTo(minX, minY + arc);
        gc.arcTo(minX, minY, minX + arc, minY, arc);

    }

    @OnThread(Tag.FXPlatform)
    public void clear()
    {
        selection.clear();
    }

    @OnThread(Tag.FXPlatform)
    public boolean contains(Frame f)
    {
        return selection.contains(f);
    }

    @OnThread(Tag.FXPlatform)
    public List<Frame> getSelected()
    {
        return Collections.unmodifiableList(selection);
    }

    /**
     * The user has moved the frame cursor down while holding shift
     * Either add or remove from selection
     */
    @OnThread(Tag.FXPlatform)
    public void toggleSelectDown(Frame f)
    {
        if (f == null)
        {
            return;
        }
        
        if (selection.size() > 0 && selection.get(0) == f)
        {
            // We are doing shift-down while at top of selection; remove top item:
            selection.remove(0);
        }
        else
        {
            // We are doing shift-down at end of selection; add to end:
            selection.add(f);
        }
    }

    /**
     * The user has moved the frame cursor up while holding shift
     * Either add or remove from selection
     */
    @OnThread(Tag.FXPlatform)
    public void toggleSelectUp(Frame f)
    {
        if (f == null)
        {
            return;
        }
        
        if (selection.size() > 0 && selection.get(selection.size() - 1) == f)
        {
            // We are doing shift-up while at bottom of selection; remove last item:
            selection.remove(selection.size() - 1);
        }
        else {
            // We are doing shift-up at top of selection; add to beginning:
            selection.add(0, f);
        }
    }

    /**
     * Gets the edit menu items for selected frames
     */
    public Map<TopLevelMenu, MenuItems> getEditMenuItems(boolean contextMenu)
    {
        return Collections.singletonMap(TopLevelMenu.EDIT, AbstractOperation.getMenuItems(this.selection, contextMenu));
    }

    public ContextMenu getContextMenu()
    {
        MenuItems ops = AbstractOperation.getMenuItems(this.selection, true);
        if (ops.isEmpty()) {
            return null;
        }
        return AbstractOperation.MenuItems.makeContextMenu(Collections.singletonMap(TopLevelMenu.EDIT, ops));
    }

    @OnThread(Tag.FXPlatform)
    public void setDeletePreview(boolean deletePreview)
    {
        //JavaFXUtil.selectStyleClass(deletePreview ? 1 : 0, selectionHighlight, "selection-highlight-normal", "selection-highlight-delete");
        this.deletePreview = deletePreview;
        this.pullUpPreview = false;
        redraw();
    }

    @OnThread(Tag.FXPlatform)
    public void setPullUpPreview(boolean pullUpPreview)
    {
        this.deletePreview = false;
        this.pullUpPreview = pullUpPreview;
        redraw();
    }

    @OnThread(Tag.FXPlatform)
    public void set(List<Frame> frames)
    {
        selection.clear();
        selection.setAll(frames);
    }

    @OnThread(Tag.FXPlatform)
    public boolean isEmpty()
    {
        return selection.isEmpty();
    }

    @OnThread(Tag.FX)
    public void addChangeListener(FXPlatformRunnable listener)
    {
        JavaFXUtil.runNowOrLater(() -> selection.addListener((ListChangeListener<Frame>)c -> listener.run()));
    }

    @OnThread(Tag.FXPlatform)
    public FrameCursor getCursorAfter()
    {
        if (selection.size() == 0)
            return null;
        else
            return (selection.get(selection.size() - 1).getCursorAfter());
    }

    @OnThread(Tag.FXPlatform)
    public FrameCursor getCursorBefore()
    {
        if (selection.size() == 0)
            return null;
        else
            return (selection.get(0).getCursorBefore());
    }

    @OnThread(Tag.FXPlatform)
    public boolean executeKey(FrameCursor cursor, final char key)
    {
        // If there is only on selected frame and it accept the key typed as an extension
        if (selection.size() == 1) {
            for (ExtensionDescription extension : selection.get(0).getAvailableExtensions(null, null)) {
                if (extension.getShortcutKey() == key &&
                        (extension.validFor(ExtensionSource.AFTER)
                        || extension.validFor(ExtensionSource.BEFORE)
                        || extension.validFor(ExtensionSource.SELECTION)))
                {
                    extension.activate();
                    return true;
                }
            }
        }

        // To disable
        if (key == '\\') {
            // If all disabled, enabled all. Otherwise, disable all.
            boolean allDisabled = getCanHaveEnabledState(false).allMatch(f -> !f.isFrameEnabled());

            // TODO Refactor the Enable/Disable FrameOperations to make them more consistent and use them instead of next lines
            editor.beginRecordingState(cursor);
            getCanHaveEnabledState(allDisabled ? true : false).forEach(t -> t.setFrameEnabled(allDisabled ? true : false));
            editor.endRecordingState(cursor);

            return true;
        }

        // Check that all the frames have exactly one extension for that key:
        if (getNonIgnored().allMatch(f -> f.getAvailableExtensions(null, null).stream()
                .filter(m -> m.validFor(ExtensionSource.SELECTION) && m.getShortcutKey() == key).count() == 1)) {
            getNonIgnored().flatMap(f -> f.getAvailableExtensions(null, null).stream())
                    .filter(m -> m.validFor(ExtensionSource.SELECTION) && m.getShortcutKey() == key)
                    .findAny().ifPresent(e -> e.activate(getNonIgnored().collect(Collectors.toList())));
            return true;
        }

        return false;
    }

    private Stream<Frame> getCanHaveEnabledState(boolean state)
    {
        return selection.stream().filter(f -> f.canHaveEnabledState(state));
    }

    private Stream<Frame> getNonIgnored()
    {
        return selection.stream().filter(f -> f.isEffectiveFrame());
    }
}
