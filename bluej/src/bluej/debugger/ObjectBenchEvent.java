package bluej.debugger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

/**
 * The event which occurs when  performing actions with the ObjectBench.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchEvent.java 323 2000-01-02 13:08:19Z ajp $
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
