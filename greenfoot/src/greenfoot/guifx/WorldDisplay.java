/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/**
 * A class for handling the world part of the main GreenfootStage window.
 */
class WorldDisplay extends BorderPane
{
    private final ImageView imageView = new ImageView();
    
    public WorldDisplay()
    {
        setCenter(imageView);
        setMinWidth(200);
        setMinHeight(200);
    }

    /**
     * Sets the world image.  Turns off any greying effect.
     */
    public void setImage(Image image)
    {
        imageView.setImage(image);
        // Now that world is valid again, turn off any greying effect:
        imageView.setEffect(null);
    }

    /**
     * Greys out the world to indicate it isn't in a valid state.
     * Turned off by next setImage call.
     */
    public void greyOutWorld()
    {
        ColorAdjust grey = new ColorAdjust(0.0, -1.0, -0.1, 0.0);
        GaussianBlur blur = new GaussianBlur();
        blur.setInput(grey);
        imageView.setEffect(blur);
    }
}
