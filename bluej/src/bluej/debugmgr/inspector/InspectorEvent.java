package bluej.debugmgr.inspector;

import java.util.*;

import bluej.debugger.DebuggerObject;

/**
 *  The event which occurs when an object is to be retrieved in an Inspector.
 *
 *@author     Duane Buck
 *@created    December 26, 2000
 *@version    $Id: InspectorEvent.java 2032 2003-06-12 05:04:28Z ajp $
 */
public class InspectorEvent extends EventObject
{

    protected DebuggerObject obj;
    protected int id;

    public final static int INSPECT = 1;
    public final static int GET = 2;

    public InspectorEvent(Object source, int id, DebuggerObject obj)
    {
        super(source);

        this.id = id;
        this.obj = obj;
    }

    public int getID()
    {
        return id;
    }

    public DebuggerObject getDebuggerObject()
    {
        return obj;
    }
}
