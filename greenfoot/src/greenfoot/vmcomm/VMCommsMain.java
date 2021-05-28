/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2021 Poul Henriksen and Michael Kolling 
 
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
import java.util.concurrent.atomic.AtomicInteger;

import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import greenfoot.guifx.GreenfootStage;
import javafx.scene.input.KeyCode;
import threadchecker.OnThread;
import threadchecker.Tag;

import static greenfoot.vmcomm.Command.*;

/**
 * VMCommsMain is an abstraction for the inter-VM communications interface ("main VM" side) in
 * Greenfoot. It encapsulates a temporary file and memory-mapped buffer.
 * 
 * @author Davin McCall
 */
public class VMCommsMain implements Closeable
{
    // The server-debug VM protocol relies on locking three distinct areas of the file:
    //  A - the server VM "put" area
    //  B - the debug VM "put" area
    //  C - a small region not used for data but to ensure synchronisation
    //
    //    Server                          Debug
    //   (holds A, C)                     (holds B)
    // [command issued/want update]
    //                                       [updates data while B is held]
    //     -> release A
    //                                    -> acquire A   (commands are available...)
    //                                    -> release B   (ensures server can get data)
    //     -> acquire B
    //        (read image)
    //                                       (read commands)
    //     -> release C
    //                                    -> acquire C   (Server has acquired B)
    //                                    -> release A
    //     -> acquire A
    //     -> release B
    //                                    -> acquire B (Server has A and C)
    //                                    -> release C
    //     -> acquire C
    //
    // The acquisition order is B-->A, A-->C, and C-->B. This ensures that there can never
    // be deadlock. No process holds all three locks at once and each process always holds at
    // least one lock.

    public static final int DEFAULT_MAPPED_SIZE = 20_000_000;
    public static final int USER_AREA_OFFSET = 0x1000; // offset in 4-byte chunks; 16KB worth.
    public static final int USER_AREA_OFFSET_BYTES = USER_AREA_OFFSET * 4;

    public static final int SERVER_AREA_OFFSET_BYTES = 4;
    public static final int SERVER_AREA_SIZE_BYTES = USER_AREA_OFFSET_BYTES - SERVER_AREA_OFFSET_BYTES;
    
    public static final int SYNC_AREA_OFFSET_BYTES = 0;
    public static final int SYNC_AREA_SIZE_BYTES = 4;
    
    private final int fileSize;
    private File shmFile;
    private FileChannel fc;
    private MappedByteBuffer sharedMemoryByte;
    private IntBuffer sharedMemory;
    private FileLock putLock;
    private FileLock syncLock;

    // Needs to be AtomicInteger because it's modified from multiple threads:
    private final AtomicInteger lastSeq = new AtomicInteger(0);
    private final List<Command> pendingCommands = new ArrayList<>();
    private int setSpeedCommandCount = 0;
    private int lastPaintSeq = -1;
    private int lastConsumedImg = -1;
    
    private boolean checkingIO = false;
    
    private boolean haveUpdatedImage = false;
    private boolean haveUpdatedErrorCount = false;
    private long lastExecStartTime;
    private int updatedSimulationSpeed = -1;
    private boolean worldChanged = false;
    private boolean worldPresentAfterChange = false;
    private int[] promptCodepoints = null;

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
    // The world counter during the previous frame (zero = no world):
    private int prevWorldCounter = 0;
    // The world cell size (0 if no world)
    private int worldCellSize;
    
    private final Thread ioThread;

    private boolean delayLoop;
    private boolean vmReadyForInvocations = false;
    private int askId = -1;
    private boolean workerWaiting = false;

    /**
     * Constructor for VMCommsMain. Creates a temporary file and maps it into memory.
     * 
     * @throws IOException  if the file could not be created or mapped.
     */
    @SuppressWarnings("resource")
    public VMCommsMain(Project project) throws IOException
    {
        fileSize = Integer.parseInt(project.getUnnamedPackage().getLastSavedProperties().getProperty("shm.size", Integer.toString(DEFAULT_MAPPED_SIZE)));
        
        shmFile = File.createTempFile("greenfoot", "shm");
        shmFile.deleteOnExit();
        fc = new RandomAccessFile(shmFile, "rw").getChannel();
        sharedMemoryByte = fc.map(MapMode.READ_WRITE, 0, fileSize);
        sharedMemory = sharedMemoryByte.asIntBuffer();
        
        // Obtain the put-area lock right from the start:
        putLock = fc.lock(SERVER_AREA_OFFSET_BYTES, SERVER_AREA_SIZE_BYTES, false);
        syncLock = fc.lock(SYNC_AREA_OFFSET_BYTES, SYNC_AREA_SIZE_BYTES, false);
        
        ioThread = new Thread("VMCommsMain") {
            @OnThread(Tag.Worker)
            public void run()
            {
                while (checkIO())
                {
                }
            }
        };
        
        ioThread.start();
    }
    
