package bluej.debugger;

import java.util.List;
import java.util.ArrayList;

/** 
 * History objects maintain a history of text strings. This serves as a
 * superclass for various histories (see, for example, ClassHistory,
 * FreeCallHistory).
 *
 * @author Michael Kolling
 *
 */
public class History
{
    private List history = null;
    private int maxLength;
    private boolean blankAtStart;
    
    /**
     * Create a empty history limited to a given maximum
     * number of entries.
     *
     * @param maxLength The maximum length of the hostory list. The
     *                  list truncates its tail when growing longer.
     * @param blankDefault If true, maintains an empty string as the
     *                  first (most recent) entry.
     */
    protected History(int maxLength, boolean blankDefault)
    {
        this.maxLength = maxLength;
        history = new ArrayList(maxLength+1);
        history.add("");
        blankAtStart = blankDefault;
    }

    /**
     * Put an entry into the history list. This method is only for 
     * initialisation through subclasses. It does not check for 
     * duplicates or maxlength.
     */
    protected void put(String value)
    {
        history.add(value);
    }

    /**
     * Return the history of used classes.
     */	
    public List getHistory()
    {
        return history;
    }

    /**
     * Add a string to the history of used strings.
     * 
     * @param newString  the new string to add
     */
    public void add(String newString)
    {
        if(newString != null && (newString.length() != 0)) {

            // remove at old position (if present) and add at front
            history.remove(newString);

            if(blankAtStart) {
                history.add(1, newString);
            }
            else {
                // remove empty entry at front
                if(((String)history.get(0)).length() == 0)
                    history.remove(0);
                history.add(0, newString);
            }

            // don't let it grow too big
            if(history.size() > maxLength)
                history.remove(maxLength);
        }
    }
}
