package bluej.utility;

import java.util.Enumeration;
import java.util.Vector;

/**
 ** @version $Id: MultiEnumeration.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** A multiplexing Enumeration.
 **/
public class MultiEnumeration implements Enumeration
{
    Vector enumerations;
    int current;
	
    public MultiEnumeration(Vector enumerations)
    {
	this.enumerations = enumerations;
	current = 0;
    }
	
    public boolean hasMoreElements()
    {
	for( ; current < enumerations.size(); current++)
	    if(((Enumeration)enumerations.elementAt(current)).hasMoreElements())
		return true;
				
	return false;
    }
	
    public Object nextElement()
    {
	for( ; current < enumerations.size(); current++) {
	    Enumeration e = (Enumeration)enumerations.elementAt(current);
	    if(e.hasMoreElements())
		return e.nextElement();
	}
				
	return null;
    }
}
