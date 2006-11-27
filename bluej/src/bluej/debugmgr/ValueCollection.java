package bluej.debugmgr;

import java.util.Iterator;


/**
 * A collection of named values (NameValue interface), which may be references
 * (objects) or primitive values.
 * 
 * @author Davin McCall
 * @version $Id: ValueCollection.java 4708 2006-11-27 00:47:57Z bquig $
 */
public interface ValueCollection
{   
    /**
     * Get an iterator through the values in this collection.
     */
    public Iterator getValueIterator();
    
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
