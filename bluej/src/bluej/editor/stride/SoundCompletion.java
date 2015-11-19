/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
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
 * A file completion for sound filenames in the scenario's "sounds/" subdirectory
 */
@OnThread(Tag.FX) class SoundCompletion implements InteractionManager.FileCompletion
{
    /** The file which the completion refers to */
    private final File file;

    /** Create a SoundCompletion for the given sound file in the project's sounds/ directory */
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

    /** The preview shows a button which will play the sound when clicked */
    @Override
    public Node getPreview(double maxWidth, double maxHeight)
    {
        // Keyboard shortcut label shown here, but implemented below via getShortcuts
        Button b = new Button("Play (F8)");
        b.setOnAction(e -> play());
        return b;
    }

    /** Plays the sound clip in full by loading it from the file */
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

    /** Actually implements the F8-to-play shortcut */
    @Override
    public Map<KeyCode, Runnable> getShortcuts()
    {
        return Collections.singletonMap(KeyCode.F8, this::play);
    }

}
