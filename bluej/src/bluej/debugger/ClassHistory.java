package bluej.debugger;

import java.util.Vector;

/** 
 * Manages an invocation history of arguments used in a package when objects 
 * created on the ObjectBench
 *
 * @author Michael Kolling
 *
 */
public class ClassHistory
{
    // ======= static (factory) section =======

    private static ClassHistory classHistory = null;

    public static ClassHistory getClassHistory(int maxLength)
    {
        if(classHistory == null)
            classHistory = new ClassHistory(maxLength);
        return classHistory;
    }

    // ======= instance section =======

    private Vector history = null;
    private int maxLength;

    private ClassHistory(int maxLength)
    {
        this.maxLength = maxLength;
        history = new Vector(maxLength);
        history.add("");
        history.add("String");
        history.add("Math");
        history.add("java.util.ArrayList");
        history.add("java.util.Random");
        history.add("java.util.");
        history.add("java.awt.");
        history.add("javax.swing.");
    }

    /**
     * Return the history of used classes.
     */	
    public Vector getHistory()
    {
        return history;
    }


    /**
     * Add a class to the history of used class names.
     * 
     * @param className  the fully qualified name of the class to add
     */
    public void addClass(String className)
    {
        if(className != null && (className.length() != 0)) {
            // remove empty entry at front
            if(((String)history.get(0)).length() == 0)
                history.remove(0);

            // remove at old position (if present) and add at front
            history.remove(className);
            history.add(0, className);

            // don't let it grow too big
            if(history.size() > 10)
                history.remove(10);
        }
    }
}
