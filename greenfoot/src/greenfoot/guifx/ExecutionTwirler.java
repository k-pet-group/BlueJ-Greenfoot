/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.vmcomm.GreenfootDebugHandler;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The execution twirler component.  This shows up if the user code has been running for too
 * long in any given segment (single act cycle, single user invocation), and allows the user
 * to easily open the debugger or restart the VM.
 */
@OnThread(Tag.FXPlatform)
public class ExecutionTwirler extends MenuButton
{
    // The animation to spin the twirl icon:
    private final Animation animation;
    private Project project;
    private GreenfootDebugHandler greenfootDebugHandler;
    private FXPlatformConsumer<Boolean> twirlListener;
    

    /**
     * Create the component
     * @param project The associated project (needed to access the debugger)
     * @param greenfootDebugHandler The debug handler for the project
     */
    public ExecutionTwirler(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        this.project = project;
        this.greenfootDebugHandler = greenfootDebugHandler;
        
        ImageView imageView = new ImageView(new Image(getClass().getClassLoader().getResourceAsStream("swirl.png")));
        imageView.setFitHeight(15.0);
        imageView.setPreserveRatio(true);
        setGraphic(imageView);

        RotateTransition rotateTransition = new RotateTransition(Duration.millis(3000), imageView);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        Timeline callTwirlListener = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (twirlListener != null)
            {
                twirlListener.accept(isVisible());
            }
        }));
        callTwirlListener.setCycleCount(Animation.INDEFINITE);
        animation = new ParallelTransition(rotateTransition, callTwirlListener);
        
        // Start invisible:
        setVisible(false);
        
        setPopupSide(Side.BOTTOM);
        // Important to use fields here, not constructor parameters, as the fields may change later
        // if the project shown in this window changes:
        getItems().setAll(
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.restart"), () -> this.project.restartVM(), null),
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.openDebugger"), () -> {
                this.project.getExecControls().show();
                this.greenfootDebugHandler.haltSimulationThread();
            }, null)
        );
    }

    /**
     * Sets a new project (and project debug handler) for this component
     */
    public void setProject(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        this.project = project;
        this.greenfootDebugHandler = greenfootDebugHandler;
    }

    /**
     * Make the twirler visible and start spinning the icon.  Does nothing if already started.
     */
    public void startTwirling()
    {
        setVisible(true);
        // Important not to play from start, as this method is called repeatedly
        // while the animation is already running:
        animation.play();
    }

    /**
     * Make the twirler invisible and stop spinning the icon.  Does nothing if already stopped.
     */
    public void stopTwirling()
    {
        setVisible(false);
        animation.stop();
        if (twirlListener != null)
        {
            twirlListener.accept(isVisible());
        }
    }

    /**
     * Set an action to execute while the twirler is twirling.
     * @param twirlListener Takes boolean (are we twirling or not?) 
     */
    public void setWhileTwirling(FXPlatformConsumer<Boolean> twirlListener)
    {
        this.twirlListener = twirlListener;
    }
}
