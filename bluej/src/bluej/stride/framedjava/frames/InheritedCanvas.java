package bluej.stride.framedjava.frames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;

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
 * Created by neil on 01/12/15.
 */
class InheritedCanvas
{
    private ClassFrame classFrame;
    public final String superClassName;
    public final FrameCanvas canvas;
    public final FrameContentRow precedingDivider;
    public final SlotLabel precedingDividerLabel;
    public final TriangleLabel optionalCollapse;

    // Note that java.lang.Object is treated as a special case: it gets a triangle label
    public InheritedCanvas(ClassFrame classFrame, InteractionManager editor, String superClassName, boolean single)
    {
        this.classFrame = classFrame;
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
                    public boolean canInsert(GreenfootFrameCategory category)
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
            public List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor)
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
        if (single)
        {
            this.precedingDividerLabel = null;
            this.precedingDivider = null;
            this.optionalCollapse = null;
        } else
        {
            if (superClassName.equals("java.lang.Object"))
            {
                this.precedingDividerLabel = new SlotLabel("Inherited from Object", "class-inherited-label");
                this.optionalCollapse = new TriangleLabel(editor, t -> canvas.growUsing(t.getProgress()), t -> canvas.shrinkUsing(t.getOppositeProgress()), new SimpleBooleanProperty(false));
                this.precedingDivider = new FrameContentRow(classFrame, precedingDividerLabel, optionalCollapse);
            } else
            {
                this.precedingDividerLabel = new SlotLabel("Inherited from " + superClassName, "class-inherited-label");
                this.precedingDivider = new FrameContentRow(classFrame, precedingDividerLabel);
                this.optionalCollapse = null;
            }
        }
    }

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

    public void shrink(SharedTransition t)
    {
        canvas.shrinkUsing(t.getOppositeProgress());
        if (precedingDividerLabel != null)
            precedingDividerLabel.shrinkVertically(t);
        if (optionalCollapse != null)
            optionalCollapse.setVisible(false);
    }
}
