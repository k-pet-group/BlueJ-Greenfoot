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
package greenfoot.guifx.classes;

import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

/**
 * The display of a single class in Greenfoot's class diagram. 
 */
public class ClassDisplay extends BorderPane
{
    private final String fullyQualifiedName;

    /**
     * @param displayName The class name to display (without package)
     * @param image The image, if any (can be null)
     * @param selectionManager The selection manager.  The constructor will add this
     *                         class to the selection manager.
     */
    public ClassDisplay(String displayName, String fullyQualifiedName, Image image, ClassDisplaySelectionManager selectionManager)
    {
        this.fullyQualifiedName = fullyQualifiedName;
        getStyleClass().add("class-display");
        setSnapToPixel(true);
        setCenter(new Label(displayName));
        setLeft(new ImageView(image));
        
        selectionManager.addClassDisplay(this);
        
        // We use a general handler not setOnMouseClicked,
        // because we don't want to interfere with other click handlers:
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            // Only single-click selects; double-click may do something else
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1)
            {
                // We don't set our state directly, but instead get the
                // selection manager to do so:
                selectionManager.select(this);
                e.consume();
            }
        });
    }

    public void setSelected(boolean selected)
    {
        JavaFXUtil.setPseudoclass("gf-selected", selected, this);
    }

    public String getQualifiedName()
    {
        return fullyQualifiedName;
    }
}
