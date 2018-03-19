/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018 Poul Henriksen and Michael Kolling 
 
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
package greenfoot.vmcomm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.utility.Debug;
import greenfoot.guifx.GreenfootStage;

import static greenfoot.vmcomm.Command.*;

/**
 * VMCommsMain is an abstraction for the inter-VM communications interface ("main VM" side) in
 * Greenfoot. It encapsulates a temporary file and memory-mapped buffer.
 * 
 * @author Davin McCall
 */
public class VMCommsMain implements Closeable
{
    private File shmFile;
    private FileChannel fc;
    private MappedByteBuffer sharedMemoryByte;
    private IntBuffer sharedMemory;

    private int lastSeq = 0;
    private final List<Command> pendingCommands = new ArrayList<>();
    private int setSpeedCommandCount = 0;
    /**
     * Because the ask request is sent as a continuous status rather than
     * a one-off event that we explicitly acknowledge, we keep track of the
     * last answer we sent so that we know if an ask request is newer than
     * the last answer or not.  That way we don't accidentally ask again
     * after the answer has been sent.
     */
    private int lastAnswer = -1;
    // A count of errors on the debug VM.  We keep count too so that we know when it has changed:
    private int previousStoppedWithErrorCount;
    // Whether the previous frame had a world present:
    private boolean hadWorld = false;

    /**
     * Constructor for VMCommsMain. Creates a temporary file and maps it into memory.
     * 
     * @throws IOException  if the file could not be created or mapped.
     */
    @SuppressWarnings("resource")
    public VMCommsMain() throws IOException
    {
        shmFile = File.createTempFile("greenfoot", "shm");
        fc = new RandomAccessFile(shmFile, "rw").getChannel();
        sharedMemoryByte = fc.map(MapMode.READ_WRITE, 0, 10_000_000L);
        sharedMemory = sharedMemoryByte.asIntBuffer();
    }
    
    /**
     * Close the communications channel, and release resources.
     */
    public void close()
    {
        try
        {
            fc.close();
        }
        catch (IOException ioe)
        {
            // There is no meaningful way to handle I/O error at this point, and anyway the file
            // is no longer needed, so we just ignore the exception.
        }
        
        shmFile = null;
        fc = null;
        sharedMemoryByte = null;
        sharedMemory = null;
    }
    
    /**
     * Get the file channel for this communication channel.
     */
    public FileChannel getChannel()
    {
        return fc;
    }
    
    /**
     * Get the shared memory buffer for this communication channel.
     */
    public MappedByteBuffer getSharedBuffer()
    {
        return sharedMemoryByte;
    }
    
    /**
     * Get the name of the file user for this communication channel.
     */
    public File getSharedFile()
    {
        return shmFile;
    }
    
    /**
     * Write commands into the shared memory buffer.
     */
    private void writeCommands(List<Command> pendingCommands)
    {
        // Number of commands:
        sharedMemory.put(pendingCommands.size());
        for (Command pendingCommand : pendingCommands)
        {
            // Start with sequence ID:
            sharedMemory.put(pendingCommand.commandSequence);
            // Put size of this command (measured in integers), including command type:
            sharedMemory.put(pendingCommand.extraInfo.length + 1);
            // Then put that many integers:
            sharedMemory.put(pendingCommand.commandType);
            sharedMemory.put(pendingCommand.extraInfo);
        }
    }

    /**
     * Check for input / send output
     */
    public void checkIO(GreenfootStage stage)
    {
        FileChannel sharedMemoryLock = this.fc;
        
        try (FileLock fileLock = sharedMemoryLock.lock())
        {
            int seq = sharedMemory.get(1);
            if (seq > lastSeq)
            {
                // The client VM has painted a new frame for us:
                lastSeq = seq;
                sharedMemory.position(1);
                sharedMemory.put(-seq);
                int width = sharedMemory.get();
                int height = sharedMemory.get();
                if (width != 0 && height != 0)
                {
                    sharedMemoryByte.position(sharedMemory.position() * 4);
                    stage.receivedWorldImage(width, height, sharedMemoryByte);
                }
                // Have to move sharedMemory position manually because
                // the sharedMemory buffer doesn't share position with sharedMemoryByte buffer:
                sharedMemory.position(sharedMemory.position() + width * height);

                // Get rid of all commands that the client has confirmed it has seen:
                int lastAckCommand = sharedMemory.get();
                if (lastAckCommand != -1)
                {
                    for (Iterator<Command> iterator = pendingCommands.iterator(); iterator.hasNext(); )
                    {
                        Command pendingCommand = iterator.next();
                        if (pendingCommand.commandSequence <= lastAckCommand)
                        {
                           if(pendingCommand.commandType==COMMAND_SET_SPEED)
                           {
                               setSpeedCommandCount = setSpeedCommandCount-1;
                           }
                            iterator.remove();
                        }
                    }
                }
                
                // If there's a new error, show the terminal at the front so that the user sees it: 
                int latestStoppedWithErrorCount = sharedMemory.get();
                if (latestStoppedWithErrorCount != previousStoppedWithErrorCount)
                {
                    stage.bringTerminalToFront();
                    previousStoppedWithErrorCount = latestStoppedWithErrorCount;
                }
                
                int highTime = sharedMemory.get();
                int lowTime = sharedMemory.get();
                long lastExecStartTime = (((long)highTime) << 32) | ((long)lowTime & 0xFFFFFFFFL);
                stage.setLastUserExecutionStartTime(lastExecStartTime);

                int simSpeed = sharedMemory.get();
                //Only send the new speed value if the pendingCommands does not include multiple setSpeed commands
                if (setSpeedCommandCount == 1)
                {
                   setSpeedCommandCount = 0;
                   stage.notifySimulationSpeed(simSpeed);
                }

                boolean worldPresent = (sharedMemory.get() == 1);
                if (worldPresent != hadWorld) {
                    if (! worldPresent) {
                        stage.worldDiscarded();
                    }
                    hadWorld = worldPresent;
                }
                
                int askId = sharedMemory.get();
                if (askId >= 0 && askId > lastAnswer)
                {
                    // Length followed by codepoints for the prompt string:
                    int askLength = sharedMemory.get();
                    int[] promptCodepoints = new int[askLength];
                    sharedMemory.get(promptCodepoints);

                    stage.receivedAsk(promptCodepoints);
                }
                sharedMemory.position(2);
                writeCommands(pendingCommands);
            }
            else if (!pendingCommands.isEmpty())
            {
                // The debug VM hasn't painted a new frame, but we have new commands to send:
                sharedMemory.position(1);
                sharedMemory.put(-lastSeq);
                writeCommands(pendingCommands);
            }
        }
        catch (IOException ex)
        {
            Debug.reportError(ex);
        }
    }
    
