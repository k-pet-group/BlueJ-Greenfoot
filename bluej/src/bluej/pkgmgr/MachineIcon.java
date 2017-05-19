/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017  Michael Kolling and John Rosenberg 
 
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class MachineIcon extends HBox
{
    private static final int NUM_BARS = 12;
    private static final double MIN_INTENSITY = 0.3;

    private final Pane barContainer;
    private final Rectangle bar;
    private final ButtonBase resetButton;
    private final DoubleProperty indicatorPosition = new SimpleDoubleProperty(0.0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final RestartVMAction resetAction;
    private ContextMenu contextMenu;
    private Animation animation;

    public MachineIcon(PkgMgrFrame pmf, RestartVMAction resetAction)
    {
        JavaFXUtil.addStyleClass(this, "machine-icon-container");
        barContainer = new Pane();
        HBox.setHgrow(barContainer, Priority.ALWAYS);
        HBox.setMargin(barContainer, new Insets(2,0,1,0));
        JavaFXUtil.addStyleClass(barContainer, "machine-icon-bar-holder");
        bar = new Rectangle();
        JavaFXUtil.addStyleClass(bar, "machine-icon-bar");
        barContainer.getChildren().add(bar);
        bar.setManaged(false);
        double width = 0.3;
        bar.layoutXProperty().bind(indicatorPosition.multiply(barContainer.widthProperty().multiply(1.0 - width)).add(2.0));
        bar.layoutYProperty().set(2.0);
        bar.widthProperty().bind(barContainer.widthProperty().multiply(width).subtract(4.0));
        bar.heightProperty().bind(barContainer.heightProperty().subtract(4.0));
        JavaFXUtil.bindPseudoclass(bar, "bj-active", running);
        this.resetAction = resetAction;
        resetButton = this.resetAction.makeButton();
        resetButton.setText(null);
        resetButton.setGraphic(drawResetArrow());
        resetButton.setFocusTraversable(false);
        JavaFXUtil.addStyleClass(resetButton, "reset-vm-button");
        JavaFXUtil.setPseudoclass("bj-no-hover", true, resetButton);
        resetButton.setOnMouseEntered(e -> JavaFXUtil.setPseudoclass("bj-no-hover", false, resetButton));
        resetButton.setOnMouseExited(e -> JavaFXUtil.setPseudoclass("bj-no-hover", true, resetButton));
        getChildren().addAll(barContainer, resetButton);
        Tooltip.install(barContainer, new Tooltip(Config.getString("tooltip.progress")));
        resetButton.setTooltip(new Tooltip(Config.getString("workIndicator.resetMachine")));
        JavaFXUtil.listenForContextMenu(barContainer, (x, y) -> {
            if (contextMenu != null)
                contextMenu.hide();
            MenuItem item = new MenuItem(Config.getString("workIndicator.resetMachine"));
            item.setOnAction(e -> this.resetAction.actionPerformed(pmf));
            contextMenu = new ContextMenu(item);
            contextMenu.show(barContainer, x, y);
            return true;
        });
    }

    private static Node drawResetArrow()
    {
        Path path = new Path();
        JavaFXUtil.addStyleClass(path, "reset-vm-button-arrow");

        path.getElements().addAll(
            new MoveTo(1, 1),
            new LineTo(9, 1),
            new MoveTo(9, 1),
            new ArcTo(5, 5, 180.0, 9.0, 11.0, false, true),
            new MoveTo(9, 11),
            new LineTo(1, 11),
            new LineTo(4, 8),
            new MoveTo(1, 11),
            new LineTo(4, 14)
        );

        return path;
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
    @OnThread(Tag.FXPlatform)
    public void setIdle()
    {
        cancelAnimation();
        running.set(false);
        indicatorPosition.set(0.0);
    }

    /**
     * Indicate that the machine is running.
     */
    @OnThread(Tag.FXPlatform)
    public void setRunning()
    {
        cancelAnimation();
        running.set(true);
        animation = new Timeline(30.0,
            new KeyFrame(Duration.ZERO, new KeyValue(indicatorPosition, 0, Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(1000), new KeyValue(indicatorPosition, 1.0, Interpolator.LINEAR)));
        animation.setAutoReverse(true);
        animation.setCycleCount(Animation.INDEFINITE);
        animation.playFromStart();
    }

    /**
     * Indicate that the machine is stopped.
     */
    @OnThread(Tag.FXPlatform)
    public void setStopped()
    {
        cancelAnimation();
    }
}

