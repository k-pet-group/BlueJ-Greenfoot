/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2012  Poul Henriksen and Michael Kolling 
 
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

import javax.sound.sampled.AudioFormat;

/**
 * Data for a sound clip.
 * 
 * @author Davin McCall
 */
public class ClipData
{
    private String url;
    private byte[] buffer;
    private AudioFormat format;
    private int activeUsers;
    private int length; // length in sample frames
    
    /**
     * Construct a ClipData with a single active user.
     */
    public ClipData(String url, byte[] buffer, AudioFormat format, int length)
    {
        this.url = url;
        this.buffer = buffer;
        this.format = format;
        this.length = length;
        this.activeUsers = 1;
    }
    
    public void addUser()
    {
        activeUsers++;
    }
    
    public boolean release()
    {
        return --activeUsers == 0;
    }
    
    public String getUrl()
    {
        return url;
    }
    
    public byte[] getBuffer()
    {
        return buffer;
    }
    
    public AudioFormat getFormat()
    {
        return format;
    }
    
    public int getLength()
    {
        return length;
    }
}
