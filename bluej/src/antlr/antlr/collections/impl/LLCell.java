package antlr.collections.impl;

/**A linked list cell, which contains a ref to the object and next cell.
 * The data,next members are public to this class, but not outside the
 * collections.impl package.
 *
 * @author Terence Parr
 * <a href=http://www.MageLang.com>MageLang Institute</a>
 */
class LLCell {
	Object data;
	LLCell next;


	public LLCell(Object o) { data=o; }
}
