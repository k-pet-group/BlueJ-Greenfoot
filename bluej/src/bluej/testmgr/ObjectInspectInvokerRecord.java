package bluej.testmgr;

import java.util.List;
import java.util.*;

import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.views.*;
import bluej.debugger.*;
import bluej.runtime.*;

/**
 * Records a single user interaction with the object construction
 * mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 1626 2003-02-11 01:46:35Z ajp $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    
    public ObjectInspectInvokerRecord(String type, String name)
    {
        this.type = type;
        this.name = name;
    }

    public ObjectInspectInvokerRecord(String type, String name, InvokerRecord ir)
    {
        this.type = type;
        this.name = name;
    }

    public boolean hasReturnValue()
    {
        return true;
    }
        
    public Class getReturnType()
    {
        return String.class;    
    }
    
    public boolean isConstructor()
    {
        return false;        
    }

    public String toString()
    {
        return "\t\t" + type + " " + name + " = " + ";\n";
    }
}
