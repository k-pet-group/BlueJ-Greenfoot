/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.actions.RestartVMAction;
import bluej.utility.Debug;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class MachineIcon extends HBox
{
    private static final int NUM_BARS = 12;
    private final Canvas indicator;
    private final ButtonBase resetButton;
    private final DoubleProperty indicatorPosition = new SimpleDoubleProperty(0.0);
    private final BooleanProperty forwards = new SimpleBooleanProperty(true);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final RestartVMAction resetAction;
    private ContextMenu contextMenu;
    private Animation animation;

    public MachineIcon(PkgMgrFrame pmf, RestartVMAction resetAction)
    {
        setAlignment(Pos.CENTER);
        indicator = new ResizableCanvas();
        HBox.setHgrow(indicator, Priority.ALWAYS);
        this.resetAction = resetAction;
        resetButton = PkgMgrFrame.createButton(this.resetAction, false, true);
        resetButton.setGraphic(drawResetArrow());
        resetButton.setFocusTraversable(false);
        JavaFXUtil.addStyleClass(resetButton, "reset-vm-button");
        JavaFXUtil.setPseudoclass("bj-no-hover", true, resetButton);
        resetButton.setOnMouseEntered(e -> JavaFXUtil.setPseudoclass("bj-no-hover", false, resetButton));
        resetButton.setOnMouseExited(e -> JavaFXUtil.setPseudoclass("bj-no-hover", true, resetButton));
        getChildren().addAll(indicator, resetButton);
        JavaFXUtil.addChangeListenerPlatform(indicatorPosition, x -> redraw());
        redraw();
        Tooltip.install(indicator, new Tooltip(Config.getString("tooltip.progress")));
        resetButton.setTooltip(new Tooltip(Config.getString("workIndicator.resetMachine")));
        JavaFXUtil.listenForContextMenu(indicator, (x, y) -> {
            if (contextMenu != null)
                contextMenu.hide();
            MenuItem item = new MenuItem(Config.getString("workIndicator.resetMachine"));
            item.setOnAction(e -> SwingUtilities.invokeLater(() -> this.resetAction.actionPerformed(pmf)));
            contextMenu = new ContextMenu(item);
            contextMenu.show(indicator, x, y);
            return true;
        });
    }

    private static Node drawResetArrow()
    {
        Canvas c = new Canvas(16, 15);
        GraphicsContext gc = c.getGraphicsContext2D();

        gc.setLineWidth(2.0);
        gc.setStroke(Color.gray(0.5));
        gc.strokeLine(1, 1, 9, 1);
        gc.strokeLine(1, 11, 9, 11);
        gc.strokeArc(7, 1, 8, 10, 270, 180, ArcType.OPEN);
        gc.strokeLine(1, 11, 4, 8);
        gc.strokeLine(1, 11, 4, 14);

        return c;
    }

    private void redraw()
    {
        double w = indicator.getWidth();
        double h = indicator.getHeight();
        GraphicsContext gc = indicator.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setLineWidth(3);
        Debug.message("Redrawing " + indicatorPosition.get() + " " + forwards.get());
        for (int i = 0; i < NUM_BARS; i++)
        {
            double pos = (double)i / (double)(NUM_BARS - 1);
            // The intensity is a function of distance from the current progress
            double intensity = 0.8 - 1.0 * Math.pow(Math.abs(pos - indicatorPosition.get()), 0.5);
            if (intensity < 0 || ((pos - indicatorPosition.get() <= 0) != forwards.get()))
                intensity = 0;
            if (running.get())
                gc.setStroke(Color.rgb(0, 0, 255, intensity));
            else
                gc.setStroke(Color.gray(0.0, intensity));
            double x = 2.5 + pos * (w-5);
            gc.strokeLine(x, 0, x, h);
        }
    }

    private void cancelAnimation()
    {
        if (animation != null)
        {
            animation.stop();
            animation = null;
        }
    }

    /**
     * Indicate that the machine is idle.
     */
    @OnThread(Tag.Swing)
    public void setIdle()
    {
        Platform.runLater(() ->
        {
            cancelAnimation();
            forwards.setValue(true);
            running.set(false);
            indicatorPosition.set(0.5);
        });
    }

    /**
     * Indicate that the machine is running.
     */
    @OnThread(Tag.Swing)
    public void setRunning()
    {
        Platform.runLater(() -> {
            cancelAnimation();
            running.set(true);
            animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(indicatorPosition, 0.5, Interpolator.LINEAR), new KeyValue(forwards, true, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(500), new KeyValue(indicatorPosition, 1.0, Interpolator.LINEAR), new KeyValue(forwards, false, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(1500), new KeyValue(indicatorPosition, 0.0, Interpolator.LINEAR), new KeyValue(forwards, true, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(2000), new KeyValue(indicatorPosition, 0.5, Interpolator.LINEAR), new KeyValue(forwards, true, Interpolator.DISCRETE))
            );
            animation.setCycleCount(Animation.INDEFINITE);
            animation.playFromStart();
        });
    }

    /**
     * Indicate that the machine is stopped.
     */
    @OnThread(Tag.Swing)
    public void setStopped()
    {
        Platform.runLater(this::cancelAnimation);
    }
}

