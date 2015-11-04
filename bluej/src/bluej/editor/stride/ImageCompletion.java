package bluej.editor.stride;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;

import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A file completion for image filenames in the scenario's "images/" subdirectory
 */
@OnThread(Tag.FX) class ImageCompletion implements InteractionManager.FileCompletion
{
    private final File file;

    public ImageCompletion(File file)
    {
        this.file = file;
    }

    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public String getType()
    {
        return "Image";
    }

    @Override
    public Node getPreview(double maxWidth, double maxHeight)
    {
        try
        {
            Image img = new Image(file.toURI().toURL().toString());
            ImageView view = new ImageView(img);
            // Only resize if it's bigger than will fit (don't want to scale up):
            if (img.getWidth() > maxWidth || img.getHeight() > maxHeight)
            {
                view.setFitHeight(maxHeight);
                view.setFitWidth(maxWidth);
                view.setPreserveRatio(true);
            } else
            {
                view.setFitHeight(0);
                view.setFitWidth(0);
                view.setPreserveRatio(true);
            }
            return view;
        } catch (MalformedURLException | IllegalArgumentException e)
        {
            return new Label("Error loading image");
        }
    }

    @Override
    public Map<KeyCode, Runnable> getShortcuts()
    {
        return null;
    }

}
