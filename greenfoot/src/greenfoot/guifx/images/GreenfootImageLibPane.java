/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2014  Poul Henriksen and Michael Kolling

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
package greenfoot.guifx.images;

import bluej.Config;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 
 * Component that shows selectors for images in the Greenfoot library of images.
 * 
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
class GreenfootImageLibPane extends HBox
{
    GreenfootImageLibPane(ImageCategorySelector categorySelector, ImageLibList imageList)
    {
        super(2);

        VBox categoryPane = new VBox(5);
        categoryPane.getChildren().addAll(new Label(Config.getString("imagelib.categories")), new ScrollPane(categorySelector));

        VBox imagePane = new VBox(5);
        imagePane.getChildren().addAll(new Label(Config.getString("imagelib.images")), new ScrollPane(imageList));

        getChildren().addAll(categoryPane, imagePane);
    }
}
