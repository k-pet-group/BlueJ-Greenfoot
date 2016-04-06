/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2016  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.gui.soundrecorder.SoundRecorderControls;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;

public class ToggleSoundAction extends ToggleAction 
{
    private SoundRecorderControls recorder;
    
    public ToggleSoundAction(String title, GProject project) {
        super(title, project);
    }

    @Override
    public ButtonModel getToggleModel() 
    {
        return new JToggleButton.ToggleButtonModel() {
            
            /**
             * Returns whether or not the sound recorder window is currently visible.
             */
            @Override
            public boolean isSelected()
            {
                return recorder != null && recorder.isVisible();
            }

            /**
             * @param b Set the state to this value
             */
            @Override
            public void setSelected(boolean b)
            {
                if (b) {
                    if (recorder == null) {
                        recorder = new SoundRecorderControls(project);
                    }
                    recorder.setVisible(true);
                } else if (recorder != null) {
                    recorder.closeAndStopRecording();
                }
            }
        };
    }
}
