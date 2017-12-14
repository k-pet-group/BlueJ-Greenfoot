/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2017  Poul Henriksen and Michael Kolling
 
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


/**
 * This class manages the saving aspect of SoundRecorderControls:
 * keeping track of whether the sound has changed since last save,
 * displaying the overwrite dialog, updating the status message
 *
 * @author Amjad Altadmri
 */
class SaveState
{
    private final Window parent;
    private final SoundRecorder recorder;
    private final TextField filenameField = new TextField();
    private final Button saveButton = new Button(Config.getString("soundRecorder.save"));
    private String lastSaveName = null;
    private SimpleBooleanProperty changedSinceSave = new SimpleBooleanProperty(false);

    SimpleBooleanProperty saved = new SimpleBooleanProperty(true);

    SaveState(Window parent, SoundRecorder recorder)
    {
        this.parent = parent;
        this.recorder = recorder;
    }

    /**
     * Builds the save row: a filename field and save button
     *
     * @param projectSoundDir
     * @return
     */
    HBox buildSaveBox(final String projectSoundDir)
    {
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> {
            if (projectSoundDir != null)
            {
                File destination = new File(projectSoundDir + filenameField.getText() + ".wav");
                if (destination.exists())
                {
                    boolean overwrite = DialogManager.askQuestionFX(parent, "sound-recorder-file-exists", new String[] {destination.getName()}) == 0;
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
        changedSinceSave.setValue(value);
    }

    /**
     * Updates the save button based on whether the filename field is blank (and whether a recording exists)
     */
    private void updateSavedStatus()
    {
        SimpleBooleanProperty differentFromSaved = new SimpleBooleanProperty();
        differentFromSaved.bind(filenameField.textProperty().isEqualTo(lastSaveName).not().or(changedSinceSave));
        saveButton.disableProperty().bind(recorder.getRecordedProperty().isNull().or(filenameField.textProperty().isEmpty()).or(differentFromSaved.not()));
        JavaFXUtil.addChangeListener(differentFromSaved, this::setSaved);
        JavaFXUtil.addChangeListener(changedSinceSave, this::setSaved);
    }

    private void setSaved(Boolean newValue)
    {
        if (recorder !=null && recorder.getRecordedProperty().isNotNull().get())
        {
            saved.set(!newValue);
        }
    }

    private void saveWAV(File destination)
    {
        recorder.writeWAV(destination);
        savedAs(filenameField.getText());
    }

    private void savedAs(String name)
    {
        changedSinceSave.set(false);
        lastSaveName = name;
    }
}