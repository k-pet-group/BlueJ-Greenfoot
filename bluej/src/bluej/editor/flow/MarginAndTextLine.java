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
