package bluej.debugmgr.objectbench;

import java.util.*;

/**
 * The event which occurs when  performing actions with the ObjectBench.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchEvent.java 2032 2003-06-12 05:04:28Z ajp $
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
