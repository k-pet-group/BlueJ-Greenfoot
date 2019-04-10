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

import bluej.utility.javafx.FXPlatformRunnable;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A graphical item that contains a margin (used for line numbers and/or breakpoint symbols, step marks, etc)
 * and a text line.
 */
@OnThread(Tag.FXPlatform)
public class MarginAndTextLine extends Region
{
    public static final int MARGIN_WIDTH = 25;
    
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
                child.resizeRelocate(0, 0, MARGIN_WIDTH, getHeight());
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

    public void setMarginGraphics(Node... nodes)
    {
        // We need all clicks to fall through to us, so make sure the graphics are mouse-transparent:
        for (Node node : nodes)
        {
            node.setMouseTransparent(true);
        }
        getChildren().setAll(textLine);
        getChildren().addAll(nodes);
    }
}
