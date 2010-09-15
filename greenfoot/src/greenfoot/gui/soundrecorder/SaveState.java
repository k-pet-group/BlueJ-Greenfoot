/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.soundrecorder;

import greenfoot.sound.SoundRecorder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;

/**
 * This class manages the saving aspect of SoundRecorderControls:
 * keeping track of whether the sound has changed since last save,
 * displaying the overwrite dialog, updating the status message
 */
class SaveState
{
    private final SoundRecorder recorder;
    private Component parent;

    private boolean changedSinceSave = false;
    private String lastSaveName = null;
    private JTextField filenameField;
    private JButton saveButton;
    private JLabel messageLabel;
    private String savedText;
    private String notSavedText;

    SaveState(Component parent, SoundRecorder recorder)
    {
        this.parent = parent;
        this.recorder = recorder;
        savedText = Config.getString("soundRecorder.saved");
        notSavedText = Config.getString("soundRecorder.notSaved");
    }

    
    public boolean hasChangedSinceSave()
    {
        return changedSinceSave;
    }
    
    private void savedAs(String name)
    {
        changedSinceSave = false;
        lastSaveName = name;
        updateSaveButtonAndLabel();
    }
    
    public void changed()
    {
        changedSinceSave = true;
        updateSaveButtonAndLabel();
    }

    // Updates the save button based on whether the filename field is blank (and whether a recording exists)
    private void updateSaveButtonAndLabel()
    {
        boolean differentFromSaved = !filenameField.getText().equals(lastSaveName) || changedSinceSave;
        
        if (recorder.getRawSound() != null) {
            messageLabel.setText(differentFromSaved ? notSavedText : savedText);
        }
        
        saveButton.setEnabled(recorder.getRawSound() != null && !filenameField.getText().isEmpty()
                  && differentFromSaved);
    }
    
    // Builds the save row: a filename field and save button
    public Box buildSaveBox(final String projectSoundDir)
    {
        Box saveBox = new Box(BoxLayout.X_AXIS);
        saveBox.add(new JLabel(Config.getString("soundRecorder.filename") + ": "));
        filenameField = new JTextField();
        filenameField.setMaximumSize(new Dimension(Short.MAX_VALUE, filenameField.getPreferredSize().height));
        filenameField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e)
            {
                updateSaveButtonAndLabel();
            }
            
            public void insertUpdate(DocumentEvent e)
            {
                updateSaveButtonAndLabel();                
            }
            
            public void changedUpdate(DocumentEvent e)
            {                
            }
        });
        saveBox.add(filenameField);
        saveBox.add(new JLabel(".wav"));
        
        saveBox.add(Box.createHorizontalStrut(12));
        
        saveButton = new JButton(Config.getString("soundRecorder.save"));
        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (projectSoundDir != null) {
                    File destination = new File(projectSoundDir + filenameField.getText() + ".wav");
                    if (destination.exists()) {
                        String[] options = null;
                        int overwrite;
                        if (Config.isMacOS()) {
                            options = new String[] { BlueJTheme.getCancelLabel(), Config.getString("soundRecorder.overwrite") };
                            overwrite = 1;
                        }
                        else {
                            options = new String[] { Config.getString("soundRecorder.overwrite"), BlueJTheme.getCancelLabel() };
                            overwrite = 0;
                        }
                        
                        if (overwrite == JOptionPane.showOptionDialog(parent,
                          Config.getString("soundRecorder.overwrite.part1") + destination.getName() + Config.getString("soundRecorder.overwrite.part2"),
                          Config.getString("soundRecorder.overwrite.title"),
                          JOptionPane.YES_NO_OPTION,
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          options, options[overwrite])) {
                            recorder.writeWAV(destination);
                            savedAs(filenameField.getText());
                        }
                    } else {
                        recorder.writeWAV(destination);
                        savedAs(filenameField.getText());
                        
                    }
                    
                }
            }
        });
        saveBox.add(saveButton);
        
        return saveBox;
    }


    JLabel createLabel()
    {
        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.GRAY);
        messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        Font font = messageLabel.getFont();
        font = font.deriveFont(10.0f);
        messageLabel.setFont(font);
        return messageLabel;
    }
}