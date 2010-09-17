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
package greenfoot.sound;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import bluej.utility.Debug;

/**
 * A class that handles recording sound from the user's microphone.
 * 
 * @author neil
 */
public class SoundRecorder
{
    private AudioFormat format;
    private AtomicBoolean keepRecording = new AtomicBoolean();
    private TargetDataLine line;
    private BlockingQueue<byte []> recordedResultQueue = new ArrayBlockingQueue<byte[]>(1);
    private byte[] recorded;
    
    public SoundRecorder()
    {
        format = new AudioFormat(22050, 8, 1, true, true);
    }

    /**
     * Starts recording.  You should make sure to call stop() exactly once after
     * each call to start().
     * 
     * The method returns a reference from which you can pick out the current state
     * of the recording. It will first be set to null, then to be a list with the first part of the recording,
     * then a list with the first and second parts, then a list with the first, second
     * and third parts.  This is the easiest way to control
     * the concurrency involved in the sound recorder that reads from this list.
     * The internal list is cloned in this method (which is therefore thread-safe), and
     * thereafter the lists put in the reference can all be accessed individually
     * without worrying about them being modified.  So any list got by a call to a get
     * won't be being used again by this method.
     * 
     * The reference should be updated roughly every half-second.
     * 
     * When the recording has finished, null will be put into the reference.
     */
    public AtomicReference<List<byte[]>> startRecording()
    {
        try {
            line = (TargetDataLine)AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
            line.open();
            if (!line.getFormat().equals(format))
                Debug.message("Format is not as expected" + line.getFormat().toString());
            line.start();
            
            keepRecording.set(true);
            
            final AtomicReference<List<byte[]>> partialResult = new AtomicReference<List<byte[]>>(null);
                       
            Runnable rec = new Runnable() {
                public void run()
                {
                    // We should get a chunk every half second, which is better than every second
                    // for updating the display as we go:
                    int bufferSize = (int) (format.getSampleRate()/2) * format.getFrameSize();
                    LinkedList<byte[]> frames = new LinkedList<byte[]>();
                    
                    while (keepRecording.get()) {
                        byte buffer[] = new byte[bufferSize];
                    
                        int bytesRead = line.read(buffer, 0, bufferSize);
                                           
                        if (bytesRead != bufferSize) {
                            keepRecording.set(false);
                        } else {
                            frames.addLast(buffer);
                            partialResult.set(new LinkedList<byte[]>(frames));
                        }
                    }
                    
                    partialResult.set(null);
                    
                    line.stop();
                    
                    boolean done = false;
                    while (!done) {
                        try {
                            recordedResultQueue.put(merge(frames));
                            done = true;
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
            };
            
            new Thread(rec).start();
            
            return partialResult;
        }
        catch (LineUnavailableException e) {
            Debug.reportError("Problem capturing sound", e);
            return null;
        }
        
    }

    /**
     * Stops recording.  Should be called exactly once for each call to start().
     */
    public void stopRecording()
    {
        keepRecording.set(false);
        recorded = null;
        while (recorded == null) {
            try {
                recorded = recordedResultQueue.take();
            }
            catch (InterruptedException e) {
            }
        }
        line.close();
    }
    
    /**
     * Writes the most recent recording to the given file in WAV format
     */
    public void writeWAV(File destination)
    {
        ByteArrayInputStream baiStream = new ByteArrayInputStream(recorded);
        AudioInputStream aiStream = new AudioInputStream(baiStream,format,recorded.length);
        try {
            AudioSystem.write(aiStream,AudioFileFormat.Type.WAVE,destination);
            aiStream.close();
            baiStream.close();
        }
        catch (IOException e) {
            Debug.reportError("Problem writing recorded sound to WAV file", e);
        }
        

    }
    
    /**
     * Helper function to merge several arrays into one.
     */
    private static byte[] merge(List<byte[]> frames)
    {
        int totalLength = 0;
        for (byte[] frame : frames) {
            totalLength += frame.length;
        }
        
        byte[] result = new byte[totalLength];
        int curOffset = 0;
        for (byte[] frame : frames) {
            System.arraycopy(frame, 0, result, curOffset, frame.length);
            curOffset += frame.length;
        }
        return result;
            
            
    }

    /**
     * Gets the raw array of bytes representing the currently recorded sound
     */
    public byte[] getRawSound()
    {
        return recorded;
    }
    
    /**
     * Trims the current sound recording (if any) to the given offsets.
     * 
     * The offsets are given as floats in the range 0 to 1.
     */
    public void trim(float begin, float end)
    {
        if (recorded != null)
        {
            float length = recorded.length;
            int beginIndex = (int)(begin * length);
            int endIndex = (int)(end * length);
            
            recorded = Arrays.copyOfRange(recorded, beginIndex, endIndex);
        }
    }

    /**
     * Gets the format that was used to record the sound.
     */
    public AudioFormat getFormat()
    {
        return format;
    }
}
