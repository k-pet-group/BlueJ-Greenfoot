import greenfoot.Actor;
// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A container class to hold the locatable objects that are in the
 * unbounded world but not in the Greenfoot visible world. Its behavior
 * as an actor is to invoke act( ) on each of the objects it holds.
 * 
 * @author Cecilia Vargas
 * @version 28 July 2006
 */
public class InvisibleFishContainer extends Actor
{
    private List list; 
    
    /**
     * Constructs a resizable container.
     */
    public InvisibleFishContainer()
    {
        list = new ArrayList();
    }

    /**
     * Makes each of its elements "act( )"
     */
   
    public void act()
    {  
        //Duplicate it to avoid Concurrent Modification Exceptions
        List duplicate = new ArrayList();
        duplicate.addAll(list);
        
        Iterator it = duplicate.iterator();
        
        while( it.hasNext() ) {
            Fish actor = (Fish) it.next();
            actor.act();
        }
    }
    
    /**
     * Adds the specified locatable object to the container.
     * @param object     object to add to the container
     */
    public void add(Locatable object)
    {   
       list.add(object);
    }

    /**
     * Removes the specified locatable object from the container.
     * @param object     object to remove from the container
     */
    public void remove(Locatable object)
    {   
       list.remove(object);
    }
    
    /**
     * Returns a string representation of the container's contents,
     * specifying the number of elements.
     * @return    the string representing the contents
     */
    public String toString()
    {
       return "List of invisible fish has these " + list.size() +
              " elements: " + list;
    }    

    /**
     * Returns the number of elements in the container.
     * @return  the number of elements in the container.
     */
    public int elementCount()
    {
        return list.size();
    }
}