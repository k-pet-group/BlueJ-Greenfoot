package bluej.utility;

import java.util.Vector;

/**
 ** @version $Id: SortableVector.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** A vector of objects that can be sorted efficiently
 **/
public class SortableVector extends Vector
{
	public void sort(Comparer c)
	{
		Utility.quicksort(c, elementData, 0, elementCount - 1);
	}
}
