package antlr.collections;

import java.util.NoSuchElementException;

/** A simple stack definition; restrictive in that you cannot
 * access arbitrary stack elements.
 *
 * @author Terence Parr
 * <a href=http://www.MageLang.com>MageLang Institute</a>
 */
public interface Stack {


	public int height();
	public Object pop() throws NoSuchElementException;
	public void push(Object o);
	public Object top() throws NoSuchElementException;
}
