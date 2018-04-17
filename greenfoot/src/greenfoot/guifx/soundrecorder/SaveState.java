/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2017,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx.soundrecorder;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.sound.SoundRecorder;

import java.io.File;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * This class manages the saving aspect of SoundRecorderControls:
 * keeping track of whether the sound has changed since last save,
 * displaying the overwrite dialog, updating the status message
 *
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
class SaveState
{
    private final Window parent;
    private final SoundRecorder recorder;
    private final TextField filenameField = new TextField();
    private final Button saveButton = new Button(Config.getString("soundRecorder.save"));
    private String lastSaveName = null;
    private final SimpleBooleanProperty saved = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty changedSinceSave = new SimpleBooleanProperty(false);
    // The directory to save into.  Can be null when there is no project open.
    private File projectSoundDir;

    SaveState(Window parent, SoundRecorder recorder)
    {
        this.parent = parent;
        this.recorder = recorder;

        JavaFXUtil.addChangeListenerPlatform(changedSinceSave, changed -> updateSavedStatus());
        JavaFXUtil.addChangeListenerPlatform(filenameField.textProperty(), text -> updateSavedStatus());
    }

    /**
     * Builds the save row: a filename field and save button
     *
     * @return a HBox which contains the gui nodes needed for saving a file
     */
    HBox buildSaveBox()
    {
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> {
            if (projectSoundDir != null)
            {
                File destination = new File(projectSoundDir, filenameField.getText() + ".wav");
                if (destination.exists())
                {
                    boolean overwrite = DialogManager.askQuestionFX(parent, "file-exists-overwrite", new String[] {destination.getName()}) == 0;
                    if (overwrite)
                    {
                        saveWAV(destination);
                    }
                }
                else
                {
                    saveWAV(destination);

                }
            }
        });
        updateSavedStatus();

        HBox fileBox = new HBox(3);
        fileBox.setAlignment(Pos.CENTER);
        fileBox.getChildren().addAll(new Label(Config.getString("soundRecorder.filename") + ": "), filenameField, new Label(".wav"));

        HBox saveBox = new HBox(20);
        saveBox.setAlignment(Pos.CENTER);
        saveBox.getChildren().addAll(fileBox, saveButton);
        return saveBox;
    }

    void changed(boolean value)
    {
        changedSinceSave.set(value);
        updateSavedStatus();
    }

    SimpleBooleanProperty savedProperty()
    {
        return saved;
    }

    /**
     * Updates the save status and save button based on whether the filename field and the recording changes.
     */
    private void updateSavedStatus()
    {
        boolean emptyRecorded =  recorder.getRawSound() == null;
        boolean differentFromSaved = !filenameField.textProperty().get().equals(lastSaveName) || changedSinceSave.get();
        saved.set(emptyRecorded || !differentFromSaved);
        boolean emptyTextField = filenameField.textProperty().isEmpty().get();
        saveButton.disableProperty().set(emptyTextField || saved.get());
    }

    private void saveWAV(File destination)
    {
        recorder.writeWAV(destination);
        savedAs(filenameField.getText());
    }

    private void savedAs(String name)
    {
        lastSaveName = name;
        changed(false);
    }

    /**
     * Set the directory to save sounds into for this project.  May be null.
     */
    public void setProjectSoundDir(File projectSoundDir)
    {
        this.projectSoundDir = projectSoundDir;
    }
}