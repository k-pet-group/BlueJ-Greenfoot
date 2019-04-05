package bluej.editor.flow;

import javafx.scene.layout.Region;

/**
 * A graphical item that contains a margin (used for line numbers and/or breakpoint symbols, step marks, etc)
 * and a text line.
 */
public class MarginAndTextLine extends Region
{
    public static final int MARGIN_WIDTH = 25;
    
    final TextLine textLine;

    public MarginAndTextLine(TextLine textLine)
    {
        this.textLine = textLine;
        getChildren().setAll(textLine);
    }

    @Override
    protected void layoutChildren()
    {
        textLine.resizeRelocate(MARGIN_WIDTH, 0, getWidth() - MARGIN_WIDTH, getHeight());
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return textLine.prefWidth(height) + MARGIN_WIDTH;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return textLine.prefHeight(width);
    }

    @Override
    protected double computeMinWidth(double height)
    {
        return textLine.minWidth(height) + MARGIN_WIDTH;
    }

    @Override
    protected double computeMinHeight(double width)
    {
        return textLine.minHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height)
    {
        return textLine.maxWidth(height) + MARGIN_WIDTH;
    }

    @Override
    protected double computeMaxHeight(double width)
    {
        return textLine.maxHeight(width);
    }
}
