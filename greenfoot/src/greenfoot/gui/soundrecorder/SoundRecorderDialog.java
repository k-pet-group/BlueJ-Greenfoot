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

import greenfoot.core.GProject;
import greenfoot.sound.MemoryAudioInputStream;
import greenfoot.sound.Sound;
import greenfoot.sound.SoundPlaybackListener;
import greenfoot.sound.SoundRecorder;
import greenfoot.sound.SoundStream;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.rmi.RemoteException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * The GUI class for the sound recorder.
 * 
 * @author neil
 *
 */
public class SoundRecorderDialog extends JDialog
{
    private SoundRecorder recorder = new SoundRecorder();
    
    private boolean selectionActive = false;
    private boolean selectionDrawing = false;
    //Begin is where it started, end is where it currently extends to
    //It's valid for end to be before beginning.
    private float selectionBegin;
    private float selectionEnd;
    
    private JButton trim;
    private JButton playStop;
    private JButton recordStop;
    private JTextField filenameField;
    private JButton saveButton;
    
    boolean playing = false;

    private SoundPanel soundPanel;
    
    private String projectSoundDir;
    
    public SoundRecorderDialog(GProject project)
    {
        soundPanel = new SoundPanel();
        recordStop = new JButton("Record");
        recordStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                //TODO keep track better than this
                if (recordStop.getText().equals("Record")) {
                    //Start recording
                    recorder.startRecording();
                    recordStop.setText("Stop recording");
                    playStop.setEnabled(false);
                } else {
                    //Stop recording
                    recorder.stopRecording();
                    playStop.setEnabled(true);
                    soundPanel.repaint();
                    recordStop.setText("Record");
                }
            }
        });
        
        trim = new JButton("Trim to selection");
        trim.setEnabled(false);
        trim.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                recorder.trim(Math.min(selectionBegin, selectionEnd), Math.max(selectionBegin, selectionEnd));
                selectionActive = false;
                updateButtons();
                repaint();
            }
        });
        
        playStop = new JButton("Play");
        playStop.setEnabled(false);
        playStop.addActionListener(new Player());
        
        Box box = new Box(BoxLayout.Y_AXIS);
        
        box.add(recordStop);
        box.add(soundPanel);
        box.add(trim);
        box.add(playStop);
        
        Box saveBox = new Box(BoxLayout.X_AXIS);
        saveBox.add(new JLabel("Filename: "));
        filenameField = new JTextField();
        filenameField.getDocument().addDocumentListener(new DocumentListener() {
            
            public void removeUpdate(DocumentEvent e)
            {
                updateSaveButton();
                
            }
            
            public void insertUpdate(DocumentEvent e)
            {
                updateSaveButton();                
            }
            
            public void changedUpdate(DocumentEvent e)
            {                
            }
        });
        saveBox.add(filenameField);
        saveBox.add(new JLabel(".wav"));
        saveButton = new JButton("Save");
        saveButton.setEnabled(false);
        projectSoundDir = null;
        try {
            projectSoundDir = project.getDir() + "/sounds/";
        }
        catch (RemoteException e) {
            projectSoundDir = null;
            Debug.reportError("Project not open when recording sounds", e);
        }
        catch (ProjectNotOpenException e) {
            projectSoundDir = null;
            Debug.reportError("Project not open when recording sounds", e);
        }
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (projectSoundDir != null)
                    recorder.writeWAV(new File(projectSoundDir + filenameField.getText() + ".wav"));                
            }
        });
        saveBox.add(saveButton);
        
        box.add(saveBox);
        
        add(box);
        pack();
        setVisible(true);
    }
    
    private void updateButtons()
    {
        trim.setEnabled(selectionActive);
        playStop.setText(selectionActive ? "Play selected" : "Play");
    }
    
    private void updateSaveButton()
    {
        saveButton.setEnabled(recorder.getRawSound() != null && !filenameField.getText().isEmpty());
    }
    
    private class Player implements ActionListener, SoundPlaybackListener
    {
        //private final Timer tim = new Timer();
        //private TimerTask repaintWhilePlaying;
        private SoundStream stream;
                
        public void actionPerformed(ActionEvent e)
        {
            if (playing) {
                stream.stop();
                //Everything will be done in the stop callback, below
            } else {
                MemoryAudioInputStream memoryStream;
                if (selectionActive) {
                    int start = getSelectionStartOffset();
                    int len = getSelectionFinishOffset() - start;
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), start, len, recorder.getFormat());
                } else {
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), recorder.getFormat());
                }
                stream = new SoundStream(memoryStream, this);
                playing = true;
                stream.play();
                playStop.setText("Stop playing");
                recordStop.setEnabled(false);
                
                /*
                repaintWhilePlaying = new TimerTask() {
                    public void run()
                    {
                        soundPanel.repaint();               
                    }
                }; 
                tim.scheduleAtFixedRate(repaintWhilePlaying, 200, 200);
                */
            }
        }

        public void playbackPaused(Sound sound)
        {
            //Shouldn't happen as we don't have a pause button
        }

        public void playbackStarted(Sound sound)
        {
            //Nothing to do            
        }

        @Override
        public void playbackStopped(Sound sound)
        {
            playStop.setText(selectionActive ? "Play selected" : "Play");
            recordStop.setEnabled(true);
            //repaintWhilePlaying.cancel();
            playing = false;
        }

        public void soundClosed(Sound sound)
        {
            // Nothing to do            
        }
    }

    private class SoundPanel extends JPanel implements MouseListener, MouseMotionListener
    {       
        private SoundPanel()
        {
            setMinimumSize(new Dimension(400, 200));
            setPreferredSize(getMinimumSize());
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        
        protected void paintComponent(Graphics g)
        {
            int width = getWidth();
            int height = getHeight();
            int middle = height / 2;
            int halfHeight = height / 2;
            byte[] sound = recorder.getRawSound();
            
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            
            if (sound != null && sound.length > 0) {
                if (selectionActive) {
                    g.setColor(Color.GRAY);
                    g.fillRect((int)(Math.min(selectionBegin, selectionEnd) * width), 0, (int)(Math.abs(selectionBegin - selectionEnd) * width), height);
                }
                
                for (int i = 0; i < width; i++) {
                    float pos = (float)i / (float)width;
                    float f = getValue(sound, pos);
                    int waveHeight = (int)(halfHeight * f);
                    if (inSelection(pos)) {
                        g.setColor(Color.YELLOW);
                    } else {
                        g.setColor(Color.GREEN);
                    }
                    g.drawLine(i, middle - waveHeight, i, middle + waveHeight);

                }
                
                // Putting a line that tracks the play is difficult.
                // The below doesn't work because it tracks where the buffer has been read up to,
                // which is often around a second away from where it is currently playing
                /*
                if (memoryStream != null && playing) {
                    g.setColor(Color.WHITE);
                    float playLength = selectionActive ? (int)(Math.abs(selectionBegin - selectionEnd) * sound.length) : sound.length;
                    int pos = (int)((float)width * ((float)memoryStream.getPosition() / playLength));
                    g.drawLine(pos, 0, pos, height);
                }
                */
            }
        }
        
        private boolean inSelection(float f)
        {
            return selectionActive && f >= Math.min(selectionBegin, selectionEnd)
              && f <= Math.max(selectionBegin, selectionEnd);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (recorder.getRawSound() != null) {
                selectionActive = false;
                selectionDrawing = true;
                selectionBegin = calculatePosition(e.getPoint());
                selectionEnd = selectionBegin;
            }            
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (selectionDrawing) {
                selectionDrawing = false;
                selectionEnd = calculatePosition(e.getPoint());
                repaint();
            }
            updateButtons();
        }    
        
        
        
        private float calculatePosition(Point p)
        {
            return ((float)p.x / getWidth());
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (selectionDrawing) {
                selectionEnd = calculatePosition(e.getPoint());
                selectionActive = true;
                repaint();
            }
            
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            // TODO Auto-generated method stub
            
        }
    }
    
    private static float getValue(byte[] arr, float x)
    {
        int index = (int)(x * (float)arr.length);
        return (float)arr[index] / 256.0f;
    }
    
    private int getSelectionStartOffset()
    {
        float start = Math.min(selectionBegin, selectionEnd);
        return (int)(start * (float)recorder.getRawSound().length);
    }
    
    private int getSelectionFinishOffset()
    {
        float start = Math.max(selectionBegin, selectionEnd);
        return (int)(start * (float)recorder.getRawSound().length);
    }
}
