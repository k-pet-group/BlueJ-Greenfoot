/*
 This file is part of the BlueJ program.
 Copyright (C) 2016 Michael Kölling and John Rosenberg

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

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Orientation;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;

/**
 * A triangle arrow shape, used for foldout panes.
 */
public class TriangleArrow extends Polygon
{
    // The base.  If you make a vertical-pointing arrow, this is the width, horizontal arrow this is the height
    public static final double TRIANGLE_BASE = 14;
    // The opposite dimension to the base (vertical: height, horizontal: width)
    public static final double TRIANGLE_DEPTH = 10;
    private final Scale scale;
    private final Orientation orientation;

    /**
     * Makes a triangle arrow, used in foldout containers.
     *
     * @param orientation The orientation.  VERTICAL makes a triangle arrow pointed upwards, HORIZONTAL makes one pointed leftwards.
     */
    public TriangleArrow(Orientation orientation)
    {
        JavaFXUtil.addStyleClass(this, "triangle-arrow");
        scale = new Scale(1.0, 1.0);
        this.orientation = orientation;
        switch (orientation)
        {
            case HORIZONTAL:
                getPoints().setAll(
                    TRIANGLE_DEPTH - 1, 1.0,
                    1.0, TRIANGLE_BASE * 0.5,
                    TRIANGLE_DEPTH - 1, TRIANGLE_BASE - 1);
                scale.setPivotX(TRIANGLE_DEPTH * 0.5);
                scale.setPivotY(TRIANGLE_BASE * 0.5);
                break;
            case VERTICAL:
                getPoints().setAll(
                    1.0, TRIANGLE_DEPTH - 1,
                    TRIANGLE_BASE * 0.5, 1.0,
                    TRIANGLE_BASE - 1, TRIANGLE_DEPTH - 1);
                scale.setPivotX(TRIANGLE_BASE * 0.5);
                scale.setPivotY(TRIANGLE_DEPTH * 0.5);
                break;
        }

        getTransforms().add(scale);
        // If we pick using the shape, it's quite a hard target to hit
        // especially at the pointy end of the triangle.  So we make it
        // a rectangle for hit detection purposes to make it a bit easier
        // to hit:
        setPickOnBounds(true);
    }

    /**
     * The scale property.  Setting this to 1.0 leaves the arrow normal,
     * i.e. facing up/left, while setting it to -1.0 reverses it,
     * making it face down/right.  You can either set it instantly, or animate
     * the transition.
     */
    public DoubleProperty scaleProperty()
    {
        if (orientation == Orientation.HORIZONTAL)
            return scale.xProperty();
        else
            return scale.yProperty();
    }
}
