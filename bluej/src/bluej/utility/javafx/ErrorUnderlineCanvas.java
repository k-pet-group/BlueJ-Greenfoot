/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.stride.framedjava.slots.TextOverlayPosition.Line;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * There are several points in the interface where we need to show
 * red squiggly error underlines, and/or straight hyperlink underlines
 * on top of some other text.  This ErrorUnderlineCanvas class manages
 * the display of those errors and underlines.
 */
public class ErrorUnderlineCanvas
{
    /**
     * By default, canvases are not resizeable.  We need to be resizeable so that
     * when we are put in a StackPane in front of another node (our usual use-case),
     * we always resize to match the size of the underlying node.
     */
    private final Canvas canvas;

    /**
     * A class to hold information about a hyperlink.
     */
    private static class HyperlinkInfo
    {
        /** More detailed information (see UnderlineInfo) */
        private final UnderlineInfo positionInfo;
        /** The position of the start of the link, in characters */
        private final int start;
        /** The position of the end of the link, in characters */
        private final int end;
        /** The action to take if the user clicks the link */
        private final FXPlatformRunnable onClick;
        /** Whether the link is currently showing or not */
        private boolean showing = true;
        private HyperlinkInfo(UnderlineInfo positionInfo, int start, int end,
                FXPlatformRunnable onClick)
        {
            this.positionInfo = positionInfo;
            this.start = start;
            this.end = end;
            this.onClick = onClick;
        }
    }

    /**
     * A class to hold information about an error underline.
     */
    private static class ErrorInfo
    {
        /** The source slot, used to given more information about the error's position */
        private final UnderlineInfo positionInfo;
        /** The position of the start of the error underline, in characters */
        private final int start;
        /** The position of the end of the error underline, in characters */
        private final int end;
        /** Whether the position is relative to the original Java source,
         * or to the Stride source.  This matters when you have things like
         * the <: operator, which becomes instanceof in the Java source, and thus
         * affects the positions differently.  Generally, Java positions will
         * probably have come from a javac compiler error and Stride positions will
         * have come from an early error.
         */
        private final boolean javaPos;
        /**
         * The action to execute when the user moves the mouse in (pass true) to
         * hover over a link, and when the user moves the mouse out (pass false) again.
         */
        private final FXPlatformConsumer<Boolean> onHover;
        private ErrorInfo(UnderlineInfo slot, int start, int end,
                boolean javaPos, FXPlatformConsumer<Boolean> onHover)
        {
            this.positionInfo = slot;
            this.start = start;
            this.end = end;
            this.javaPos = javaPos;
            this.onHover = onHover;
        }
    }

    /**
     * More information about the position of underline extents 
     */
    public static interface UnderlineInfo
    {
        /**
         * Given a position (and whether it is a Java position or not),
         * gives back the corresponding TextOverlayPosition
         */
        public TextOverlayPosition getOverlayLocation(int caretPos, boolean javaPos);

        /**
         * Gets all the lines between two given positions (which may be java positions or not).
         * 
         * Default implementation assumes a single text-line slot, and thus
         * just delegates to getOverlayLocation for each end.  Override for multi-line slots.
         */
        default public List<Line> getAllLines(int start, int end, boolean javaPos)
        {
            return TextOverlayPosition.groupIntoLines(Arrays.asList(getOverlayLocation(start, javaPos), getOverlayLocation(end, javaPos)));
        }
    }

    /**
     * An error which will be added to the errors list when, and only when,
     * the specified boolean property changes.  Generally used to add an error
     * once a frame becomes non-fresh.
     */
    private class PendingError implements ChangeListener<Boolean>
    {
        /**
         *  The value to listen to for changes.  On first change, we will add the error to the errors list.
         */
        private final ObservableValue<Boolean> prop;
        /**
         * The error to add to the list once prop changes.
         */
        private final ErrorInfo error;

        public PendingError(ObservableValue<Boolean> prop, ErrorInfo err)
        {
            this.prop = prop;
            this.error = err;
            prop.addListener(this);
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> a, Boolean b, Boolean c)
        {
            errors.add(error);
            JavaFXUtil.runNowOrLater(() -> redraw());
            cancel();
        }

        public void cancel()
        {
            prop.removeListener(this);
        }
    }
    
    /** All the hyper links in the canvas (may not actually be drawn, but are being tracked) */
    private final List<HyperlinkInfo> hyperlinks = new ArrayList<>();
    /** All the errors in the canvas (all will be drawn) */
    private final List<ErrorInfo> errors = new ArrayList<>();

