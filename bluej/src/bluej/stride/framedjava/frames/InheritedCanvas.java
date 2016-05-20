/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016  Michael Kolling and John Rosenberg

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
package bluej.stride.framedjava.frames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;

import bluej.Config;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameTypeCheck;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.TriangleLabel;
import bluej.utility.javafx.SharedTransition;

/**
 * A class to manage each canvas in the inherited items.
 * 
 * This class keeps track of the canvas with the inherited methods/fields in it, and also
 * the divider which occurs beforehand.
 */
class InheritedCanvas
{
    // The name of the super-class which these inherited items come from:
    public final String superClassName;
    // The canvas containing the inherited items:
    public final FrameCanvas canvas;
    // The row before the canvas, containing the label and optional collapse triangle.
    // Will be null (missing) if there is only one super-class.
    public final FrameContentRow precedingDivider;
    // The label with the super class name in:
    public final SlotLabel precedingDividerLabel;
    // The triangle to open/collapse the section.  Is null for all cases except java.lang.Object.
    public final TriangleLabel optionalCollapse;

    // Note that java.lang.Object is treated as a special case: it gets a triangle label
    public InheritedCanvas(ClassFrame classFrame, InteractionManager editor, String superClassName, boolean single)
    {
        this.canvas = new FrameCanvas(editor, new CanvasParent()
        {
            @Override
            public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
            {
                return null;
            }

            @Override
            public FrameTypeCheck check(FrameCanvas canvasBase)
            {
                return new FrameTypeCheck() {
                    @Override
                    public boolean canInsert(StrideCategory category)
                    {
                        // Can never insert manually into inherited canvas:
                        return false;
                    }

                    @Override
                    public boolean canPlace(Class<? extends Frame> type)
                    {
                        return Arrays.asList(InheritedMethodFrame.class, InheritedFieldFrame.class).contains(type);
                    }
                };  
            }

            @Override
            public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursor)
            {
                return Collections.emptyList();
            }

            @Override
            public Frame getFrame()
            {
                return classFrame;
            }

            @Override
            public InteractionManager getEditor()
            {
                return editor;
            }

            @Override
            public void modifiedCanvasContent()
            {
                // No need to do anything on modification, as it was programmatic
            }
        }, "class-inherited-")
        {

            @Override
            public FrameCursor findClosestCursor(double sceneX, double sceneY, List<Frame> exclude, boolean isDrag, boolean canDescend)
            {
                return null;
            }

            @Override
            public FrameCursor getFirstCursor()
            {
                return null;
            }

            @Override
            public FrameCursor getLastCursor()
            {
                return null;
            }
        };
        this.superClassName = superClassName;
        // If we are the only super-class, don't show an extra header:
        if (single)
        {
            this.precedingDividerLabel = null;
            this.precedingDivider = null;
            this.optionalCollapse = null;
        } else
        {
            if (superClassName.equals("java.lang.Object"))
            {
                String text = Config.getString("frame.class.inherited.from").replace("$", "Object");
                this.precedingDividerLabel = new SlotLabel(text, "class-inherited-label");
                this.optionalCollapse = new TriangleLabel(editor, t -> canvas.growUsing(t.getProgress()), t -> canvas.shrinkUsing(t.getOppositeProgress()), new SimpleBooleanProperty(false));
                this.precedingDivider = new FrameContentRow(classFrame, precedingDividerLabel, optionalCollapse);
            }
            else
            {
                String text = Config.getString("frame.class.inherited.from").replace("$", superClassName);
                this.precedingDividerLabel = new SlotLabel(text, "class-inherited-label");
                this.precedingDivider = new FrameContentRow(classFrame, precedingDividerLabel);
                this.optionalCollapse = null;
            }
        }
    }

    // Animates the growth of the inherited canvas from nothing to fully shown:
    public void grow(SharedTransition t)
    {
        if (optionalCollapse == null || optionalCollapse.expandedProperty().get())
            canvas.growUsing(t.getProgress());
        if (precedingDividerLabel != null)
        {
            precedingDividerLabel.growVertically(t);
            precedingDividerLabel.setLeftPadding(this.canvas.leftMargin().get());
        }
        if (optionalCollapse != null)
            optionalCollapse.setVisible(true);
    }

    // Animates the shrinking of the inherited canvas from fully shown to nothing:
    public void shrink(SharedTransition t)
    {
        canvas.shrinkUsing(t.getOppositeProgress());
        if (precedingDividerLabel != null)
            precedingDividerLabel.shrinkVertically(t);
        if (optionalCollapse != null)
            optionalCollapse.setVisible(false);
    }
}
