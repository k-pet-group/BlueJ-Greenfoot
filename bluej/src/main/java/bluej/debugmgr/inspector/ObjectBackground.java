/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr.inspector;

import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 29/09/2016.
 */
@OnThread(Tag.FXPlatform)
public class ObjectBackground extends ResizableCanvas
{
    private final double cornerSize;
    private final ObservableDoubleValue lineWidth;

    public ObjectBackground(double cornerSize, ObservableDoubleValue lineWidth)
    {
        this.cornerSize = cornerSize;
        this.lineWidth = lineWidth;
        JavaFXUtil.addChangeListenerPlatform(widthProperty(), w -> redrawContent());
        JavaFXUtil.addChangeListenerPlatform(heightProperty(), h -> redrawContent());
        JavaFXUtil.addChangeListenerPlatform(lineWidth, d -> redrawContent());
    }

    private void redrawContent()
    {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);
        
        final Paint fill = new javafx.scene.paint.Color(227.0 / 255.0, 71.0 / 255.0, 71.0 / 255.0, 1.0);
        gc.setFill(fill);
        double l = lineWidth.get();
        // To make sure the line is visible on the canvas and not truncated, we move the coordinates by the
        // line width amount away from the edge.  Remember that these methods take width and height, so the
        // width needs to subtract 2*l (one for the left, one for the right) and similar for the height.
        
        // Need a slightly increased corner size for the fill so that it doesn't show up outside the stroke:
        gc.fillRoundRect(l, l, w-2*l, h-2*l, cornerSize*1.1, cornerSize*1.1);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(l);
        gc.strokeRoundRect(l, l, w-2*l, h-2*l, cornerSize, cornerSize);
    }
}
