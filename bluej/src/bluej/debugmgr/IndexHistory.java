/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A history of Strings that maintains an internal cursor to provide
 * 'getNext'/'getPrevious'-style methods to traverse the history list. 
 * 
 * @author mik
 */
@OnThread(Tag.Any)
public class IndexHistory extends History {

    private int currentIndex;
    
    /**
     * 
     * @param maxLength Number of entries in history list
     */
    public IndexHistory(int maxLength)
    {
        super(maxLength, true);
        currentIndex = 0;
    }
    
    /**
     * Add a string to the history of used strings.
     * 
     * @param newString  the new string to add
     */
    public void add(String newString)
    {
        super.add(newString);
        currentIndex = 0;
    }
    
    /**
     * Get the previous history entry. Calling this repeatedly walks
     * back through the history
     * 
     * @return The previous history entry.
     */
    public String getPrevious()
    {
        if(currentIndex+1 < history.size()) {
            currentIndex++;
            return (String) history.get(currentIndex);
        }
        else {
            return null;
        }
    }

    /**
     * Get the next history entry. Calling this repeatedly walks
     * forward through the history
     * 
     * @return The next history entry.
     */
    public String getNext()
    {
        if(currentIndex > 0) {
            currentIndex--;
            return (String) history.get(currentIndex);
        }
        else {
            return null;
        }
    }

}
