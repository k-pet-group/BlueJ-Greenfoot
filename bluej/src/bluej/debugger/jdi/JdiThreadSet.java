/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.*;

import bluej.utility.Debug;

import com.sun.jdi.ThreadReference;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A wrapper around a TreeSet that helps us
 * store JdiThreads.
 * 
 * @author  Michael Kolling
 */
@OnThread(Tag.Any)
public class JdiThreadSet extends HashSet<JdiThread>
{
    /**
     * Construct an empty thread set.
     * 
     */
    public JdiThreadSet()
    {
        super();
    }

    /**
     * Find the thread in the set representing the thread reference specified.
     */
    public JdiThread find(ThreadReference thread)
    {
        for(Iterator<JdiThread> it=iterator(); it.hasNext(); ) {
            JdiThread currentThread = (JdiThread)it.next();
            if(currentThread.sameThread(thread)) {
                return currentThread;
            }
        }
        Debug.reportError("Encountered thread not in ThreadSet!");
        return null;
    }

    /**
     * Remove the given thread from the set.
     */
    public JdiThread removeThread(ThreadReference thread)
    {
        for(Iterator<JdiThread> it=iterator(); it.hasNext(); ) {
            JdiThread jdiThread = it.next();
            if(jdiThread.sameThread(thread)) {
                it.remove();
                return jdiThread;
            }
        }
        Debug.reportError("Unknown thread died!");
        return null;
    }
}
