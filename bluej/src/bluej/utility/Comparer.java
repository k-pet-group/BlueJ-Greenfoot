package bluej.utility;

/**
 * A interface for compares two objects
 *
 * @author  Michael Cahill
 * @version $Id: Comparer.java 318 2000-01-02 13:02:13Z ajp $
 * @see     Utility#sort()
 */
public interface Comparer
{
	/**
	 * @return -1 if a < b
	 * @return 0 if a == b
	 * @return 1 if a > b
	 */
	int cmp(Object a, Object b);
}
