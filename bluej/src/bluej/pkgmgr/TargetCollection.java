package bluej.pkgmgr;

import java.util.*;

import bluej.pkgmgr.target.*;

/**
 * 
 *
 * @author  Andrew Patterson
 * @version $Id: TargetCollection.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class TargetCollection
{
    /** all the targets in a package */
    protected HashMap targets = new HashMap();

    public Iterator iterator()
    {
        return targets.values().iterator();
    }

    public Iterator sortediterator()
    {
        return new TreeSet(targets.values()).iterator();
    }

    public Target get(String identifierName)
    {
	return (Target) targets.get(identifierName);
    }

    public Target remove(String identifierName)
    {
        return (Target) targets.remove(identifierName);
    }

    public void add(String identifierName, Target target)
    {
        targets.put(identifierName, target);
    }
    
    public String toString()
    {
    	return targets.toString();
    }
}

