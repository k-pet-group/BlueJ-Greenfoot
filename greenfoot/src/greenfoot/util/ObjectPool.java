package greenfoot.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Abstract class that can be used to pool objects. 
 */
public abstract class ObjectPool<T>
{
    private ArrayList<T> pool;
    
    public ObjectPool() 
    {
        pool = new ArrayList<T>();
    }
    
    /**
     * Add all objects in the collection to the pool. From here on the objects 
     * in the collections should no longer be referenced. 
     */
    public void add(Collection<T> c) 
    {
        pool.addAll(c);
    }
    
    /**
     * Add one object to the pool. From here on the object should no longer be 
     * referenced. 
     */
    public void add(T o) 
    {
        pool.add(o);
    }
    
    /**
     * Gets an object. From the pool if available.
     */
    public T get() 
    {
        
        if(pool.isEmpty()) {
            return createNew();
        }
        else {
            return pool.remove(pool.size()-1);
        }
    }
    
    /**
     * Implement this method so that it constructs a new object of the given
     * type.
     */
    protected abstract T createNew();
    
    /**
     * Empties this pool.
     */ 
    public void reset() {
        pool = new ArrayList<T>();
    }
}
