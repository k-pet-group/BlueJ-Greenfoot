package bluej.testmgr;

import java.util.List;
import java.util.*;

import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.views.*;
import bluej.debugger.*;
import bluej.runtime.*;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ (assuming it returns a
 * value).
 *
 * @author  Andrew Patterson
 * @version $Id: MethodInvokerRecord.java 1626 2003-02-11 01:46:35Z ajp $
 */
public class MethodInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    private String command;

    public MethodInvokerRecord(String type, String command)
    {
        this.type = type;
        this.command = command;
    }

    public void setResultName(String name)
    {
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
    
    public boolean isLocal()
    {
        return false;    
    }

    public String toString()
    {
        return "\t\t" + command + ";\n";
    }

}
