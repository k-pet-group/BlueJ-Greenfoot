/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,20162,2018  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.objectbench;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface for listening for object selection events.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public interface ObjectBenchInterface
{
    /**
     * Add a listener for events on this object bench.
     * @param l  The listener to add
     */
    public void addObjectBenchListener(ObjectBenchListener l);
    
    /**
     * Remove a listener so that it no longer receives events.
     * @param l  The listener to remove
     */
    public void removeObjectBenchListener(ObjectBenchListener l);
    
    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    public boolean hasObject(String name);
    
    /**
     * Add an object to the bench with the specified name and type.
     * 
     * @param object  The object to add to the bench.
     * @param type    The type of the object.
     * @param name    The desired name of the object as it should appear on the bench.
     * 
     * @return  The name actually used (may not match desired name in case of clash).
     */
    public String addObject(DebuggerObject object, GenTypeClass type, String name);
}
