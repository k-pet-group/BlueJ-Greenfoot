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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The display of a single class in Greenfoot's class diagram. 
 * 
 * Note that due to anticipated re-use of this item in the right-hand class diagram,
 * and the import-class dialog and maybe other places too, this class itself only
 * handles display and selection.  All other functionality (e.g. context menus)
 * is done externally as it is not used in all instances of ClassDisplay.
 */
@OnThread(Tag.FXPlatform)
public class ClassDisplay extends StackPane
{
    private final String fullyQualifiedName;
    // We make child panes mainly so that we can apply
    // the stripe fill to the background but not to the content:
    private final BorderPane stripePane;
    private final Label contentLabel;

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
        contentLabel = new Label(displayName);
        BorderPane content = new BorderPane(contentLabel);
        setImage(image);
        JavaFXUtil.addStyleClass(content, "class-display-content");
        stripePane = new BorderPane();
        getChildren().addAll(stripePane, content);
        
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

    /**
     * Sets the graphical selected state of this class display
     */
    public void setSelected(boolean selected)
    {
        JavaFXUtil.setPseudoclass("gf-selected", selected, this);
    }

    /**
     * Gets the qualified name of the class
     */
    public String getQualifiedName()
    {
        return fullyQualifiedName;
    }

    /**
     * Sets the image to the left of the class name
     */
    public void setImage(Image image)
    {
        if (image == null)
        {
            contentLabel.setGraphic(null);
        }
        else
        {
            ImageView imageView = new ImageView(image);
            // Max size 16x16, but don't scale up image if it's smaller than that:
            imageView.setFitHeight(Math.min(image.getHeight(), 16));
            imageView.setFitWidth(Math.min(image.getWidth(), 16));
            imageView.setPreserveRatio(true);
            contentLabel.setGraphic(imageView);
        }
    }

    /**
     * Sets the stripe pattern of this class (use Color.TRANSPARENT to mean none)
     */
    public void setStripePattern(Paint stripeFill)
    {
        stripePane.setBackground(new Background(new BackgroundFill(stripeFill, null, null)));
    }
}