    /** Any extra drawing actions to take when redrawing the canvas.
     * This includes things like drawing on selection in expression slots,
     * or drawing fake carets during code completion.
     */
    @OnThread(value = Tag.FX, requireSynchronized = true)
    private final List<FXConsumer<GraphicsContext>> extraRedraw = new ArrayList<>();

    /**
     * The pending errors (see PendingError class)
     */
    private final List<PendingError> pending = new ArrayList<>();

    /**
     * As soon as the user moves to a position where a hover would do something,
     * we start a timer to run the hover-begin action.  If this item is non-null, it cancels
     * that timer.  So if you run this while the timer is ticking, it will stop
     * any hover-begin from ever being executed.  If you run this after the hover has
     * begun, it does nothing, but is harmless.
     */
    private FXPlatformRunnable cancelBeginHover;
    /**
     * Once the hover has begun, this gets set to the action which will stop the hover
     * (by passing false to it).  So when the user moves the mouse away, we run
     * this to stop the hover.
     */
    private FXPlatformConsumer<Boolean> currentHover;

    /**
     * Creates an ErrorUnderlineCanvas.
     * 
     * @param mouseableContainer The node on which to listen for mouse events relating
     * to hover, etc.  We can't listen on our own canvas, because we make it mouse
     * transparent.  (Otherwise the canvas would steal all events away from the underlying
     * node we overlay.)
     */
    public ErrorUnderlineCanvas(Node mouseableContainer)
    {
        canvas = new ResizableCanvas(this::redraw);
        canvas.setMouseTransparent(true);
        mouseableContainer.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            synchronized (ErrorUnderlineCanvas.this)
            {
                if (cancelBeginHover != null)
                    cancelBeginHover.run();
                if (currentHover != null)
                {
                    currentHover.accept(false);
                    currentHover = null;
                }
                cancelBeginHover = JavaFXUtil.runAfter(Duration.millis(500), () -> {
                    hoverAt(e.getSceneX(), e.getSceneY());
                });
            }
        });
        mouseableContainer.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            synchronized (ErrorUnderlineCanvas.this)
            {
                if (cancelBeginHover != null)
                {
                    cancelBeginHover.run();
                    cancelBeginHover = null;
                }
            }
        });
    }

    /**
     * Clears the canvas, then redraws all errors, all currently-showing hyperlinks, and any extra redraw actions.
     */
    @OnThread(Tag.FXPlatform)
    public void redraw()
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(0.75);
        gc.setStroke(Color.RED);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        for (ErrorInfo error : errors)
        {
            for (Line line : error.positionInfo.getAllLines(error.start, error.end, error.javaPos))
            {
                TextOverlayPosition startTOP = line.getStart();
                TextOverlayPosition endTOP = line.getEnd();
                Point2D start = canvas.sceneToLocal(startTOP.getSceneX(), startTOP.getSceneBaselineY());
                Point2D end = canvas.sceneToLocal(endTOP.getSceneX(), endTOP.getSceneBaselineY());
                double width = end.getX() - start.getX();

                width = Math.max(width, 15.0); // Make it a minimum size

                int n = (int) (width / 2) + 1;
                double[] xPoints = new double[n + 1];
                double[] yPoints = new double[n + 1];
                for (int j = 0; j <= n; j++)
                {
                    xPoints[j] = start.getX() + j * 2;
                    yPoints[j] = start.getY() + 3 * (j % 2);
                }
                gc.strokePolyline(xPoints, yPoints, n + 1);
            }
        }
        
        // Underline in front of error marker, so you see them even when there's an error        
        gc.setLineWidth(0.75);
        gc.setStroke(Color.BLACK);
        
        for (HyperlinkInfo link : hyperlinks)
        {
            if (link.showing)
            {
                TextOverlayPosition startTOP = link.positionInfo.getOverlayLocation(link.start, false);
                TextOverlayPosition endTOP = link.positionInfo.getOverlayLocation(link.end, false);
                Point2D start = canvas.sceneToLocal(startTOP.getSceneX(), startTOP.getSceneBaselineY());
                Point2D end = canvas.sceneToLocal(endTOP.getSceneX(), endTOP.getSceneBaselineY());
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }

        doExtraRedraw(gc);
    }

    @OnThread(Tag.FXPlatform)
    private synchronized void doExtraRedraw(GraphicsContext gc)
    {
        extraRedraw.forEach(c -> c.accept(gc));
    }

    /**
     * Clears all error markers which originate from the given slot
     * (according to the parameter passed to addError, checked by reference equality).
     * 
     * Redraws afterwards.
     */
    @OnThread(Tag.FXPlatform)
    public void clearErrorMarkers(UnderlineInfo origin)
    {
        for (int i = 0; i < errors.size();)
        {
            if (errors.get(i).positionInfo == origin)
            {
                errors.remove(i);
            }
            else
                i += 1;
        }
        pending.forEach(PendingError::cancel);
        pending.clear();
        redraw();
    }
    
    
    
    /**
     * Draws a red underline between the two given caret positions.  Will remain
     * until clearErrorMarkers() is called with the same origin parameter.
     * @param start   The start position
     * @param end     The end position
     * @param javaPos Whether this is a Java position or not
     * @param onHover Will be passed true to start the hover display, false again to stop it
     * @param visible The property tracking whether the error should be visible yet.
     *                If false, error will only be added once it turns true (but then
     *                will remain visible forever after, even if it changes back to false)
     */
    @OnThread(Tag.FXPlatform)
    public void addErrorMarker(UnderlineInfo origin, int start, int end, boolean javaPos, FXPlatformConsumer<Boolean> onHover, ObservableBooleanValue visible)
    {
        ErrorInfo err = new ErrorInfo(origin, start, end, javaPos, onHover);
        if (visible.get() == false)
        {
            pending.add(new PendingError(visible, err));
        }
        else
        {
            errors.add(err);
            redraw();
        }
            
    }

    /**
     * Removes all hyperlinks (but does not touch errors), and redraws.
     */
    @OnThread(Tag.FXPlatform)
    public void clearUnderlines()
    {
        hyperlinks.clear();
        redraw();
    }

    /**
     * Adds an underline and redraws.
     * 
     * @param info More information, see UnderlineInfo
     * @param startPosition Start of hyperlink
     * @param endPosition End of hyperlink
     * @param onClick Action to run if the hyperlink is clicked.
     */
    @OnThread(Tag.FXPlatform)
    public void addUnderline(UnderlineInfo info, int startPosition, int endPosition, FXPlatformRunnable onClick)
    {
        hyperlinks.add(new HyperlinkInfo(info, startPosition, endPosition, onClick));
        redraw();
    }

    /**
     * Checks if there is a link at the given X position (assuming a one-line canvas)
     * If there is, returns the action the link would trigger.  If not, returns null.
     */
    public FXPlatformRunnable linkFromX(double sceneX)
    {
        for (HyperlinkInfo link : hyperlinks)
        {
            double startX = link.positionInfo.getOverlayLocation(link.start, false).getSceneX();
            double endX = link.positionInfo.getOverlayLocation(link.end, false).getSceneX();
            if (sceneX >= startX && sceneX < endX)
            {
                return link.onClick;
            }
        }
        return null;
    }

    /**
     * Tells the overlay where the mouse hover is.  Only hyperlinks at this position are set to be drawn,
     * others are left out
     * @param pos position closest to mouse cursor
     * @return The action to perform if clicked
     */
    public FXPlatformRunnable hoverAtPos(int pos)
    {
        FXPlatformRunnable r = null;
        for (HyperlinkInfo link : hyperlinks)
        {
            link.showing = false;
            if (pos >= link.start && pos <= link.end)
            {
                if (r == null)
                {
                    r = link.onClick;
                    link.showing = true;
                }
            }
        }
        Platform.runLater(this::redraw);
        return r;
    }

    /**
     * Adds an extra action to run whenever this canvas redraws.
     * The action will take place after all errors and hyperlinks are drawn.
     * Extra redraw actions are executed in the order in which they are added.
     */
    @OnThread(Tag.FX)
    public synchronized void addExtraRedraw(FXConsumer<GraphicsContext> redraw)
    {
        extraRedraw.add(redraw);        
    }

    @OnThread(Tag.FXPlatform)
    private synchronized void hoverAt(double sceneX, double sceneY)
    {
        for (ErrorInfo error : errors)
        {
            double left = error.positionInfo.getOverlayLocation(error.start, error.javaPos).getSceneX();
            double right = error.positionInfo.getOverlayLocation(error.end, error.javaPos).getSceneX();
            double top = error.positionInfo.getOverlayLocation(error.start, error.javaPos).getSceneTopY();
            double bottom = error.positionInfo.getOverlayLocation(error.end, error.javaPos).getSceneBottomY();
                        
            if (top <= sceneY && sceneY <= bottom && left <= sceneX && sceneX <= right)
            {
                error.onHover.accept(true);
                currentHover = error.onHover;
            }
        }
    }

    public Node getNode()
    {
        return canvas;
    }

    public Point2D localToScene(double x, double y)
    {
        return canvas.localToScene(x, y);
    }

    public Point2D sceneToLocal(double x, double y)
    {
        return canvas.sceneToLocal(x, y);
    }
    
    public Point2D sceneToLocal(Point2D p)
    {
        return canvas.sceneToLocal(p);
    }

    public double getHeight()
    {
        return canvas.getHeight();
    }
    
}
