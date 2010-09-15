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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.Utility;

/**
 * The GUI class for the sound recorder.
 * 
 * @author neil
 */
public class SoundRecorderControls extends JFrame implements WindowListener
{
    SoundRecorder recorder = new SoundRecorder();
    
    // Indicates whether the selection is currently in use (and should be displayed/used)
    // If the selection is zero width, this will be false
    private boolean selectionActive = false;
    // Indicates whether the user is currently in the middle of drawing a selection
    private boolean selectionDrawing = false;
    //Begin is where the user started dragging, end is where the user has dragged it to.
    //It's valid for end to be before beginning, if they drag right to left.
    private float selectionBegin;
    private float selectionEnd;
    
    private JButton trim;
    private JButton playStop;
    private JButton recordStop;
    
    private boolean playing = false;
    //Position will only be valid when "playing" is true:
    private long playbackPosition;
    private boolean recording = false;
    // Reference will be only be valid when "recording" is true:
    private AtomicReference<List<byte[]>> currentRecording;

    private SoundPanel soundPanel;
    
    private SaveState saveState = new SaveState(this, recorder);
    
    private final String playLabel;
    private final String playSelectionLabel;
    private final String stopPlayLabel;
    private final String recordLabel;
    private final String stopRecordLabel;
    
    /**
     * Creates a SoundRecorderDialog that will save the sounds
     * in the sounds directory of the given project.
     */
    public SoundRecorderControls(GProject project)
    {        
        playLabel = Config.getString("soundRecorder.play");
        playSelectionLabel = Config.getString("soundRecorder.playSelection");
        stopPlayLabel = Config.getString("soundRecorder.stopPlay");
        recordLabel = Config.getString("soundRecorder.record");
        stopRecordLabel = Config.getString("soundRecorder.stopRecord");
        setTitle(Config.getString("soundRecorder.title"));
        buildUI(project);
        
        Image icon = BlueJTheme.getApplicationIcon("greenfoot");
        if (icon != null) {
            setIconImage(icon);
        }
    }
    
