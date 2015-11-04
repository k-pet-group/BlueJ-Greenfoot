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

public class ErrorUnderlineCanvas
{
    private final Canvas canvas = new Canvas()
    {
        @Override
        public boolean isResizable()
        {
            return true;
        }
        
        @Override
        public void resize(double width, double height)
        {
            setWidth(width);
            setHeight(height);
        }



        @Override
        public double minWidth(double height)
        {
            return 0;
        }

        @Override
        public double minHeight(double width)
        {
            return 0;
        }

        @Override
        public double prefWidth(double height)
        {
            return 0;
        }

        @Override
        public double prefHeight(double width)
        {
            return 0;
        }

        @Override
        public double maxWidth(double height)
        {
            return Double.MAX_VALUE;
        }

        @Override
        public double maxHeight(double width)
        {
            return Double.MAX_VALUE;
        }
        
    };
    private static class HyperlinkInfo
    {
        private final UnderlineInfo positionInfo;
        private final int start;
        private final int end;
        private final FXRunnable onClick;
        private boolean showing = true;
        private HyperlinkInfo(UnderlineInfo positionInfo, int start, int end,
                FXRunnable onClick)
        {
            this.positionInfo = positionInfo;
            this.start = start;
            this.end = end;
            this.onClick = onClick;
        }
    }
    
    private static class ErrorInfo
    {
        private final EditableSlot positionInfo;
        private final int start;
        private final int end;
        private final boolean javaPos;
        private final FXConsumer<Boolean> onHover;
        private ErrorInfo(EditableSlot slot, int start, int end,
                boolean javaPos, FXConsumer<Boolean> onHover)
        {
            this.positionInfo = slot;
            this.start = start;
            this.end = end;
            this.javaPos = javaPos;
            this.onHover = onHover;
        }
    }
    
    private final List<HyperlinkInfo> hyperlinks = new ArrayList<>();
    
    private final List<ErrorInfo> errors = new ArrayList<>();
    
    private final List<FXConsumer<GraphicsContext>> extraRedraw = new ArrayList<>();
    
    private final List<PendingError> pending = new ArrayList<>();
    
    private FXRunnable hoverCancel;
    private FXConsumer<Boolean> currentHover;
    
    public ErrorUnderlineCanvas(Node mouseableContainer)
    {
        canvas.setMouseTransparent(true);
        mouseableContainer.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            synchronized (ErrorUnderlineCanvas.this)
            {
                if (hoverCancel != null)
                    hoverCancel.run();
                if (currentHover != null)
                {
                    currentHover.accept(false);
                    currentHover = null;
                }
                hoverCancel = JavaFXUtil.runAfter(Duration.millis(500), () -> {
                    hoverAt(e.getSceneX(), e.getSceneY());
                });
            }
        });
        mouseableContainer.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            synchronized (ErrorUnderlineCanvas.this)
            {
                if (hoverCancel != null)
                {
                    hoverCancel.run();
                    hoverCancel = null;
                }
            }
        });
    }
    
    public static interface UnderlineInfo
    {
        public TextOverlayPosition getOverlayLocation(int caretPos, boolean javaPos);

        // Default implementation assumes single text-line slot:
        default public List<Line> getAllLines(int start, int end, boolean javaPos)
        {
            return TextOverlayPosition.groupIntoLines(Arrays.asList(getOverlayLocation(start, javaPos), getOverlayLocation(end, javaPos)));
        }
    }

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
        
        extraRedraw.forEach(c -> c.accept(gc));
    }

    public void clearErrorMarkers(EditableSlot origin)
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
    
    // To be added once fresh:
    private class PendingError implements ChangeListener<Boolean> 
    {
        private final ObservableValue<Boolean> prop;
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
            redraw();
            cancel();
        }
        
        public void cancel()
        {
            prop.removeListener(this);
        }
    };
    
    /**
     * Draws a red underline between the two given caret positions.  Will remain
     * until clearErrorMarkers() is called.
     * @param startCaretPos
     * @param endCaretPos
     * @param onHover Will be passed true to start the hover display, false again to stop it
     */
    public void addErrorMarker(EditableSlot origin, int start, int end, boolean javaPos, FXConsumer<Boolean> onHover, ObservableBooleanValue visible)
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

    public void clearUnderlines()
    {
        hyperlinks.clear();
        redraw();
    }
    

    public void addUnderline(UnderlineInfo info, int startPosition, int endPosition, FXRunnable onClick)
    {
        hyperlinks.add(new HyperlinkInfo(info, startPosition, endPosition, onClick));
        redraw();
    }
    
    public FXRunnable linkFromX(double sceneX)
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
     * Tells the overlay where the mouse hover is.  Only links at this position are set to be drawn,
     * others are left out
     * @param pos position closest to mouse cursor
     * @return The action to perform if clicked
     */
    public FXRunnable hoverAtPos(int pos)
    {
        FXRunnable r = null;
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

    public void addExtraRedraw(FXConsumer<GraphicsContext> redraw)
    {
        extraRedraw.add(redraw);        
    }
    
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
