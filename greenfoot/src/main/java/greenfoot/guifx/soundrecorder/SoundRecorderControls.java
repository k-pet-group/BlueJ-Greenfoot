/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2014,2017  Poul Henriksen and Michael Kolling
 
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

import bluej.BlueJTheme;
import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import greenfoot.sound.MemoryAudioInputStream;
import greenfoot.sound.Sound;
import greenfoot.sound.SoundPlaybackListener;
import greenfoot.sound.SoundRecorder;
import greenfoot.sound.SoundStream;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * The GUI class for the sound recorder.
 * 
 * @author neil
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class SoundRecorderControls extends Stage
{
    private Player player = new Player();
    private SoundRecorder recorder = new SoundRecorder();

    // Indicates whether the selection is currently in use (and should be displayed/used)
    // If the selection is zero width, this will be false
    private boolean selectionActive = false;
    // Indicates whether the user is currently in the middle of drawing a selection
    private boolean selectionDrawing = false;
    //Begin is where the user started dragging, end is where the user has dragged it to.
    //It's valid for end to be before beginning, if they drag right to left.
    private double selectionBegin;
    private double selectionEnd;

    private boolean playing = false;
    //Position will only be valid when "playing" is true:
    private long playbackPosition;
    private boolean recording = false;
    // Reference will be only be valid when "recording" is true:
    private AtomicReference<List<byte[]>> currentRecording;
    private SoundPanel soundPanel = new SoundPanel();
    private SaveState saveState = new SaveState(this, recorder);

    private final String playLabel = Config.getString("soundRecorder.play");
    private final String playSelectionLabel = Config.getString("soundRecorder.playSelection");
    private final String stopPlayLabel = Config.getString("soundRecorder.stopPlay");
    private final String recordLabel = Config.getString("soundRecorder.record");
    private final String stopRecordLabel = Config.getString("soundRecorder.stopRecord");

    private Button trim = new Button(Config.getString("soundRecorder.trim"));
    private Button playStop = new Button(playLabel);
    private Button recordStop = new Button(recordLabel);

    private final SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(false);

    /**
     * Creates a SoundRecorderDialog that will save the sounds
     * in the sounds directory of the given project.
     */
    public SoundRecorderControls(Project project)
    {
        this.setWidth(450);
        this.setHeight(400);
        this.setMinWidth(450);
        this.setMinHeight(400);
        setTitle(Config.getString("soundRecorder.title"));
        Image icon = BlueJTheme.getApplicationFxIcon("greenfoot", false);
        if (icon != null)
        {
            getIcons().add(icon);
        }

        setOnShown(e -> showingProperty.set(true));
        setOnHidden(e -> showingProperty.set(false));
        buildUI();
        setProject(project);
    }

    private void buildUI()
    {
        BorderPane soundAndControls = new BorderPane(soundPanel, null, null, buildControlBox(), null);
        soundAndControls.setPadding(new Insets(12));
        BorderPane.setMargin(soundPanel, new Insets(12,12,12,12));
        soundAndControls.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(5, 5, 5, 5, false), null)));//new Insets(12)
        VBox.setVgrow(soundAndControls, Priority.ALWAYS);

        Button closeButton = new Button(Config.getString("soundRecorder.close"));
        JavaFXUtil.addChangeListener(saveState.savedProperty(), newValue ->
                closeButton.setText(Config.getString(newValue ? "soundRecorder.close" : "soundRecorder.close.without.saving")));
        closeButton.setOnAction(event -> close());
        this.setOnCloseRequest(event -> stopRecording());

        VBox contentPane = new VBox(20);
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPadding(new Insets(12));
        contentPane.getChildren().addAll(soundAndControls, saveState.buildSaveBox(), closeButton);
        this.setScene(new Scene(contentPane));
    }

    /**
     * Change the project associated with this sound recorder.
     */
    public void setProject(Project project)
    {
        saveState.setProjectSoundDir(getSoundDir(project));
    }

    /**
     * Builds the controls: record/trim/play
     */
    private Pane buildControlBox()
    {
        recordStop.setFocusTraversable(false);
        recordStop.setOnAction(event ->
        {
            if (!recording)
            {
                //Start recording
                currentRecording = recorder.startRecording();
                recordStop.setText(stopRecordLabel);
                playStop.setDisable(true);
                recording = true;
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    List<byte[]> lastValue = null;
                    public void run()
                    {
                        List<byte[]> curValue = currentRecording.get();
                        if (curValue != lastValue)
                            Platform.runLater(soundPanel::paintComponent);
                        if (lastValue != null && curValue == null)
                            cancel();
                        lastValue = curValue;
                    }
                }, 100, 200);
            }
            else
            {
                stopRecording();
            }
        });

        trim.setDisable(true);
        trim.setFocusTraversable(false);
        trim.setOnAction(event ->
        {
            recorder.trim(Math.min(selectionBegin, selectionEnd), Math.max(selectionBegin, selectionEnd));
            saveState.changed(true);
            selectionActive = false;
            updateButtons();
            soundPanel.paintComponent();
        });

        playStop.setDisable(true);
        playStop.setFocusTraversable(false);
        playStop.setOnAction(event -> player.act());

        HBox controls = new HBox(10); //new Pane() //new FlowLayout(FlowLayout.CENTER, 2, 0));
        controls.setAlignment(Pos.CENTER);
        controls.getChildren().addAll(recordStop, playStop, trim);
        return controls;
    }

    /**
     * Gets the sounds directory for the given project.  Project may be null.
     *
     * @return the sound directory as a file.  Will return null if the project is null.
     */
    private static File getSoundDir(Project project)
    {
        if (project != null)
        {
            return new File(project.getProjectDir(), "sounds");
        }
        else
        {
            return null;
        }
    }

    /**
     * Updates trim and play buttons based on whether the selection is active
     */
    private void updateButtons()
    {
        trim.setDisable(!selectionActive);
        playStop.setText(selectionActive ? playSelectionLabel : playLabel);
    }
    
    /**
     * A class that handles playing sound, controlled by a play/stop button (for which this is the ActionListener).
     */
    private class Player implements SoundPlaybackListener
    {
        private final Timer timer = new Timer();
        private TimerTask repaintWhilePlaying;
        private SoundStream stream;

        @OnThread(Tag.FXPlatform)
        public void act()
        {
            if (playing)
            {
                if (stream != null)
                    stream.stop(); //Everything will be done in the stop callback, below
            }
            else
            {
                MemoryAudioInputStream memoryStream;
                final int start;
                if (selectionActive)
                {
                    start = getSelectionStartOffset();
                    int len = getSelectionFinishOffset() - start;
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), start, len, recorder.getFormat());
                }
                else
                {
                    start = 0;
                    memoryStream = new MemoryAudioInputStream(recorder.getRawSound(), recorder.getFormat());
                }
                stream = new SoundStream(memoryStream, this);
                playing = true;
                playbackPosition = start;
                stream.play();
                playStop.setText(stopPlayLabel);
                recordStop.setDisable(true);
                
                repaintWhilePlaying = new TimerTask() {
                    @Override
                    public void run()
                    {
                        playbackPosition = start + stream.getLongFramePosition();
                        Platform.runLater(soundPanel::paintComponent);
                    }
                };
                timer.scheduleAtFixedRate(repaintWhilePlaying, 50, 100);
            }
        }

        @OnThread(Tag.Any)
        public void playbackPaused(Sound sound)
        {
            //Shouldn't happen as we don't have a pause button
        }

        @OnThread(Tag.Any)
        public void playbackStarted(Sound sound)
        {
            //Nothing to do
        }

        @OnThread(Tag.Any)
        public void playbackStopped(Sound sound)
        {
            Platform.runLater(() ->
            {
                updateButtons();
                recordStop.setDisable(false);
                repaintWhilePlaying.cancel();
                playing = false;
                soundPanel.paintComponent();
            });
        }

        @OnThread(Tag.Any)
        public void soundClosed(Sound sound)
        {
            // Nothing to do            
        }
    }

    /**
     * A panel for displaying the recorded sound.
     */
    @OnThread(Tag.FXPlatform)
    private class SoundPanel extends ResizableCanvas
    {
        private SoundPanel()
        {
            this.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
            this.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
            this.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
            onResize = this::paintComponent;
        }

        protected void paintComponent()
        {
            GraphicsContext g = getGraphicsContext2D();

            double width = getWidth();
            double height = getHeight();
            double middle = height / 2;
            double halfHeight = height / 2;
            byte[] sound = recorder.getRawSound();

            g.setFill(Color.BLACK);
            g.fillRect(0, 0, width, height);

            if (recording || (sound != null && sound.length > 0))
            {
                if (selectionActive)
                {
                    g.setFill(Color.GRAY);
                    g.fillRect(Math.min(selectionBegin, selectionEnd) * width, 0,
                               Math.abs(selectionBegin - selectionEnd) * width, height);
                }

                // Get this outside the loop to make sure it's consistent:
                byte[][] rec = null;
                int recLength = 0;
                if (recording)
                {
                    List<byte[]> recList = currentRecording.get();
                    if (recList != null)
                    {
                        rec = recList.toArray(new byte[0][]);
                        for (byte[] chunk : rec)
                        {
                            //Shouldn't have any null chunks, but just in case:
                            int chunkLength = chunk == null ? 0 : chunk.length;
                            recLength += chunkLength;
                        }
                    }
                }
                else
                {
                    recLength = sound.length;
                }

                int curRecChunk = 0;
                int prevChunksLength = 0;

                for (int i = 0; i < width; i++)
                {
                    float pos = (float) i / (float) width;
                    float f = 0;
                    // Use rec test rather than "recording" in case "recording" changes mid-paint:
                    if (rec != null)
                    {
                        int index = (int) (pos * (float) recLength);
                        if (recLength == 0 || index >= recLength)
                        {
                            // No data yet:
                            f = 0.0f;
                        }
                        else
                        {
                            // We have a list of chunks that make up the current recording:
                            //  Skip forward to right chunk if needed:
                            while (index >= prevChunksLength + rec[curRecChunk].length)
                            {
                                prevChunksLength += rec[curRecChunk].length;
                                curRecChunk += 1;
                            }
                            f = (float) rec[curRecChunk][index - prevChunksLength] / 128.0f;
                        }
                    }
                    else if (sound != null)
                    {
                        int index = (int) (pos * (float) sound.length);
                        f = (float) sound[index] / 128.0f;
                    }
                    //Looks slightly better if we don't draw all the way to the edge, so use 90%:
                    int waveHeight = (int) (halfHeight * f * 0.9f);

                    g.setStroke(inSelection(pos) ? Color.YELLOW : Color.LIME);
                    g.strokeLine(i, middle - waveHeight, i, middle + waveHeight);
                }

                if (playing)
                {
                    g.setStroke(Color.RED);
                    float playPosRel = (float) playbackPosition / (float) recLength;
                    int pos = (int) (playPosRel * (float) width);
                    g.strokeLine(pos, 0, pos, height);//
                }
            }
        }

        /**
         *  Works out whether the given number (0->1) is inside the current selection (if there is one)
         *
         * @param f value to test if it is in the selected range
         * @return true if the passed value is within the selected range
         */
        private boolean inSelection(float f)
        {
            return selectionActive && f >= Math.min(selectionBegin, selectionEnd)
              && f <= Math.max(selectionBegin, selectionEnd);
        }

        public void mousePressed(MouseEvent e)
        {
            if (recorder.getRawSound() != null)
            {
                // Selection only becomes active if they drag.
                // Otherwise this is just a click and actually ends up removing the selection:
                selectionActive = false;
                selectionDrawing = true;
                selectionBegin = calculatePosition(e.getX());
                selectionEnd = selectionBegin;
            }            
        }

        public void mouseReleased(MouseEvent e)
        {
            if (selectionDrawing)
            {
                selectionDrawing = false;
                selectionEnd = calculatePosition(e.getX());
                if (selectionBegin == selectionEnd)
                    selectionActive = false;
                paintComponent();
            }
            updateButtons();
        }

        public void mouseDragged(MouseEvent e)
        {
            if (selectionDrawing)
            {
                selectionEnd = calculatePosition(e.getX());
                selectionActive = true;
                paintComponent();
            }
        }

        private double calculatePosition(double x)
        {
            double pos = x / getWidth();
            // Clamp to the range 0->1:
            pos = Math.max(0, pos);
            pos = Math.min(1, pos);
            return pos;
        }
    }

    /**
     * Gets the start of the selection as an index into the raw sound array
     *
     * @return the index of the selection start
     */
    private int getSelectionStartOffset()
    {
        double start = Math.min(selectionBegin, selectionEnd);
        float length = recorder.getRawSound().length;
        return (int)(start * length);
    }

    /**
     * Gets the finish of the selection as an index into the raw sound array
     *
     * @return the index of the selection end
     */
    private int getSelectionFinishOffset()
    {
        double finish = Math.max(selectionBegin, selectionEnd);
        float length = recorder.getRawSound().length;
        return (int)(finish * length);
    }
    
    private void stopRecording()
    {
        if (recording)
        {
            recorder.stopRecording();
            playStop.setDisable(false);
            saveState.changed(true);
            soundPanel.paintComponent();
            recordStop.setText(recordLabel);
            recording = false;
        }
    }

    public SimpleBooleanProperty getShowingProperty()
    {
        return showingProperty;
    }
}
