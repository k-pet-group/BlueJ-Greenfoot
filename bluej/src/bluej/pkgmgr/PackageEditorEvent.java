package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import bluej.views.*;
import bluej.debugger.DebuggerObject;

/**
 * The event which occurs while editing a package
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditorEvent.java 583 2000-06-26 01:51:17Z mik $
 */
public class PackageEditorEvent extends EventObject
{
    public final static int TARGET_CALLABLE = 1;
    public final static int TARGET_REMOVE = 2;
    public final static int TARGET_OPEN = 3;

    public final static int OBJECT_PUTONBENCH = 4;

    protected int id;
    protected CallableView cv;
    protected DebuggerObject obj;
    protected String name;
    protected String instname;

    public PackageEditorEvent(Object source, int id)
    {
        super(source);
        this.id = id;
    }

    public PackageEditorEvent(Object source, int id, String packageName)
    {
        super(source);

        this.id = id;
        this.name = packageName;
    }

    public PackageEditorEvent(Object source, int id, CallableView cv)
    {
        super(source);

        if (id != TARGET_CALLABLE)
            throw new IllegalArgumentException();

        this.id = id;
        this.cv = cv;
    }

    public PackageEditorEvent(Object source, int id, DebuggerObject obj,
                                String instanceName, String fieldName)
    {
        super(source);

        if (id != OBJECT_PUTONBENCH)
            throw new IllegalArgumentException();

        this.id = id;
        this.obj = obj;
        this.name = fieldName;
        this.instname = instanceName;
    }

    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getFieldName()
    {
        return name;
    }

    public String getInstanceName()
    {
        return instname;
    }

    public CallableView getCallable()
    {
        return cv;
    }

    public DebuggerObject getDebuggerObject()
    {
        return obj;
    }
}
