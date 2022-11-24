/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class ActivityIndicator extends StackPane
{
    private ProgressBar progressBar;
    private Label messageLabel;
    private Timeline animation;

    public ActivityIndicator()
    {
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        getChildren().add(progressBar);
        messageLabel = new Label();
        messageLabel.setVisible(false);
        getChildren().add(messageLabel);
    }
    
    /**
     * Set the activity indicator's running state. This is safe to call
     * from any thread.
     * 
     * @param running  The new running state
     */
    public void setRunning(boolean running)
    {
        messageLabel.setVisible(!running);
        progressBar.setVisible(running);
        if (animation != null)
        {
            // Either way, we stop the animation (later we restart, or leave stopped):
            animation.stop();
            animation = null;
        }
        
        if (running)
        {
            animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.millis(1000), new KeyValue(progressBar.progressProperty(), 1.0)));
            animation.setAutoReverse(true);
            animation.setCycleCount(Animation.INDEFINITE);
            animation.playFromStart();
        }
    }
    /**
     * Set message to display when the activity indicator is not in a running state.
     * @param msg 
     */
    public void setMessage(String msg)
    {
        if (msg != null){
            messageLabel.setText(msg);
        }
    }
}
