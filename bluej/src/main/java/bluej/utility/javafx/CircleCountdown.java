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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Created by neil on 21/03/2016.
 */
public class CircleCountdown extends Canvas
{
    // Goes from 1 down to 0:
    private final SimpleDoubleProperty time = new SimpleDoubleProperty(1);
    private final Timeline timeline;

    //TODO allow stroke color to be set from CSS
    public CircleCountdown(double size, Color strokeColor, Duration duration)
    {
        super(size, size);
        timeline = new Timeline(20.0, new KeyFrame(duration, new KeyValue(time, 0)));

        FXConsumer<Number> update = t -> {
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, size, size);
            gc.setStroke(strokeColor);
            gc.setLineWidth(3);
            gc.strokeArc(size * 0.5, size * 0.5, size * 0.5 - 4, size * 0.5 - 4, 90, t.doubleValue() * 360, ArcType.OPEN);
        };
        update.accept(1.0);
        JavaFXUtil.addChangeListener(time, update);

        timeline.play();
    }

    public void addOnFinished(FXRunnable action)
    {
        JavaFXUtil.addChangeListener(time, t -> {
            if (t.doubleValue() == 0.0)
                action.run();
        });
    }

    public void stop()
    {
        timeline.stop();
    }
}
