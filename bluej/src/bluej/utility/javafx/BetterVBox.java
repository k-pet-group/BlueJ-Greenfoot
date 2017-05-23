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

import bluej.utility.Debug;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A VBox that behaves better with respect to sizing its children and
 * particularly FlowPane children.
 * 
 * Possibly, this behaviour could be achieve by tweaking VBox in just
 * the right way.  But this class works, and that will do for me.
 */
public class BetterVBox extends Pane
{
    private final double minWidth;
    
    private final DoubleProperty spacingProperty = new SimpleDoubleProperty(0.0);
    public final DoubleProperty spacingProperty() { return spacingProperty; }
    
    public BetterVBox(double minWidth)
    {
        this.minWidth = minWidth;
    }
    
    @Override
    protected void layoutChildren()
    {
        // If we have a non-integer margins, we get annoying aliasing on the borders, so here we clamp
        // the margins to the nearest integer:

        final Insets padding = getPadding();
        double y = (int)padding.getTop();
        final double contentWidth = getWidth() - (int)padding.getLeft() - (int)padding.getRight();
        for (Node n : getManagedChildren())
        {
            y += getTopMarginFor(n);
            double w = Math.min(contentWidth - (int)getLeftMarginFor(n) - (int)getRightMarginFor(n), n.maxWidth(-1));
            double h = n.prefHeight(w);
            n.resizeRelocate((int) padding.getLeft() + (int)getLeftMarginFor(n), y, w, h);
            y += h + spacingProperty.get() + getBottomMarginFor(n);
        }
        //if (getHeight() != 0 && y > getHeight() - (int)padding.getBottom())
        //{
            //Debug.message("Warning in BetterVBox size calculation; overpainted: " + y + " on " + getHeight());
        //}
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }

    @Override
    protected double computeMinWidth(double height)
    {
        return minWidth;
    }

    @Override
    protected double computeMinHeight(double width)
    {
        final Insets padding = getPadding();
        final double contentWidth = width - (int)padding.getLeft() - (int)padding.getRight();
        double reqMin = (getMinHeight() == Region.USE_COMPUTED_SIZE || getMinHeight() == Region.USE_PREF_SIZE) ? 0 : getMinHeight();
        return Math.max(reqMin, getManagedChildren().stream().mapToDouble(n -> getTopMarginFor(n) + getBottomMarginFor(n) + n.prefHeight(contentWidth)).sum()) + (int)padding.getTop() + (int)padding.getBottom();
    }

    @Override
    protected double computePrefWidth(double height)
    {
        // This is just set to a high enough value that we are unlikely to reach it:
        return 2000.0;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        if (width == -1 || width == 2000)
        {
            Debug.printCallStack("Problem in BetterVBox: calculating height first, which should not happen -- are you running ScenicView?");
            width = 2000;
        }
        final Insets padding = getPadding();
        final double contentWidth = width - (int)padding.getLeft() - (int)padding.getRight();
        double reqMin = (getMinHeight() == Region.USE_COMPUTED_SIZE || getMinHeight() == Region.USE_PREF_SIZE) ? 0 : getMinHeight();
        // Note: this deliberately does not use streams because it is performance-critical:
        double contentHeight = 0;
        for (Node n : getManagedChildren())
        {
            contentHeight += getTopMarginFor(n) + getBottomMarginFor(n) + n.prefHeight(contentWidth - getLeftMarginFor(n) - getRightMarginFor(n));
        }
        return Math.max(reqMin, contentHeight) + (int)padding.getTop() + (int)padding.getBottom();
    }

    @Override
    protected double computeMaxHeight(double width)
    {
        return computePrefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height)
    {
        return Double.MAX_VALUE;
    }

    @Override
    public Orientation getContentBias()
    {
        return Orientation.HORIZONTAL;
    }

    // Can be overridden in subclasses
    public double getTopMarginFor(Node n) { return 0; }

    public double getBottomMarginFor(Node n) { return 0; }

    public double getLeftMarginFor(Node n) { return 0; }

    public double getRightMarginFor(Node n) { return 0; }
}