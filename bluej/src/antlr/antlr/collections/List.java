package antlr.collections;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**A simple List interface that describes operations
 * on a list; overly simplistic, but this definition is sufficient
 * for instructional purposes.
 *
 * @author Terence Parr
 * <a href=http://www.MageLang.com>MageLang Institute</a>
 */
public interface List {


	public void add(Object o); // can insert at head or append.
	public void append(Object o);
	public Object elementAt(int index) throws NoSuchElementException;
	public Enumeration elements();
	public boolean includes(Object o);
	public int length();
}
