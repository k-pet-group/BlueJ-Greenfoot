/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot;

import java.util.*;

/**
 * A set which allows specifying iteration order according to class of contained
 * objects.
 * 
 * <p>TreeActorSet is really an ordered "set of sets". Each set corresponds to
 * a class; any actors of that class are put in the set, along with any actors
 * of subclasses as long as there isn't a more appropriate set for them.
 * 
 * @author Davin McCall
 */
public class TreeActorSet extends AbstractSet<Actor>
{
    private List<ActorSet> subSets;
    
    /** ActorSet for objects of a class without a specific z-order */
    private ActorSet generalSet;
    
    private HashMap<Class<?>, ActorSet> classSets;
    
    /**
     * Construct an empty TreeActorSet.
     */
    public TreeActorSet()
    {
        subSets = new LinkedList<ActorSet>();
        generalSet = new ActorSet();
        subSets.add(generalSet);
        
        classSets = new HashMap<Class<?>, ActorSet>();
    }
    
    /**
     * Set the iteration order of objects. The first given class will have
     * objects of its class last in the iteration order, the next will have
     * objects second last in iteration order, and so on. This hold if it is
     * reversed. If it is not reversed it will return the first one first and so
     * on.
     * 
     * Objects not belonging to any of the specified classes will be first in
     * the iteration order if reversed, or last if not reversed
     * 
     * @param reverse
     *            Whether to reverse the order or not. 
     */
    public void setClassOrder(boolean reverse, Class<?> ... classes)
    {
        HashMap<Class<?>, ActorSet> oldClassSets = classSets;
        classSets = new HashMap<Class<?>, ActorSet>();
        
        // A list of classes we need to sweep the superclass set of
        LinkedList<Class<?>> sweepClasses = new LinkedList<Class<?>>();
        
        // For each listed class, use the ActorSet from the original classSets
        // if it exists, or create a new one if not
        for (int i = 0; i < classes.length; i++) {
            ActorSet oldSet = oldClassSets.remove(classes[i]);
            if (oldSet == null) {
                // There was no old set for this class. We'll need to check
                // the superclass set for actors which actually belong in
                // the new set.
                sweepClasses.add(classes[i]);
                oldSet = new ActorSet();
            }
            classSets.put(classes[i], oldSet);
        }
        
        // There may be objects in a set for some class A which
        // belong in the set for class B which is derived from A.
        // Now we'll "sweep" such sets.

        Set<Class<?>> sweptClasses = new HashSet<Class<?>>();
        
        while (! sweepClasses.isEmpty()) {
            Class<?> sweepClass = sweepClasses.removeFirst().getSuperclass();
            ActorSet sweepSet = classSets.get(sweepClass);
            while (sweepSet == null) {
                sweepClass = sweepClass.getSuperclass();
                if (sweepClass == null) {
                    sweepSet = generalSet;
                }
                else {
                    sweepSet = classSets.get(sweepClass);
                }
            }
            
            if (! sweptClasses.contains(sweepClass)) {
                sweptClasses.add(sweepClass);
                // go through sweep set
                Iterator<Actor> i = sweepSet.iterator();
                while (i.hasNext()) {
                    Actor actor = i.next();
                    ActorSet set = setForActor(actor);
                    if (set != sweepSet) {
                        set.add(actor); // add to the specific set
                        i.remove(); // remove from the general set
                    }
                }
            }
        }
        
        // Now, for any old subsets not yet handled, move all the actors into
        // the appropriate set. ("Not yet handled" means that the old subset
        // has no equivalent in the new sets).
        Iterator<Map.Entry<Class<?>,ActorSet>> ei = oldClassSets.entrySet().iterator();
        for ( ; ei.hasNext(); ) {
            Map.Entry<Class<?>,ActorSet> entry = ei.next();
            ActorSet destinationSet = setForClass(entry.getKey());
            destinationSet.addAll(entry.getValue());
        }
        
        // Finally, re-create the subsets list
        subSets.clear();
        if(reverse) {
            subSets.add(generalSet);
            for (int i = classes.length; i > 0; ) {
                subSets.add(classSets.get(classes[--i]));
            }
        }
        else {
            for (int i = 0; i < classes.length; i++) {
                subSets.add(classSets.get(classes[i]));
            }
            subSets.add(generalSet);
        }
    }
    
    public Iterator<Actor> iterator()
    {
        return new TasIterator();
    }
    
    public int size()
    {
        int size = 0;
        for (Iterator<ActorSet> i = subSets.iterator(); i.hasNext(); ) {
            size += i.next().size();
        }
        return size;
    }
    
    public boolean add(Actor o)
    {
        if (o == null) {
            throw new UnsupportedOperationException("Cannot add null actor.");
        }
        
        return setForActor(o).add(o);
    }
    
    public boolean remove(Actor o)
    {
        return setForActor(o).remove(o);
    }
    
    public boolean contains(Actor o)
    {
        return setForActor(o).contains(o);
    }
    
    /**
     * Get the actor set for a particular actor, depending on its class.
     */
    private ActorSet setForActor(Actor o)
    {
        Class<?> oClass = o.getClass();
        return setForClass(oClass);
    }
    
    private ActorSet setForClass(Class<?> oClass)
    {
        ActorSet set = classSets.get(oClass);
        
        // There might be a set for some superclass
        while (set == null && oClass != Object.class) {
            oClass = oClass.getSuperclass();
            set = classSets.get(oClass);
        }
        
        if (set == null) {
            set = generalSet;
        }
        return set;
    }
    
    /**
     * An iterator for a TreeActorSet
     * 
     * @author Davin McCall
     */
    class TasIterator implements Iterator<Actor>
    {
        private Iterator<ActorSet> setIterator;
        private ActorSet currentSet;
        private Iterator<Actor> actorIterator;
        
        public TasIterator()
        {
            setIterator = subSets.iterator();
            currentSet = setIterator.next();
            while (currentSet.isEmpty() && setIterator.hasNext()) {
                currentSet = setIterator.next();
            }
            actorIterator = currentSet.iterator();
        }
        
        public void remove()
        {
            actorIterator.remove();
        }
        
        public Actor next()
        {
            hasNext(); // update iterator if necessary
            return actorIterator.next();
        }
        
        public boolean hasNext()
        {
            if (actorIterator.hasNext()) {
                return true;
            }
            
            if (! setIterator.hasNext()) {
                return false;
            }
            
            while (setIterator.hasNext()) {
                currentSet = setIterator.next();
                if (! currentSet.isEmpty()) {
                    break;
                }
            }
            
            actorIterator = currentSet.iterator();
            return actorIterator.hasNext();
        }
    }
}
