/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.Config;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * A graphical item that contains a margin (used for line numbers and/or breakpoint symbols, step marks, etc)
 * and a text line.
 */
@OnThread(Tag.FXPlatform)
public class MarginAndTextLine extends Region
{
    public static final int TEXT_LEFT_EDGE = 27;
    public static final double LINE_X = 24.5;
    public static final double MARGIN_RIGHT = 23;
    private final Line dividerLine;
    private final int lineNumberToDisplay;
    private boolean hoveringMargin = false;
    // Does not include the hover icon, which is added dynamically:
    private final EnumSet<MarginDisplay> displayItems = EnumSet.noneOf(MarginDisplay.class);

    public static enum MarginDisplay
    {
        // Important that step mark is after breakpoint, so that it appears in front:
        LINE_NUMBER, BREAKPOINT_HOVER, BREAKPOINT, STEP_MARK, ERROR;
    }
    
    final EnumMap<MarginDisplay, Node> cachedIcons = new EnumMap<MarginDisplay, Node>(MarginDisplay.class);
    final TextLine textLine;

    public MarginAndTextLine(int lineNumberToDisplay, TextLine textLine, FXPlatformRunnable onClick)
    {
        this.dividerLine = new Line(LINE_X, 0.5, LINE_X, 1);
        dividerLine.getStyleClass().add("flow-margin-line");
        this.lineNumberToDisplay = lineNumberToDisplay;
        this.textLine = textLine;
        getChildren().setAll(textLine);
        getStyleClass().add("margin-and-text-line");
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getX() < LINE_X)
            {
                if (e.getButton() == MouseButton.PRIMARY && !e.isShiftDown())
                {
                    onClick.run();
                }
                e.consume();
            }
        });
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            hoveringMargin = e.getX() < LINE_X;
            setMarginGraphics(EnumSet.copyOf(displayItems));
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hoveringMargin = false;
            setMarginGraphics(EnumSet.copyOf(displayItems));
        });
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void layoutChildren()
    {
        for (Node child : getChildren())
        {
            if (child == textLine)
            {
                textLine.resizeRelocate(TEXT_LEFT_EDGE, 0, getWidth() - TEXT_LEFT_EDGE, getHeight());
            }
            else if (child == dividerLine)
            {
                dividerLine.setEndY(getHeight() - 0.5);
            }
            else
            {
                double height = child.prefHeight(-1);
                double width = child.prefWidth(-1);
                child.resizeRelocate(MARGIN_RIGHT - width, (getHeight() - height) / 2.0, width, height);
            }
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefWidth(double height)
    {
        return textLine.prefWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefHeight(double width)
    {
        return textLine.prefHeight(width);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMinWidth(double height)
    {
        return textLine.minWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMinHeight(double width)
    {
        return textLine.minHeight(width);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMaxWidth(double height)
    {
        return textLine.maxWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMaxHeight(double width)
    {
        return textLine.maxHeight(width);
    }

    public void setMarginGraphics(EnumSet<MarginDisplay> displayItems)
    {
        this.displayItems.clear();
        this.displayItems.addAll(displayItems);
        EnumSet<MarginDisplay> toAdd = EnumSet.copyOf(displayItems);
        if (hoveringMargin && !toAdd.contains(MarginDisplay.BREAKPOINT))
            toAdd.add(MarginDisplay.BREAKPOINT_HOVER);
        ArrayList<Node> content = new ArrayList<>();
        content.add(textLine);
        content.add(dividerLine);

        for (MarginDisplay display : toAdd)
        {
            content.add(cachedIcons.computeIfAbsent(display, d -> {
                switch (d)
                {
                    case LINE_NUMBER:
                        Label label = new Label(Integer.toString(lineNumberToDisplay));
                        label.setEllipsisString("\u2026");
                        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
                        JavaFXUtil.addStyleClass(label, "flow-line-label");
                        return label;
                    case STEP_MARK:
                        return makeStepMarkIcon();
                    case BREAKPOINT:
                        return makeBreakpointIcon();
                    case BREAKPOINT_HOVER:
                        Node hover = makeBreakpointIcon();
                        hover.setOpacity(0.3);
                        Tooltip.install(hover, new Tooltip(Config.getString("editor.set.breakpoint.hint")));
                        return hover;
                    case ERROR:
                    default:
                        return new Label(""); //TODO
                }
            }));
        }
        getChildren().setAll(content);
    }


    // Red octagon with white STOP on it.  By doing it as a shape rather than
    // image file, we get it looking good on all HiDPI displays.
    private static Node makeBreakpointIcon()
    {
        Node icon = Config.makeStopIcon(false);
        JavaFXUtil.addStyleClass(icon, "moe-breakpoint-icon");
        return icon;
    }

    private static Node makeStepMarkIcon()
    {
        Shape arrow = Config.makeArrowShape(false);
        JavaFXUtil.addStyleClass(arrow, "moe-step-mark-icon");
        return arrow;
    }
}
