package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import bluej.views.*;

/**
 * The event which occurs while editing a package
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditorEvent.java 505 2000-05-24 05:44:24Z ajp $
 */
public class PackageEditorEvent extends EventObject
{
    public final static int TARGET_CALLABLE = 1;
    public final static int TARGET_REMOVE = 2;
    public final static int TARGET_OPEN = 3;

    public final static int DIALOG_MESSAGE = 1;
    public final static int DIALOG_MESSAGETEXT = 2;
    public final static int DIALOG_ERROR = 2;

    protected int id;
    protected CallableView cv;
    protected String name;

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

        this.id = id;
        this.cv = cv;
    }

    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public CallableView getCallable()
    {
        return cv;
    }
}
