/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

/**
 * Indicates a problem with accessing the storage
 * <p>
 * This indicates that there was a problem reading from/writing to the storage, typically:
 * 
 * <ul>
 * <li>A problem accessing the CSV file in the local storage (e.g. bad permissions, disconnected USB stick)
 * <li>A problem accessing the Gallery storage (may be caused by maintenance work on the gallery)
 * <li>If storage is not supported, this exception will be thrown every time.
 * </ul>
 * <p>
 * In general, the way to deal with this exception is to live with not using
 * storage for this session.  Warn the user that their data cannot be loaded/saved,
 * and proceed as best as you can.
 */
public class GreenfootStorageException extends Exception
{

    public GreenfootStorageException(String message)
    {
        super(message);
    }

}
