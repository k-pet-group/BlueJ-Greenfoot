package bluej.utility;

import java.util.Iterator;
import java.util.List;

/**
 ** @version $Id: MultiIterator.java 1417 2002-10-18 07:56:39Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** A multiplexing Iterator.
 **/
public class MultiIterator implements Iterator
{
    List iterations;
    int current;
	
    public MultiIterator(List iterations)
    {
        this.iterations = iterations;
        current = 0;
    }
	
    public boolean hasNext()
    {
        for( ; current < iterations.size(); current++)
            if(((Iterator)iterations.get(current)).hasNext())
                return true;
	
        return false;
    }

    public Object next()
    {
        for( ; current < iterations.size(); current++) {
            Iterator it = (Iterator)iterations.get(current);
            if(it.hasNext())
                return it.next();
        }
		
        return null;
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
