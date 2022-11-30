/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork;


import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface to represent a teamwork command.
 * 
 * @author Davin McCall
 */
public interface TeamworkCommand
{
    /**
     * Cancel execution of the command. This can be called from any
     * thread, and should return immediately.
     */
    @OnThread(Tag.Any)
    public void cancel();
    
    /**
     * Complete execution of the command, and get the result.
     * Command execution might not begin until this method is called.
     */
    @OnThread(Tag.Worker)
    public TeamworkCommandResult getResult();
}
