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
    private Vector history = null;
    private int maxLength;

    public ClassHistory(int maxLength)
    {
        this.maxLength = maxLength;
        history = new Vector(maxLength);
        history.add("java.lang.String");
        history.add("java.lang.Math");
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
     * Adds a call to the history of a particular datatype
     * 
     * @param objectType  the object's class
     * @param argument  the parameter 
     * @return the List containing the appropriate history of invocations
     */
    public void addClass(String className)
    {
        if(className != null && (className.length() != 0)) {
            history.add(0, className);
        }
    }
}
