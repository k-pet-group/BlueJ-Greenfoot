package bluej.utility;

/**
 ** @version $Id: Comparer.java 49 1999-04-28 03:01:02Z ajp $
 ** @author Michael Cahill
 ** A interface for compares two objects
 ** @see Utility#sort()
 **/
public interface Comparer
{
	/**
	 ** @return -1 if a < b
	 ** @return 0 if a == b
	 ** @return 1 if a > b
	 **/
	int cmp(Object a, Object b);
}
