package bluej.debugmgr;


/** 
 * This class implements a singleton history object for library class
 * invocations.
 *
 * @author Michael Kolling
 * @version $Id: ClassHistory.java 2158 2003-08-06 10:39:13Z mik $
 *
 */
public class ClassHistory extends History
{
    // ======= static (factory) section =======

    private static ClassHistory classHistory = null;

    /**
     * Get the class history singleton. The first time this method
     * is called, the 'maxLength' parameter determines the history
     * size. The parameter has no effect on subsequent calls.
     */
    public static ClassHistory getClassHistory(int maxLength)
    {
        if(classHistory == null)
            classHistory = new ClassHistory(maxLength);
        return classHistory;
    }

    // ======= instance section =======

    /**
     * Initialise this history with some often-used classes for convinience.
     */
    private ClassHistory(int maxLength)
    {
        super(maxLength, false);
        put("java.lang.String");
        put("java.lang.Math");
        put("java.util.ArrayList");
        put("java.util.Random");
        put("java.util.");
        put("java.awt.");
        put("javax.swing.");
    }
}
