package bluej.debugmgr;

import java.util.Iterator;


/**
 * A collection of named values (NameValue interface), which may be references
 * (objects) or primitive values.
 * 
 * @author Davin McCall
 * @version $Id: ValueCollection.java 3478 2005-07-26 02:46:05Z davmac $
 */
public interface ValueCollection
{   
    /**
     * Get an iterator through the values.
     */
    public Iterator getValueIterator();
    
    public NamedValue getNamedValue(String name);
}
