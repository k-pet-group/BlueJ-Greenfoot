package bluej.debugger;

import java.util.Vector;

/** 
 * This class implements a singleton history object for library class
 * invocations.
 *
 * @author Michael Kolling
 *
 */
public class FreeCallHistory extends History
{
    // ======= static (factory) section =======

    private static FreeCallHistory callHistory = null;

    /**
     * Get the call history singleton. The first time this method
     * is called, the 'maxLength' parameter determines the history
     * size. The parameter has no effect on subsequent calls.
     */
    public static FreeCallHistory getCallHistory(int maxLength)
    {
        if(callHistory == null)
            callHistory = new FreeCallHistory(maxLength);
        return callHistory;
    }

    // ======= instance section =======

    private FreeCallHistory(int maxLength)
    {
        super(maxLength, true);
    }
}
