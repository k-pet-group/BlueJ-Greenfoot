package bluej.utility;

/**
 ** @version $Id: Comparer.java 36 1999-04-27 04:04:54Z mik $
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
	public int cmp(Object a, Object b);
}
