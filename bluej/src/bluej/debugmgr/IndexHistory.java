package bluej.debugmgr;

/**
 * A history of Strings that maintains an internal cursor to provide
 * 'getNext'/'getPrevious'-style methods to traverse the history list. 
 * 
 * @author mik
 */

public class IndexHistory extends History {

    private int currentIndex;
    
    /**
     * 
     * @param maxLength Number of entries in history list
     */
    public IndexHistory(int maxLength)
    {
        super(maxLength, true);
        currentIndex = 0;
    }
    
    /**
     * Add a string to the history of used strings.
     * 
     * @param newString  the new string to add
     */
    public void add(String newString)
    {
        super.add(newString);
        currentIndex = 0;
    }
    
    /**
     * Get the previous history entry. Calling this repeatedly walks
     * back through the history
     * 
     * @return The previous history entry.
     */
    public String getPrevious()
    {
        if(currentIndex+1 < history.size()) {
            currentIndex++;
            return (String) history.get(currentIndex);
        }
        else {
            return null;
        }
    }

    /**
     * Get the next history entry. Calling this repeatedly walks
     * forward through the history
     * 
     * @return The next history entry.
     */
    public String getNext()
    {
        if(currentIndex > 0) {
            currentIndex--;
            return (String) history.get(currentIndex);
        }
        else {
            return null;
        }
    }

}
