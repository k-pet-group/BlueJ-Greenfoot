package bluej.debugmgr.objectbench;

import java.util.*;

import bluej.debugmgr.NamedValue;

/**
 * The event which occurs when  performing actions with the ObjectBench.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchEvent.java 4277 2006-05-15 23:43:11Z polle $
 */
public class ObjectBenchEvent extends EventObject
{
    public final static int OBJECT_SELECTED = 1;

    protected NamedValue value;
    protected int id;

    public ObjectBenchEvent(Object source, int id, NamedValue value)
    {
        super(source);

        this.id = id;
        this.value = value;
    }

    public int getID()
    {
        return id;
    }

    public NamedValue getValue()
    {
        return value;
    }
}