    /**
     * Send an "instantiate world" command.
     */
    public void instantiateWorld()
    {
        pendingCommands.add(new Command(COMMAND_INSTANTIATE_WORLD));
    }
    
    /**
     * Send a "discard world" command.
     */
    public void discardWorld()
    {
        pendingCommands.add(new Command(COMMAND_DISCARD_WORLD));
    }
    
    /**
     * Send an answer (after receving an "ask" request).
     */
    public void sendAnswer(String answer)
    {
        Command answerCommand = new Command(COMMAND_ANSWERED, answer.codePoints().toArray());
        pendingCommands.add(answerCommand);
        // Remember that we've now answered:
        lastAnswer = answerCommand.commandSequence;
    }
    
    /**
     * Send an updated property value.
     * @param key    The property name
     * @param value  The property value
     */
    public void sendProperty(String key, String value)
    {
        int[] keyCodepoints = key.codePoints().toArray();
        int[] valueCodepoints = value == null ? new int[0] : value.codePoints().toArray();
        int[] combined = new int[1 + keyCodepoints.length + 1 + valueCodepoints.length];
        combined[0] = keyCodepoints.length;
        System.arraycopy(keyCodepoints, 0, combined, 1, keyCodepoints.length);
        combined[1 + keyCodepoints.length] = value == null ? -1 : valueCodepoints.length;
        System.arraycopy(valueCodepoints, 0, combined, 2 + keyCodepoints.length, valueCodepoints.length);
        pendingCommands.add(new Command(COMMAND_PROPERTY_CHANGED, combined));
    }
    
    /**
     * Send an "act" command.
     */
    public void act()
    {
        pendingCommands.add(new Command(COMMAND_ACT));
    }
    
    /**
     * Send a "run simulation" command.
     */
    public void runSimulation()
    {
        pendingCommands.add(new Command(COMMAND_RUN));
    }

    /**
     * Send a "pause simulation" command.
     */
    public void pauseSimulation()
    {
        pendingCommands.add(new Command(COMMAND_PAUSE));
    }
    
    /**
     * Continue a mouse drag, identified by the given id. Note that drags are initiated by
     * a pick request executed via a separate mechanism
     * 
     * @see greenfoot.core.PickActorHelper
     */
    public void continueDrag(int dragId, int x, int y)
    {
        pendingCommands.add(new Command(COMMAND_CONTINUE_DRAG, dragId, x, y));
    }
    
    /**
     * End a drag, identified by the given id.
     */
    public void endDrag(int dragId)
    {
        pendingCommands.add(new Command(COMMAND_END_DRAG, dragId));
    }
    
    /**
     * Send a key event.
     * 
     * @param eventType   The event type
     * @param ordinal     The key code ordinal
     */
    public void sendKeyEvent(int eventType, int ordinal)
    {
        pendingCommands.add(new Command(eventType, ordinal));
    }
    
    /**
     * Send a mouse event.
     * 
     * @param eventType   The event type
     * @param x           The mouse x-coordinate
     * @param y           The mouse y-coordinate
     * @param button      The button pressed (for button events)
     * @param clickCount  The click count (for click events)
     */
    public void sendMouseEvent(int eventType, int x, int y, int button, int clickCount)
    {
        pendingCommands.add(new Command(eventType, x, y, button, clickCount));
    }

    /**
     * Set the simulation speed to a specified value
     *
     * @param speed   The speed value
     */
    public void setSimulationSpeed(int speed)
    {
        pendingCommands.add(new Command(COMMAND_SET_SPEED, speed));
        //Keeps track of how many setSpeed commands exist in the pendingCommand list. This is useful to avoid speedslider jittering movement.
        setSpeedCommandCount = setSpeedCommandCount+1;

    }

}
