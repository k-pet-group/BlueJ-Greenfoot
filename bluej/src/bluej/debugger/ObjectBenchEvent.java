package bluej.debugger;

import java.util.*;

/**
 * The event which occurs when  performing actions with the ObjectBench.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchEvent.java 1818 2003-04-10 13:31:55Z fisker $
 */
public class ObjectBenchEvent extends EventObject
{
    public final static int OBJECT_SELECTED = 1;

    protected ObjectWrapper wrapper;
    protected int id;

    public ObjectBenchEvent(Object source, int id, ObjectWrapper wrapper)
    {
        super(source);

        this.id = id;
        this.wrapper = wrapper;
    }

    public int getID()
    {
        return id;
    }

    public ObjectWrapper getWrapper()
    {
        return wrapper;
    }
}
