package bluej.editor.stride;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.media.AudioClip;

import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A file completion for sound filenames in the scenario's "images/" subdirectory
 */
@OnThread(Tag.FX) class SoundCompletion implements InteractionManager.FileCompletion
{
    private final File file;

    public SoundCompletion(File file)
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
        return "Sound";
    }

    @Override
    public Node getPreview(double maxWidth, double maxHeight)
    {
        Button b = new Button("Play (F8)");
        b.setOnAction(e -> play());
        return b;
    }

    private void play()
    {
        try
        {
            AudioClip clip = new AudioClip(file.toURI().toURL().toString());
            clip.play();
        } catch (MalformedURLException ex)
        {
            Debug.reportError(ex);
        }
    }

    @Override
    public Map<KeyCode, Runnable> getShortcuts()
    {
        return Collections.singletonMap(KeyCode.F8, this::play);
    }

}
