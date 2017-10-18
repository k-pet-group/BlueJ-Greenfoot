

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Write a description of JavaFX class CatView here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class CatView extends Application
{
    private Image[] images;
    private ImageView imageView;
    private int curImage = 0;

    @Override
    public void start(Stage stage) throws Exception
    {
        images = new Image[] {
            new Image("cat1.jpg"),
            new Image("cat2.jpg"),
            new Image("cat3.jpg")
        };
        imageView = new ImageView(images[curImage]);
        stage.setScene(makeScene());
        stage.setTitle("Cat Pictures");
        stage.sizeToScene();

        // Show the Stage (window)
        stage.show();
        
    }
    
    private Scene makeScene()
    {
        BorderPane pane = new BorderPane(imageView);
        pane.getStyleClass().add("image-wrapper");
        Scene scene = new Scene(pane);
        
        return scene;
    }

    public void nextImage()
    {
        curImage = (curImage + 1) % images.length;
        imageView.setImage(images[curImage]);
        imageView.getScene().getWindow().sizeToScene();
    }
}
