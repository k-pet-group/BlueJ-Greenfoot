package bluej.utility;

/**
 * A interface for compares two objects
 *
 * @author  Michael Cahill
 * @version $Id: Comparer.java 1459 2002-10-23 12:13:12Z jckm $
 * @see     Utility#quicksort(Comparer, Object[], int, int)
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
