package antlr.collections;

public interface Enumerator {


	/**Return the element under the cursor; return null if !valid() or
	 * if called before first next() call.
	 */
	public Object cursor();
	/**Return the next element in the enumeration; first call to next()
	 * returns the first element.
	 */
	public Object next();
	/**Any more elements in the enumeration? */
	public boolean valid();
}
