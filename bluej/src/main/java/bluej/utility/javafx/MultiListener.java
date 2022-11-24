/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.utility.Utility;

/**
 * A class that allows you to attach a listener to each item in a stream.
 * Each time the stream may have changed, you can call listenOnlyTo to attach
 * listeners to every item in the stream.  The class keeps track of which items
 * it is already listening to, so it ensures that it will never attach multiple
 * listeners to any given item.  Also, when an item leaves the stream, the listener
 * will be removed.
 */
public class MultiListener<T>
{
    public static interface RemoveAndUpdate
    {
        public void removeListener();
        // By making the update default to nothing, we can use a lambda for remove:
        public default void updateListener() { }
    }
    
    private static class BooleanAndRemoveAndUpdate
    {
        public boolean flaggedForRemoval = false;
        public final RemoveAndUpdate removeAndUpdate;
        BooleanAndRemoveAndUpdate(RemoveAndUpdate removeAndUpdate)
        {
            this.removeAndUpdate = removeAndUpdate;
        }
    }
    
    private final IdentityHashMap<T, BooleanAndRemoveAndUpdate> listening = new IdentityHashMap<>();
    private final Function<T, RemoveAndUpdate> addListener;
    
    /**
     * 
     * @param addListener The function that attaches a listener to the given item, and
     * gives back functions for removing and updating the listener in future.
     * @param removeListener The function that removes a listener from the given item
     * @param updateListener The function that is called when an item remains in the stream from last time 
     */
    public MultiListener(Function<T, RemoveAndUpdate> addListener)
    {
        this.addListener = addListener;
    }
    
    public void listenOnlyTo(Stream<T> items)
    {
        // Flag everything for removal from hash set:
        listening.forEach((k, v) -> v.flaggedForRemoval = true);
        
        for (T t : Utility.iterableStream(items))
        {
            BooleanAndRemoveAndUpdate value = listening.get(t);
            if (value != null)
            {
                // Keep listening:
                value.removeAndUpdate.updateListener();
                value.flaggedForRemoval = false;
            }
            else
            {
                // Add to listening set:
                listening.put(t, new BooleanAndRemoveAndUpdate(addListener.apply(t)));
            }
        }
        // Clean up our listening set:
        // (Must collect into list, not iterate over stream, to avoid ConcurrentModificationException)
        List<Entry<T, BooleanAndRemoveAndUpdate>> stale = listening.entrySet().stream().filter(e -> e.getValue().flaggedForRemoval).collect(Collectors.toList());
        stale.forEach(e -> {e.getValue().removeAndUpdate.removeListener(); listening.remove(e.getKey()); });
    }

    public void stopListening()
    {
        listening.forEach((k, v) -> v.removeAndUpdate.removeListener());
        listening.clear();
    }
}
