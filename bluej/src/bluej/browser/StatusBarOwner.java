package bluej.browser;

/**
 * A simple interface to be implemented by any classes containing a
 * status bar which accessible by other classes.
 * 
 * @author $Author: mik $
 * @version $Id: StatusBarOwner.java 36 1999-04-27 04:04:54Z mik $
 */
public interface StatusBarOwner {
	/**
	 * Change the text on the status bar.
	 * 
	 * @param status the text to be displayed on the status bar.
	 */
	public void setStatus(String status);
}