    /**
     * Close the communications channel, and release resources.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
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
     * Get the name of the file used for this communication channel.
     */
    public File getSharedFile()
    {
        return shmFile;
    }

    /**
     * Get the size of the file used for this communication channel.
     */
    public int getSharedFileSize()
    {
        return fileSize;
    }

    /**
     * Write commands into the shared memory buffer.
     */
    private synchronized void writeCommands(List<Command> pendingCommands)
    {
        // Number of commands:
        int pendingCountPos = sharedMemory.position();
        sharedMemory.put(pendingCommands.size());
        
        int numIssued = 0;
        for (Command pendingCommand : pendingCommands)
        {
            // sequence, type, extra info:
            int totalLength = pendingCommand.extraInfo.length + 2;
            if (sharedMemory.position() + totalLength > USER_AREA_OFFSET)
            {
                // We can't write all commands in the available buffer:
                sharedMemory.put(pendingCountPos, numIssued);
                if (numIssued == 0)
                {
                    // I don't imagine this should ever happen, but let's make sure we get
                    // something meaningful in the log if it does:
                    throw new RuntimeException("Single command exceeds buffer size");
                }
                return;
            }
            
            // Start with sequence ID:
            sharedMemory.put(pendingCommand.commandSequence);
            // Put size of this command (measured in integers), including command type:
            sharedMemory.put(pendingCommand.extraInfo.length + 1);
            // Then put that many integers:
            sharedMemory.put(pendingCommand.commandType);
            sharedMemory.put(pendingCommand.extraInfo);
            numIssued++;
        }
    }
    
    /**
     * Get the world cell size, if it is known.
     * 
     * @return  The cell size in pixels, or 0 if there is no world.
     */
    public int getWorldCellSize()
    {
        return worldCellSize;
    }

