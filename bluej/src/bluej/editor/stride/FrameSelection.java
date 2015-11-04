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
package bluej.editor.stride;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.operations.AbstractOperation.ItemLabel;
import bluej.stride.slots.EditableSlot.SortedMenuItem;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.MultiListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.operations.AbstractOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot.MenuItems;
import bluej.stride.slots.EditableSlot.TopLevelMenu;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;

/**
 * A class for keeping track of the current frame selection.  A frame selection is
 * a contiguous group of frames in a single canvas, and is represented graphically
 * by a rectangle around the frames.
 */
public class FrameSelection
{
    private final ObservableList<Frame> selection = FXCollections.observableList(new ArrayList<Frame>());
    private final Canvas selectionHighlight = new Canvas();
    private final InteractionManager editor;
    private boolean deletePreview;
    private boolean pullUpPreview;

    public FrameSelection(InteractionManager editor)
    {
        this.editor = editor;
        selectionHighlight.setMouseTransparent(true);
        deletePreview = false;
        pullUpPreview = false;

        // To update the position of the rectangle, we added a listener to the list of frames,
        // and the position of each frame within the list, by using a MultiListener:

        Function<Frame, MultiListener.RemoveAndUpdate> removeAndUpdate = f -> {
            FXRunnable removeA = JavaFXUtil.addChangeListener(f.getNode().localToSceneTransformProperty(), x -> redraw());
            FXRunnable removeB = JavaFXUtil.addChangeListener(f.getNode().boundsInLocalProperty(), x -> redraw());
            return JavaFXUtil.sequence(removeA, removeB)::run;
        };

        MultiListener<Frame> positionListener = new MultiListener<Frame>(removeAndUpdate);

        selection.addListener((ListChangeListener<Frame>) arg -> {
            redraw();
            positionListener.listenOnlyTo(selection.stream());
        });
    }

    /**
     * Recalculates position of selection rectangle (or removes it if selection has become empty)
     */
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

    public void clear()
    {
        selection.clear();
    }
    public boolean contains(Frame f)
    {
        return selection.contains(f);
    }

    public List<Frame> getSelected()
    {
        return Collections.unmodifiableList(selection);
    }

    /**
     * The user has moved the frame cursor down while holding shift
     * Either add or remove from selection
     */
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
     * Gets the context menu items which are valid across the whole selection
     */
    public MenuItems getMenuItems(boolean contextMenu)
    {
        if (selection.size() == 0) {
            return new MenuItems(FXCollections.observableArrayList());
        }
        else if (selection.size() == 1) {
            // Everything appears as-is in a selection of size 1:
            return asMenuItems(selection.get(0).getContextOperations(editor), 0, contextMenu);
        }

        HashMap<String, List<FrameOperation>> ops = new HashMap<>();
        for (Frame f : selection) {
            for (FrameOperation op : f.getContextOperations(editor)) {
                if (!ops.containsKey(op.getIdentifier())) {
                    ops.put(op.getIdentifier(), new ArrayList<>());
                }
                ops.get(op.getIdentifier()).add(op);
            }
        }

        List<FrameOperation> r = new ArrayList<>();

        for (final List<FrameOperation> opEntry : ops.values()) {
            // If all blocks had this operation:
            FrameOperation frameOperation = opEntry.get(0);
            if ((frameOperation.combine() == AbstractOperation.Combine.ALL && opEntry.size() == selection.size())
                    || frameOperation.combine() == AbstractOperation.Combine.ANY
                    || (frameOperation.combine() == AbstractOperation.Combine.ONE && selection.size() == 1)) {
                r.add(frameOperation);
            }
        }
        return asMenuItems(r, 0, contextMenu);
    }

    /**
     * Gets the edit menu items for selected frames
     */
    public Map<TopLevelMenu, MenuItems> getEditMenuItems(boolean contextMenu)
    {
        return Collections.singletonMap(TopLevelMenu.EDIT, getMenuItems(contextMenu));
    }

    private static MenuItems asMenuItems(List<FrameOperation> originalOps, int depth, boolean contextMenu)
    {
        // Only keep ones that fit context menu flag:
        List<FrameOperation> ops = originalOps.stream().filter(op -> contextMenu || !op.onlyOnContextMenu()).collect(Collectors.toList());

        List<SortedMenuItem> r = new ArrayList<>();
        Set<ItemLabel> subMenuNames = ops.stream().filter(op -> op.getLabels().size() > depth + 1).map(op -> op.getLabels().get(depth)).collect(Collectors.toSet());
        subMenuNames.forEach(subMenuName -> {
            final MenuItems menuItems = asMenuItems(ops.stream().filter(op -> op.getLabels().get(depth).equals(subMenuName)).collect(Collectors.toList()), depth + 1, contextMenu);
            Menu subMenu = menuItems.makeSubMenu();
            subMenu.textProperty().bind(subMenuName.getLabel());
            r.add(subMenuName.getOrder().item(subMenu));
        });
        
        List<FrameOperation> opsAtRightLevel = ops.stream().filter(op -> op.getLabels().size() == depth + 1).collect(Collectors.toList());

        Map<FrameOperation, SortedMenuItem> opsAtRightLevelItems = new IdentityHashMap<>();

        for (FrameOperation op : opsAtRightLevel)
        {
            SortedMenuItem item = op.getMenuItem(contextMenu);
            r.add(item);
            opsAtRightLevelItems.put(op, item);
        }
        
        return new MenuItems(FXCollections.observableArrayList(r)) {

            @Override
            public void onShowing()
            {
                opsAtRightLevel.forEach(op -> {
                    final SortedMenuItem sortedMenuItem = opsAtRightLevelItems.get(op);
                    final MenuItem item = sortedMenuItem.getItem();
                    if (item instanceof CustomMenuItem)
                        op.onMenuShowing((CustomMenuItem) item);
                });
            }

            @Override
            public void onHidden()
            {
                opsAtRightLevel.forEach(op -> {
                    final SortedMenuItem sortedMenuItem = opsAtRightLevelItems.get(op);
                    final MenuItem item = sortedMenuItem.getItem();
                    if (item instanceof CustomMenuItem)
                        op.onMenuHidden((CustomMenuItem) item);
                });
            }
            
        };
    }

    public ContextMenu getContextMenu()
    {
        MenuItems ops = getMenuItems(true);
        if (ops.isEmpty()) {
            return null;
        }
        return MenuItems.makeContextMenu(Collections.singletonMap(TopLevelMenu.EDIT, ops));
    }
    
    public void setDeletePreview(boolean deletePreview)
    {
        //JavaFXUtil.selectStyleClass(deletePreview ? 1 : 0, selectionHighlight, "selection-highlight-normal", "selection-highlight-delete");
        this.deletePreview = deletePreview;
        this.pullUpPreview = false;
        redraw();
    }

    public void setPullUpPreview(boolean pullUpPreview)
    {
        this.deletePreview = false;
        this.pullUpPreview = pullUpPreview;
        redraw();
    }

    public void set(List<Frame> frames)
    {
        selection.clear();
        selection.setAll(frames);
    }

    public boolean isEmpty()
    {
        return selection.isEmpty();
    }

    public void addChangeListener(ListChangeListener<Frame> listener)
    {
        selection.addListener(listener);
    }

    public FrameCursor getCursorAfter()
    {
        if (selection.size() == 0)
            return null;
        else
            return (selection.get(selection.size() - 1).getCursorAfter());
    }

    public FrameCursor getCursorBefore()
    {
        if (selection.size() == 0)
            return null;
        else
            return (selection.get(0).getCursorBefore());
    }
}
