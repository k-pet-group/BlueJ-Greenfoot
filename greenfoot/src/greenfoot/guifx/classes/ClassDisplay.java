package greenfoot.guifx.classes;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/**
 * The display of a single class in Greenfoot's class diagram. 
 */
public class ClassDisplay extends BorderPane
{
    /**
     * @param name The class name to display (without package)
     * @param image The image, if any (can be null)
     */
    public ClassDisplay(String name, Image image)
    {
        getStyleClass().add("class-display");
        setSnapToPixel(true);
        setCenter(new Label(name));
        setLeft(new ImageView(image));
    }
}