    /**
     * Check for input / send output, and apply received data to the stage.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized boolean checkIO(GreenfootStage stage)
    {
        if (checkingIO)
        {
            return vmReadyForInvocations; // avoid re-entrancy
        }
        
        checkingIO = true;

        // We should only draw if either the world didn't change, or there
        // was a change, but a world is left.  i.e. don't draw if the world got removed:
        boolean shouldDraw = !worldChanged || worldPresentAfterChange;
        if (worldChanged)
        {
            stage.worldChanged(worldPresentAfterChange);
            worldChanged = false;
        }
        
        if (haveUpdatedImage && shouldDraw)
        {
            // skip: sequence number, last paint sequence, then:
            IntBuffer copy = sharedMemory.asReadOnlyBuffer();
            copy.position(USER_AREA_OFFSET + 2);
            int width = copy.get();
            int height = copy.get();
            stage.receivedWorldImage(width, height, copy);
            haveUpdatedImage = false;
            lastConsumedImg = lastPaintSeq;
        }
        
        if (haveUpdatedErrorCount)
        {
            stage.bringTerminalToFront();
            haveUpdatedErrorCount = false;
        }
        
        if (updatedSimulationSpeed != -1)
        {
            stage.notifySimulationSpeed(updatedSimulationSpeed);
            updatedSimulationSpeed = -1;
        }        
        
        if (promptCodepoints != null && askId > lastAnswer)
        {
            stage.receivedAsk(askId, promptCodepoints);
            promptCodepoints = null;
        }
        else
        {
            stage.cancelAsk();
        }

        stage.setLastUserExecutionStartTime(lastExecStartTime, delayLoop);
            
        checkingIO = false;
        
        notifyAll(); // wake IO thread
        return vmReadyForInvocations;
    }

    /**
     * Check for input / send output
     * @return true If we should continue processing, false if not.
     */
    @OnThread(Tag.Worker)
    private boolean checkIO()
    {
        FileChannel sharedMemoryLock = this.fc;

        // We are holding the lock for the main put area:
        sharedMemory.position(1);
        sharedMemory.put(-lastSeq.get());
        sharedMemory.put(lastConsumedImg);
        writeCommands(pendingCommands);
        
        FileLock fileLock = null;
        
        try
        {
            putLock.release();
            fileLock = sharedMemoryLock.lock(USER_AREA_OFFSET_BYTES, fileSize - USER_AREA_OFFSET_BYTES, false);
            syncLock.release();

            int seq = sharedMemory.get(USER_AREA_OFFSET);
            if (seq > lastSeq.get())
            {
                // The client VM has painted a new frame for us:
                lastSeq.set(seq);

                synchronized (this)
                {
                    sharedMemory.position(USER_AREA_OFFSET + 1);
                    int paintSeq = sharedMemory.get();
                    int width = sharedMemory.get();
                    int height = sharedMemory.get();
                    if (width != 0 && height != 0 && paintSeq != lastPaintSeq)
                    {
                        lastPaintSeq = paintSeq;
                        haveUpdatedImage = true;
                    }
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
                                if(pendingCommand.commandType == COMMAND_SET_SPEED)
                                {
                                    setSpeedCommandCount = setSpeedCommandCount - 1;
                                }
                                iterator.remove();
                            }
                        }
                    }
                    
                    // If there's a new error, show the terminal at the front so that the user sees it: 
                    int latestStoppedWithErrorCount = sharedMemory.get();
                    if (latestStoppedWithErrorCount != previousStoppedWithErrorCount)
                    {
                        //stage.bringTerminalToFront();
                        previousStoppedWithErrorCount = latestStoppedWithErrorCount;
                        haveUpdatedErrorCount = true;
                    }
                    
                    int highTime = sharedMemory.get();
                    int lowTime = sharedMemory.get();
                    lastExecStartTime = (((long)highTime) << 32) | ((long)lowTime & 0xFFFFFFFFL);
    
                    int simSpeed = sharedMemory.get();
                    // Only send the new speed value if the pendingCommands does not include setSpeed commands
                    if (setSpeedCommandCount == 0)
                    {
                        updatedSimulationSpeed = simSpeed;
                    }
    
                    int worldCounter = sharedMemory.get();
                    // If the new counter is different (zero/non-zero change, or incremented),
                    // store that into our fields:
                    if (worldCounter != prevWorldCounter)
                    {
                        worldChanged = true;
                        worldPresentAfterChange = worldCounter != 0;
                        prevWorldCounter = worldCounter;
                    }
                    
                    worldCellSize = sharedMemory.get();
                    
                    int askId = sharedMemory.get();
                    if (askId > 0)
                    {
                        if (askId > lastAnswer)
                            this.askId = askId;
                        // Length followed by codepoints for the prompt string:
                        int askLength = sharedMemory.get();
                        promptCodepoints = new int[askLength];
                        sharedMemory.get(promptCodepoints);
                    }

                    int delayLoopStatus = sharedMemory.get();
                    delayLoop = delayLoopStatus == 1;
                    int vmReadyStatus = sharedMemory.get();
                    vmReadyForInvocations = vmReadyStatus == 1;
                }
            }
        }
        catch (IOException ex)
        {
            Debug.reportError(ex);
        }
        catch (IllegalArgumentException ex)
        {
            // Happens when world size is too large: swallow quietly, as will happen repeatedly.
            // The exception will be reported to the user from the debug VM side.
        }
        finally
        {
            // Re-acquire the put-area lock (A), and then release the get-area lock (B)
            // before re-acquiring the sync lock (C):
            try
            {
                putLock = fc.lock(SERVER_AREA_OFFSET_BYTES, SERVER_AREA_SIZE_BYTES, false);
                if (fileLock != null)
                {
                    fileLock.release();
                }
                syncLock = fc.lock(SYNC_AREA_OFFSET_BYTES, SYNC_AREA_SIZE_BYTES, false);
            }
            catch (IOException ex)
            {
                Debug.reportError(ex);
            }
        }
        
        
        // To avoid consuming close to 100% CPU, we wait on the animation timer:
        synchronized (this)
        {
            try
            {
                workerWaiting = true;
                wait();
                workerWaiting = false;
            }
            catch (InterruptedException ie)
            {
                // Nothing needs to be done.
            }
            return shmFile != null;
        }
    }
    
    /**
     * Send an "instantiate world" command.
     */
    public synchronized void instantiateWorld(String className)
    {
        pendingCommands.add(new Command(COMMAND_INSTANTIATE_WORLD, className.codePoints().toArray()));
    }
    
    /**
     * Send a "discard world" command.
     */
    public synchronized void discardWorld()
    {
        pendingCommands.add(new Command(COMMAND_DISCARD_WORLD));
    }
    
    /**
     * Send an answer (after receving an "ask" request).
     */
    public synchronized void sendAnswer(int askIdBeingAnswered, String answer)
    {
        Command answerCommand = new Command(COMMAND_ANSWERED, answer.codePoints().toArray());
        pendingCommands.add(answerCommand);
        // Remember that we've now answered:
        lastAnswer = askIdBeingAnswered;
    }
    
    /**
     * Send an updated property value.
     * @param key    The property name
     * @param value  The property value
     */
    public synchronized void sendProperty(String key, String value)
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
    public synchronized void act()
    {
        pendingCommands.add(new Command(COMMAND_ACT));
    }
    
    /**
     * Send a "run simulation" command.
     */
    public synchronized void runSimulation()
    {
        pendingCommands.add(new Command(COMMAND_RUN));
    }

    /**
     * Send a "pause simulation" command.
     */
    public synchronized void pauseSimulation()
    {
        pendingCommands.add(new Command(COMMAND_PAUSE));
    }
    
    /**
     * Continue a mouse drag, identified by the given id. Note that drags are initiated by
     * a pick request executed via a separate mechanism
     * 
     * @see greenfoot.core.PickActorHelper
     */
    public synchronized void continueDrag(int dragId, int x, int y)
    {
        pendingCommands.add(new Command(COMMAND_CONTINUE_DRAG, dragId, x, y));
    }
    
    /**
     * End a drag, identified by the given id.
     */
    public synchronized void endDrag(int dragId)
    {
        pendingCommands.add(new Command(COMMAND_END_DRAG, dragId));
    }
    
    /**
     * Send a key event.
     * 
     * @param eventType   The event type
     * @param keyCode     The key code, from KeyEvent.getCode()
     * @param keyText     The key text, from KeyEvent.getText()
     */
    public synchronized void sendKeyEvent(int eventType, KeyCode keyCode, String keyText)
    {
        int[] textCodePoints = keyText.codePoints().toArray();

        // Ordinal from KeyCode, followed by text codepoints:
        int[] data = new int[textCodePoints.length + 1];
        data[0] = keyCode.ordinal();
        System.arraycopy(textCodePoints, 0, data, 1, textCodePoints.length);
        
        pendingCommands.add(new Command(eventType, data));
    }
    
    /**
     * Send a mouse event.
     * 
     * @param eventType   The event type
     * @param x           The mouse x-coordinate (in pixels)
     * @param y           The mouse y-coordinate (in pixels)
     * @param button      The button pressed (for button events)
     * @param clickCount  The click count (for click events)
     */
    public synchronized void sendMouseEvent(int eventType, int x, int y, int button, int clickCount)
    {
        pendingCommands.add(new Command(eventType, x, y, button, clickCount));
    }

    /**
     * Set the simulation speed to a specified value
     *
     * @param speed   The speed value
     */
    public synchronized void setSimulationSpeed(int speed)
    {
        pendingCommands.add(new Command(COMMAND_SET_SPEED, speed));
        // Keeps track of how many setSpeed commands exist in the pendingCommand list.
        // This is useful to avoid speedSlider jittering movement.
        setSpeedCommandCount = setSpeedCommandCount + 1;
    }

    /**
     * The debug VM has terminated.  We re-use the same shared memory file,
     * so we must reset our state ready for a new debug VM.
     */
    @OnThread(Tag.VMEventHandler)
    public synchronized void vmTerminated()
    {
        // We should only interfere with the worker thread's state if it's currently in its waiting phase:
        while (!workerWaiting)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }
        lastSeq.addAndGet(1000);
        pendingCommands.clear();        
        setSpeedCommandCount = 0;
        lastAnswer = -1;
        previousStoppedWithErrorCount = 0;
        prevWorldCounter = 0;
        
        // Zero the buffer:
        sharedMemoryByte.position(0);
        sharedMemoryByte.put(new byte[fileSize], 0, fileSize);
        vmReadyForInvocations = false;
    }

    /**
     * The world display has gained or lost focus
     * @param focused true if the world display gained focus, false if it lost focus
     */
    public synchronized void worldFocusChanged(boolean focused)
    {
        pendingCommands.add(new Command(focused ? COMMAND_WORLD_FOCUS_GAINED : COMMAND_WORLD_FOCUS_LOST));
    }

    /**
     * Gets the last sequence identifier that we've received from the user VM
     */
    public int getLastSeq()
    {
        return lastSeq.get();
    }
}
