package bluej.pkgmgr;

import java.util.*;

/**
 * 
 *
 * @author  Andrew Patterson
 * @version $Id: TargetCollection.java 1819 2003-04-10 13:47:50Z fisker $
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

