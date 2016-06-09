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
 * This class implements a singleton history object for library class
 * invocations.
 *
 * @author Michael Kolling
 * @version $Id: ClassHistory.java 16001 2016-06-09 15:35:27Z nccb $
 *
 */
@OnThread(Tag.Any)
public class ClassHistory extends History
{
    // ======= static (factory) section =======

    private static ClassHistory classHistory = null;

    /**
     * Get the class history singleton. The first time this method
     * is called, the 'maxLength' parameter determines the history
     * size. The parameter has no effect on subsequent calls.
     */
    public static ClassHistory getClassHistory(int maxLength)
    {
        if(classHistory == null)
            classHistory = new ClassHistory(maxLength);
        return classHistory;
    }

    // ======= instance section =======

    /**
     * Initialise this history with some often-used classes for convinience.
     */
    private ClassHistory(int maxLength)
    {
        super(maxLength, false);
        put("java.lang.String");
        put("java.lang.Math");
        put("java.util.ArrayList");
        put("java.util.Random");
        put("java.util.");
        put("java.awt.");
        put("javax.swing.");
    }
}
