package greenfoot;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A set which allows specifying iteration order according to class of contained
 * objects.
 * 
 * @author Davin McCall
 */
public class TreeActorSet extends AbstractSet<Actor>
{
    private List<ActorSet> subSets;
    
    /** ActorSet for objects of a class without a specific z-order */
    private ActorSet generalSet;
    
    private HashMap<Class, ActorSet> classSets;
    
    /**
     * Construct an empty TreeActorSet.
     */
    public TreeActorSet()
    {
        subSets = new LinkedList<ActorSet>();
        generalSet = new ActorSet();
        subSets.add(generalSet);
        
        classSets = new HashMap<Class, ActorSet>();
    }
    
    /**
     * Set the iteration order of objects. The first given class will have
     * objects of its class first in the iteration order, and so on.
     * Objects not belonging to any of the specified classes will be last
     * in the iteration order.
     */
    public void setClassOrder(Class ... classes)
    {
        HashMap<Class, ActorSet> oldClassSets = classSets;
        classSets = new HashMap<Class, ActorSet>();
        
        // For the moment assume that we don't need to go through the
        // general set - i.e. assume all classes were already given
        // a z-order
        boolean needGeneralSweep = false;
        
        // For each listed class, use the ActorSet from the original classSets
        // if it exists, or create a new one if not
        for (int i = 0; i < classes.length; i++) {
            ActorSet oldSet = oldClassSets.remove(classes[i]);
            if (oldSet == null) {
                // There was no old set for this class
                oldSet = new ActorSet();
                needGeneralSweep = true;
            }
            classSets.put(classes[i], oldSet);
        }
        
        if (needGeneralSweep) {
            // We need to go through the general set and possibly re-assign
            // each actor to a set
            Iterator<Actor> i = generalSet.iterator();
            while (i.hasNext()) {
                Actor actor = i.next();
                Class oClass = actor.getClass();
                ActorSet set = classSets.get(oClass);
                if (set != null) {
                    set.add(actor); // add to the specific set
                    i.remove(); // remove from the general set
                }
            }
        }
        
        // Now, for any sets left in the old set, move all the actors into the
        // general set
        for (Iterator<ActorSet> i = oldClassSets.values().iterator(); i.hasNext(); ) {
            ActorSet entry = i.next();
            generalSet.addAll(entry);
        }
        
        // Finally, re-create the subsets list
        subSets.clear();
        subSets.add(generalSet);
        for (int i = classes.length; i > 0; ) {
            subSets.add(classSets.get(classes[--i]));
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
        Class oClass = o.getClass();
        ActorSet set = classSets.get(oClass);
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