    // Builds the controls: record/trim/play
    private JPanel buildControlBox()
    {
        recordStop = new JButton(recordLabel);
        Utility.changeToMacButton(recordStop);
        recordStop.setFocusable(false);
        recordStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (!recording) {
                    //Start recording
                    currentRecording = recorder.startRecording();
                    recordStop.setText(stopRecordLabel);
                    playStop.setEnabled(false);
                    recording = true;
                    new Timer().scheduleAtFixedRate(new TimerTask() {
                        List<byte[]> lastValue = null;
                        public void run()
                        {
                            List<byte[]> curValue = currentRecording.get();
                            if (curValue != lastValue)
                                soundPanel.repaint();
                            if (lastValue != null && curValue == null)
                                cancel();
                            lastValue = curValue;
                            
                        }
                    }, 100, 200);
                } else {
                    //Stop recording
                    stopRecording();
                }
            }
        });
        
        trim = new JButton(Config.getString("soundRecorder.trim"));
        trim.setEnabled(false);
        trim.setFocusable(false);
        Utility.changeToMacButton(trim);
        trim.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                recorder.trim(Math.min(selectionBegin, selectionEnd), Math.max(selectionBegin, selectionEnd));
                saveState.changed();
                selectionActive = false;
                updateButtons();
                repaint();
            }
        });
        
        playStop = new JButton(playLabel);
        Utility.changeToMacButton(playStop);
        playStop.setFocusable(false);
        playStop.setEnabled(false);
        playStop.addActionListener(new Player());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        controls.add(recordStop);
        controls.add(playStop);
        controls.add(trim);

        return controls;
    }
    
    
    
    private void buildUI(GProject project)
    {
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);

        soundPanel = new SoundPanel();

        JLabel messageLabel = saveState.createLabel();

        JPanel soundAndControls = new JPanel(new BorderLayout(0,0));
        
            soundAndControls.add(soundPanel, BorderLayout.CENTER);

            JPanel controls = new JPanel(new BorderLayout(0,0));
                controls.add(buildControlBox(), BorderLayout.CENTER);
                controls.add(messageLabel, BorderLayout.SOUTH);
            soundAndControls.add(controls, BorderLayout.SOUTH);
            soundAndControls.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
 
        contentPane.add(soundAndControls);
        
        contentPane.add(Box.createVerticalStrut(12));      
        
        contentPane.add(saveState.buildSaveBox(getSoundDir(project)));
        
        contentPane.add(Box.createVerticalStrut(12));

        JButton done = new JButton(Config.getString("soundRecorder.done"));
        done.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                closeAndStopRecording();
            }
        });
        done.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(done);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        
        pack();
        setVisible(true);
    }

    // Gets the sounds directory for the given project, or null if there is a problem
    private static String getSoundDir(GProject project)
    {
        return project.getDir() + File.separator + "sounds" + File.separator;
    }
    
    // Updates trim and play buttons based on whether the selection is active
    private void updateButtons()
    {
        trim.setEnabled(selectionActive);
        playStop.setText(selectionActive ? playSelectionLabel : playLabel);
    }
    
    /**
     * A class that handles playing sound, controlled by a play/stop button (for which this is the ActionListener).
     *
     */
    private class Player implements ActionListener, SoundPlaybackListener
    {
        private final Timer tim = new Timer();
        private TimerTask repaintWhilePlaying;
        private SoundStream stream;
                
        public void actionPerformed(ActionEvent e)
        {
            if (playing) {
                stream.stop();
                //Everything will be done in the stop callback, below
            } else {
                MemoryAudioInputStream memoryStream;
                final int start;
                if (selectionActive) {
                    start = getSelectionStartOffset();
                    int len = getSelectionFinishOffset() - start;
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), start, len, recorder.getFormat());
                } else {
                    start = 0;
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), recorder.getFormat());
                }
                stream = new SoundStream(memoryStream, this);
                playing = true;
                playbackPosition = start;
                stream.play();
                playStop.setText(stopPlayLabel);
                recordStop.setEnabled(false);
                
                repaintWhilePlaying = new TimerTask() {
                    public void run()
                    {
                        playbackPosition = start + stream.getLongFramePosition();
                        soundPanel.repaint();
                    }
                }; 
                tim.scheduleAtFixedRate(repaintWhilePlaying, 50, 100);
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

        public void playbackStopped(Sound sound)
        {
            updateButtons();
            recordStop.setEnabled(true);
            repaintWhilePlaying.cancel();
            playing = false;
            soundPanel.repaint();
        }

        public void soundClosed(Sound sound)
        {
            // Nothing to do            
        }
    }

    /**
     * A panel for displaying a recorded sound.
     */
    private class SoundPanel extends JPanel implements MouseListener, MouseMotionListener
    {       
        private SoundPanel()
        {
            setPreferredSize(new Dimension(400, 200));
            setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        
        @SuppressWarnings("cast")
        protected void paintComponent(Graphics g)
        {
            int width = getWidth();
            int height = getHeight();
            int middle = height / 2;
            int halfHeight = height / 2;
            byte[] sound = recorder.getRawSound();
            
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            
            if (recording || (sound != null && sound.length > 0)) {
                if (selectionActive) {
                    g.setColor(Color.GRAY);
                    g.fillRect((int)(Math.min(selectionBegin, selectionEnd) * width), 0, (int)(Math.abs(selectionBegin - selectionEnd) * width), height);
                }
                
                // Get this outside the loop to make sure it's consistent:
                byte[][] rec = null;
                int recLength = 0;
                if (recording) {
                    List<byte[]> recList = currentRecording.get();
                    if (recList != null) {
                        rec = recList.toArray(new byte[0][]);
                        for (byte[] chunk : rec) {
                            //Shouldn't have any null chunks, but just in case:
                            int chunkLength = chunk == null ? 0 : chunk.length;
                            recLength += chunkLength;
                        }
                    }
                }
                else
                    recLength = sound.length;
                int curRecChunk = 0;
                int prevChunksLength = 0;
                
                for (int i = 0; i < width; i++) {
                    float pos = (float)i / (float)width;
                    float f;
                    // Use rec test rather than "recording" in case "recording" changes mid-paint:
                    if (rec != null) {
                        int index = (int)(pos * (float)recLength);
                        if (recLength == 0 || index >= recLength) {
                            // No data yet:
                            f = 0.0f;
                        } else {                            
                            // We have a list of chunks that make up the current recording:
                            //  Skip forward to right chunk if needed:
                            while (index >= prevChunksLength + rec[curRecChunk].length) {
                                prevChunksLength += rec[curRecChunk].length;
                                curRecChunk += 1;
                            }
                            f = (float)rec[curRecChunk][index - prevChunksLength] / 128.0f;
                        }
                    } else {
                        int index = (int)(pos * (float)sound.length);
                        f = (float)sound[index] / 128.0f;
                    }
                    //Looks slightly better if we don't draw all the way to the edge, so use 90%:
                    int waveHeight = (int)(halfHeight * f * 0.9f);
                    
                    if (inSelection(pos)) {
                        g.setColor(Color.YELLOW);
                    } else {
                        g.setColor(Color.GREEN);
                    }
                    g.drawLine(i, middle - waveHeight, i, middle + waveHeight);

                }
                
                if (playing) {
                    g.setColor(Color.RED);
                    float playPosRel = (float)playbackPosition / (float)recLength;
                    int pos = (int)(playPosRel * (float)width);
                    g.drawLine(pos, 0, pos, height);
                }
            }
        }
        
        // Works out whether the given number (0->1) is inside the current selection (if there is one)
        private boolean inSelection(float f)
        {
            return selectionActive && f >= Math.min(selectionBegin, selectionEnd)
              && f <= Math.max(selectionBegin, selectionEnd);
        }

        public void mousePressed(MouseEvent e)
        {
            if (recorder.getRawSound() != null) {
                // Selection only becomes active if they drag.
                // Otherwise this is just a click and actually ends up removing the selection:
                selectionActive = false;
                selectionDrawing = true;
                selectionBegin = calculatePosition(e.getPoint());
                selectionEnd = selectionBegin;
            }            
        }

        public void mouseReleased(MouseEvent e)
        {
            if (selectionDrawing) {
                selectionDrawing = false;
                selectionEnd = calculatePosition(e.getPoint());
                if (selectionBegin == selectionEnd)
                    selectionActive = false;
                repaint();
            }
            updateButtons();
        }    
        
                
        private float calculatePosition(Point p)
        {
            float pos = (float)p.x / getWidth();
            // Clamp to the range 0->1:
            pos = Math.max(0, pos);
            pos = Math.min(1, pos);
            return pos;
        }

        public void mouseDragged(MouseEvent e)
        {
            if (selectionDrawing) {
                selectionEnd = calculatePosition(e.getPoint());
                selectionActive = true;
                repaint();
            }
            
        }

        public void mouseClicked(MouseEvent e) {}
        
        public void mouseEntered(MouseEvent e) {}
        
        public void mouseExited(MouseEvent e) {}

        public void mouseMoved(MouseEvent e) {}
    }
    
    // Gets the start of the selection as an index into the raw sound array 
    private int getSelectionStartOffset()
    {
        float start = Math.min(selectionBegin, selectionEnd);
        float length = recorder.getRawSound().length;
        return (int)(start * length);
    }
    
    // Gets the finish of the selection as an index into the raw sound array
    private int getSelectionFinishOffset()
    {
        float finish = Math.max(selectionBegin, selectionEnd);
        float length = recorder.getRawSound().length;
        return (int)(finish * length);
    }
    
    
    // WindowListener:
    
    public void windowClosing(WindowEvent e)
    {
        closeAndStopRecording();
    }

    public void closeAndStopRecording()
    {
        stopRecording();
        setVisible(false);
        
    }

    public void windowActivated(WindowEvent e) {}
    
    public void windowClosed(WindowEvent e) {}
    
    public void windowDeactivated(WindowEvent e) {}
    
    public void windowDeiconified(WindowEvent e) {}
    
    public void windowIconified(WindowEvent e) {}
    
    public void windowOpened(WindowEvent e) {}

    private void stopRecording()
    {
        if (recording) {
            recorder.stopRecording();
            playStop.setEnabled(true);
            saveState.changed();
            soundPanel.repaint();
            recordStop.setText(recordLabel);
            recording = false;
        }
    }

}
