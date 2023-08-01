/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2023  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.sound;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.prefmgr.MiscPrefPanelItem;
import bluej.prefmgr.PrefMgr;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * A GUI preference panel allowing selection of recording and playback device.
 * 
 * The identifier is saved as a String.  If we can't find it on load, we treat
 * it as the default device (which is stored as null here, and the empty string in a file)
 */
@OnThread(Tag.FXPlatform)
public class SoundPreferencePanel extends GridPane implements MiscPrefPanelItem
{
    private final ComboBox<Mixer> inputDevice = new ComboBox<>();
    private final ComboBox<Mixer> outputDevice = new ComboBox<>();
    
    public SoundPreferencePanel()
    {
        // Initialise available items first:
        
        // This is the right way round; target lines are input devices, source lines are output devices:
        initialize(inputDevice, true, mixer -> Arrays.stream(mixer.getTargetLineInfo()).anyMatch(i -> i.getLineClass().equals(TargetDataLine.class)));
        initialize(outputDevice, false, mixer -> Arrays.stream(mixer.getSourceLineInfo()).anyMatch(i -> i.getLineClass().equals(SourceDataLine.class)));
        
        Label inputLabel = new Label(Config.getString("greenfoot.sound.input"));
        add(inputLabel, 0, 0);
        add(inputDevice, 1, 0);
        Label outputLabel = new Label(Config.getString("greenfoot.sound.output"));
        add(outputLabel, 0, 1);
        add(outputDevice, 1, 1);
        Label warning = new Label(Config.getString("greenfoot.sound.warning"));
        warning.setWrapText(true);
        add(warning, 0, 2);
        setColumnSpan(warning, 2);
        setVgap(7);
        setHgap(5);
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(GridPane.USE_PREF_SIZE);
        cc0.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setMinWidth(GridPane.USE_PREF_SIZE);
        getColumnConstraints().addAll(cc0, cc1);
    }

    /**
     * Initialize the given combo box with the list of available mixers
     * 
     * @param audioDropdown The combo box to add the items to.  The selection will be loaded from the preferences file, or system default if blank
     * @param input Whether this an input device (true) or output device (false)
     * @param valid The test for whether the mixer is a valid option to add.  Some mixers are things like volume controls, not actual input or output devices.
     */
    private static void initialize(ComboBox<Mixer> audioDropdown, boolean input, Predicate<Mixer> valid)
    {
        audioDropdown.getItems().clear();
        // We always add null, which is the system default:
        audioDropdown.getItems().add(null);
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (valid.test(mixer))
            {
                audioDropdown.getItems().add(mixer);
            }
        }
        
        // Load item from preferences file:
        audioDropdown.getSelectionModel().select(SoundUtils.loadMixer(audioDropdown.getItems(), input));

        audioDropdown.setConverter(new StringConverter<Mixer>()
        {
            @Override
            public String toString(Mixer object)
            {
                return object == null ? "Default" : object.getMixerInfo().getName();
            }

            @Override
            public Mixer fromString(String string)
            {
                return null;
            }
        });
    }

    /**
     * Gets the selected input mixer from the combo box, or null if the default should be used.
     */
    public Mixer getInputMixer()
    {
        return inputDevice.getValue();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void beginEditing(Project project)
    {
        inputDevice.getSelectionModel().select(SoundUtils.loadMixer(inputDevice.getItems(), true));
        outputDevice.getSelectionModel().select(SoundUtils.loadMixer(outputDevice.getItems(), false));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void commitEditing(Project project)
    {
        Config.putPropString(PrefMgr.GREENFOOT_SOUND_INPUT_DEVICE, inputDevice.getValue() == null ? "" : inputDevice.getValue().getMixerInfo().toString());
        Config.putPropString(PrefMgr.GREENFOOT_SOUND_OUTPUT_DEVICE, outputDevice.getValue() == null ? "" : outputDevice.getValue().getMixerInfo().toString());
    }

    @Override
    public void revertEditing(Project project)
    {
    }
    
    @Override
    public List<Node> getMiscPanelContents()
    {
        return List.of(this);
    }

    @Override
    public String getMiscPanelTitle()
    {
        return Config.getString("greenfoot.sound.prefs");
    }
}
