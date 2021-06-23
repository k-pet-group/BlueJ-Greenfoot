/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2021  Michael Kolling and John Rosenberg 
 
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

import java.util.*;

import bluej.debugmgr.NamedValue;
import javafx.geometry.Point2D;

/**
 * The event which occurs when  performing actions with the ObjectBench.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchEvent.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ObjectBenchEvent extends EventObject
{
    public final static int OBJECT_SELECTED = 1;
    
    // Can be multiple values in Greenfoot, where they can be on top of each other:
    private final NamedValue[] values;
    private final int id;
    // Will be null outside Greenfoot:
    private final Point2D screenPosition;

    public ObjectBenchEvent(Object source, int id, NamedValue[] values, Point2D screenPosition)
    {
        super(source);

        this.id = id;
        this.values = values;
        this.screenPosition = screenPosition;
    }

    public int getID()
    {
        return id;
    }

    public NamedValue[] getValues()
    {
        return values;
    }

    /**
     * If applicable (e.g. a Greenfoot actor) returns an array with X, Y position.
     * If not applicable, returns null.
     */
    public Point2D getScreenPosition()
    {
        return screenPosition;
    }
}
