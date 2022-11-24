/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016  Michael Kolling and John Rosenberg 
 
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

import java.util.List;
import java.util.ArrayList;

import threadchecker.OnThread;
import threadchecker.Tag;

/** 
 * History objects maintain a history of text strings. This serves as a
 * superclass for various histories (see, for example, ClassHistory,
 * FreeCallHistory).
 *
 * @author Michael Kolling
 */
@OnThread(Tag.Any)
public class History
{
    protected List<String> history = null;
    protected int maxLength;
    private boolean blankAtStart;
    
    /**
     * Create a empty history limited to a given maximum
     * number of entries.
     *
     * @param maxLength The maximum length of the hostory list. The
     *                  list truncates its tail when growing longer.
     * @param blankDefault If true, maintains an empty string as the
     *                  first (most recent) entry.
     */
    protected History(int maxLength, boolean blankDefault)
    {
        this.maxLength = maxLength;
        history = new ArrayList<String>(maxLength+1);
        history.add("");
        blankAtStart = blankDefault;
    }

    /**
     * Put an entry into the history list. This method is only for 
     * initialisation through subclasses. It does not check for 
     * duplicates or maxlength.
     */
    protected void put(String value)
    {
        history.add(value);
    }

    /**
     * Return the history of used classes.
     */
    public List<String> getHistory()
    {
        return history;
    }

    /**
     * Add a string to the history of used strings.
     * 
     * @param newString  the new string to add
     */
    public void add(String newString)
    {
        if(newString != null && (newString.length() != 0)) {

            // remove at old position (if present) and add at front
            history.remove(newString);

            if(blankAtStart) {
                history.add(1, newString);
            }
            else {
                // remove empty entry at front
                if(((String)history.get(0)).length() == 0)
                    history.remove(0);
                history.add(0, newString);
            }

            // don't let it grow too big
            if(history.size() > maxLength)
                history.remove(maxLength);
        }
    }
}
