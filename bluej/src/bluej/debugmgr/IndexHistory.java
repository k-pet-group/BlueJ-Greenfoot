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
        super(maxLength, false);
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
    
    public String getPrevious()
    {
        if(currentIndex >= history.size()) {
            return null;
        }
        else {
            currentIndex++;
            return (String) history.get(currentIndex-1);
        }
    }
}
