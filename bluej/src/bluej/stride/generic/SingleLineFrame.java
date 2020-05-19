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
package bluej.stride.generic;

import bluej.Config;
import bluej.collect.StrideEditReason;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.InterfaceFrame;
import bluej.utility.javafx.AbstractOperation.Combine;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SingleLineFrame extends Frame
{
    protected final SlotLabel previewSemi = new SlotLabel("", "preview-semi") {
        @Override
        public void setView(View oldView, View v, SharedTransition animate)
        {
            if (v == View.JAVA_PREVIEW) {
                setText(";");

            /*RotateTransition rt = new RotateTransition(Duration.millis(1000), previewSemi.getNode());
            rt.setFromAngle(180.0);
            rt.setToAngle(360.0);
            rt.play();*/
                getNode().opacityProperty().bind(animate.getProgress());
                animate.addOnStopped(() -> getNode().opacityProperty().unbind());
            }
            else {
                setText("");
            }
        }
    }; 
    private final ObservableList<FrameState> recentValues = FXCollections.observableArrayList();

    public SingleLineFrame(InteractionManager editor, String caption, String stylePrefix)
    {
        super(editor, caption, stylePrefix);
    }
    
    @Override
    public double lowestCursorY()
    {
        Bounds sceneBounds = getNode().localToScene(getNode().getBoundsInLocal());
        return sceneBounds.getMaxY();
    }
    
    @Override
    public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
    {
        Bounds headBounds = getHeaderRow().getSceneBounds();
            
        // Slight bias towards dropping before:
        if (sceneY <= 2 + (headBounds.getMinY() + headBounds.getMaxY()) / 2) {
            return prevCursor;
        }
        return nextCursor;
    }

    protected boolean isInInterface(FrameCanvas parentCanvas)
    {
        if (parentCanvas == null) {
            bluej.utility.Debug.printCallStack("parentCanvas shouldn't be null");
            return false;
        }
        return parentCanvas.getParent() instanceof InterfaceFrame;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> ops = new ArrayList<>(super.getContextOperations());

        int i = 0;
        for (FrameState state : recentValues)
        {
            // The first value in the list should always be the most current value,
            // so we don't actually display that on the menu.  (But we keep it because
            // once there's a change, it will become index 1, and thus shown)
            if (i > 0)
            {
                ops.add(new FrameOperation(getEditor(), "revert" + i, Combine.ONE)
                {
                    @Override
                    @OnThread(Tag.FXPlatform)
                    protected void execute(List<Frame> frames)
                    {
                        editor.recordEdits(StrideEditReason.FLUSH);
                        getParentCanvas().replaceBlock(SingleLineFrame.this, Loader.loadElement(state.value).createFrame(editor));
                        editor.recordEdits(StrideEditReason.UNDO_LOCAL);
                    }

                    @Override
                    public List<ItemLabel> getLabels()
                    {
                        return Arrays.asList(l(Config.getString("frame.slot.recent"), MenuItemOrder.RECENT_VALUES), l("0", MenuItemOrder.RECENT_VALUES));
                    }

                    @Override
                    protected CustomMenuItem initializeCustomItem()
                    {
                        ImageView view = new ImageView(state.picture);
                        CustomMenuItem item = new CustomMenuItem(view);
                        return item;
                    }

                    @Override
                    public boolean onlyOnContextMenu()
                    {
                        return true;
                    }
                });
            }
            i += 1;
        }
        return ops;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void lostFocus()
    {
        super.lostFocus();
        saveAsRecent();
    }

    @OnThread(Tag.FXPlatform)
    protected void saveAsRecent()
    {
        if (!(this instanceof CodeFrame))
            return;

        CodeElement code = ((CodeFrame)this).getCode();
        if (code == null)
            return;
        Element el = code.toXML();
        String xml = el.toXML();
        int existingRecent = Utility.findIndex(recentValues, fs -> fs.cachedXML.equals(xml));
        if (existingRecent != -1)
        {
            // Just need to re-order the list.  Don't need to consider length as it won't change
            FrameState fs = recentValues.remove(existingRecent);
            recentValues.add(0, fs);
        }
        else
        {
            JavaFXUtil.runAfterCurrent(() -> {
                Image pic = takeShot(Arrays.asList(this), null);
                FrameState s = new FrameState(pic, el, xml);
                // No need to worry about duplicates because we've checked that already
                // Add to front of list:
                recentValues.add(0, s);
                // Trim to 3:
                while (recentValues.size() > 4)
                    recentValues.remove(4);
            });
        }
    }

    private static class FrameState
    {
        public final Element value;
        public final String cachedXML;
        public final Image picture;

        public FrameState(Image picture, Element value, String xml)
        {
            this.picture = picture;
            // We must take a copy because value is probably a LocatableElement, which retains
            // a lot of references which we don't want to keep alive, we just need the XML part:
            this.value = new Element(value);
            this.cachedXML = xml;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FrameState state = (FrameState) o;

            return cachedXML.equals(state.cachedXML);

        }

        @Override
        public int hashCode()
        {
            return cachedXML.hashCode();
        }
    }
}
