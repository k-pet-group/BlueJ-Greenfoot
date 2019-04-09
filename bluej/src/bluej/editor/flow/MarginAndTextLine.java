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

    public MarginAndTextLine(TextLine textLine)
    {
        this.textLine = textLine;
        setMouseTransparent(true);
        getChildren().setAll(textLine);
        getStyleClass().add("margin-and-text-line");
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void layoutChildren()
    {
        textLine.resizeRelocate(MARGIN_WIDTH, 0, getWidth() - MARGIN_WIDTH, getHeight());
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
}
