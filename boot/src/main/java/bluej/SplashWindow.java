/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2015,2017  Michael Kolling and John Rosenberg
 
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
package bluej;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * This class implements a splash window that can be displayed while BlueJ is
 * starting up.
 *
 * @author  Michael Kolling
 */
public class SplashWindow extends Stage
{
    /**
     * Construct a splash window.
     * @param image
     */
    public SplashWindow(Image image)
    {
        super(StageStyle.TRANSPARENT);
        ImageView imageView = new ImageView(image);
        BorderPane borderPane = new BorderPane(imageView);
        borderPane.setBackground(null);
        ProgressBar progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        borderPane.setBottom(progress);
        progress.setVisible(false);
        Scene scene = new Scene(borderPane);
        scene.setFill(null);
        setScene(scene);


        // centre on screen
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        setX((screenBounds.getWidth() - image.getWidth()) / 2);
        setY((screenBounds.getHeight() - image.getHeight()) / 2);
        setOnShown(e -> {
            toFront();
        });
        show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(5000), e -> {
            if (isShowing()) {
                progress.setVisible(true);
            }
        }));
        timeline.setCycleCount(1);
        timeline.play();
    }
}

