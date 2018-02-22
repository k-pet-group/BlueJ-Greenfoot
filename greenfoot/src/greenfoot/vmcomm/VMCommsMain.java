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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * VMCommsMain is an abstraction for the inter-VM communications interface ("main VM" side) in
 * Greenfoot. It encapsulates a temporary file and memory-mapped buffer.
 * 
 * @author Davin McCall
 */
public class VMCommsMain implements Closeable
{
    File shmFile;
    FileChannel fc;
    MappedByteBuffer sharedMemoryByte;
    
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
}
