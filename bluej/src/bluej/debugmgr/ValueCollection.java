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

import java.util.Iterator;

import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * A collection of named values (NameValue interface), which may be references
 * (objects) or primitive values.
 * 
 * @author Davin McCall
 * @version $Id: ValueCollection.java 16607 2016-09-29 14:00:49Z nccb $
 */
public interface ValueCollection
{   
    /**
     * Get an iterator through the values in this collection.
     */
    public Iterator<? extends NamedValue> getValueIterator();
    
    /**
     * Get a value by name, in this collection or in a parent scope. This may delegate to
     * another collection to provide scoping, and in particular, may provide access to
     * values not seen by the iterator returned by getValueIterator().
     * 
     * @param name   The name of the value to retrieve
     * @return       The value, or null if it does not exist.
     */
    public NamedValue getNamedValue(String name);
}
