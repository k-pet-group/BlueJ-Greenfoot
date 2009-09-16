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
package bluej.utility;

import java.util.Iterator;
import java.util.List;

/**
 * A multiplexing Iterator.
 * @author Michael Cahill
 * @author Michael Kolling
 */
public class MultiIterator<T> implements Iterator<T>
{
    List<Iterator<? extends T>> iterations;
    int current;
	
    public MultiIterator(List<Iterator<? extends T>> iterations)
    {
        this.iterations = iterations;
        current = 0;
    }
	
    public boolean hasNext()
    {
        for( ; current < iterations.size(); current++)
            if((iterations.get(current)).hasNext())
                return true;
	
        return false;
    }

    public T next()
    {
        for( ; current < iterations.size(); current++) {
            Iterator<? extends T> it = iterations.get(current);
            if(it.hasNext()) {
                return it.next();
            }
        }
		
        return null;
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
