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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
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
    public static final int MARGIN_WIDTH = 25;
    
    public static enum MarginDisplay
    {
        // Important that step mark is after breakpoint, so that it appears in front:
        BREAKPOINT, STEP_MARK, ERROR;
    }
    
    final EnumMap<MarginDisplay, Node> cachedIcons = new EnumMap<MarginDisplay, Node>(MarginDisplay.class);
    final TextLine textLine;

    public MarginAndTextLine(TextLine textLine, FXPlatformRunnable onClick)
    {
        this.textLine = textLine;
        getChildren().setAll(textLine);
        getStyleClass().add("margin-and-text-line");
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getX() < MARGIN_WIDTH)
            {
                if (e.getButton() == MouseButton.PRIMARY && !e.isShiftDown())
                {
                    onClick.run();
                }
                e.consume();
            }
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
                textLine.resizeRelocate(MARGIN_WIDTH, 0, getWidth() - MARGIN_WIDTH, getHeight());
            }
            else
            {
                double height = child.prefHeight(-1);
                double width = child.prefWidth(-1);
                // Leave two pixels space at edge of margin:
                child.resizeRelocate(MARGIN_WIDTH - width - 2, (getHeight() - height) / 2.0, width, height);
            }
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefWidth(double height)
    {
        return textLine.prefWidth(height) + MARGIN_WIDTH;
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
        return textLine.minWidth(height) + MARGIN_WIDTH;
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
        return textLine.maxWidth(height) + MARGIN_WIDTH;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMaxHeight(double width)
    {
        return textLine.maxHeight(width);
    }

    public void setMarginGraphics(EnumSet<MarginDisplay> displayItems)
    {
        ArrayList<Node> content = new ArrayList<>();
        content.add(textLine);

        for (MarginDisplay display : displayItems)
        {
            content.add(cachedIcons.computeIfAbsent(display, d -> {
                switch (d)
                {
                    case STEP_MARK:
                        return makeStepMarkIcon();
                    case BREAKPOINT:
                        return makeBreakpointIcon();
                    case ERROR:
                    default:
                        return new Label(""); //TODO
                }
            }));
            // We need all clicks to fall through to us, so make sure the graphics are mouse-transparent:
            content.get(content.size() - 1).setMouseTransparent(true);
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
